// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A carried Light Source's burn, the data component {@code atouchofdarkness:burning}: the ticks it has
 * {@code left} of the {@code full} it started with, which its item bar shows, and whether it was
 * {@code relit}, so it burns away to nothing. A Torch has none until it first burns, so a fresh one
 * stacks with no other and reads the config when it starts.
 */
public record Burning(int left, int full, boolean relit) {

    public static final Codec<Burning> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("left").forGetter(Burning::left),
            Codec.INT.fieldOf("full").forGetter(Burning::full),
            Codec.BOOL.optionalFieldOf("relit", false).forGetter(Burning::relit)
    ).apply(instance, Burning::new));

    public static final StreamCodec<ByteBuf, Burning> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Burning::left,
            ByteBufCodecs.VAR_INT, Burning::full,
            ByteBufCodecs.BOOL, Burning::relit,
            Burning::new);

    /** A fresh burn of this many ticks. */
    public static Burning fresh(int full) {
        return new Burning(full, full, false);
    }

    /** The burn of a Spent Torch relit, a quarter of a fresh Torch's. */
    public static Burning relit(int fresh) {
        int quarter = Burn.relit(fresh);
        return new Burning(quarter, quarter, true);
    }

    Burning withLeft(int left) {
        return new Burning(left, full, relit);
    }
}
