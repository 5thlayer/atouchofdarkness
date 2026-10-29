// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

/**
 * The burn arithmetic every Light Source shares, with no Minecraft types so JUnit runs it on a plain
 * JVM. A Light Source burns by the level's time of day, not its game time, so sleep and
 * {@code /time} burn it too: a tick of fuel for each tick the clock moves on, at a rate that pauses
 * it or doubles it in rain. Fuel, burn and caps are in ticks.
 */
public final class Burn {

    /** The rates a Light Source burns at, ticks of fuel to a tick of the clock. */
    public static final int PAUSED = 0;
    public static final int STILL = 1;
    public static final int IN_RAIN = 2;

    /** Fuel gives this many times its vanilla furnace burn time. */
    public static final int FUEL_FACTOR = 10;

    /** A relit Light Source burns this fraction of a fresh one's time, 1 in 4. */
    private static final int RELIT_DIVISOR = 4;

    private Burn() {
    }

    /** Ticks of the clock between two readings of it: none for a clock set back. */
    public static long elapsed(long last, long now) {
        return Math.max(now - last, 0);
    }

    /** What is left of the fuel once the clock has moved on this far at this rate; never below nothing. */
    public static int burn(int left, long elapsed, int rate) {
        if (rate == PAUSED) {
            return left;
        }
        // Checked first, so a long jump of the clock can't overflow.
        if (elapsed >= left) {
            return 0;
        }
        return (int) Math.max(left - elapsed * rate, 0);
    }

    /** The burn time of a fresh Light Source relit. */
    public static int relit(int fresh) {
        return fresh / RELIT_DIVISOR;
    }

    /** The fuel after adding an item of this vanilla burn time, up to the cap; the overflow is lost. */
    public static int refuel(int left, int burnTime, int cap) {
        return (int) Math.min((long) left + (long) burnTime * FUEL_FACTOR, cap);
    }

    /** Whether the whole of an item of this vanilla burn time fits into the room the fuel leaves under the cap. */
    public static boolean fits(int left, int burnTime, int cap) {
        return (long) left + (long) burnTime * FUEL_FACTOR <= cap;
    }

    /**
     * The light of a Light Source that dims as its fuel runs down: the first of the four levels above ¾
     * of its cap, the second above ½, the third above ¼, the fourth above nothing, and none once dry.
     */
    public static int step(int left, int cap, int[] levels) {
        if (left <= 0) {
            return 0;
        }
        // Quarters crossed from the top, counted without division's rounding.
        long fourTimes = 4L * left;
        if (fourTimes > 3L * cap) {
            return levels[0];
        }
        if (fourTimes > 2L * cap) {
            return levels[1];
        }
        return fourTimes > cap ? levels[2] : levels[3];
    }

    /** An item bar's width, out of this many pixels, for so much of the full amount: rounded, never past full. */
    public static int bar(int part, int full, int width) {
        return Math.clamp(Math.round((float) width * part / full), 0, width);
    }

    /**
     * A comparator's reading of a Light Source's fuel, 0 to 15 for how full it is: the fifteenths of its
     * cap it holds, rounded up, so it reads 0 only once dry and 15 for its top fifteenth.
     */
    public static int signal(int left, int cap) {
        if (left <= 0) {
            return 0;
        }
        return (int) Math.min((15L * left + cap - 1) / cap, 15);
    }
}
