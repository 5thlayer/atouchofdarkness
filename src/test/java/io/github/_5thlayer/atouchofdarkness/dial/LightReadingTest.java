// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** What the Vesper Dial's meter shows: the light at the eyes out of 15, and the Lit check's answer as its state. */
class LightReadingTest {

    @Test
    void theValueIsTheLightAtTheEyes() {
        assertEquals(new LightReading(14F, LightReading.LIT), LightReading.of(14, true));
        assertEquals(new LightReading(0F, LightReading.IN_THE_DARK), LightReading.of(0, false));
    }

    @Test
    void theStateIsTheLitCheckNotTheThreshold() {
        // A full moon or the End's sky keeps a player Lit at a low reading.
        assertEquals(new LightReading(4F, LightReading.LIT), LightReading.of(4, true));
        // Below the threshold or not, an unlit player is In the Dark.
        assertEquals(new LightReading(4F, LightReading.IN_THE_DARK), LightReading.of(4, false));
    }

    @Test
    void theRowIsFullAtFifteen() {
        assertEquals(15F, LightReading.MAX);
        assertEquals(new LightReading(15F, LightReading.LIT), LightReading.of(15, true));
    }
}
