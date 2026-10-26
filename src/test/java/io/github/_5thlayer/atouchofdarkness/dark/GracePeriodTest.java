// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The fraction of the Grace Period drained, which the client's vignette deepens with. */
class GracePeriodTest {

    private static final GracePeriod.Settings SETTINGS = new GracePeriod.Settings(200, 40, 200);

    @Test
    void drainsWithTheDarkAndHoldsFullOnceSpent() {
        var period = new GracePeriod();
        assertEquals(0F, period.drainedFraction(SETTINGS));
        tick(period, true, 50);
        assertEquals(0.25F, period.drainedFraction(SETTINGS));
        tick(period, true, 150);
        assertEquals(1F, period.drainedFraction(SETTINGS));
        tick(period, true, 100);
        assertEquals(1F, period.drainedFraction(SETTINGS));
    }

    @Test
    void holdsInBriefLightAndEmptiesOnceRefilled() {
        var period = new GracePeriod();
        tick(period, true, 100);
        tick(period, false, 39);
        assertEquals(0.5F, period.drainedFraction(SETTINGS));
        tick(period, false, 1);
        assertEquals(0F, period.drainedFraction(SETTINGS));
    }

    /** A config that shortens the Grace Period leaves a player drained past it, which reads full. */
    @Test
    void neverPassesFull() {
        var period = new GracePeriod();
        tick(period, true, 150);
        assertEquals(1F, period.drainedFraction(new GracePeriod.Settings(100, 40, 200)));
    }

    private static void tick(GracePeriod period, boolean inTheDark, int ticks) {
        for (int i = 0; i < ticks; i++) {
            period.tick(inTheDark, SETTINGS);
        }
    }
}
