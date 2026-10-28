// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github._5thlayer.atouchof.meter.MeterPayload;
import io.github._5thlayer.atouchof.meter.MeterSync;
import io.github._5thlayer.atouchofdarkness.dial.LightReading;
import io.github._5thlayer.atouchofdarkness.dial.VesperDial;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * The Vesper Dial's meter: while a Dial is in either hand, the Pillar hands the Runtime the light at
 * the player's eyes, as the half steps of 15 the Runtime sends, and the Lit check's answer as its
 * state, and clears it once the Dial is in neither. The light and the Lit check are the Dark's own, tested there; what the Runtime draws is
 * checked by hand in the dev client, which no game test loads. Each test stands on the
 * {@link DarkFixture}, and every test's name starts {@code dial_}.
 */
final class DialTests {

    /** What the Runtime last told each player's client of the Dial's meter, until it was cleared. */
    private static final Map<UUID, MeterPayload> TOLD = new ConcurrentHashMap<>();

    static {
        MeterSync.onSend((player, payload) -> {
            if (payload.meter().equals(VesperDial.METER_ID)) {
                TOLD.put(player.getUUID(), payload);
            }
        });
        MeterSync.onClear((player, meter) -> {
            if (meter.equals(VesperDial.METER_ID)) {
                TOLD.remove(player.getUUID());
            }
        });
    }

    private DialTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("dial_meter_shows_the_light_and_lit_in_either_hand", 40, DialTests::eitherHand);
        tests.test("dial_meter_shows_in_the_dark_below_the_threshold", 20, DialTests::inTheDark);
        tests.test("dial_meter_is_cleared_once_the_dial_leaves_the_hands", 40, DialTests::cleared);
    }

    /** At light 10 in the main hand, 14 of the 20 half steps, Lit; moved to the off hand at 15, all 20, Lit. */
    private static void eitherHand(GameTestHelper helper) {
        var player = caged(helper, 10);
        player.setItemInHand(InteractionHand.MAIN_HAND, VesperDial.VESPER_DIAL.toStack());
        helper.startSequence()
                .thenWaitUntil(() -> expectTold(helper, player, 14, LightReading.LIT))
                .thenExecute(() -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    player.setItemInHand(InteractionHand.OFF_HAND, VesperDial.VESPER_DIAL.toStack());
                    light(helper, 15);
                })
                .thenWaitUntil(() -> expectTold(helper, player, 20, LightReading.LIT))
                .thenExecute(() -> leave(player))
                .thenSucceed();
    }

    /** At light 4, just below the threshold of 5: 6 half steps, In the Dark. */
    private static void inTheDark(GameTestHelper helper) {
        var player = caged(helper, 4);
        player.setItemInHand(InteractionHand.MAIN_HAND, VesperDial.VESPER_DIAL.toStack());
        helper.startSequence()
                .thenWaitUntil(() -> expectTold(helper, player, 6, LightReading.IN_THE_DARK))
                .thenExecute(() -> leave(player))
                .thenSucceed();
    }

    /** Shown while held, then put away into the inventory: the Runtime is told to stop drawing it. */
    private static void cleared(GameTestHelper helper) {
        var player = caged(helper, 10);
        player.setItemInHand(InteractionHand.MAIN_HAND, VesperDial.VESPER_DIAL.toStack());
        helper.startSequence()
                .thenWaitUntil(() -> expectTold(helper, player, 14, LightReading.LIT))
                .thenExecute(() -> {
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    // Past the hotbar: add() would take the empty selected slot, the main hand.
                    player.getInventory().setItem(Inventory.getSelectionSize(), VesperDial.VESPER_DIAL.toStack());
                })
                .thenWaitUntil(() -> helper.assertTrue(!TOLD.containsKey(player.getUUID()),
                        "with the Dial put away the meter still shows " + TOLD.get(player.getUUID())))
                .thenExecute(() -> leave(player))
                .thenSucceed();
    }

    private static void expectTold(GameTestHelper helper, ServerPlayer player, int halves, int state) {
        var told = TOLD.get(player.getUUID());
        helper.assertTrue(told != null && told.halves() == halves && told.state() == state && told.trend() == 0,
                "the meter was told " + told + ", not " + halves + " halves in state " + state + " with no trend");
    }
}
