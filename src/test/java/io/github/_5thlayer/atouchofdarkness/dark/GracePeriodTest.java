// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.EnumSet;
import java.util.Set;

import io.github._5thlayer.atouchofdarkness.dark.GracePeriod.Cue;
import org.junit.jupiter.api.Test;

/**
 * The fraction of the Grace Period drained, which the client's vignette deepens with, and the warning's
 * place where a Grace Period or interval is no longer than its lead, which the game tests at the
 * defaults never reach.
 */
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

    @Test
    void aShortGracePeriodWarnsAsItIsEntered() {
        var settings = new GracePeriod.Settings(30, 40, 200);
        var period = new GracePeriod();
        assertEquals(EnumSet.of(Cue.ENTER, Cue.WARNING), period.tick(true, settings));
        assertEquals(Set.of(), tick(period, true, 28, settings));
        assertEquals(EnumSet.of(Cue.STRIKE), period.tick(true, settings));
    }

    @Test
    void aShortIntervalWarnsTheTickAfterEachStrike() {
        var settings = new GracePeriod.Settings(200, 40, 30);
        var period = new GracePeriod();
        tick(period, true, 200, settings);
        assertEquals(EnumSet.of(Cue.WARNING), period.tick(true, settings));
        assertEquals(Set.of(), tick(period, true, 28, settings));
        assertEquals(EnumSet.of(Cue.STRIKE), period.tick(true, settings));
        assertEquals(EnumSet.of(Cue.WARNING), period.tick(true, settings));
    }

    /** Shortened from 200 to 100 at 90 drained, 10 ticks from the Strike: the next tick warns. */
    @Test
    void aShortenedGracePeriodStillWarns() {
        var period = new GracePeriod();
        tick(period, true, 90, SETTINGS);
        assertEquals(EnumSet.of(Cue.WARNING), period.tick(true, new GracePeriod.Settings(100, 40, 200)));
    }

    /** What the ticks had the player hear, all together. */
    private static Set<Cue> tick(GracePeriod period, boolean inTheDark, int ticks, GracePeriod.Settings settings) {
        var heard = EnumSet.noneOf(Cue.class);
        for (int i = 0; i < ticks; i++) {
            heard.addAll(period.tick(inTheDark, settings));
        }
        return heard;
    }

    private static void tick(GracePeriod period, boolean inTheDark, int ticks) {
        tick(period, inTheDark, ticks, SETTINGS);
    }
}
