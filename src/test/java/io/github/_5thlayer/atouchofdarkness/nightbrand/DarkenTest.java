// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The darkening arithmetic: a Spent Torch soaks the Dark up a tick at a time, over three Grace Periods. */
class DarkenTest {

    @Test
    void threeGracePeriodsOfTheDefaultTenSeconds() {
        assertEquals(600, Darken.full(3, 200));
    }

    @Test
    void aLongConfigNeverOverflows() {
        assertEquals(Integer.MAX_VALUE, Darken.full(3, Integer.MAX_VALUE));
    }

    @Test
    void progressIsKeptBetweenDips() {
        int first = Darken.darken(0, 250, 600);
        int second = Darken.darken(first, 250, 600);
        assertEquals(500, second);
        assertFalse(Darken.done(second, 600));
        assertTrue(Darken.done(Darken.darken(second, 100, 600), 600));
    }

    @Test
    void neverDarkensPastFull() {
        assertEquals(600, Darken.darken(590, 100, 600));
    }

    @Test
    void aShortenedDarkeningFinishesOneAlreadyPastIt() {
        assertTrue(Darken.done(500, 400));
    }
}
