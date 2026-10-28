// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

/**
 * The Vesper Dial's meter's states, in the order it registers them, so each one's ordinal is the index
 * the meter is set: Lit, or In the Dark. A player's state comes from the Dark's own Lit check, not from
 * the light against the threshold, so a full moon or the End's sky shows Lit at a low reading.
 */
public enum LightState {
    LIT(""),
    IN_THE_DARK("_dark");

    private final String suffix;

    LightState(String suffix) {
        this.suffix = suffix;
    }

    /** The state of a player whom the Lit check finds Lit or not. */
    public static LightState of(boolean lit) {
        return lit ? LIT : IN_THE_DARK;
    }

    /** What follows each of the state's sprite names: nothing when Lit, {@code _dark} In the Dark. */
    public String suffix() {
        return suffix;
    }
}
