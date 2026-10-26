// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment whose batch runs with the Dark's config set as a pack author might set it, and set
 * back after it. The config is the server's, so no test of the defaults can run beside it.
 */
final class WithDarkConfig implements TestEnvironmentDefinition<WithDarkConfig.Values> {

    /** A threshold of 8, a 2 s Grace Period, a refill delay of half a second, and a Strike of 4 every 2 s. */
    static final Values SET = new Values(8, 40, 10, 4.0, 40);

    static final WithDarkConfig INSTANCE = new WithDarkConfig();

    static final MapCodec<WithDarkConfig> CODEC = MapCodec.unit(INSTANCE);

    private WithDarkConfig() {
    }

    /** Every value of the Dark's config. */
    record Values(int litThreshold, int gracePeriod, int refillDelay, double strikeDamage, int strikeInterval) {

        static Values current() {
            return new Values(DarkConfig.LIT_THRESHOLD.get(), DarkConfig.GRACE_PERIOD.get(), DarkConfig.REFILL_DELAY.get(),
                    DarkConfig.STRIKE_DAMAGE.get(), DarkConfig.STRIKE_INTERVAL.get());
        }

        void apply() {
            DarkConfig.LIT_THRESHOLD.set(litThreshold);
            DarkConfig.GRACE_PERIOD.set(gracePeriod);
            DarkConfig.REFILL_DELAY.set(refillDelay);
            DarkConfig.STRIKE_DAMAGE.set(strikeDamage);
            DarkConfig.STRIKE_INTERVAL.set(strikeInterval);
        }
    }

    @Override
    public Values setup(ServerLevel level) {
        var before = Values.current();
        SET.apply();
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Values before) {
        before.apply();
    }

    @Override
    public MapCodec<WithDarkConfig> codec() {
        return CODEC;
    }
}
