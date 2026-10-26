// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

/**
 * One player's Grace Period (GLOSSARY.md), ticked once a game tick. It drains while the player is in
 * the Dark, and a Strike lands the tick it runs out, then every interval while they stay there. It
 * refills only once they have been Lit for the refill delay without a break; light shorter than that
 * pauses it. A new one is full, which is what a respawned player gets. {@link Dark} keeps it with the
 * player.
 */
public final class GracePeriod {

    /** The ticks, each 1/20 s, that the config sets. */
    public record Settings(int length, int refillDelay, int strikeInterval) {
    }

    /** Ticks of the Grace Period spent in the Dark. */
    private int drained;

    /** Ticks Lit without a break. */
    private int litFor;

    /** Whether the first Strike has landed since the Grace Period last refilled. */
    private boolean spent;

    /** Ticks in the Dark since the last Strike, once the first has landed. */
    private int sinceStrike;

    public GracePeriod() {
    }

    GracePeriod(int drained, int litFor, boolean spent, int sinceStrike) {
        this.drained = drained;
        this.litFor = litFor;
        this.spent = spent;
        this.sinceStrike = sinceStrike;
    }

    int drained() {
        return drained;
    }

    int litFor() {
        return litFor;
    }

    boolean spent() {
        return spent;
    }

    int sinceStrike() {
        return sinceStrike;
    }

    /** Ticks in the Dark until the next Strike: what is left of the Grace Period, or of the interval once it has run out. */
    int ticksToStrike(Settings settings) {
        return Math.max(spent ? settings.strikeInterval() - sinceStrike : settings.length() - drained, 0);
    }

    /** One tick, in the Dark or out of it; true when a Strike lands on it. */
    public boolean tick(boolean inTheDark, Settings settings) {
        if (!inTheDark) {
            litFor = Math.min(litFor + 1, settings.refillDelay());
            if (litFor >= settings.refillDelay()) {
                drained = 0;
                spent = false;
                sinceStrike = 0;
            }
            return false;
        }
        litFor = 0;
        if (drained < settings.length()) {
            drained++;
        }
        // At or past the length, as a config that shortens it leaves a player already in the Dark.
        if (drained < settings.length()) {
            return false;
        }
        if (!spent) {
            spent = true;
            return true;
        }
        sinceStrike++;
        if (sinceStrike >= settings.strikeInterval()) {
            sinceStrike = 0;
            return true;
        }
        return false;
    }
}
