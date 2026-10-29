// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jspecify.annotations.Nullable;

/**
 * A placed Light Source's {@link Fuel}, a Lamp's or a Campfire's, the {@link FuelledLight} it is in
 * saying only what differs between them. Each tick brings it up to date, burning it by the time of day
 * at the light's rate, then lets the light show it.
 *
 * <p>Fuel goes in the one way, whoever brings it: a player's hand or a Lampad. The light says whether
 * the whole of an item fits in the room it has left, and takes one, ten times its burn time up to its
 * cap. A player's hand feeds one that isn't full and loses the overflow; a Lampad feeds only an item that
 * fits whole. Fuel never turns a Lamp on or off.
 *
 * <p>Reads, the fuel left and a comparator's signal, are as of the last tick and burn nothing; anything
 * that changes the fuel brings it up to date first.
 */
public final class PlacedFuel {

    private PlacedFuel() {
    }

    /** Whether a placed light takes it as fuel: any furnace fuel but a tool. */
    public static boolean isFuel(ItemStack stack, Level level) {
        return HandFuel.burnTime(stack, level) > 0;
    }

    /** The ticks of fuel the placed light there has left, as of its last tick; 0 if there is none. */
    public static int left(BlockGetter level, BlockPos pos) {
        var fuel = stored(level.getBlockEntity(pos));
        return fuel == null ? 0 : fuel.left();
    }

    /** Gives the placed light there this much fuel, burning from its next tick. */
    public static void set(Level level, BlockPos pos, int left) {
        var entity = level.getBlockEntity(pos);
        if (stored(entity) == null) {
            throw new IllegalArgumentException("no placed light at " + pos);
        }
        entity.setData(Light.FUEL, Fuel.unclocked(left));
    }

    /** A comparator's reading of how full the placed light there is, 0 only once dry. */
    static int signal(Level level, BlockPos pos) {
        var entity = level.getBlockEntity(pos);
        var fuel = stored(entity);
        return fuel == null ? 0 : Burn.signal(fuel.left(), ((FuelledLight) entity.getBlockState().getBlock()).cap());
    }

    /** Whether the block there is a placed light with room for the whole of one of these. */
    public static boolean fits(ServerLevel level, BlockPos pos, ItemStack stack) {
        var feeding = Feeding.of(level, pos, stack);
        return feeding != null && Burn.fits(feeding.fuel.left(), feeding.burnTime, feeding.light.cap());
    }

    /**
     * Puts one of these into the placed light there, unless it is full, the overflow lost; false if it
     * took none. The stack is left as it was, for the one who fed it to use up. Fed, it is lit again if its
     * block lets it.
     */
    public static boolean take(ServerLevel level, BlockPos pos, ItemStack stack) {
        var feeding = Feeding.of(level, pos, stack);
        if (feeding == null || feeding.fuel.left() >= feeding.light.cap()) {
            return false;
        }
        feeding.fuel.refuel(feeding.burnTime, feeding.light.cap());
        feeding.entity.setChanged();
        show(level, pos, feeding.state, feeding.light.shown(level, pos, feeding.state.setValue(BlockStateProperties.LIT, true), feeding.fuel));
        return true;
    }

    /**
     * The fuel in the player's hand put into the placed light there, one at a time unless it is full,
     * as a block's {@code useItemOn}; null if it isn't fuel, for the block's own use of the item.
     */
    static @Nullable InteractionResult feedByHand(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        if (!isFuel(stack, level) || !(level.getBlockEntity(pos) instanceof BlockEntity entity)
                || !(entity.getBlockState().getBlock() instanceof FuelledLight)) {
            return null;
        }
        // The client doesn't know the fuel.
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!take((ServerLevel) level, pos, stack)) {
            return InteractionResult.CONSUME;
        }
        HandFuel.feed(stack, level, pos, player, hand);
        return InteractionResult.SUCCESS_SERVER;
    }

    /** A placed light's tick on the server: its fuel brought up to date, then shown. */
    static void tick(Level level, BlockPos pos, BlockState state, BlockEntity entity) {
        if (level instanceof ServerLevel server && state.getBlock() instanceof FuelledLight light) {
            var fuel = upToDate(server, pos, state, light, entity);
            if (fuel != null) {
                show(server, pos, state, light.shown(server, pos, state, fuel));
            }
        }
    }

    /** The fuel of the placed light there, burnt up to now, or null if there is none. */
    static @Nullable Fuel upToDate(ServerLevel level, BlockPos pos, BlockState state) {
        return state.getBlock() instanceof FuelledLight light && level.getBlockEntity(pos) instanceof BlockEntity entity
                ? upToDate(level, pos, state, light, entity) : null;
    }

    private static @Nullable Fuel upToDate(ServerLevel level, BlockPos pos, BlockState state, FuelledLight light, BlockEntity entity) {
        var fuel = stored(entity);
        if (fuel != null && fuel.burn(level.getOverworldClockTime(), light.rate(level, pos, state))) {
            entity.setChanged();
        }
        return fuel;
    }

    private static void show(ServerLevel level, BlockPos pos, BlockState state, BlockState shown) {
        if (shown != state) {
            level.setBlockAndUpdate(pos, shown);
        }
    }

    /** The fuel attached to a placed light's block entity, or null if it isn't one. */
    private static @Nullable Fuel stored(@Nullable BlockEntity entity) {
        if (entity == null || !(entity.getBlockState().getBlock() instanceof FuelledLight)) {
            return null;
        }
        return entity.getData(Light.FUEL);
    }

    /** A placed light there, its fuel burnt up to now, and the burn time of one of these. */
    private record Feeding(FuelledLight light, BlockEntity entity, BlockState state, Fuel fuel, int burnTime) {

        static @Nullable Feeding of(ServerLevel level, BlockPos pos, ItemStack stack) {
            var state = level.getBlockState(pos);
            int burnTime = HandFuel.burnTime(stack, level);
            if (!(state.getBlock() instanceof FuelledLight light) || burnTime <= 0
                    || !(level.getBlockEntity(pos) instanceof BlockEntity entity)) {
                return null;
            }
            var fuel = upToDate(level, pos, state, light, entity);
            return fuel == null ? null : new Feeding(light, entity, state, fuel, burnTime);
        }
    }
}
