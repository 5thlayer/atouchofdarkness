// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import java.util.function.Supplier;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * How a player feels the Dark before it strikes: a sound on entering it, a warning 2 s before each
 * Strike and a sound for each Strike, every one with a subtitle in {@code sounds.json} and heard by
 * that player alone, and the {@link DarkState} sent to their client, whose vignette follows it. No meter,
 * and nothing on the Runtime's HUD.
 */
public final class DarkCues {

    private static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ATouchOfDarkness.MOD_ID);

    public static final DeferredHolder<SoundEvent, SoundEvent> ENTER = sound("dark.enter");
    public static final DeferredHolder<SoundEvent, SoundEvent> WARNING = sound("dark.warning");
    public static final DeferredHolder<SoundEvent, SoundEvent> STRIKE = sound("dark.strike");

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ATouchOfDarkness.MOD_ID);

    /** What no state is, as a player has been sent none. */
    private static final DarkState UNSENT = new DarkState(false, -1F);

    /**
     * The state the player's client was last sent. Never saved nor copied, so a player who joins or
     * respawns is sent theirs afresh.
     */
    private static final Supplier<AttachmentType<DarkState>> SENT = ATTACHMENTS.register("sent_dark_state",
            () -> AttachmentType.builder(() -> UNSENT).build());

    private DarkCues() {
    }

    static void register(IEventBus modBus) {
        SOUNDS.register(modBus);
        ATTACHMENTS.register(modBus);
        modBus.addListener(DarkCues::registerPayloads);
    }

    /** With no handler here: the client registers its own, which the vignette is. */
    private static void registerPayloads(RegisterPayloadHandlersEvent event) {
        event.registrar("1").playToClient(DarkState.TYPE, DarkState.STREAM_CODEC);
    }

    /** Plays the tick's cue to the player. */
    static void play(ServerPlayer player, GracePeriod.Cue cue) {
        switch (cue) {
            case NONE -> {
            }
            case ENTER -> play(player, ENTER, SoundSource.AMBIENT);
            case WARNING -> play(player, WARNING, SoundSource.HOSTILE);
            case STRIKE -> play(player, STRIKE, SoundSource.HOSTILE);
        }
    }

    private static void play(ServerPlayer player, Holder<SoundEvent> sound, SoundSource source) {
        player.connection.send(new ClientboundSoundPacket(sound, source, player.getX(), player.getEyeY(), player.getZ(),
                1F, 1F, player.getRandom().nextLong()));
    }

    /** Sends the player's client their state if it changed, and if their client can read it. */
    static void send(ServerPlayer player, DarkState state) {
        if (state.equals(player.getData(SENT)) || !player.connection.hasChannel(DarkState.TYPE)) {
            return;
        }
        player.setData(SENT, state);
        player.connection.send(state);
    }

    private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
        return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, path)));
    }
}
