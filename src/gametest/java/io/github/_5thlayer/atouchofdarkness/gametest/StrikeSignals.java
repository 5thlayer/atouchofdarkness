// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import io.github._5thlayer.atouchof.signal.Strike;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;

/**
 * Listens for the Runtime's {@link Strike} signal as another Pillar would, and counts each player's,
 * so a test can read how many times its player was struck whatever the damage dealt.
 */
@EventBusSubscriber(modid = ATouchOfDarkness.MOD_ID)
final class StrikeSignals {

    /**
     * Raised on the server thread, which runs every test. Never cleared, since each test's player has
     * a UUID of its own.
     */
    private static final Map<UUID, Integer> HEARD = new HashMap<>();

    private StrikeSignals() {
    }

    @SubscribeEvent
    static void onStrike(Strike strike) {
        HEARD.merge(strike.player().getUUID(), 1, Integer::sum);
    }

    /** How many times the Strike signal has been raised for the player. */
    static int raisedFor(ServerPlayer player) {
        return HEARD.getOrDefault(player.getUUID(), 0);
    }
}
