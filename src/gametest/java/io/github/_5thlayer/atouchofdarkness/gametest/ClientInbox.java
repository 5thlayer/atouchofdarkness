// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.ArrayList;
import java.util.List;

import io.github._5thlayer.atouchofdarkness.dark.DarkState;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ClientboundCustomPayloadPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;

/**
 * What the server sent a {@link DarkFixture#survivalPlayer}'s client from the test's start, each packet
 * with the test tick it was read on. The player's connection writes into an embedded channel, which is
 * read once a tick.
 */
final class ClientInbox {

    private record Received(long tick, Packet<?> packet) {
    }

    private final List<Received> received = new ArrayList<>();

    private ClientInbox() {
    }

    /** Starts reading what the server sends the player, leaving out what it sent as they joined. */
    static ClientInbox of(GameTestHelper helper, ServerPlayer player) {
        var inbox = new ClientInbox();
        var channel = (EmbeddedChannel) player.connection.getConnection().channel();
        channel.flushOutbound();
        channel.outboundMessages().clear();
        helper.onEachTick(() -> inbox.read(helper.getTick(), channel));
        return inbox;
    }

    private void read(long tick, EmbeddedChannel channel) {
        // The server flushes only the connections it accepted, so this one is flushed here.
        channel.flushOutbound();
        Object message;
        while ((message = channel.readOutbound()) != null) {
            if (message instanceof ClientboundBundlePacket bundle) {
                bundle.subPackets().forEach(packet -> received.add(new Received(tick, packet)));
            } else if (message instanceof Packet<?> packet) {
                received.add(new Received(tick, packet));
            }
        }
    }

    /** The ticks the sound was heard on, oldest first. */
    List<Long> heard(SoundEvent sound) {
        return received.stream()
                .filter(each -> each.packet() instanceof ClientboundSoundPacket packet && packet.getSound().value().equals(sound))
                .map(Received::tick)
                .toList();
    }

    /** The last Dark state the client was sent, or null if none. */
    DarkState lastDarkState() {
        DarkState last = null;
        for (var each : received) {
            if (each.packet() instanceof ClientboundCustomPayloadPacket(DarkState state)) {
                last = state;
            }
        }
        return last;
    }
}
