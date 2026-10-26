// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/**
 * A placed Light Source's fuel, the attachment {@code atouchofdarkness:fuel} on its block entity: the
 * ticks it has left, and the time of day it was last brought up to date at, which it keeps in its save,
 * so it burns the time its chunk was unloaded once loaded again. One just placed has no clock yet.
 * Changed in place, so its clock moves on each tick without marking the block entity changed: only
 * a change of what is left needs saving, since nothing burns while it is out.
 */
public final class Fuel {

    private static final long NO_CLOCK = Long.MIN_VALUE;

    static final MapCodec<Fuel> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("left").forGetter(Fuel::left),
            Codec.LONG.fieldOf("clock").forGetter(fuel -> fuel.clock)
    ).apply(instance, Fuel::new));

    private int left;
    private long clock;

    private Fuel(int left, long clock) {
        this.left = left;
        this.clock = clock;
    }

    /** This much fuel, with no clock yet, so it burns from the first time it is brought up to date. */
    public static Fuel unclocked(int left) {
        return new Fuel(left, NO_CLOCK);
    }

    /** The ticks of fuel left. */
    public int left() {
        return left;
    }

    /** Brings it up to date with the time of day now, burning at this rate since it last was; true if any burned. */
    boolean burn(long now, int rate) {
        long elapsed = clock == NO_CLOCK ? 0 : Burn.elapsed(clock, now);
        clock = now;
        int before = left;
        left = Burn.burn(left, elapsed, rate);
        return left != before;
    }

    /** Adds an item of this vanilla burn time, up to the cap; the overflow is lost. */
    void refuel(int burnTime, int cap) {
        left = Burn.refuel(left, burnTime, cap);
    }
}
