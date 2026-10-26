// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;

/**
 * The one way fuel goes into a placed Light Source, a Lamp or a Campfire, whoever brings it: a player's
 * hand or a Lampad. The light says whether the whole of an item fits in the room it has left, and takes
 * one, ten times its burn time up to its cap. A player's hand feeds one that isn't full and loses the
 * overflow; a Lampad feeds only an item that fits whole. Fuel never turns a Lamp on or off.
 */
public final class PlacedFuel {

    private PlacedFuel() {
    }

    /** Whether the block there is a placed light with room for the whole of one of these. */
    public static boolean fits(ServerLevel level, BlockPos pos, ItemStack stack) {
        var state = level.getBlockState(pos);
        int burnTime = HandFuel.burnTime(stack, level);
        if (!(state.getBlock() instanceof FuelledLight light) || burnTime <= 0) {
            return false;
        }
        var fuel = light.upToDate(level, pos, state);
        return fuel != null && Burn.fits(fuel.left(), burnTime, light.cap());
    }

    /**
     * Puts one of these into the placed light there, unless it is full, the overflow lost; false if it
     * took none. The stack is left as it was, for the one who fed it to use up.
     */
    public static boolean take(ServerLevel level, BlockPos pos, ItemStack stack) {
        var state = level.getBlockState(pos);
        int burnTime = HandFuel.burnTime(stack, level);
        if (!(state.getBlock() instanceof FuelledLight light) || burnTime <= 0) {
            return false;
        }
        var fuel = light.upToDate(level, pos, state);
        if (fuel == null || fuel.left() >= light.cap()) {
            return false;
        }
        fuel.refuel(burnTime, light.cap());
        light.fuelled(level, pos, state, fuel);
        return true;
    }
}
