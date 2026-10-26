// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import java.util.EnumSet;
import java.util.Set;

/**
 * One player's Grace Period (GLOSSARY.md), ticked once a game tick. It drains while the player is in
 * the Dark, and a Strike lands the tick it runs out, then every interval while they stay there. It
 * refills only once they have been Lit for the refill delay without a break; light shorter than that
 * pauses it. A new one is full, which is what a respawned player gets. {@link Dark} keeps it with the
 * player.
 */
public final class GracePeriod {

    /** The warning's lead on each Strike: 2 s. */
    public static final int WARNING_LEAD = 40;

    /** What a tick has the player hear. */
    public enum Cue {
        /** The Grace Period starts to drain from full. */
        ENTER,
        /** A Strike lands in {@link #WARNING_LEAD} ticks or fewer, unless the player reaches light first. */
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

    /**
     * Whether the coming Strike has been warned of. Not kept with the player, so one who joins within
     * the warning's lead of a Strike is warned again.
     */
    private boolean warned;

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
     * One tick, in the Dark or out of it, and what it has the player hear. The warning comes on the
     * first tick in the Dark with {@link #WARNING_LEAD} or fewer left before the coming Strike, so a
     * Grace Period or interval shorter than that, or a config that shortens one, warns at once.
     */
    public Set<Cue> tick(boolean inTheDark, Settings settings) {
        if (!inTheDark) {
            litFor = Math.min(litFor + 1, settings.refillDelay());
            if (litFor >= settings.refillDelay()) {
                drained = 0;
                spent = false;
                sinceStrike = 0;
                warned = false;
            }
            return EnumSet.noneOf(Cue.class);
        }
        litFor = 0;
        var cues = EnumSet.noneOf(Cue.class);
        if (drained == 0) {
            cues.add(Cue.ENTER);
        }
        if (strikes(settings)) {
            warned = false;
            cues.add(Cue.STRIKE);
        } else if (!warned && ticksToStrike(settings) <= WARNING_LEAD) {
            warned = true;
            cues.add(Cue.WARNING);
        }
        return cues;
    }

    /** Drains one tick in the Dark; true when a Strike lands on it. */
    private boolean strikes(Settings settings) {
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
