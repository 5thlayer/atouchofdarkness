// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.List;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment whose batch runs with the Dark's config set as a pack author might set it, and set
 * back after it: a threshold of 5, a 2 s Grace Period, a refill delay of half a second, and a Strike of 4
 * every 2 s. The config is the server's, so no test of the defaults can run beside it.
 */
final class WithDarkConfig implements TestEnvironmentDefinition<List<Object>> {

    static final int LIT_THRESHOLD = 5;
    static final int GRACE_PERIOD = 40;
    static final int REFILL_DELAY = 10;
    static final double STRIKE_DAMAGE = 4;
    static final int STRIKE_INTERVAL = 40;

    static final WithDarkConfig INSTANCE = new WithDarkConfig();

    static final MapCodec<WithDarkConfig> CODEC = MapCodec.unit(INSTANCE);

    private WithDarkConfig() {
    }

    @Override
    public List<Object> setup(ServerLevel level) {
        var before = List.<Object>of(DarkConfig.LIT_THRESHOLD.get(), DarkConfig.GRACE_PERIOD.get(), DarkConfig.REFILL_DELAY.get(),
                DarkConfig.STRIKE_DAMAGE.get(), DarkConfig.STRIKE_INTERVAL.get());
        DarkConfig.LIT_THRESHOLD.set(LIT_THRESHOLD);
        DarkConfig.GRACE_PERIOD.set(GRACE_PERIOD);
        DarkConfig.REFILL_DELAY.set(REFILL_DELAY);
        DarkConfig.STRIKE_DAMAGE.set(STRIKE_DAMAGE);
        DarkConfig.STRIKE_INTERVAL.set(STRIKE_INTERVAL);
        return before;
    }

    @Override
    public void teardown(ServerLevel level, List<Object> before) {
        DarkConfig.LIT_THRESHOLD.set((Integer) before.get(0));
        DarkConfig.GRACE_PERIOD.set((Integer) before.get(1));
        DarkConfig.REFILL_DELAY.set((Integer) before.get(2));
        DarkConfig.STRIKE_DAMAGE.set((Double) before.get(3));
        DarkConfig.STRIKE_INTERVAL.set((Integer) before.get(4));
    }

    @Override
    public MapCodec<WithDarkConfig> codec() {
        return CODEC;
    }
}
