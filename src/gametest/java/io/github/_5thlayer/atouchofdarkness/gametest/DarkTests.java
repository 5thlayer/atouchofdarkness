// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.concurrent.atomic.AtomicReference;

import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.github._5thlayer.atouchofdarkness.dark.StrikeDeathMessage;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.CombatEntry;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

/**
 * The Dark: a survival player whose eyes are below the lit threshold loses their Grace Period, and is
 * struck when it runs out and every 10 s after, until they reach light. Each test stands on the
 * {@link DarkFixture}. Every test's name starts {@code dark_}.
 */
final class DarkTests {

    private DarkTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("dark_strikes_below_the_threshold_when_the_grace_period_ends_and_every_10_s", AFTER_FIRST + 205, DarkTests::strikesThenRepeats);
        tests.test("dark_light_at_the_threshold_never_strikes", QUIET + 5, DarkTests::neverStruckAtTheThreshold);
        tests.test("dark_darkness_shorter_than_the_grace_period_never_strikes", 400, DarkTests::briefDarkness);
        tests.test("dark_flickering_light_refills_nothing", 330, DarkTests::flickering);
        tests.test("dark_armour_softens_a_strike", AFTER_FIRST + 5, DarkTests::armourSoftens);
        tests.test("dark_never_strikes_creative_spectator_or_mobs", QUIET + 5, DarkTests::exempt);
        tests.test("dark_night_vision_changes_nothing", AFTER_FIRST + 5, DarkTests::nightVision);
        tests.test("dark_respawned_player_has_a_full_grace_period", 330, DarkTests::respawnRefills);
        tests.test("dark_death_message_names_the_dark", QUIET + 5, DarkTests::deathMessage);

