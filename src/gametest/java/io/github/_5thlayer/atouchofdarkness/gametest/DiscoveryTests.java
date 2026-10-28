// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.caged;
import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.leave;

import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import io.github._5thlayer.atouchof.discovery.DiscoverySync;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.discovery.Discoveries;
import io.github._5thlayer.atouchofdarkness.lampad.Lampad;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;

/**
 * Discovery (suite ADR-0006): each of the Pillar's six Discoveries, the advancement
 * {@code atouchofdarkness:discovery/<id>}, is done the first time its player meets what discovers it,
 * and for no one else. A pack that removes one finds it known, and with the {@code discovery} config off
 * everything is. What the server tells the client is what its EMI pages read.
 */
final class DiscoveryTests {

    /** What the server last told each player's client is known of this Pillar's things. */
    private static final Map<UUID, Set<String>> TOLD = new ConcurrentHashMap<>();

    private static final BlockPos MEETS = new BlockPos(2, 1, 2);
    private static final BlockPos STAYS_LIT = new BlockPos(6, 1, 2);
    private static final BlockPos VOTIVE = new BlockPos(4, 1, 6);

    static {
        DiscoverySync.onSend((player, payload) -> {
            if (payload.namespace().equals(ATouchOfDarkness.MOD_ID)) {
                TOLD.put(player.getUUID(), payload.known());
            }
        });
    }

    private DiscoveryTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        // A short Grace Period, so the Strike comes soon.
        tests.in("discovery_quick_strike", WithDarkConfig.INSTANCE)
                .test("discovery_each_is_done_the_first_time_its_player_meets_it_and_for_no_one_else", 200, DiscoveryTests::eachMet);
        tests.withPack("discovery_removed")
                .test("discovery_a_removed_advancement_counts_as_known", 1, DiscoveryTests::removed);
        tests.in("discovery_off", WithDiscoveryOff.INSTANCE)
                .test("discovery_switched_off_everything_is_known", 1, DiscoveryTests::switchedOff);
    }

    /**
     * One player in the Dark holds a Lamp, then two Spent Torches, then one, which darkens; the Dark
     * strikes them; they hold a Nightbrand, then summon a Lampad. Each Discovery is done only once its
     * thing is met. The other, Lit, meets nothing and discovers nothing.
     */
    private static void eachMet(GameTestHelper helper) {
        var player = caged(helper, MEETS, 0);
        var other = caged(helper, STAYS_LIT, 15);
        for (var id : Discoveries.IDS) {
            expectKnown(helper, player, id, false);
        }
        helper.setBlock(VOTIVE, Blocks.GOLD_BLOCK);
        LampTests.lamp(helper, Light.LAMP.get(), VOTIVE.above(), 0);
        helper.startSequence()
                .thenExecute(() -> player.getInventory().add(Light.LAMP_ITEM.toStack()))
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.FUEL, true))
                .thenExecute(() -> {
                    expectKnown(helper, player, Discoveries.SPENT_TORCH, false);
                    // Two in a hand never darken, so a few ticks of them discover no Nightbrand.
                    player.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack(2));
                })
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.SPENT_TORCH, true))
                .thenExecuteAfter(5, () -> {
                    expectKnown(helper, player, Discoveries.NIGHTBRAND, false);
                    player.getMainHandItem().setCount(1);
                })
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.NIGHTBRAND, true))
                .thenExecute(() -> {
                    helper.assertTrue(player.getMainHandItem().has(Nightbrand.DARKENING), "the torch had not begun darkening");
                    // Put away before it becomes a Nightbrand, which would discover the Votive.
                    player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                })
                .thenWaitUntil(() -> helper.assertTrue(player.getHealth() < DarkFixture.HEALTH, "the Dark has not struck"))
                .thenExecute(() -> expectKnown(helper, player, Discoveries.DARK, true))
                .thenExecute(() -> {
                    expectKnown(helper, player, Discoveries.VOTIVE, false);
                    player.getInventory().add(Nightbrand.NIGHTBRAND.toStack());
                })
                .thenWaitUntil(() -> expectKnown(helper, player, Discoveries.VOTIVE, true))
                .thenExecute(() -> {
                    expectKnown(helper, player, Discoveries.LAMPAD, false);
                    LampTests.useOn(helper, player, Light.TORCH.toStack(), VOTIVE.above());
                    helper.assertBlockPresent(Lampad.VOTIVE.get(), VOTIVE);
                    expectKnown(helper, player, Discoveries.LAMPAD, true);
                    for (var id : Discoveries.IDS) {
                        expectKnown(helper, other, id, false);
                    }
                    expectTold(helper, player, Set.copyOf(Discoveries.IDS));
                    leave(player);
                    leave(other);
                })
                .thenSucceed();
    }

    /** The pack removes the Lampad's advancement: she is known from the start, and nothing else is. */
    private static void removed(GameTestHelper helper) {
        var lampad = Discoveries.record().advancementId(Discoveries.LAMPAD);
        helper.assertTrue(helper.getLevel().getServer().getAdvancements().get(lampad) == null, "the pack did not remove " + lampad);
        var player = caged(helper, MEETS, 15);
        expectTold(helper, player, Set.of(Discoveries.LAMPAD));
        leave(player);
        helper.succeed();
    }

    /** With Discovery off, a new player knows every thing, and their client is told so. */
    private static void switchedOff(GameTestHelper helper) {
        var player = caged(helper, MEETS, 15);
        for (var id : Discoveries.IDS) {
            expectKnown(helper, player, id, true);
        }
        expectTold(helper, player, Set.copyOf(Discoveries.IDS));
        leave(player);
        helper.succeed();
    }

    private static void expectTold(GameTestHelper helper, ServerPlayer player, Set<String> known) {
        var told = TOLD.get(player.getUUID());
        helper.assertTrue(known.equals(told), "the client was told " + told + ", not " + known);
    }

    private static void expectKnown(GameTestHelper helper, ServerPlayer player, String id, boolean known) {
        helper.assertTrue(Discoveries.record().isKnown(player, id) == known, id + (known ? " is not known" : " is known too soon"));
    }
}
