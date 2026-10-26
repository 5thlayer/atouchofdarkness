// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

/**
 * The Pillar's entry point. It registers the Pillar's own creative tab. The game tests hook
 * themselves in from the {@code gametest} source set, which never ships (libworks ADR 0011).
 */
@Mod(ATouchOfDarkness.MOD_ID)
public final class ATouchOfDarkness {

    /** The mod id, which gradle.properties' {@code mod_id} must match. */
    public static final String MOD_ID = "atouchofdarkness";

    public ATouchOfDarkness(IEventBus modBus) {
        DarknessTab.register(modBus);
    }
}
