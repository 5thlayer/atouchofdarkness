// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Light Sources' common config, {@code atouchofdarkness-light.toml}, beside the Dark's. Every
 * number is a starting value to tune in play (suite ADR-0001). Times are in ticks of the time of day,
 * 24000 to a day.
 */
public final class LightConfig {

    /** Its file, in the {@code config} folder. */
    public static final String FILE = "atouchofdarkness-light.toml";

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.IntValue TORCH_BURN_TIME;
    public static final ModConfigSpec.IntValue CAMPFIRE_CAP;
    public static final ModConfigSpec.IntValue LAMP_CAP;
    public static final ModConfigSpec.BooleanValue VANILLA_LIGHT;

    static {
        var builder = new ModConfigSpec.Builder();
        VANILLA_LIGHT = builder
                .comment("Brings vanilla's light back: the recipes of its torches, lanterns, candles, campfires and other cheap light, and the light of every block in atouchofdarkness:unlit, whose tag it empties.",
                        "Takes hold as the world's data loads or reloads; a block lit before, or in a chunk saved before the Pillar was added, keeps the light it had until a block beside it changes.")
                .define("vanillaLight", false);
        builder.push("torch");
        TORCH_BURN_TIME = builder
                .comment("Ticks a fresh Torch burns while held. The soul Torch burns twice as long, and a relit Spent Torch a quarter as long.")
                .defineInRange("burnTime", 6000, 1, Integer.MAX_VALUE);
        builder.pop();
        builder.push("campfire");
        CAMPFIRE_CAP = builder
                .comment("Ticks of fuel a Campfire holds, half a day; it is placed with half of it. The soul Campfire holds twice as much.")
                .defineInRange("cap", 12000, 2, Integer.MAX_VALUE);
        builder.pop();
        builder.push("lamp");
        LAMP_CAP = builder
                .comment("Ticks of fuel a Lamp holds, a day. The soul Lamp holds twice as much.")
                .defineInRange("cap", 24000, 1, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private LightConfig() {
    }
}
