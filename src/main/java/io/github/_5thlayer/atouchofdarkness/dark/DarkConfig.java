// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * The Pillar's common config, {@code atouchofdarkness-common.toml}: whether Discovery is on, and the
 * Dark's part. Every number is a starting value to tune in play (suite ADR-0001). Times are in ticks, 20 to a second.
 */
public final class DarkConfig {

    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.BooleanValue DISCOVERY;

    public static final ModConfigSpec.IntValue LIT_THRESHOLD;

    public static final ModConfigSpec.IntValue GRACE_PERIOD;

    public static final ModConfigSpec.IntValue REFILL_DELAY;

    public static final ModConfigSpec.DoubleValue STRIKE_DAMAGE;

    public static final ModConfigSpec.IntValue STRIKE_INTERVAL;

    static {
        var builder = new ModConfigSpec.Builder();
        DISCOVERY = builder
                .comment("Whether recipe viewers' information pages say nothing of a thing until the player has discovered it. Off, every page shows.")
                .define("discovery", true);
        builder.push("dark");
        LIT_THRESHOLD = builder
                .comment("The light at a player's eyes, vanilla's raw brightness, at or above which they are Lit. The open sky reads 4 at night.")
                .defineInRange("litThreshold", 5, 0, 15);
        GRACE_PERIOD = builder
                .comment("Ticks a player can be in the Dark before the first Strike.")
                .defineInRange("gracePeriod", 200, 1, Integer.MAX_VALUE);
        REFILL_DELAY = builder
                .comment("Ticks a player must be Lit without a break before the Grace Period refills.")
                .defineInRange("refillDelay", 40, 0, Integer.MAX_VALUE);
        STRIKE_DAMAGE = builder
                .comment("A Strike's damage on Normal, before armour. Peaceful deals none, Easy half and Hard half again.")
                .defineInRange("strikeDamage", 10.0, 0.0, 1024.0);
        STRIKE_INTERVAL = builder
                .comment("Ticks between Strikes once the Grace Period has run out.")
                .defineInRange("strikeInterval", 200, 1, Integer.MAX_VALUE);
        builder.pop();
        SPEC = builder.build();
    }

    private DarkConfig() {
    }

    static GracePeriod.Settings gracePeriodSettings() {
        return new GracePeriod.Settings(GRACE_PERIOD.getAsInt(), REFILL_DELAY.getAsInt(), STRIKE_INTERVAL.getAsInt());
    }
}
