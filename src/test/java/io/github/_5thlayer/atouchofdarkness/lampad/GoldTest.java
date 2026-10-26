// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** The gold a Lampad takes at each renewal, a nugget per 8 Lamps in her range at the defaults. */
class GoldTest {

    @Test
    void aNuggetForEachEightLampsRoundedUp() {
        assertEquals(0, Gold.owed(0, 8));
        assertEquals(1, Gold.owed(1, 8));
        assertEquals(1, Gold.owed(8, 8));
        assertEquals(2, Gold.owed(9, 8));
        assertEquals(3, Gold.owed(17, 8));
    }

    @Test
    void eachNuggetPaysForEightLamps() {
        assertEquals(0, Gold.paidFor(0, 8));
        assertEquals(16, Gold.paidFor(2, 8));
    }

    @Test
    void manyNuggetsPayForNoMoreThanEveryLamp() {
        assertEquals(Integer.MAX_VALUE, Gold.paidFor(Integer.MAX_VALUE, 8));
    }
}
