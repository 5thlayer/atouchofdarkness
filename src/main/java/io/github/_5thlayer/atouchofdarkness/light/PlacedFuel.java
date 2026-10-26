// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * The one way fuel goes into a placed Light Source, a Lamp or a Campfire, whoever brings it: a player's
 * hand or a Lampad. The light says whether the whole of an item fits in the room it has left, and takes
 * one, ten times its burn time up to its cap. A player's hand feeds one that isn't full and loses the
 * overflow; a Lampad feeds only an item that fits whole. Fuel never turns a Lamp on or off.
 */
public final class PlacedFuel {

    private PlacedFuel() {
    }

    /** Whether a placed light takes it as fuel: any furnace fuel but a tool. */
    public static boolean isFuel(ItemStack stack, Level level) {
        return HandFuel.burnTime(stack, level) > 0;
    }

    /** Whether the block there is a placed light with room for the whole of one of these. */
    public static boolean fits(ServerLevel level, BlockPos pos, ItemStack stack) {
        var feeding = Feeding.of(level, pos, stack);
        return feeding != null && Burn.fits(feeding.fuel.left(), feeding.burnTime, feeding.light.cap());
    }

    /**
     * Puts one of these into the placed light there, unless it is full, the overflow lost; false if it
     * took none. The stack is left as it was, for the one who fed it to use up.
     */
    public static boolean take(ServerLevel level, BlockPos pos, ItemStack stack) {
        var feeding = Feeding.of(level, pos, stack);
        if (feeding == null || feeding.fuel.left() >= feeding.light.cap()) {
            return false;
        }
        feeding.fuel.refuel(feeding.burnTime, feeding.light.cap());
        feeding.light.fuelled(level, pos, feeding.state, feeding.fuel);
        return true;
    }

    /** A placed light there, its fuel burnt up to now, and the burn time of one of these. */
    private record Feeding(FuelledLight light, BlockState state, Fuel fuel, int burnTime) {

        static @Nullable Feeding of(ServerLevel level, BlockPos pos, ItemStack stack) {
            var state = level.getBlockState(pos);
            int burnTime = HandFuel.burnTime(stack, level);
            if (!(state.getBlock() instanceof FuelledLight light) || burnTime <= 0) {
                return null;
            }
            var fuel = light.upToDate(level, pos, state);
            return fuel == null ? null : new Feeding(light, state, fuel, burnTime);
        }
    }
}
