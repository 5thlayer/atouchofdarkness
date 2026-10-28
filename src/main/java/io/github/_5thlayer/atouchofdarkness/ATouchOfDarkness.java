// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness;

import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import io.github._5thlayer.atouchofdarkness.dial.VesperDial;
import io.github._5thlayer.atouchofdarkness.discovery.Discoveries;
import io.github._5thlayer.atouchofdarkness.lampad.Lampad;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;

/**
 * The Pillar's entry point. It registers the Pillar's own creative tab, the Dark, the Light Sources,
 * the Nightbrand, the Vesper Dial and the meter the Runtime draws for it, the Lampad, the Discoveries
 * the Runtime keeps for it and their configs.
 * The game tests hook themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(ATouchOfDarkness.MOD_ID)
public final class ATouchOfDarkness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofdarkness";

    public ATouchOfDarkness(IEventBus modBus, ModContainer container) {
        DarknessTab.register(modBus);
        Dark.register(modBus);
        Light.register(modBus);
        Nightbrand.register(modBus);
        VesperDial.register(modBus);
        Lampad.register(modBus);
        Discoveries.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, DarkConfig.SPEC);
        container.registerConfig(ModConfig.Type.COMMON, LightConfig.SPEC, LightConfig.FILE);
    }
}
