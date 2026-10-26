// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The steep arithmetic: a Spent Torch soaks the Dark up a tick at a time, over three Grace Periods. */
class SteepTest {

    @Test
    void threeGracePeriodsOfTheDefaultTenSeconds() {
        assertEquals(600, Steep.full(3, 200));
    }

    @Test
    void aLongConfigNeverOverflows() {
        assertEquals(Integer.MAX_VALUE, Steep.full(3, Integer.MAX_VALUE));
    }

    @Test
    void progressIsKeptBetweenDips() {
        int first = Steep.steep(0, 250, 600);
        int second = Steep.steep(first, 250, 600);
        assertEquals(500, second);
        assertFalse(Steep.done(second, 600));
        assertTrue(Steep.done(Steep.steep(second, 100, 600), 600));
    }

    @Test
    void neverSteepsPastFull() {
        assertEquals(600, Steep.steep(590, 100, 600));
    }

    @Test
    void aShortenedSteepFinishesOneAlreadyPastIt() {
        assertTrue(Steep.done(500, 400));
    }
}
