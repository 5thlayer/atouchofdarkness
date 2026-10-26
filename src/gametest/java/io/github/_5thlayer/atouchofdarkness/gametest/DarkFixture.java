// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LightBlock;

/**
 * The shared fixture of the tests of a player in the Dark: a survival player with 100 health, so the
 * Strikes a test counts never kill it, standing on the platform, shut in a stone cage where neither sky
 * nor block light reaches, with their eyes lit by vanilla's light block at the level a test needs, or
 * else under the open sky. The light engine is settled after each block placed.
 */
final class DarkFixture {

    /** Where the player stands, on the platform's stone floor. */
    static final BlockPos STAND = new BlockPos(4, 1, 4);

    /** The block at the player's eyes, which the Dark reads. */
    static final BlockPos EYES = STAND.above();

    static final float HEALTH = 100F;

    /** When the first Strike lands at the defaults, a tick or two after the 10 s Grace Period. */
    static final int BEFORE_FIRST = 195;
    static final int AFTER_FIRST = 210;

    /** Long enough for a Strike at the defaults to have landed, had one been coming. */
    static final int QUIET = 255;

    private DarkFixture() {
    }

    /** A survival player with 100 health, shut in the cage with their eyes at this light. */
    static ServerPlayer caged(GameTestHelper helper, int light) {
        return caged(helper, STAND, light);
    }

    static ServerPlayer caged(GameTestHelper helper, BlockPos stand, int light) {
        cage(helper, stand);
        light(helper, stand.above(), light);
        return standing(helper, stand);
    }

    /** A survival player with 100 health on the platform under the open sky, with no block light at their eyes. */
    static ServerPlayer inTheOpen(GameTestHelper helper, BlockPos stand) {
        settleLight(helper, stand);
        return standing(helper, stand);
    }

    private static ServerPlayer standing(GameTestHelper helper, BlockPos stand) {
        var player = survivalPlayer(helper);
        standAt(helper, stand, player);
        withHealth(player);
        return player;
    }

    /** Gives the player 100 health. */
    static void withHealth(ServerPlayer player) {
        player.getAttribute(Attributes.MAX_HEALTH).setBaseValue(HEALTH);
        player.setHealth(HEALTH);
    }

    /** Stone on every side of the two blocks over the stand, and above them; the platform is below. */
    static void cage(GameTestHelper helper, BlockPos stand) {
        for (var side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(stand.relative(side), Blocks.STONE);
            helper.setBlock(stand.above().relative(side), Blocks.STONE);
        }
        helper.setBlock(stand.above(2), Blocks.STONE);
        settleLight(helper, stand);
    }

    static void light(GameTestHelper helper, int level) {
        light(helper, EYES, level);
    }

    static void light(GameTestHelper helper, BlockPos eyes, int level) {
        helper.setBlock(eyes, level == 0 ? Blocks.AIR.defaultBlockState() : Blocks.LIGHT.defaultBlockState().setValue(LightBlock.LEVEL, level));
        settleLight(helper, eyes);
    }

    /**
     * Waits for the light engine to light what was just placed. It runs on a thread of its own, and the
     * game-test server ticks faster than 20 a second, so a block's light would otherwise show a varying
     * number of ticks late, and a test of the refill delay would read a shorter spell of light.
     */
    static void settleLight(GameTestHelper helper, BlockPos pos) {
        settleLight(helper.getLevel(), helper.absolutePos(pos));
    }

    /** The same, in any level, at a position in its own coordinates. */
    static void settleLight(ServerLevel level, BlockPos at) {
        var engine = level.getChunkSource().getLightEngine();
        var settled = engine.waitForPendingTasks(SectionPos.blockToSectionCoord(at.getX()), SectionPos.blockToSectionCoord(at.getZ()));
        // The engine updates in batches, each scheduled from here.
        while (!settled.isDone()) {
            engine.tryScheduleUpdate();
            Thread.onSpinWait();
        }
    }

    static void standAt(GameTestHelper helper, BlockPos stand, ServerPlayer player) {
        var at = helper.absolutePos(stand);
        player.snapTo(at.getX() + 0.5, at.getY(), at.getZ() + 0.5);
    }

    /**
     * A player on the server, in survival. {@link GameTestHelper#makeMockServerPlayerInLevel} makes one
     * that is always in Creative, which the Dark never strikes.
     */
    static ServerPlayer survivalPlayer(GameTestHelper helper) {
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
    static void expectStruck(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        helper.runAtTickTime(tick, () -> assertTaken(helper, player, tick, taken));
    }

    static void finishAt(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        helper.runAtTickTime(tick, () -> {
            assertTaken(helper, player, tick, taken);
            leave(player);
            helper.succeed();
        });
    }

    static void assertTaken(GameTestHelper helper, ServerPlayer player, int tick, float taken) {
        float actual = HEALTH - player.getHealth();
        helper.assertTrue(actual == taken, "by tick " + tick + " the player took " + actual + ", not " + taken);
    }

    /** Takes the player off the server, so the Dark stops ticking them once their test is done. */
    static void leave(ServerPlayer player) {
        player.level().getServer().getPlayerList().remove(player);
    }
}
