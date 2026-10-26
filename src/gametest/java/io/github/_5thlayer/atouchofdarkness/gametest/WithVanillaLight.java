// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

/**
 * A test environment whose batch runs with the light config's {@code vanillaLight} switch on, as a pack
 * that wants vanilla's light back would set it, and set back after it. The switch takes hold as the data
 * loads, so each way it reloads the server's data, which on the server thread waits for it.
 */
final class WithVanillaLight implements TestEnvironmentDefinition<Boolean> {

    static final WithVanillaLight INSTANCE = new WithVanillaLight();

    static final MapCodec<WithVanillaLight> CODEC = MapCodec.unit(INSTANCE);

    private WithVanillaLight() {
    }

    @Override
    public Boolean setup(ServerLevel level) {
        boolean before = LightConfig.VANILLA_LIGHT.get();
        switchTo(level.getServer(), true);
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Boolean before) {
        switchTo(level.getServer(), before);
    }

    @Override
    public MapCodec<WithVanillaLight> codec() {
        return CODEC;
    }

    private static void switchTo(MinecraftServer server, boolean on) {
        LightConfig.VANILLA_LIGHT.set(on);
        server.reloadResources(server.getPackRepository().getSelectedIds()).join();
    }
}
