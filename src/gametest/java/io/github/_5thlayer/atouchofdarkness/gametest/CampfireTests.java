// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;
import static io.github._5thlayer.atouchofdarkness.gametest.LightFixture.*;

import java.util.List;

import io.github._5thlayer.atouchofdarkness.light.Light;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * The Campfire: crafted in vanilla's shape, placed already burning at half its cap, fed fuel by hand
 * up to it, its light falling as its fuel does, and burning by the time of day, loaded or not, until it
 * goes out. A test that moves the clock runs in a batch of its own. Every test's name starts
 * {@code campfire_}.
 */
final class CampfireTests {

    /** The soul Campfire's cap at the defaults. */
    private static final int SOUL_CAP = 2 * CAMPFIRE_CAP;

    /** Ticks short of a burn the clock is moved, and then past it, as the clock also runs a tick a tick. */
    private static final int MARGIN = 100;

    /** Ticks vanilla's campfire takes to cook raw beef. */
    private static final int COOKING = 600;

    private CampfireTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("campfire_crafted_and_placed_at_half_its_cap_and_fuel_adds_x10_up_to_it", 5, CampfireTests::fuel);
        tests.test("campfire_light_steps_15_12_9_6_soul_11_9_7_5_as_its_fuel_falls", 5, CampfireTests::steps);
        tests.test("campfire_cooks_food_while_burning", COOKING + 20, CampfireTests::cooks);
        tests.test("campfire_vanilla_campfires_become_campfires", 5, CampfireTests::vanillaCampfires);
        tests.in("campfire_goes_out", KeepsClock.INSTANCE)
                .test("campfire_goes_out_dry_over_a_night_slept_through_and_while_unloaded", 10, CampfireTests::goesOut);
        tests.in("campfire_rain", new AtNight(MoonPhase.FULL_MOON, Weather.Type.RAIN))
                .test("campfire_burns_twice_as_fast_in_rain_under_the_open_sky", 10, CampfireTests::rain);
    }

    /**
     * Each Campfire crafts in vanilla's shape. Placed by a player, it holds half its cap; a stick, of
     * 100 ticks in a furnace, adds 1000, and a coal, of 1600, fills it to the cap, the rest lost.
     */
    private static void fuel(GameTestHelper helper) {
        assertCrafts(helper, "campfire", List.of(Items.AIR, Items.STICK, Items.AIR, Items.STICK, Items.COAL, Items.STICK, Items.OAK_LOG, Items.OAK_LOG, Items.OAK_LOG),
                Light.CAMPFIRE_ITEM.get());
        assertCrafts(helper, "soul_campfire", List.of(Items.AIR, Items.STICK, Items.AIR, Items.STICK, Items.SOUL_SOIL, Items.STICK, Items.OAK_LOG, Items.OAK_LOG, Items.OAK_LOG),
                Light.SOUL_CAMPFIRE_ITEM.get());

        var player = survivalPlayerAt(helper, STAND);
        var campfire = new BlockPos(2, 1, 2);
        var soul = new BlockPos(6, 1, 2);
        place(helper, player, Light.CAMPFIRE_ITEM.get(), campfire);
        place(helper, player, Light.SOUL_CAMPFIRE_ITEM.get(), soul);
        assertFuel(helper, campfire, CAMPFIRE_CAP / 2);
        assertFuel(helper, soul, SOUL_CAP / 2);

        useOn(helper, player, new ItemStack(Items.STICK), campfire, Direction.UP);
        assertFuel(helper, campfire, CAMPFIRE_CAP / 2 + 1000);
        var coal = new ItemStack(Items.COAL, 2);
        useOn(helper, player, coal, campfire, Direction.UP);
        assertFuel(helper, campfire, CAMPFIRE_CAP);
        helper.assertTrue(coal.getCount() == 1, "fuelling took " + (2 - coal.getCount()) + " coal, not 1");
        leave(player);
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, String recipe, List<Item> grid, Item campfire) {
        var result = crafted(helper, recipe, 3, 3, grid);
        helper.assertTrue(result.is(campfire), recipe + " gives " + result);
    }

    /** The player places the item on the floor, so the block stands at {@code at}. */
    private static void place(GameTestHelper helper, ServerPlayer player, Item item, BlockPos at) {
        useOn(helper, player, new ItemStack(item), at.below(), Direction.UP);
        helper.assertBlockPresent(((BlockItem) item).getBlock(), at);
    }

    private static void assertFuel(GameTestHelper helper, BlockPos at, int left) {
        int fuel = LightFixture.fuel(helper, at);
        helper.assertTrue(fuel == left, "the Campfire at " + at + " holds " + fuel + " fuel, not " + left);
    }

    /**
     * Four Campfires and four soul Campfires, at their caps and at ¾, ½ and ¼ of them, burning on a tick
     * or two: 15, 12, 9 and 6, and the soul ones 11, 9, 7 and 5.
     */
    private static void steps(GameTestHelper helper) {
        int[] fractions = {4, 3, 2, 1};
        int[] lights = {15, 12, 9, 6};
        int[] soulLights = {11, 9, 7, 5};
        for (int i = 0; i < 4; i++) {
            campfire(helper, Light.CAMPFIRE.get(), new BlockPos(1 + 2 * i, 1, 2), CAMPFIRE_CAP * fractions[i] / 4);
            campfire(helper, Light.SOUL_CAMPFIRE.get(), new BlockPos(1 + 2 * i, 1, 6), SOUL_CAP * fractions[i] / 4);
        }
        helper.runAtTickTime(3, () -> {
            for (int i = 0; i < 4; i++) {
                assertLight(helper, new BlockPos(1 + 2 * i, 1, 2), lights[i]);
                assertLight(helper, new BlockPos(1 + 2 * i, 1, 6), soulLights[i]);
            }
            helper.succeed();
        });
    }

    /** Raw beef put on a burning Campfire by a player is cooked in vanilla's time. */
    private static void cooks(GameTestHelper helper) {
        var player = survivalPlayerAt(helper, STAND);
        var campfire = new BlockPos(4, 1, 2);
        helper.setBlock(campfire, Light.CAMPFIRE.get());
        useOn(helper, player, new ItemStack(Items.BEEF), campfire, Direction.UP);
        leave(player);
        helper.runAtTickTime(COOKING + 10, () -> {
            // Vanilla drops it with a random push.
            helper.assertItemEntityPresent(Items.COOKED_BEEF);
            helper.succeed();
        });
    }

    /** Three campfires and two soul campfires of vanilla's in a player's inventory become the Pillar's, stack for stack. */
    private static void vanillaCampfires(GameTestHelper helper) {
        var player = survivalPlayerAt(helper, STAND);
        player.getInventory().setItem(9, new ItemStack(Items.CAMPFIRE, 3));
        player.getInventory().setItem(10, new ItemStack(Items.SOUL_CAMPFIRE, 2));
        helper.runAtTickTime(3, () -> {
            var inventory = player.getInventory();
            helper.assertTrue(inventory.countItem(Items.CAMPFIRE) == 0 && inventory.countItem(Items.SOUL_CAMPFIRE) == 0,
                    "vanilla campfires are still in the inventory");
            helper.assertTrue(inventory.getItem(9).is(Light.CAMPFIRE_ITEM) && inventory.getItem(9).getCount() == 3,
                    "the campfires became " + inventory.getItem(9));
            helper.assertTrue(inventory.getItem(10).is(Light.SOUL_CAMPFIRE_ITEM) && inventory.getItem(10).getCount() == 2,
                    "the soul campfires became " + inventory.getItem(10));
            leave(player);
            helper.succeed();
        });
    }

    /**
     * Two Campfires placed with half their cap. One stays loaded while the clock moves on past its fuel,
     * as sleep does: it burns until then, and goes out. The other is unloaded meanwhile, saved and taken
     * away, then loaded again from its save: it goes out at once.
     */
    private static void goesOut(GameTestHelper helper) {
        var loaded = new BlockPos(2, 1, 4);
        var unloaded = new BlockPos(6, 1, 4);
        helper.setBlock(loaded, Light.CAMPFIRE.get());
        helper.setBlock(unloaded, Light.CAMPFIRE.get());
        var registries = helper.getLevel().registryAccess();
        var saved = new CompoundTag[1];
        helper.runAtTickTime(2, () -> {
            saved[0] = helper.getBlockEntity(unloaded, CampfireBlockEntity.class).saveWithFullMetadata(registries);
            helper.setBlock(unloaded, Blocks.AIR);
            KeepsClock.advance(helper, CAMPFIRE_CAP / 2 - MARGIN);
        });
        helper.runAtTickTime(4, () -> {
            assertLit(helper, loaded);
            helper.setBlock(unloaded, Light.CAMPFIRE.get());
            helper.getBlockEntity(unloaded, CampfireBlockEntity.class)
                    .loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved[0]));
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(6, () -> {
            assertOut(helper, loaded);
            assertOut(helper, unloaded);
            helper.succeed();
        });
    }

    /** At midnight in rain, a Campfire under the open sky burns out in half the time, one under a roof doesn't. */
    private static void rain(GameTestHelper helper) {
        var open = new BlockPos(1, 1, 1);
        // The framework roofs each test with barriers, which keep the rain off as any roof would.
        helper.setBlock(open.above(4), Blocks.AIR);
        var roofed = new BlockPos(6, 1, 6);
        helper.setBlock(open, Light.CAMPFIRE.get());
        helper.setBlock(roofed, Light.CAMPFIRE.get());
        helper.setBlock(roofed.above(), Blocks.STONE);
        helper.runAtTickTime(2, () -> KeepsClock.advance(helper, CAMPFIRE_CAP / 4 - MARGIN));
        helper.runAtTickTime(4, () -> {
            assertLit(helper, open);
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(6, () -> {
            assertOut(helper, open);
            assertLit(helper, roofed);
            helper.succeed();
        });
    }

    /** Lit, and giving this light. */
    private static void assertLight(GameTestHelper helper, BlockPos at, int light) {
        var state = helper.getBlockState(at);
        int given = state.getLightEmission(helper.getLevel(), helper.absolutePos(at));
        helper.assertTrue(state.getValue(CampfireBlock.LIT) && given == light, "the Campfire at " + at + " is " + state + ", giving " + given + ", not " + light);
    }

    private static void assertLit(GameTestHelper helper, BlockPos at) {
        var state = helper.getBlockState(at);
        helper.assertTrue(state.getValue(CampfireBlock.LIT) && state.getLightEmission(helper.getLevel(), helper.absolutePos(at)) > 0,
                "the Campfire at " + at + " went out: " + state);
    }

    /** Out, and giving no light. */
    private static void assertOut(GameTestHelper helper, BlockPos at) {
        var state = helper.getBlockState(at);
        helper.assertTrue(!state.getValue(CampfireBlock.LIT) && state.getLightEmission(helper.getLevel(), helper.absolutePos(at)) == 0,
                "the Campfire at " + at + " is still burning: " + state);
    }
}
