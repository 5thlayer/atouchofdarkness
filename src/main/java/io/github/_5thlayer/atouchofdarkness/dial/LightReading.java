// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

/**
 * What the Vesper Dial's meter shows: the light at the carrier's eyes, out of {@link #MAX}, and the
 * index of its state, Lit or In the Dark, which comes from the Dark's own Lit check and not from the
 * light against the threshold, so a full moon or the End's sky shows Lit at a low reading. Plain
 * Java, with no Minecraft types.
 */
public record LightReading(float value, int state) {

    /** The light that fills the row, as vanilla's brightest. */
    public static final float MAX = 15F;

    /** The index of each of the meter's states, in the order it registers them. */
    public static final int LIT = 0;
    public static final int IN_THE_DARK = 1;

    /** The reading of this light at the eyes, for a player whom the Lit check finds Lit or not. */
    public static LightReading of(int light, boolean lit) {
        return new LightReading(light, lit ? LIT : IN_THE_DARK);
    }
}
