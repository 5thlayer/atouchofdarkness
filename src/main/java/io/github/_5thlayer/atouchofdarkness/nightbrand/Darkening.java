// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Spent Torch's darkening, the data component {@code atouchofdarkness:darkening}: the ticks it has
 * {@code darkened} in the Dark of the {@code full} darkening the server's config set when it last darkened,
 * which its item bar shows, as a client has no copy of that config. A Spent Torch has none until it
 * first darkens, so a fresh one stacks with the others.
 */
public record Darkening(int darkened, int full) {

    public static final Codec<Darkening> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("darkened").forGetter(Darkening::darkened),
            Codec.INT.fieldOf("full").forGetter(Darkening::full)
    ).apply(instance, Darkening::new));

    public static final StreamCodec<ByteBuf, Darkening> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Darkening::darkened,
            ByteBufCodecs.VAR_INT, Darkening::full,
            Darkening::new);

    /** The darkening after this many more ticks in the Dark, of a full darkening the config now sets. */
    Darkening darken(long ticks, int full) {
        return new Darkening(Darken.darken(darkened, ticks, full), full);
    }

    /** The ticks in the Dark it has left before it is a Nightbrand, none once done. */
    public int left() {
        return Math.max(full - darkened, 0);
    }

    boolean done() {
        return Darken.done(darkened, full);
    }
}
