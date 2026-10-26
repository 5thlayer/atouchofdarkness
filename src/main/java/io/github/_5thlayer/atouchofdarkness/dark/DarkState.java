// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/**
 * What the server tells a player's client of the Dark, whenever it changes: whether they are in it, and
 * how much of their Grace Period is drained, from 0 to 1. The client's vignette follows it.
 */
public record DarkState(boolean inTheDark, float drained) implements CustomPacketPayload {

    /** Out of the Dark with a full Grace Period, as a client starts. */
    public static final DarkState CLEAR = new DarkState(false, 0F);

    public static final Type<DarkState> TYPE = new Type<>(Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "dark_state"));

    public static final StreamCodec<ByteBuf, DarkState> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.BOOL, DarkState::inTheDark,
            ByteBufCodecs.FLOAT, DarkState::drained,
            DarkState::new);

    @Override
    public Type<DarkState> type() {
        return TYPE;
    }
}