        tests.in("peaceful", new WithDifficulty(Difficulty.PEACEFUL))
                .test("dark_strike_deals_0_on_peaceful", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 0F));
        tests.in("easy", new WithDifficulty(Difficulty.EASY))
                .test("dark_strike_deals_5_on_easy", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 5F));
        tests.in("hard", new WithDifficulty(Difficulty.HARD))
                .test("dark_strike_deals_15_on_hard", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 15F));

        tests.withPack("overworld_not_dark")
                .test("dark_dimension_outside_the_tag_never_strikes", QUIET + 5, helper -> finishAt(helper, caged(helper, 0), QUIET, 0F));

        var configured = tests.in("dark_config", WithDarkConfig.INSTANCE);
        configured.test("dark_config_sets_threshold_grace_period_damage_and_interval", 100, DarkTests::configuredStrikes);
        configured.test("dark_config_sets_the_refill_delay", 90, DarkTests::configuredRefill);
    }

    /** At light 4, just below the threshold of 5, and on Normal: 10 when the Grace Period ends, and 10 more 10 s later. */
    private static void strikesThenRepeats(GameTestHelper helper) {
        var player = caged(helper, 4);
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        expectStruck(helper, player, AFTER_FIRST, 10F);
        expectStruck(helper, player, BEFORE_FIRST + 200, 10F);
        finishAt(helper, player, AFTER_FIRST + 200, 20F);
    }

    private static void neverStruckAtTheThreshold(GameTestHelper helper) {
        finishAt(helper, caged(helper, 5), QUIET, 0F);
    }

    private static void briefDarkness(GameTestHelper helper) {
        var player = caged(helper, 0);
        helper.runAtTickTime(150, () -> light(helper, 15));
        helper.runAtTickTime(210, () -> light(helper, 0));
        finishAt(helper, player, 395, 0F);
    }

    /**
     * 150 ticks in the Dark, then Lit for 20 and dark for 10, five times over: never Lit for 2 s, so
     * the Grace Period runs out on the last dark spell, 200 dark ticks in.
     */
    private static void flickering(GameTestHelper helper) {
        var player = caged(helper, 0);
        for (int spell = 0; spell < 5; spell++) {
            int lit = 150 + spell * 30;
            helper.runAtTickTime(lit, () -> light(helper, 15));
            helper.runAtTickTime(lit + 20, () -> light(helper, 0));
        }
        expectStruck(helper, player, 280, 0F);
        finishAt(helper, player, 315, 10F);
    }

    private static void dealsOnTheDifficulty(GameTestHelper helper, float damage) {
        var player = caged(helper, 0);
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        finishAt(helper, player, AFTER_FIRST, damage);
    }

    private static void armourSoftens(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemSlot(EquipmentSlot.HEAD, new ItemStack(Items.DIAMOND_HELMET));
        player.setItemSlot(EquipmentSlot.CHEST, new ItemStack(Items.DIAMOND_CHESTPLATE));
        player.setItemSlot(EquipmentSlot.LEGS, new ItemStack(Items.DIAMOND_LEGGINGS));
        player.setItemSlot(EquipmentSlot.FEET, new ItemStack(Items.DIAMOND_BOOTS));
        // The armour counts from the player's next tick, which only a client's connection runs.
        player.doTick();
        helper.runAtTickTime(AFTER_FIRST, () -> {
            float taken = HEALTH - player.getHealth();
            helper.assertTrue(taken > 0F && taken < 10F, "a player in diamond armour took " + taken + " from a Strike, not between 0 and 10");
            leave(player);
            helper.succeed();
        });
    }

    /** A Creative player, a Spectator and a villager, each in a cage of their own. */
    private static void exempt(GameTestHelper helper) {
        var creative = caged(helper, new BlockPos(2, 1, 2), 0);
        creative.setGameMode(GameType.CREATIVE);
        var spectator = caged(helper, new BlockPos(6, 1, 2), 0);
        spectator.setGameMode(GameType.SPECTATOR);
        var villagerStand = new BlockPos(2, 1, 6);
        cage(helper, villagerStand);
        var villager = helper.spawn(EntityType.VILLAGER, villagerStand);
        float villagerHealth = villager.getHealth();
        helper.runAtTickTime(QUIET, () -> {
            assertTaken(helper, creative, QUIET, 0F);
            assertTaken(helper, spectator, QUIET, 0F);
            helper.assertTrue(villager.getHealth() == villagerHealth,
                    "a villager in the Dark went from " + villagerHealth + " to " + villager.getHealth() + " health");
            leave(creative);
            leave(spectator);
            helper.succeed();
        });
    }

    private static void nightVision(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, 20 * 60));
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        finishAt(helper, player, AFTER_FIRST, 10F);
    }

    /**
     * A player 100 ticks into the Dark dies and respawns into it. Had the Grace Period carried over, the
     * Strike would land 100 ticks later; a full one holds for 200.
     */
    private static void respawnRefills(GameTestHelper helper) {
        var player = caged(helper, 0);
        var respawned = new AtomicReference<ServerPlayer>();
        helper.runAtTickTime(100, () -> {
            var level = helper.getLevel();
            player.hurtServer(level, level.damageSources().genericKill(), Float.MAX_VALUE);
            helper.assertTrue(player.isDeadOrDying(), "the player survived being killed");
            // The client's Respawn button.
            player.connection.handleClientCommand(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.PERFORM_RESPAWN));
            var reborn = level.getServer().getPlayerList().getPlayer(player.getUUID());
            helper.assertTrue(reborn != null && reborn != player, "the player did not respawn");
            reborn.connection.markClientLoaded();
            standAt(helper, STAND, reborn);
            respawned.set(reborn);
        });
        helper.runAtTickTime(250, () -> {
            var reborn = respawned.get();
            helper.assertTrue(reborn.getHealth() == reborn.getMaxHealth(),
                    "the respawned player was struck " + (reborn.getMaxHealth() - reborn.getHealth()) + " within 150 ticks");
        });
        helper.runAtTickTime(100 + AFTER_FIRST, () -> {
            var reborn = respawned.get();
            float taken = reborn.getMaxHealth() - reborn.getHealth();
            helper.assertTrue(taken == 10F, "the respawned player took " + taken + " by the end of a full Grace Period, not 10");
            leave(reborn);
            helper.succeed();
        });
    }

    /** The Strike that kills reads one of the death messages, and every one of them, fleeing or not, names the Babau. */
    private static void deathMessage(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setHealth(5F);
        helper.startSequence().thenWaitUntil(() -> helper.assertTrue(player.isDeadOrDying(), "the player is alive")).thenExecute(() -> {
            var source = player.getLastDamageSource();
            helper.assertTrue(source != null && source.is(Dark.STRIKE), "the player died of " + source + ", not a Strike");
            // The player's combat log is cleared as they die, so the damage type's message is asked again.
            assertNamesTheBabau(helper, source.type().deathMessageType().getMessageFunction()
                    .getDeathMessage(player, new CombatEntry(source, 10F, null, 0F), null));
            for (int variant = 1; variant <= StrikeDeathMessage.VARIANTS; variant++) {
                assertNamesTheBabau(helper, StrikeDeathMessage.message(player, variant));
            }
            var zombie = helper.spawn(EntityType.ZOMBIE, STAND.east(3));
            player.setLastHurtByMob(zombie);
            for (int variant = 1; variant <= StrikeDeathMessage.VARIANTS; variant++) {
                var fleeing = StrikeDeathMessage.message(player, variant).getString();
                assertNamesTheBabau(helper, StrikeDeathMessage.message(player, variant));
                helper.assertTrue(fleeing.contains("Zombie"), "the fleeing death message reads \"" + fleeing + "\"");
            }
            zombie.discard();
            leave(player);
        }).thenSucceed();
    }

    private static void assertNamesTheBabau(GameTestHelper helper, Component message) {
        String text = message.getString();
        helper.assertTrue(text.contains("the Babau") || text.startsWith("The Babau"), "the death message reads \"" + text + "\"");
    }

    /**
     * At light 7, below the configured threshold of 8 and above the default 5: a Strike of 4 when the
     * 40-tick Grace Period runs out, and another 40 ticks later.
     */
    private static void configuredStrikes(GameTestHelper helper) {
        var player = caged(helper, 7);
        expectStruck(helper, player, WithDarkConfig.SET.gracePeriod() - 5, 0F);
        expectStruck(helper, player, WithDarkConfig.SET.gracePeriod() + 10, 4F);
        expectStruck(helper, player, WithDarkConfig.SET.gracePeriod() + WithDarkConfig.SET.strikeInterval() - 5, 4F);
        finishAt(helper, player, WithDarkConfig.SET.gracePeriod() + WithDarkConfig.SET.strikeInterval() + 10, 8F);
    }

    /**
     * 30 ticks in the Dark, 25 Lit, then 30 more: the configured 10-tick refill delay refilled it, where
     * the default 40 would not have.
     */
    private static void configuredRefill(GameTestHelper helper) {
        var player = caged(helper, 0);
        helper.runAtTickTime(30, () -> light(helper, 15));
        helper.runAtTickTime(55, () -> light(helper, 0));
        finishAt(helper, player, 85, 0F);
    }
}
