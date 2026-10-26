// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

/**
 * One player's Grace Period (GLOSSARY.md), ticked once a game tick. It drains while the player is in
 * the Dark, and a Strike lands the tick it runs out, then every interval while they stay there. It
 * refills only once they have been Lit for the refill delay without a break; light shorter than that
 * pauses it. A new one is full, which is what a respawned player gets. Plain Java, so the JUnit tests
 * run it with no Minecraft on the class path; {@link Dark} keeps it with the player.
 */
public final class GracePeriod {

    /** The ticks, each 1/20 s, that the config sets. */
    public record Settings(int length, int refillDelay, int strikeInterval) {
    }

    /** Ticks of the Grace Period spent in the Dark. */
    private int drained;

    /** Ticks Lit without a break. */
    private int litFor;

    /** Ticks in the Dark since the last Strike, once the Grace Period has run out. */
    private int sinceStrike;

    public GracePeriod() {
    }

    GracePeriod(int drained, int litFor, int sinceStrike) {
        this.drained = drained;
        this.litFor = litFor;
        this.sinceStrike = sinceStrike;
    }

    int drained() {
        return drained;
    }

    int litFor() {
        return litFor;
    }

    int sinceStrike() {
        return sinceStrike;
    }

    /** One tick, Lit or in the Dark; true when a Strike lands on it. */
    public boolean tick(boolean lit, Settings settings) {
        if (lit) {
            litFor = Math.min(litFor + 1, settings.refillDelay());
            if (litFor >= settings.refillDelay()) {
                drained = 0;
                sinceStrike = 0;
            }
            return false;
        }
        litFor = 0;
        if (drained < settings.length()) {
            drained++;
            return drained == settings.length();
        }
        sinceStrike++;
        if (sinceStrike >= settings.strikeInterval()) {
            sinceStrike = 0;
            return true;
        }
        return false;
    }
}
