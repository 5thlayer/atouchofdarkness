// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A Strike's base damage scales to nothing on Peaceful, half on Easy, all on Normal and half again on Hard. */
class StrikeScalingTest {

    @Test
    void scalesByDifficulty() {
        // Vanilla's difficulty ids, Peaceful to Hard.
        assertEquals(0F, StrikeScaling.scale(10F, 0));
        assertEquals(5F, StrikeScaling.scale(10F, 1));
        assertEquals(10F, StrikeScaling.scale(10F, 2));
        assertEquals(15F, StrikeScaling.scale(10F, 3));
    }
}
