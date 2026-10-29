// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Lamp, placed: vanilla's lantern, standing or hanging, lit while it is on with fuel, its
 * {@link Fuel} in a {@link LampBlockEntity}. Lit, it burns by the time of day, loaded or not, rain or
 * not, and goes dark once dry, on still. An empty hand turns it off, or on if it has fuel, and so does
 * redstone, as a door's: a rise in the signal turns it on, a fall off, and a hand toggle in between holds
 * until the next change; placed into a live signal, it is placed on. Any furnace fuel used on it adds ten
 * times its burn time, up to its cap, through {@link PlacedFuel}, which never turns it on or off: one on
 * and dry shines again, one off stays off. A comparator reads how full it is.
 */
public final class LampBlock extends LanternBlock implements EntityBlock, FuelledLight {

    public static final MapCodec<LampBlock> CODEC = simpleCodec(LampBlock::new);

    /** Switched on, by hand or by redstone, with fuel or dry. */
    public static final BooleanProperty ON = BooleanProperty.create("on");

    /** On, with fuel: the one it shines in. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    /** The signal it last saw, so only a change in it switches the Lamp. */
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;

    public LampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(ON, false).setValue(LIT, false).setValue(POWERED, false));
    }

    @Override
    public MapCodec<LampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(ON, LIT, POWERED);
    }

    /** Its cap, the item's. */
    @Override
    public int cap() {
        return ((LampItem) asItem()).cap();
    }

    /** On if the Lamp placed was, or if it is placed into a live signal, as a door placed so opens; lit if it is on with fuel. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        if (state == null) {
            return null;
        }
        var stack = context.getItemInHand();
        boolean powered = context.getLevel().hasNeighborSignal(context.getClickedPos());
        boolean on = LampItem.lit(stack) || powered;
        return state.setValue(ON, on).setValue(LIT, on && LampItem.fuel(stack) > 0).setValue(POWERED, powered);
    }

    /** A change in the signal turns it on, lit if it has fuel, or off. */
    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, @Nullable Orientation orientation, boolean movedByPiston) {
        super.neighborChanged(state, level, pos, block, orientation, movedByPiston);
        boolean signal = level.hasNeighborSignal(pos);
        if (!(level instanceof ServerLevel server) || signal == state.getValue(POWERED)) {
            return;
        }
        // Burnt up to now, as by a hand, so turning it off charges what it owed.
        var fuel = PlacedFuel.upToDate(server, pos, state);
        if (fuel == null) {
            return;
        }
        boolean lit = signal && fuel.left() > 0;
        if (lit != state.getValue(LIT)) {
            playSwitchSound(level, pos, lit);
        }
        level.setBlockAndUpdate(pos, state.setValue(POWERED, signal).setValue(ON, signal).setValue(LIT, lit));
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LampBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (!(level instanceof ServerLevel) || type != Light.LAMP_ENTITY.get()) {
            return null;
        }
        return PlacedFuel::tick;
    }

    /** None: it takes the item's. */
    @Override
    public int placedFuel() {
        return 0;
    }

    /** By the time of day while lit, rain or not. */
    @Override
    public int rate(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getValue(LIT) ? Burn.STILL : Burn.PAUSED;
    }

    /** Lit while it is on with fuel, and dark once dry, on still. */
    @Override
    public BlockState shown(ServerLevel level, BlockPos pos, BlockState state, Fuel fuel) {
        return state.setValue(LIT, state.getValue(LIT) && state.getValue(ON) && fuel.left() > 0);
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full it is, 0 only once dry. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return PlacedFuel.signal(level, pos);
    }

    /** Fuel, taken one at a time unless it is full, and never a tool. With anything else in hand, the item's own use. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        var fed = PlacedFuel.feedByHand(stack, level, pos, player, hand);
        return fed == null ? InteractionResult.PASS : fed;
    }

    /** Off, or on if it has fuel. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LampBlockEntity)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean on = !state.getValue(ON);
        // Burnt up to now, so turning it off charges what it owed and turning it on none of the time it was off.
        var fuel = PlacedFuel.upToDate((ServerLevel) level, pos, state);
        if (fuel == null || fuel.left() == 0 && on) {
            return InteractionResult.PASS;
        }
        level.setBlockAndUpdate(pos, state.setValue(ON, on).setValue(LIT, on));
        playSwitchSound(level, pos, on);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A flint and steel's strike turned on, a fire put out turned off. */
    private static void playSwitchSound(Level level, BlockPos pos, boolean on) {
        level.playSound(null, pos, on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1F);
    }
}
