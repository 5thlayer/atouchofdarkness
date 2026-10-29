// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * A placed Light Source's block whose {@link Fuel} {@link PlacedFuel} keeps: what differs between the
 * Lamp's and the Campfire's.
 */
interface FuelledLight {

    /** The fuel it holds at most. */
    int cap();

    /** The fuel it is placed with, before anything gives it any. */
    int placedFuel();

    /** The {@link Burn} rate it burns at now, in this state. */
    int rate(ServerLevel level, BlockPos pos, BlockState state);

    /** The state it is fed in, before its fuel is shown: lit again, if its block lets it. */
    default BlockState relit(BlockState state) {
        return state.setValue(BlockStateProperties.LIT, true);
    }

    /** The state that shows this fuel, from this one: lit only if this one is and the fuel lets it. */
    BlockState shown(ServerLevel level, BlockPos pos, BlockState state, Fuel fuel);
}
