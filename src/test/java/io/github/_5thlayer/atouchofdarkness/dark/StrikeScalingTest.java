// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/** A Strike's base damage scales to nothing on Peaceful, half on Easy, all on Normal and half again on Hard. */
class StrikeScalingTest {

    /** Vanilla's difficulty ids. */
    private static final int PEACEFUL = 0;
    private static final int EASY = 1;
    private static final int NORMAL = 2;
    private static final int HARD = 3;

    @Test
    void peacefulDealsNothing() {
        assertEquals(0F, StrikeScaling.scale(10F, PEACEFUL));
    }

    @Test
    void easyDealsHalf() {
        assertEquals(5F, StrikeScaling.scale(10F, EASY));
    }

    @Test
    void normalDealsTheBase() {
        assertEquals(10F, StrikeScaling.scale(10F, NORMAL));
    }

    @Test
    void hardDealsHalfAgain() {
        assertEquals(15F, StrikeScaling.scale(10F, HARD));
    }

    @Test
    void theFactorsApplyToAnyBase() {
        assertEquals(2F, StrikeScaling.scale(4F, EASY));
        assertEquals(6F, StrikeScaling.scale(4F, HARD));
    }
}
