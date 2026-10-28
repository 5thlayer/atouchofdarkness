// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import io.github._5thlayer.atouchofdarkness.discovery.Discoveries;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment whose batch runs with the {@code discovery} config switched off, as a pack author
 * might switch it, and set back after it. The config is the server's, so no test of Discovery can run beside it.
 */
final class WithDiscoveryOff implements TestEnvironmentDefinition<Boolean> {

    static final WithDiscoveryOff INSTANCE = new WithDiscoveryOff();

    static final MapCodec<WithDiscoveryOff> CODEC = MapCodec.unit(INSTANCE);

    private WithDiscoveryOff() {
    }

    @Override
    public Boolean setup(ServerLevel level) {
        boolean before = DarkConfig.DISCOVERY.getAsBoolean();
        DarkConfig.DISCOVERY.set(false);
        // Set here rather than in the file, so no config reload tells the clients.
        Discoveries.record().retell();
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Boolean before) {
        DarkConfig.DISCOVERY.set(before);
        Discoveries.record().retell();
    }

    @Override
    public MapCodec<WithDiscoveryOff> codec() {
        return CODEC;
    }
}
