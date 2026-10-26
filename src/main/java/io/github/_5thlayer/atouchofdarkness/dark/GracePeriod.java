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

    /** The warning's lead on each Strike: 2 s. */
    public static final int WARNING = 40;

    /** What a tick has the player hear, beside nothing. */
    public enum Cue {
        NONE,
        /** The Grace Period starts to drain from full. */
        ENTER,
        /** A Strike lands in {@link #WARNING} ticks, unless the player reaches light first. */
        WARNING,
        STRIKE
    }

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

    /** How much of the Grace Period is drained, from 0 when full to 1 once the first Strike has landed. */
    public float drainedFraction(Settings settings) {
        return spent ? 1F : (float) Math.min(drained, settings.length()) / settings.length();
    }

    /**
     * One tick, in the Dark or out of it, and what it has the player hear. A Grace Period or interval of
     * 2 s or less has no warning, and a cue that falls on the tick of another gives way to it.
     */
    public Cue tick(boolean inTheDark, Settings settings) {
        if (!inTheDark) {
            litFor = Math.min(litFor + 1, settings.refillDelay());
            if (litFor >= settings.refillDelay()) {
                drained = 0;
                spent = false;
                sinceStrike = 0;
            }
            return Cue.NONE;
        }
        litFor = 0;
        boolean full = drained == 0;
        if (drained < settings.length()) {
            drained++;
        }
        // At or past the length, as a config that shortens it leaves a player already in the Dark.
        if (drained < settings.length()) {
            return full ? Cue.ENTER : warning(settings);
        }
        if (!spent) {
            spent = true;
            return Cue.STRIKE;
        }
        sinceStrike++;
        if (sinceStrike >= settings.strikeInterval()) {
            sinceStrike = 0;
            return Cue.STRIKE;
        }
        return warning(settings);
    }

    private Cue warning(Settings settings) {
        return ticksToStrike(settings) == WARNING ? Cue.WARNING : Cue.NONE;
    }
}
