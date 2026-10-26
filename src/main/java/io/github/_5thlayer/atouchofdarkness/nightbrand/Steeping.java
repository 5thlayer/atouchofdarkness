// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * A Spent Torch's steep, the data component {@code atouchofdarkness:steeping}: the ticks it has
 * {@code steeped} in the Dark of the {@code full} steep the server's config set when it last steeped,
 * which its item bar shows, as a client has no copy of that config. A Spent Torch has none until it
 * first steeps, so a fresh one stacks with the others.
 */
public record Steeping(int steeped, int full) {

    public static final Codec<Steeping> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            Codec.INT.fieldOf("steeped").forGetter(Steeping::steeped),
            Codec.INT.fieldOf("full").forGetter(Steeping::full)
    ).apply(instance, Steeping::new));

    public static final StreamCodec<ByteBuf, Steeping> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, Steeping::steeped,
            ByteBufCodecs.VAR_INT, Steeping::full,
            Steeping::new);

    /** The steep after this many more ticks in the Dark, of a full steep the config now sets. */
    Steeping steep(long ticks, int full) {
        return new Steeping(Steep.steep(steeped, ticks, full), full);
    }

    boolean done() {
        return Steep.done(steeped, full);
    }
}
