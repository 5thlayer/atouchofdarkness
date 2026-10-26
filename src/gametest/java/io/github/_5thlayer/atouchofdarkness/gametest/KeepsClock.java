// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;

/**
 * A test environment whose batch moves the Overworld's clock on, as sleep or {@code /time add} does,
 * and sets it back after. The clock is the server's, so a test that moves it runs in a batch of its own.
 */
final class KeepsClock implements TestEnvironmentDefinition<Long> {

    static final KeepsClock INSTANCE = new KeepsClock();

    static final MapCodec<KeepsClock> CODEC = MapCodec.unit(INSTANCE);

    private KeepsClock() {
    }

    /** Moves the time of day on this many ticks at once, as sleeping through the night does. */
    static void advance(GameTestHelper helper, long ticks) {
        var level = helper.getLevel();
        var overworld = level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        var clocks = level.getServer().clockManager();
        clocks.setTotalTicks(overworld, clocks.getTotalTicks(overworld) + ticks);
    }

    @Override
    public Long setup(ServerLevel level) {
        return level.getServer().clockManager().getTotalTicks(level.registryAccess().getOrThrow(WorldClocks.OVERWORLD));
    }

    @Override
    public void teardown(ServerLevel level, Long before) {
        level.getServer().clockManager().setTotalTicks(level.registryAccess().getOrThrow(WorldClocks.OVERWORLD), before);
    }

    @Override
    public MapCodec<KeepsClock> codec() {
        return CODEC;
    }
}
