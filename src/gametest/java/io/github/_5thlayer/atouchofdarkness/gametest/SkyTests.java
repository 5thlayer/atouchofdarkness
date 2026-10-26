// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.portal.TeleportTransition;
import net.minecraft.world.phys.Vec3;

/**
 * The sky keeps a player Lit beyond the light at their eyes: on a clear night under a full moon, and
 * always under the sky of a dimension tagged {@code atouchofdarkness:sky_always_lit}, the End's by
 * default, but never under a roof. The night sky reads 4, below the threshold of 5, so under any other
 * sky a player is in the Dark. Every test's name starts {@code sky_}.
 */
final class SkyTests {

    /** Under the open sky, clear of the cage at {@link DarkFixture#STAND}. */
    private static final BlockPos OPEN = new BlockPos(1, 1, 1);

    /**
     * Where the End's tests build, over the void between the main island and the outer ones, far from
     * the dragon.
     */
    private static final BlockPos END_OPEN = new BlockPos(0, 100, 600);
    private static final BlockPos END_ROOFED = END_OPEN.east(4);

    private SkyTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.in("full_moon", new AtNight(MoonPhase.FULL_MOON, Weather.Type.CLEAR))
                .test("sky_full_moon_lights_the_open_sky_but_not_under_a_roof", QUIET + 5, SkyTests::fullMoon);
        tests.in("full_moon_rain", new AtNight(MoonPhase.FULL_MOON, Weather.Type.RAIN))
                .test("sky_rain_hides_the_full_moon", AFTER_FIRST + 5, helper -> struckInTheOpen(helper));
        tests.in("waning_gibbous", new AtNight(MoonPhase.WANING_GIBBOUS, Weather.Type.CLEAR))
                .test("sky_another_moon_lights_nothing", AFTER_FIRST + 5, helper -> struckInTheOpen(helper));
        tests.test("sky_end_open_sky_is_lit_but_not_under_a_roof", QUIET + 5, SkyTests::end);
        tests.in("new_moon_overworld_sky_always_lit", new WithGameTestPack("overworld_sky_always_lit"), new AtNight(MoonPhase.NEW_MOON, Weather.Type.CLEAR))
                .test("sky_pack_adds_a_dimension_to_sky_always_lit", QUIET + 5, helper -> finishAt(helper, inTheOpen(helper, OPEN), QUIET, 0F));
    }

    /** The player under the full moon is never struck, the one in the cage beside them is. */
    private static void fullMoon(GameTestHelper helper) {
        var open = inTheOpen(helper, OPEN);
        var roofed = caged(helper, 0);
        helper.runAtTickTime(QUIET, () -> {
            assertTaken(helper, open, QUIET, 0F);
            assertTaken(helper, roofed, QUIET, 10F);
            leave(open);
            leave(roofed);
            helper.succeed();
        });
    }

    private static void struckInTheOpen(GameTestHelper helper) {
        var player = inTheOpen(helper, OPEN);
        expectStruck(helper, player, BEFORE_FIRST, 0F);
        finishAt(helper, player, AFTER_FIRST, 10F);
    }

    /** One player under the End's open sky, one under a roof beside them, both on stone over the void. */
    private static void end(GameTestHelper helper) {
        var end = helper.getLevel().getServer().getLevel(Level.END);
        helper.assertTrue(end != null, "the server has no End");
        floor(end, END_OPEN);
        floor(end, END_ROOFED);
        for (var side : Direction.Plane.HORIZONTAL) {
            end.setBlockAndUpdate(END_ROOFED.relative(side), Blocks.STONE.defaultBlockState());
            end.setBlockAndUpdate(END_ROOFED.above().relative(side), Blocks.STONE.defaultBlockState());
        }
        end.setBlockAndUpdate(END_ROOFED.above(2), Blocks.STONE.defaultBlockState());
        settleLight(end, END_OPEN);
        var open = inTheEnd(helper, end, END_OPEN);
        var roofed = inTheEnd(helper, end, END_ROOFED);
        helper.runAtTickTime(QUIET, () -> {
            assertTaken(helper, open, QUIET, 0F);
            assertTaken(helper, roofed, QUIET, 10F);
            leave(open);
            leave(roofed);
            helper.succeed();
        });
    }

    private static void floor(ServerLevel level, BlockPos stand) {
        level.setBlockAndUpdate(stand.below(), Blocks.STONE.defaultBlockState());
    }

    private static ServerPlayer inTheEnd(GameTestHelper helper, ServerLevel end, BlockPos stand) {
        var player = survivalPlayer(helper);
        player.teleport(new TeleportTransition(end, Vec3.atBottomCenterOf(stand), Vec3.ZERO, 0F, 0F, TeleportTransition.DO_NOTHING));
        // As a client says once it has arrived; till then nothing hurts the player.
        player.hasChangedDimension();
        withHealth(player);
        return player;
    }
}
