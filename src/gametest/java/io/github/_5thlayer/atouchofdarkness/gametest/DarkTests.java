// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.Difficulty;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;

/**
 * The Dark: a survival player whose eyes are below the lit threshold loses their Grace Period, and is
 * struck when it runs out and every 10 s after, until they reach light. Each test shuts a player in a
 * stone cage on the platform, where neither sky nor block light reaches, and lights their eyes with
 * vanilla's light block at the level it needs. A player is given 100 health, so the Strikes it counts
 * never kill it. Every test's name starts {@code dark_}.
 */
final class DarkTests {

    /** Where the player stands, on the platform's stone floor. */
    private static final BlockPos STAND = new BlockPos(4, 1, 4);

    /** The block at the player's eyes, which the Dark reads. */
    private static final BlockPos EYES = STAND.above();

    private static final float HEALTH = 100F;

    /** When the first Strike lands at the defaults, a tick or two after the 10 s Grace Period, give or take the light settling. */
    private static final int BEFORE_FIRST = 195;
    private static final int AFTER_FIRST = 210;

    private DarkTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("dark_strikes_when_the_grace_period_ends_and_every_10_s", 425, DarkTests::strikesThenRepeats);
        tests.test("dark_light_below_the_threshold_strikes", AFTER_FIRST + 5, helper -> struckAtLight(helper, 2));
        tests.test("dark_light_at_the_threshold_never_strikes", 260, helper -> neverStruckAtLight(helper, 3));
        tests.test("dark_darkness_shorter_than_the_grace_period_never_strikes", 400, DarkTests::briefDarkness);
        tests.test("dark_flickering_light_refills_nothing", 330, DarkTests::flickering);
        tests.test("dark_strike_deals_10_on_normal", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 10F));
        tests.test("dark_armour_softens_a_strike", AFTER_FIRST + 5, DarkTests::armourSoftens);
        tests.test("dark_creative_player_is_never_struck", 260, helper -> neverStruckIn(helper, GameType.CREATIVE));
        tests.test("dark_spectator_is_never_struck", 260, helper -> neverStruckIn(helper, GameType.SPECTATOR));
        tests.test("dark_mobs_are_never_struck", 260, DarkTests::mobsNeverStruck);
        tests.test("dark_night_vision_changes_nothing", AFTER_FIRST + 5, DarkTests::nightVision);
        tests.test("dark_respawned_player_has_a_full_grace_period", 330, DarkTests::respawnRefills);
        tests.test("dark_death_message_names_the_dark", 260, DarkTests::deathMessage);
        tests.test("dark_tag_holds_the_overworld_nether_and_end", 1, DarkTests::defaultTag);

        tests.in("peaceful", new WithDifficulty(Difficulty.PEACEFUL))
                .test("dark_strike_deals_0_on_peaceful", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 0F));
        tests.in("easy", new WithDifficulty(Difficulty.EASY))
                .test("dark_strike_deals_5_on_easy", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 5F));
        tests.in("hard", new WithDifficulty(Difficulty.HARD))
                .test("dark_strike_deals_15_on_hard", AFTER_FIRST + 5, helper -> dealsOnTheDifficulty(helper, 15F));

        tests.withPack("overworld_not_dark")
                .test("dark_dimension_outside_the_tag_never_strikes", 260, helper -> neverStruckAtLight(helper, 0));

        var configured = tests.in("dark_config", WithDarkConfig.INSTANCE);
        configured.test("dark_config_sets_threshold_grace_period_damage_and_interval", 100, DarkTests::configuredStrikes);
        configured.test("dark_config_sets_the_refill_delay", 80, DarkTests::configuredRefill);
    }

    private static void strikesThenRepeats(GameTestHelper helper) {
        var player = caged(helper, 0);
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        expectStruck(helper, player, AFTER_FIRST, 10F);
        expectStruck(helper, player, BEFORE_FIRST + 200, 10F);
        finishAt(helper, player, AFTER_FIRST + 200, 20F);
    }

    private static void struckAtLight(GameTestHelper helper, int light) {
        var player = caged(helper, light);
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        finishAt(helper, player, AFTER_FIRST, 10F);
    }

    private static void neverStruckAtLight(GameTestHelper helper, int light) {
        finishAt(helper, caged(helper, light), 255, 0F);
    }

    /** 150 ticks in the Dark, 60 Lit, then 180 more in the Dark: the Grace Period refilled in between. */
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

    private static void neverStruckIn(GameTestHelper helper, GameType mode) {
        var player = caged(helper, 0);
        player.setGameMode(mode);
        finishAt(helper, player, 255, 0F);
    }

    private static void mobsNeverStruck(GameTestHelper helper) {
        cage(helper);
        var villager = helper.spawn(EntityType.VILLAGER, STAND);
        float health = villager.getHealth();
        helper.runAtTickTime(255, () -> {
            helper.assertTrue(villager.getHealth() == health, "a villager in the Dark went from " + health + " to " + villager.getHealth() + " health");
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
            standInTheCage(helper, reborn);
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

    private static void deathMessage(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setHealth(5F);
        helper.succeedWhen(() -> {
            helper.assertTrue(player.isDeadOrDying(), "the player is alive");
            var source = player.getLastDamageSource();
            helper.assertTrue(source != null && source.is(Dark.STRIKE), "the player died of " + source + ", not a Strike");
            String message = source.getLocalizedDeathMessage(player).getString();
            helper.assertTrue(message.contains("struck by the Dark"), "the death message reads \"" + message + "\"");
            leave(player);
        });
    }

    private static void defaultTag(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        for (var dimension : List.of(Level.OVERWORLD, Level.NETHER, Level.END)) {
            var level = server.getLevel(dimension);
            helper.assertTrue(level != null && level.dimensionTypeRegistration().is(Dark.DIMENSIONS),
                    dimension.identifier() + "'s dimension type is not in #" + Dark.DIMENSIONS.location());
        }
        helper.succeed();
    }

    /**
     * At light 4, below the configured threshold of 5: a Strike of 4 when the 40-tick Grace Period
     * runs out, and another 40 ticks later.
     */
    private static void configuredStrikes(GameTestHelper helper) {
        var player = caged(helper, 4);
        expectStruck(helper, player, WithDarkConfig.GRACE_PERIOD - 5, 0F);
        expectStruck(helper, player, WithDarkConfig.GRACE_PERIOD + 10, 4F);
        expectStruck(helper, player, WithDarkConfig.GRACE_PERIOD + WithDarkConfig.STRIKE_INTERVAL - 5, 4F);
        finishAt(helper, player, WithDarkConfig.GRACE_PERIOD + WithDarkConfig.STRIKE_INTERVAL + 10, 8F);
    }

    /** 30 ticks in the Dark, 15 Lit, then 30 more: the configured 10-tick refill delay refilled it. */
    private static void configuredRefill(GameTestHelper helper) {
        var player = caged(helper, 0);
        helper.runAtTickTime(30, () -> light(helper, 15));
        helper.runAtTickTime(45, () -> light(helper, 0));
        finishAt(helper, player, 75, 0F);
    }

    /** A survival player with 100 health, shut in the cage with their eyes at this light. */
    private static ServerPlayer caged(GameTestHelper helper, int light) {
        cage(helper);
        light(helper, light);
        var player = survivalPlayer(helper);
        standInTheCage(helper, player);
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        player.setHealth(HEALTH);
        return player;
    }

    /** Stone on every side of the player's two blocks, and above them; the platform is below. */
    private static void cage(GameTestHelper helper) {
        for (var side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(STAND.relative(side), Blocks.STONE);
            helper.setBlock(EYES.relative(side), Blocks.STONE);
        }
        helper.setBlock(EYES.above(), Blocks.STONE);
    }

    private static void light(GameTestHelper helper, int level) {
        helper.setBlock(EYES, level == 0 ? Blocks.AIR.defaultBlockState() : Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level));
    }

    private static void standInTheCage(GameTestHelper helper, ServerPlayer player) {
        var at = helper.absolutePos(STAND);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    /**
     * A player on the server, in survival. {@link GameTestHelper#makeMockServerPlayerInLevel} makes one
     * that is always in Creative, which the Dark never strikes.
     */
    private static ServerPlayer survivalPlayer(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "dark-test-player"), false);
        var player = new ServerPlayer(server, helper.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        server.getPlayerList().placeNewPlayer(connection, player, cookie);
        // As a client says once it has loaded the world; till then, or 60 ticks, nothing hurts the player.
        player.connection.markClientLoaded();
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    /** At the tick, the player has taken this much from Strikes. */
    private static void expectStruck(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        helper.runAtTickTime(tick, () -> assertTaken(helper, player, tick, taken));
    }

    private static void finishAt(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        helper.runAtTickTime(tick, () -> {
            assertTaken(helper, player, tick, taken);
            leave(player);
            helper.succeed();
        });
    }

    private static void assertTaken(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        float actual = HEALTH - player.getHealth();
        helper.assertTrue(actual == taken, "by tick " + tick + " the player took " + actual + ", not " + taken);
    }

    /** Takes the player off the server, so the Dark stops ticking them once their test is done. */
    private static void leave(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
