// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

/**
 * The steep arithmetic, with no Minecraft types so JUnit runs it on a plain JVM. A Spent Torch soaks the
 * Dark up a tick for each game tick its carrier spends in the Dark, and keeps what it has soaked between
 * dips, until it has steeped for its full time and is a Nightbrand. Times are in ticks.
 */
public final class Steep {

    private Steep() {
    }

    /** The full steep, so many Grace Periods long; never past what an int holds. */
    public static int full(int gracePeriods, int gracePeriod) {
        return (int) Math.min((long) gracePeriods * gracePeriod, Integer.MAX_VALUE);
    }

    /** What the torch has steeped after this many more ticks in the Dark, never past the full steep. */
    public static int steep(int steeped, long ticks, int full) {
        return (int) Math.min(steeped + ticks, full);
    }

    /** Whether it has steeped its full time, which a config that shortens the steep can make so at once. */
    public static boolean done(int steeped, int full) {
        return steeped >= full;
    }
}
