// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/** A placed Light Source's block that {@link PlacedFuel} feeds: the Lamp's and the Campfire's. */
interface FuelledLight {

    /** Its fuel, burnt up to now, or null without its block entity. */
    @Nullable Fuel upToDate(ServerLevel level, BlockPos pos, BlockState state);

    /** The fuel it holds at most. */
    int cap();

    /** Saves the fuel just added, and shows it. */
    void fuelled(ServerLevel level, BlockPos pos, BlockState state, Fuel fuel);
}
