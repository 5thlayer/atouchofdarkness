// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

/**
 * What a Lampad's binding costs in gold at each renewal, with no Minecraft types so JUnit runs it on a
 * plain JVM: a nugget for each so many Lamps in her range, the count rounded up, and the Lamps that what
 * she took pays for.
 */
public final class Gold {

    private Gold() {
    }

    /** The nuggets owed for this many Lamps. */
    public static int owed(int lamps, int lampsPerNugget) {
        return Math.ceilDiv(lamps, lampsPerNugget);
    }

    /** The Lamps these nuggets pay for. */
    public static int paidFor(int nuggets, int lampsPerNugget) {
        return (int) Math.min((long) nuggets * lampsPerNugget, Integer.MAX_VALUE);
    }
}
