// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

/**
 * What a Lampad's binding costs in gold at each renewal, with no Minecraft types so JUnit runs it on a
 * plain JVM: a nugget for each so many Lamps in her range, the count rounded up, and the Lamps that what
 * she took pays for if it fell short.
 */
public final class Gold {

    private Gold() {
    }

    /** The nuggets owed for this many Lamps. */
    public static int owed(int lamps, int lampsPerNugget) {
        return Math.ceilDiv(lamps, lampsPerNugget);
    }

    /**
     * The Lamps the nuggets taken pay for, of those owed: every Lamp, those placed later too, if she took
     * all she was owed, or else so many for each nugget.
     */
    public static int paidFor(int taken, int owed, int lampsPerNugget) {
        return taken >= owed ? Integer.MAX_VALUE : (int) Math.min((long) taken * lampsPerNugget, Integer.MAX_VALUE);
    }
}
