// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.clock.WorldClocks;
import net.minecraft.world.level.MoonPhase;

/**
 * A test environment whose batch runs at midnight in the Overworld under one phase of the moon, in one
 * weather, both set back after it. The clock is paused through the batch, since its test may start
 * thousands of ticks after this setup, and dawn comes 4330 after midnight. The game-test server never
 * cycles the weather, and the rain is set at once both ways, so none fades into this batch or the next.
 * The clock and the weather are the server's, so no test of another hour can run beside it.
 */
record AtNight(MoonPhase moon, TestEnvironmentDefinition.Weather.Type weather) implements TestEnvironmentDefinition<AtNight.Before> {

    static final MapCodec<AtNight> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            MoonPhase.CODEC.fieldOf("moon").forGetter(AtNight::moon),
            TestEnvironmentDefinition.Weather.Type.CODEC.fieldOf("weather").forGetter(AtNight::weather)
    ).apply(instance, AtNight::new));

    /** Midnight, a day into the moon's phase: the night sky reads 4 from dusk at 13670 to dawn at 22330. */
    private static final int MIDNIGHT = 18000;

    record Before(long clock, TestEnvironmentDefinition.Weather.Type weather) {
    }

    @Override
    public Before setup(ServerLevel level) {
        var clocks = level.getServer().clockManager();
        var overworld = level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        long before = clocks.getTotalTicks(overworld);
        clocks.setTotalTicks(overworld, moon.startTick() + MIDNIGHT);
        clocks.setPaused(overworld, true);
        var weatherBefore = new TestEnvironmentDefinition.Weather(weather).setup(level);
        settle(level, weather);
        return new Before(before, weatherBefore);
    }

    @Override
    public void teardown(ServerLevel level, Before before) {
        new TestEnvironmentDefinition.Weather(weather).teardown(level, before.weather());
        var clocks = level.getServer().clockManager();
        var overworld = level.registryAccess().getOrThrow(WorldClocks.OVERWORLD);
        // The game-test server's clock always runs, so there is no pause to restore.
        clocks.setPaused(overworld, false);
        clocks.setTotalTicks(overworld, before.clock());
        settle(level, before.weather());
    }

    /** The weather otherwise fades in or out over a second or more, Lit or not as it goes. */
    private static void settle(ServerLevel level, TestEnvironmentDefinition.Weather.Type weather) {
        level.setRainLevel(weather != TestEnvironmentDefinition.Weather.Type.CLEAR ? 1F : 0F);
        level.setThunderLevel(weather == TestEnvironmentDefinition.Weather.Type.THUNDER ? 1F : 0F);
        level.updateSkyBrightness();
    }

    @Override
    public MapCodec<AtNight> codec() {
        return CODEC;
    }
}
