// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Campfire (GLOSSARY.md), and the soul Campfire: vanilla's campfire, which cooks as vanilla's does
 * in vanilla's block entity, with its {@link Fuel} attached. Placed, it holds half its cap. It burns by
 * the time of day while lit, twice as fast in rain under the open sky, and its light falls a step at each
 * quarter of its cap its fuel falls below, until it goes out dry. Any furnace fuel used on it adds ten
 * times its burn time, up to the cap, through {@link PlacedFuel}, and lights it again. A comparator
 * reads how full it is.
 */
public final class FuelledCampfireBlock extends CampfireBlock implements FuelledLight {

    public static final MapCodec<CampfireBlock> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.BOOL.fieldOf("soul").forGetter(block -> ((FuelledCampfireBlock) block).soul),
            propertiesCodec()
    ).apply(instance, (soul, properties) -> soul ? soul(properties) : campfire(properties)));

    /** Its light while lit, from its fuel; the block gives none while out. */
    public static final IntegerProperty LIGHT = BlockStateProperties.LEVEL;

    private static final int[] LIGHTS = {15, 12, 9, 6};
    private static final int[] SOUL_LIGHTS = {11, 9, 7, 5};

    private final boolean soul;

    private FuelledCampfireBlock(boolean soul, Properties properties) {
        // Vanilla's sparks and fire damage: the soul one has no sparks and burns harder.
        super(!soul, soul ? 2 : 1, properties);
        this.soul = soul;
        registerDefaultState(defaultBlockState().setValue(LIGHT, lights()[0]));
    }

    static FuelledCampfireBlock campfire(Properties properties) {
        return new FuelledCampfireBlock(false, properties);
    }

    static FuelledCampfireBlock soul(Properties properties) {
        return new FuelledCampfireBlock(true, properties);
    }

    @Override
    public MapCodec<CampfireBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        super.createBlockStateDefinition(builder);
        builder.add(LIGHT);
    }

    /** The light it gives in this state: its fuel's while lit, none while out. */
    public static int light(BlockState state) {
        return state.getValue(LIT) ? state.getValue(LIGHT) : 0;
    }

    /** The fuel it holds at most, from the config: the soul Campfire's is twice the Campfire's. */
    @Override
    public int cap() {
        return LightConfig.CAMPFIRE_CAP.getAsInt() * (soul ? 2 : 1);
    }

    /** The fuel it is placed with. */
    int placedFuel() {
        return cap() / 2;
    }

    private int[] lights() {
        return soul ? SOUL_LIGHTS : LIGHTS;
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        var state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(LIGHT, Burn.step(placedFuel(), cap(), lights()));
    }

    /**
     * Fuel, taken one at a time unless it is full. Anything else is vanilla's, such as food to cook, and
     * so are tools, which a furnace burns too: a wooden shovel still puts it out.
     */
    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit) {
        if (HandFuel.burnTime(stack, level) <= 0 || !(level.getBlockEntity(pos) instanceof CampfireBlockEntity)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }
        // The client doesn't know the fuel.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!PlacedFuel.take((ServerLevel) level, pos, stack)) {
            return InteractionResult.CONSUME;
        }
        HandFuel.feed(stack, level, pos, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    /** How full it is, 0 only once dry. */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire ? Burn.signal(campfire.getData(Light.FUEL).left(), cap()) : 0;
    }

    /** Vanilla's, which cooks while lit, then the burn. */
    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        var vanilla = super.getTicker(level, state, type);
        if (!(level instanceof ServerLevel) || type != BlockEntityType.CAMPFIRE) {
            return vanilla;
        }
        return (tickLevel, pos, tickState, entity) -> {
            if (vanilla != null) {
                vanilla.tick(tickLevel, pos, tickState, entity);
            }
            burn((ServerLevel) tickLevel, pos, tickState, (CampfireBlockEntity) entity);
        };
    }

    @Override
    public @Nullable Fuel upToDate(ServerLevel level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire ? upToDate(level, pos, state, campfire) : null;
    }

    /** Lit again, unless it is underwater, and its light shown. */
    @Override
    public void fuelled(ServerLevel level, BlockPos pos, BlockState state, Fuel fuel) {
        if (level.getBlockEntity(pos) instanceof CampfireBlockEntity campfire) {
            campfire.setChanged();
        }
        level.setBlockAndUpdate(pos, shown(state.setValue(LIT, !state.getValue(WATERLOGGED)), fuel));
    }

    /** Brings its fuel up to date with the time of day, and shows it. */
    private void burn(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        var shown = shown(state, upToDate(level, pos, state, campfire));
        if (shown != state) {
            if (state.getValue(LIT) && !shown.getValue(LIT)) {
                level.playSound(null, pos, SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1F, 1F);
            }
            level.setBlockAndUpdate(pos, shown);
        }
    }

    /**
     * Its fuel, burnt by the time of day since it was last brought up to date while lit, twice as fast in
     * rain under the open sky. Out, its clock still moves, so it burns nothing for the time it was out once
     * lit again.
     */
    private static Fuel upToDate(ServerLevel level, BlockPos pos, BlockState state, CampfireBlockEntity campfire) {
        var fuel = campfire.getData(Light.FUEL);
        int rate = !state.getValue(LIT) ? Burn.PAUSED : level.isRainingAt(pos.above()) ? Burn.IN_RAIN : Burn.STILL;
        if (fuel.burn(level.getOverworldClockTime(), rate)) {
            campfire.setChanged();
        }
        return fuel;
    }

    /** The state that shows this fuel: its light, and out once dry. */
    private BlockState shown(BlockState state, Fuel fuel) {
        int light = Burn.step(fuel.left(), cap(), lights());
        return state.setValue(LIGHT, light).setValue(LIT, state.getValue(LIT) && light > 0);
    }
}
