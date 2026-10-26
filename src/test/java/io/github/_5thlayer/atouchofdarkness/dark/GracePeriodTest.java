// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * The Grace Period's rules, tick by tick: it drains in the Dark, a Strike lands when it runs out and
 * again every interval after, and it refills only after unbroken light. The game tests check the same
 * on a real player; these name the edges.
 */
class GracePeriodTest {

    /** The defaults: a 10 s Grace Period, a 2 s refill and a Strike every 10 s. */
    private static final GracePeriod.Settings DEFAULTS = new GracePeriod.Settings(200, 40, 200);

    @Test
    void theFirstStrikeLandsAsTheGracePeriodRunsOut() {
        var grace = new GracePeriod();
        assertEquals(List.of(200), strikes(grace, false, 200));
    }

    @Test
    void strikesRepeatEveryIntervalInTheDark() {
        var grace = new GracePeriod();
        assertEquals(List.of(200, 400, 600), strikes(grace, false, 600));
    }

    @Test
    void lightingUpBeforeTheGracePeriodRunsOutNeverStrikes() {
        var grace = new GracePeriod();
        assertEquals(List.of(), strikes(grace, false, 199));
        assertEquals(List.of(), strikes(grace, true, 40));
        assertEquals(List.of(), strikes(grace, false, 199));
    }

    @Test
    void unbrokenLightRefillsTheGracePeriod() {
        var grace = new GracePeriod();
        strikes(grace, false, 150);
        strikes(grace, true, 40);
        assertEquals(List.of(200), strikes(grace, false, 200));
    }

    @Test
    void lightShorterThanTheRefillDelayRefillsNothing() {
        var grace = new GracePeriod();
        strikes(grace, false, 150);
        strikes(grace, true, 39);
        assertEquals(List.of(50), strikes(grace, false, 50));
    }

    @Test
    void flickeringInAndOutOfLightRefillsNothing() {
        var grace = new GracePeriod();
        var struck = new ArrayList<Integer>();
        int tick = 0;
        // 30 ticks lit, 10 dark, over and over: never 40 lit in a row.
        for (int round = 0; round < 25 && struck.isEmpty(); round++) {
            for (int i = 0; i < 30; i++) {
                tick++;
                grace.tick(true, DEFAULTS);
            }
            for (int i = 0; i < 10; i++) {
                tick++;
                if (grace.tick(false, DEFAULTS)) {
                    struck.add(tick);
                }
            }
        }
        // The 200th dark tick is in the 20th round.
        assertEquals(List.of(20 * 40), struck);
    }

    @Test
    void lightPausesTheStrikesWithoutRefilling() {
        var grace = new GracePeriod();
        strikes(grace, false, 300);
        strikes(grace, true, 39);
        assertEquals(List.of(100), strikes(grace, false, 100));
    }

    @Test
    void theSettingsAreRead() {
        var grace = new GracePeriod();
        var shorter = new GracePeriod.Settings(20, 5, 30);
        assertEquals(List.of(20, 50), strikes(grace, false, 50, shorter));
        strikes(grace, true, 5, shorter);
        assertEquals(List.of(20), strikes(grace, false, 20, shorter));
    }

    private static List<Integer> strikes(GracePeriod grace, boolean lit, int ticks) {
        return strikes(grace, lit, ticks, DEFAULTS);
    }

    /** The ticks, counted from 1, on which a Strike landed. */
    private static List<Integer> strikes(GracePeriod grace, boolean lit, int ticks, GracePeriod.Settings settings) {
        var struck = new ArrayList<Integer>();
        for (int tick = 1; tick <= ticks; tick++) {
            if (grace.tick(lit, settings)) {
                struck.add(tick);
            }
        }
        return struck;
    }
}
