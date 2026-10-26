// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** The burn arithmetic every Light Source shares, by the time of day. */
class BurnTest {

    @Test
    void burnsByTheTimeOfDayThatPassed() {
        assertEquals(1, Burn.elapsed(18_000, 18_001));
        assertEquals(6000, Burn.elapsed(18_000, 24_000));
    }

    @Test
    void aClockSetBackBurnsNothing() {
        assertEquals(0, Burn.elapsed(18_000, 1000));
    }

    @Test
    void burnsOneTickOfFuelForATickOfDayTime() {
        assertEquals(5900, Burn.burn(6000, 100, Burn.STILL));
    }

    @Test
    void pausedBurnsNothing() {
        assertEquals(6000, Burn.burn(6000, 24_000, Burn.PAUSED));
    }

    @Test
    void rainBurnsTwiceAsFast() {
        assertEquals(5800, Burn.burn(6000, 100, Burn.IN_RAIN));
        assertEquals(0, Burn.burn(6000, 3000, Burn.IN_RAIN));
    }

    @Test
    void burnsOutAtNothingLeft() {
        assertEquals(0, Burn.burn(6000, 6000, Burn.STILL));
        assertEquals(0, Burn.burn(100, Long.MAX_VALUE / 2, Burn.IN_RAIN));
    }

    @Test
    void relitBurnsAQuarterAsLong() {
        assertEquals(1500, Burn.relit(6000));
    }

    @Test
    void fuelGivesTenTimesItsBurnTimeUpToTheCap() {
        // Coal burns 1600 ticks in a furnace; a day is 24000.
        assertEquals(16_000, Burn.refuel(0, 1600, 24_000));
        assertEquals(24_000, Burn.refuel(10_000, 1600, 24_000));
    }

    @Test
    void aWholeItemFitsOnlyIntoTheRoomLeft() {
        // Coal adds 16000; a day is 24000.
        assertTrue(Burn.fits(0, 1600, 24_000));
        assertTrue(Burn.fits(8000, 1600, 24_000));
        assertFalse(Burn.fits(8001, 1600, 24_000));
        assertFalse(Burn.fits(24_000, 1, 24_000));
    }

    @Test
    void overflowIsLost() {
        int full = Burn.refuel(20_000, 1600, 24_000);
        assertEquals(24_000, full);
        assertEquals(24_000, Burn.refuel(full, 1600, 24_000));
    }

    /** The Campfire's curve, from the spec: 15 above ¾ of the cap, 12 above ½, 9 above ¼, 6 above 0. */
    @Test
    void stepsDownAsFuelFalls() {
        int[] campfire = {15, 12, 9, 6};
        assertEquals(15, Burn.step(12_000, 12_000, campfire));
        assertEquals(15, Burn.step(9001, 12_000, campfire));
        assertEquals(12, Burn.step(9000, 12_000, campfire));
        assertEquals(12, Burn.step(6001, 12_000, campfire));
        assertEquals(9, Burn.step(6000, 12_000, campfire));
        assertEquals(9, Burn.step(3001, 12_000, campfire));
        assertEquals(6, Burn.step(3000, 12_000, campfire));
        assertEquals(6, Burn.step(1, 12_000, campfire));
        assertEquals(0, Burn.step(0, 12_000, campfire));
    }

    /** The soul Campfire's, raised by one so its last step clears the threshold of 5. */
    @Test
    void soulStepsDownTheSameWay() {
        int[] soul = {11, 9, 7, 5};
        assertEquals(11, Burn.step(24_000, 24_000, soul));
        assertEquals(9, Burn.step(12_001, 24_000, soul));
        assertEquals(7, Burn.step(6001, 24_000, soul));
        assertEquals(5, Burn.step(1, 24_000, soul));
    }

    /**
     * A comparator's reading, from the spec: 15 full, 0 only dry, and any fuel left at least 1, each of
     * the fifteen steps a fifteenth of the cap, so a full one still reads 15 once it has burned a little.
     */
    @Test
    void comparatorReadsHowFullItIs() {
        assertEquals(15, Burn.signal(24_000, 24_000));
        assertEquals(15, Burn.signal(22_401, 24_000));
        assertEquals(14, Burn.signal(22_400, 24_000));
        assertEquals(8, Burn.signal(12_000, 24_000));
        assertEquals(1, Burn.signal(1, 24_000));
        assertEquals(0, Burn.signal(0, 24_000));
    }
}
