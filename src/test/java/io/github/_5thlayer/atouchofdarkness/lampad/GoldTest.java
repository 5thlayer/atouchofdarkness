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
    void aShortPaymentPaysForEightLampsANugget() {
        assertEquals(0, Gold.paidFor(0, 2, 8));
        assertEquals(8, Gold.paidFor(1, 2, 8));
        assertEquals(16, Gold.paidFor(2, 3, 8));
    }

    @Test
    void aFullPaymentPaysForEveryLamp() {
        assertEquals(Integer.MAX_VALUE, Gold.paidFor(2, 2, 8));
        assertEquals(Integer.MAX_VALUE, Gold.paidFor(0, 0, 8));
    }
}
