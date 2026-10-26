// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
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
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Lamp, placed: vanilla's lantern, standing or hanging, lit while it is on with fuel, its
 * {@link Fuel} in a {@link LampBlockEntity}. Lit, it burns by the time of day, loaded or not, rain or
 * not, and goes dark once dry. An empty hand turns it off, or on if it has fuel; any furnace fuel used on
 * it adds ten times its burn time, up to its cap, and lights a dry one.
 */
public final class LampBlock extends LanternBlock implements EntityBlock {

    public static final MapCodec<LampBlock> CODEC = simpleCodec(LampBlock::new);

    /** On, with fuel. */
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public LampBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(LIT, false));
    }

    @Override
    public MapCodec<LampBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIT);
    }

    /** Its cap, the item's. */
    private int cap() {
        return ((LampItem) asItem()).cap();
    }

    /** Lit if the Lamp placed was. */
    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIT, LampItem.lit(context.getItemInHand()));
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
        return (tickLevel, pos, tickState, entity) -> {
            var fuel = upToDate((ServerLevel) tickLevel, tickState, (LampBlockEntity) entity);
            if (tickState.getValue(LIT) && fuel.left() == 0) {
                tickLevel.setBlockAndUpdate(pos, tickState.setValue(LIT, false));
            }
        };
    }

    /** Its fuel, burnt by the time of day while lit since it was last brought up to date. */
    private static Fuel upToDate(ServerLevel level, BlockState state, LampBlockEntity lamp) {
        var fuel = lamp.fuel();
        if (fuel.burn(level.getOverworldClockTime(), state.getValue(LIT) ? Burn.STILL : Burn.PAUSED)) {
            lamp.setChanged();
        }
        return fuel;
    }

    /** Fuel, taken one at a time unless it is full, and never a tool. With anything else in hand, the item's own use. */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) {
            return InteractionResult.TRY_WITH_EMPTY_HAND;
        }
        int burnTime = HandFuel.burnTime(stack, level);
        if (burnTime <= 0 || !(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
            return InteractionResult.PASS;
        }
        // The client doesn't know the fuel.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        var fuel = upToDate((ServerLevel) level, state, lamp);
        int left = fuel.left();
        if (left >= cap()) {
            return InteractionResult.CONSUME;
        }
        fuel.refuel(burnTime, cap());
        lamp.setChanged();
        if (left == 0 && !state.getValue(LIT)) {
            level.setBlockAndUpdate(pos, state.setValue(LIT, true));
        }
        HandFuel.feed(stack, level, pos, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** Off, or on if it has fuel. */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof LampBlockEntity lamp)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        boolean on = !state.getValue(LIT);
        // Burnt up to now, so turning it off charges what it owed and turning it on none of the time it was off.
        if (upToDate((ServerLevel) level, state, lamp).left() == 0 && on) {
            return InteractionResult.PASS;
        }
        level.setBlockAndUpdate(pos, state.setValue(LIT, on));
        level.playSound(null, pos, on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 1F);
        return InteractionResult.SUCCESS_SERVER;
    }
}
