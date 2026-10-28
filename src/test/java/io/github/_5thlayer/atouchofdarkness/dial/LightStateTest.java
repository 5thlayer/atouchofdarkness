// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The Vesper Dial's meter's state: the Lit check's answer, as the index of a state with its own sprites. */
class LightStateTest {

    @Test
    void litIsTheFirstStateAndInTheDarkTheSecond() {
        assertEquals(0, LightState.of(true).ordinal());
        assertEquals(1, LightState.of(false).ordinal());
    }

    @Test
    void eachStateHasItsOwnSprites() {
        assertEquals("", LightState.LIT.suffix());
        assertEquals("_dark", LightState.IN_THE_DARK.suffix());
    }
}
