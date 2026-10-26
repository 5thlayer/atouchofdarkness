// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment whose batch runs with a Lampad's binding cut to 5 s, as a pack author might set
 * the light config's {@code lampad.binding}, and set back after it. The config is the server's, so no
 * test of the defaults can run beside it.
 */
final class WithLampadConfig implements TestEnvironmentDefinition<Integer> {

    /** The binding it sets, in ticks. */
    static final int BINDING = 100;

    static final WithLampadConfig INSTANCE = new WithLampadConfig();

    static final MapCodec<WithLampadConfig> CODEC = MapCodec.unit(INSTANCE);

    private WithLampadConfig() {
    }

    @Override
    public Integer setup(ServerLevel level) {
        int before = LightConfig.LAMPAD_BINDING.get();
        LightConfig.LAMPAD_BINDING.set(BINDING);
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Integer before) {
        LightConfig.LAMPAD_BINDING.set(before);
    }

    @Override
    public MapCodec<WithLampadConfig> codec() {
        return CODEC;
    }
}
