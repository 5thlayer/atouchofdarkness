// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.List;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.light.Burning;
import io.github._5thlayer.atouchofdarkness.light.CarriedLightBlock;
import io.github._5thlayer.atouchofdarkness.light.HeldLightSource;
import io.github._5thlayer.atouchofdarkness.light.Light;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Torch: crafted burning, carried and never placed, burning by the time of day only while held, and
 * lighting the world at its carrier's eyes (ADR 0002) until it burns out into a Spent Torch, which relights
 * for a quarter of the time. Tests of a player in the Dark stand on the {@link DarkFixture}; a test that
 * moves the clock runs in a batch of its own. Every test's name starts {@code torch_}.
 */
final class TorchTests {

    /** A fresh Torch's burn at the defaults, 5 min of the time of day, and the soul Torch's. */
    private static final int BURN = 6000;
    private static final int SOUL_BURN = 2 * BURN;
    private static final int RELIT_BURN = BURN / 4;

    /** Ticks short of a burn the clock is moved, and then past it, as the clock also runs a tick a tick. */
    private static final int MARGIN = 100;

    private TorchTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("torch_recipe_gives_one_burning_torch_and_torches_never_stack", 1, TorchTests::recipe);
        tests.test("torch_held_keeps_its_carrier_lit_at_14_soul_10_and_its_light_follows", QUIET + 5, TorchTests::carried);
        tests.test("torch_goes_out_underwater", 10, TorchTests::underwater);
        tests.test("torch_light_goes_into_water_never_into_a_solid_block_and_keeps_monsters_away", 40, TorchTests::whereTheLightGoes);
        tests.test("torch_dropped_gives_no_light_and_an_orphaned_light_removes_itself", 60, TorchTests::orphans);
        tests.test("torch_neither_torch_nor_spent_torch_can_be_placed", 5, TorchTests::neverPlaced);
        tests.test("torch_tooltips_hint_and_count_down_with_advanced_tooltips", 1, TorchTests::tooltips);
        tests.test("torch_vanilla_torches_become_torches_and_broken_ones_drop_a_spent_torch", 10, TorchTests::vanillaTorches);
        tests.in("torch_burns_out", KeepsClock.INSTANCE)
                .test("torch_burns_out_over_5_min_held_soul_twice_as_long_and_never_elsewhere", 20, TorchTests::burnsOut);
        tests.in("torch_swapped", KeepsClock.INSTANCE)
                .test("torch_swapped_out_of_hand_keeps_the_burn_it_owed", 10, TorchTests::swappedOut);
        tests.in("torch_relit", KeepsClock.INSTANCE)
                .test("torch_spent_relit_by_each_means_burns_a_quarter_as_long_then_is_gone", 20, TorchTests::relit);
        tests.in("torch_rain", new AtNight(MoonPhase.FULL_MOON, Weather.Type.RAIN))
                .test("torch_burns_twice_as_fast_in_rain_under_the_open_sky", 20, TorchTests::rain);
    }

    /** Coal or charcoal over a stick, and over soul soil or soul sand for the soul Torch: one, burning, alone in its stack. */
    private static void recipe(GameTestHelper helper) {
        for (var coal : List.of(Items.COAL, Items.CHARCOAL)) {
            assertCrafts(helper, "torch", List.of(coal, Items.STICK), Light.TORCH.get(), Light.TORCH_LIGHT);
            for (var soul : List.of(Items.SOUL_SOIL, Items.SOUL_SAND)) {
                assertCrafts(helper, "soul_torch", List.of(coal, Items.STICK, soul), Light.SOUL_TORCH.get(), Light.SOUL_TORCH_LIGHT);
            }
        }
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, String recipe, List<Item> column, Item torch, int light) {
        var id = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, recipe));
        var holder = helper.getLevel().getServer().getRecipeManager().byKey(id);
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "no crafting recipe " + id.identifier());
        var crafting = (CraftingRecipe) holder.get().value();
        var input = CraftingInput.of(1, column.size(), column.stream().map(ItemStack::new).toList());
        helper.assertTrue(crafting.matches(input, helper.getLevel()), recipe + " doesn't take " + column);
        var result = crafting.assemble(input);
        helper.assertTrue(result.is(torch) && result.getCount() == 1, recipe + " from " + column + " gives " + result);
        helper.assertTrue(heldLight(result) == light, recipe + " gives a torch of light " + heldLight(result) + ", not " + light);
        helper.assertTrue(result.getMaxStackSize() == 1, recipe + " gives a torch that stacks to " + result.getMaxStackSize());
    }

    /**
     * Three players in the Dark's cage, holding a Torch, a soul Torch in the off hand, and one of each: never
     * struck, with the light at their eyes 14, 10 and 14. A fourth, in the open, steps two blocks on, and
     * their light goes with them.
     */
    private static void carried(GameTestHelper helper) {
        var torch = caged(helper, new BlockPos(2, 1, 2), 0);
        torch.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        var soul = caged(helper, new BlockPos(6, 1, 2), 0);
        soul.setItemInHand(InteractionHand.OFF_HAND, Light.SOUL_TORCH.toStack());
        var both = caged(helper, new BlockPos(2, 1, 6), 0);
        both.setItemInHand(InteractionHand.MAIN_HAND, Light.SOUL_TORCH.toStack());
        both.setItemInHand(InteractionHand.OFF_HAND, Light.TORCH.toStack());
        var from = new BlockPos(6, 1, 5);
        var to = new BlockPos(6, 1, 7);
        var walker = inTheOpen(helper, from);
        walker.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        helper.runAtTickTime(5, () -> {
            assertCarriedLight(helper, from.above(), Light.TORCH_LIGHT);
            standAt(helper, to, walker);
        });
        helper.runAtTickTime(10, () -> {
            assertCarriedLight(helper, to.above(), Light.TORCH_LIGHT);
            helper.assertBlockState(from.above(), state -> state.isAir(), state -> Component.literal("the light stayed behind as " + state));
        });
        helper.runAtTickTime(QUIET, () -> {
            assertCarriedLight(helper, new BlockPos(2, 2, 2), Light.TORCH_LIGHT);
            assertCarriedLight(helper, new BlockPos(6, 2, 2), Light.SOUL_TORCH_LIGHT);
            assertCarriedLight(helper, new BlockPos(2, 2, 6), Light.TORCH_LIGHT);
            for (var player : List.of(torch, soul, both)) {
                assertTaken(helper, player, QUIET, 0F);
                leave(player);
            }
            leave(walker);
            helper.succeed();
        });
    }

    /**
     * A player whose eyes are below the water's surface: the Torch in their hand goes out at once into a
     * Spent Torch, and the relit one in their other hand is gone, so relighting is never endless light.
     */
    private static void underwater(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        var relit = Light.TORCH.toStack();
        relit.set(Light.BURNING, Burning.relit(BURN));
        player.setItemInHand(InteractionHand.OFF_HAND, relit);
        helper.setBlock(EYES, Blocks.WATER);
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(player.getMainHandItem().is(Light.SPENT_TORCH), "underwater the Torch became " + player.getMainHandItem());
            helper.assertTrue(player.getOffhandItem().isEmpty(), "underwater the relit Torch became " + player.getOffhandItem());
            leave(player);
            helper.succeed();
        });
    }

    /**
     * One carrier whose eye block is water, though their eyes are above its surface, so the Torch burns:
     * the light goes into the water, which is there again once the Torch is put away. One whose eye block
     * is stone: it stays stone. And one in the Dark's cage: no monster could spawn at their feet.
     */
    private static void whereTheLightGoes(GameTestHelper helper) {
        var wet = new BlockPos(2, 1, 2);
        var swimmer = caged(helper, wet, 0);
        helper.setBlock(wet.above(), Blocks.WATER);
        // Eyes 1.62 above the feet, at 0.92 into a block whose water stands 0.89 high.
        var at = helper.absolutePos(wet);
        swimmer.snapTo(at.getX() + 0.5, at.getY() + 0.3, at.getZ() + 0.5);
        swimmer.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());

        var walled = new BlockPos(6, 1, 2);
        var stuck = caged(helper, walled, 0);
        helper.setBlock(walled.above(), Blocks.STONE);
        stuck.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());

        var spawner = caged(helper, new BlockPos(4, 1, 6), 0);
        var feet = helper.absolutePos(new BlockPos(4, 1, 6));
        helper.assertTrue(Monster.isDarkEnoughToSpawn(helper.getLevel(), feet, helper.getLevel().getRandom()),
                "a monster could not spawn in the cage before the Torch");
        spawner.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());

        helper.runAtTickTime(5, () -> {
            var light = helper.getBlockState(wet.above());
            helper.assertTrue(light.is(Light.CARRIED_LIGHT) && light.getValue(CarriedLightBlock.WATERLOGGED),
                    "the light in the water is " + light);
            helper.assertTrue(swimmer.getMainHandItem().is(Light.TORCH), "the Torch above the water became " + swimmer.getMainHandItem());
            helper.assertTrue(helper.getBlockState(walled.above()).is(Blocks.STONE), "the stone became " + helper.getBlockState(walled.above()));
            swimmer.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            settleLight(helper, new BlockPos(4, 2, 6));
            helper.assertFalse(Monster.isDarkEnoughToSpawn(helper.getLevel(), feet, helper.getLevel().getRandom()),
                    "a monster could spawn at the feet of a player holding a Torch");
        });
        helper.runAtTickTime(10, () -> {
            var water = helper.getBlockState(wet.above());
            helper.assertTrue(water.is(Blocks.WATER) && water.getFluidState().isSource(), "with the Torch put away the water became " + water);
            for (var player : List.of(swimmer, stuck, spawner)) {
                leave(player);
            }
            helper.succeed();
        });
    }

    /**
     * A Torch dropped on the ground gives no light, and a carried light with no carrier, one left
     * in the air, one in water, and one whose carrier logged out, is gone within its second's check.
     */
    private static void orphans(GameTestHelper helper) {
        var dropped = new BlockPos(4, 1, 4);
        helper.spawnItem(Light.TORCH.get(), dropped);
        var inAir = new BlockPos(2, 2, 2);
        helper.setBlock(inAir, Light.CARRIED_LIGHT.get().defaultBlockState().setValue(CarriedLightBlock.LEVEL, Light.TORCH_LIGHT));
        var inWater = new BlockPos(6, 1, 2);
        cage(helper, inWater.below());
        helper.setBlock(inWater, Light.CARRIED_LIGHT.get().defaultBlockState()
                .setValue(CarriedLightBlock.LEVEL, Light.TORCH_LIGHT).setValue(CarriedLightBlock.WATERLOGGED, true));
        var stand = new BlockPos(2, 1, 6);
        var leaver = inTheOpen(helper, stand);
        leaver.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        helper.runAtTickTime(3, () -> {
            assertCarriedLight(helper, stand.above(), Light.TORCH_LIGHT);
            leave(leaver);
        });
        helper.runAtTickTime(50, () -> {
            settleLight(helper, dropped);
            int light = helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(dropped));
            helper.assertTrue(light == 0, "a dropped Torch lit its block to " + light);
            helper.assertTrue(helper.getBlockState(inAir).isAir(), "the orphan in the air is still " + helper.getBlockState(inAir));
            var water = helper.getBlockState(inWater);
            helper.assertTrue(water.is(Blocks.WATER), "the orphan in water became " + water);
            helper.assertTrue(helper.getBlockState(stand.above()).isAir(), "the light of the player who left is still " + helper.getBlockState(stand.above()));
            helper.succeed();
        });
    }

    /** A Torch, a soul Torch and a Spent Torch used on the floor: nothing is placed. */
    private static void neverPlaced(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var floor = new BlockPos(5, 0, 4);
        for (var item : List.of(Light.TORCH.get(), Light.SOUL_TORCH.get(), Light.SPENT_TORCH.get())) {
            var stack = new ItemStack(item);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var hit = new BlockHitResult(Vec3.atCenterOf(helper.absolutePos(floor)).add(0, 0.5, 0), Direction.UP, helper.absolutePos(floor), false);
            player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
            helper.assertTrue(helper.getBlockState(floor.above()).isAir(), item + " placed " + helper.getBlockState(floor.above()));
        }
        leave(player);
        helper.succeed();
    }

    /**
     * A Torch's tooltip says it burns only while held, and with advanced tooltips (F3+H) how long it has
     * left: a fresh one its full 5 min, one burnt down 2 min 30 s, a fresh soul Torch 10 min. A Spent
     * Torch's says how to relight it.
     */
    private static void tooltips(GameTestHelper helper) {
        var burnt = Light.TORCH.toStack();
        burnt.set(Light.BURNING, new Burning(BURN / 2, BURN, false));
        assertTooltip(helper, Light.TORCH.toStack(), TooltipFlag.NORMAL, "Burns only while held", null);
        assertTooltip(helper, Light.TORCH.toStack(), TooltipFlag.ADVANCED, "Burns only while held", "5:00 of burn left");
        assertTooltip(helper, burnt, TooltipFlag.ADVANCED, "Burns only while held", "2:30 of burn left");
        assertTooltip(helper, Light.SOUL_TORCH.toStack(), TooltipFlag.ADVANCED, "Burns only while held", "10:00 of burn left");
        assertTooltip(helper, Light.SPENT_TORCH.toStack(), TooltipFlag.ADVANCED,
                "Relight with flint and steel, a fire charge, fire, lava or a Torch", null);
        helper.succeed();
    }

    /** The tooltip has the hint, and the countdown or, given none, no countdown at all. */
    private static void assertTooltip(GameTestHelper helper, ItemStack stack, TooltipFlag flag, String hint, @Nullable String countdown) {
        var lines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, flag).stream().map(Component::getString).toList();
        helper.assertTrue(lines.contains(hint), stack + "'s tooltip " + lines + " lacks \"" + hint + "\"");
        boolean counted = lines.stream().anyMatch(line -> line.endsWith("of burn left"));
        helper.assertTrue(countdown == null ? !counted : lines.contains(countdown), stack + "'s tooltip " + lines + " with " + flag + " should read " + countdown);
    }

    /**
     * Three torches and two soul torches of vanilla's in a player's inventory become three Torches and two
     * soul Torches, one to a slot; vanilla's torch, wall torch and soul torch broken each drop a Spent Torch.
     */
    private static void vanillaTorches(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        player.getInventory().setItem(9, new ItemStack(Items.TORCH, 3));
        player.getInventory().setItem(10, new ItemStack(Items.SOUL_TORCH, 2));
        var placed = List.of(new BlockPos(1, 1, 1), new BlockPos(7, 1, 1), new BlockPos(1, 1, 7));
        helper.setBlock(placed.get(0), Blocks.TORCH);
        helper.setBlock(new BlockPos(7, 1, 2), Blocks.STONE);
        helper.setBlock(placed.get(1), Blocks.WALL_TORCH.defaultBlockState().setValue(WallTorchBlock.FACING, Direction.NORTH));
        helper.setBlock(placed.get(2), Blocks.SOUL_TORCH);
        helper.runAtTickTime(3, () -> {
            var inventory = player.getInventory();
            helper.assertTrue(inventory.countItem(Items.TORCH) == 0 && inventory.countItem(Items.SOUL_TORCH) == 0,
                    "vanilla torches are still in the inventory");
            helper.assertTrue(inventory.countItem(Light.TORCH.get()) == 3, inventory.countItem(Light.TORCH.get()) + " Torches, not 3");
            helper.assertTrue(inventory.countItem(Light.SOUL_TORCH.get()) == 2, inventory.countItem(Light.SOUL_TORCH.get()) + " soul Torches, not 2");
            for (var pos : placed) {
                helper.assertTrue(!helper.getBlockState(pos).isAir(), "the torch at " + pos + " fell");
                helper.getLevel().destroyBlock(helper.absolutePos(pos), true);
                helper.assertItemEntityPresent(Light.SPENT_TORCH.get(), pos, 1.0);
            }
            helper.assertItemEntityNotPresent(Items.TORCH);
            helper.assertItemEntityNotPresent(Items.SOUL_TORCH);
            leave(player);
            helper.succeed();
        });
    }

    /**
     * Held, a Torch lasts 5 min of the time of day, moved on at once as sleep does, and a soul Torch twice
     * that, each ending as a Spent Torch. One in the inventory, a chest, on the ground or a Spectator's hand
     * hasn't burned at all.
     */
    private static void burnsOut(GameTestHelper helper) {
        var holder = caged(helper, new BlockPos(2, 1, 2), 0);
        holder.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        var soulHolder = caged(helper, new BlockPos(6, 1, 2), 0);
        soulHolder.setItemInHand(InteractionHand.MAIN_HAND, Light.SOUL_TORCH.toStack());
        holder.getInventory().setItem(9, Light.TORCH.toStack());
        var spectator = caged(helper, new BlockPos(4, 1, 4), 0);
        spectator.setGameMode(GameType.SPECTATOR);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        var chest = new BlockPos(6, 1, 6);
        helper.setBlock(chest, Blocks.CHEST);
        var chestEntity = helper.getBlockEntity(chest, ChestBlockEntity.class);
        chestEntity.setItem(0, Light.TORCH.toStack());
        var dropped = helper.spawnItem(Light.TORCH.get(), new BlockPos(2, 1, 6));

        helper.runAtTickTime(1, () -> KeepsClock.advance(helper, BURN - MARGIN));
        helper.runAtTickTime(3, () -> {
            assertHolds(helper, holder, Light.TORCH.get());
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(5, () -> {
            assertHolds(helper, holder, Light.SPENT_TORCH.get());
            assertHolds(helper, soulHolder, Light.SOUL_TORCH.get());
            for (var unburnt : List.of(holder.getInventory().getItem(9), chestEntity.getItem(0), dropped.getItem(), spectator.getMainHandItem())) {
                helper.assertTrue(unburnt.is(Light.TORCH) && !unburnt.has(Light.BURNING), "a Torch out of a player's hand burned: " + unburnt);
            }
            KeepsClock.advance(helper, SOUL_BURN - BURN - 3 * MARGIN);
        });
        helper.runAtTickTime(7, () -> {
            assertHolds(helper, soulHolder, Light.SOUL_TORCH.get());
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(9, () -> {
            assertHolds(helper, soulHolder, Light.SPENT_TORCH.get());
            leave(spectator);
            leave(holder);
            leave(soulHolder);
            helper.succeed();
        });
    }

    /**
     * A Torch held while the clock moves on less than a second, then swapped out of hand for the next
     * hotbar slot, still burned for that time: swapping it away never dodges its burn.
     */
    private static void swappedOut(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        helper.runAtTickTime(2, () -> KeepsClock.advance(helper, 10));
        helper.runAtTickTime(4, () -> player.getInventory().setSelectedSlot(1));
        helper.runAtTickTime(6, () -> {
            var burning = player.getInventory().getItem(0).get(Light.BURNING);
            helper.assertTrue(burning != null && burning.left() <= BURN - 10, "the Torch swapped out of hand burned " + burning);
            leave(player);
            helper.succeed();
        });
    }

    /**
     * Five Spent Torches, relit by flint and steel, a fire charge, a Torch in the other hand, fire and
     * lava: each a Torch of a quarter of a fresh one's burn, which then burns away to nothing.
     */
    private static void relit(GameTestHelper helper) {
        var flint = relighter(helper, new BlockPos(1, 1, 1), new ItemStack(Items.FLINT_AND_STEEL));
        var charge = relighter(helper, new BlockPos(1, 1, 4), new ItemStack(Items.FIRE_CHARGE, 2));
        var torch = relighter(helper, new BlockPos(1, 1, 7), Light.TORCH.toStack());
        var fire = relighter(helper, new BlockPos(4, 1, 1), ItemStack.EMPTY);
        var firePos = new BlockPos(5, 1, 1);
        helper.setBlock(firePos.below(), Blocks.NETHERRACK);
        helper.setBlock(firePos, Blocks.FIRE);
        fire.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atBottomCenterOf(helper.absolutePos(firePos)).add(0, 0.03, 0));
        var lava = relighter(helper, new BlockPos(4, 1, 6), ItemStack.EMPTY);
        var lavaPos = new BlockPos(5, 1, 6);
        helper.setBlock(lavaPos, Blocks.LAVA);
        lava.lookAt(EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(helper.absolutePos(lavaPos)));
        var relit = List.of(flint, charge, torch, fire, lava);

        for (var player : relit) {
            player.gameMode.useItem(player, helper.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND);
        }
        helper.setBlock(firePos, Blocks.AIR);
        helper.setBlock(lavaPos, Blocks.AIR);
        for (var player : relit) {
            var burning = player.getMainHandItem().get(Light.BURNING);
            helper.assertTrue(player.getMainHandItem().is(Light.TORCH) && burning != null && burning.relit() && burning.full() == RELIT_BURN,
                    "a Spent Torch relit became " + player.getMainHandItem() + " burning " + burning);
        }
        helper.assertTrue(flint.getOffhandItem().getDamageValue() == 1, "the flint and steel took " + flint.getOffhandItem().getDamageValue() + " wear");
        helper.assertTrue(charge.getOffhandItem().getCount() == 1, "the fire charges are " + charge.getOffhandItem().getCount() + ", not 1");

        helper.runAtTickTime(1, () -> KeepsClock.advance(helper, RELIT_BURN - MARGIN));
        helper.runAtTickTime(3, () -> {
            relit.forEach(player -> assertHolds(helper, player, Light.TORCH.get()));
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(5, () -> {
            relit.forEach(player -> helper.assertTrue(player.getMainHandItem().isEmpty(), "a relit Torch burned out into " + player.getMainHandItem()));
            assertHolds(helper, torch, InteractionHand.OFF_HAND, Light.TORCH.get());
            relit.forEach(DarkFixture::leave);
            helper.succeed();
        });
    }

    private static ServerPlayer relighter(GameTestHelper helper, BlockPos stand, ItemStack offHand) {
        var player = survivalPlayer(helper);
        standAt(helper, stand, player);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        player.setItemInHand(InteractionHand.OFF_HAND, offHand);
        return player;
    }

    /** At midnight in rain, a Torch held under the open sky burns out in half the time, one under a roof doesn't. */
    private static void rain(GameTestHelper helper) {
        var stand = new BlockPos(1, 1, 1);
        // The framework roofs each test with barriers, which keep the rain off as any roof would.
        helper.setBlock(stand.above(4), Blocks.AIR);
        var open = inTheOpen(helper, stand);
        open.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        var roofed = caged(helper, 0);
        roofed.setItemInHand(InteractionHand.MAIN_HAND, Light.TORCH.toStack());
        helper.runAtTickTime(1, () -> KeepsClock.advance(helper, BURN / 2 - MARGIN));
        helper.runAtTickTime(3, () -> {
            assertHolds(helper, open, Light.TORCH.get());
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(5, () -> {
            assertHolds(helper, open, Light.SPENT_TORCH.get());
            assertHolds(helper, roofed, Light.TORCH.get());
            leave(open);
            leave(roofed);
            helper.succeed();
        });
    }

    private static void assertHolds(GameTestHelper helper, ServerPlayer player, Item item) {
        assertHolds(helper, player, InteractionHand.MAIN_HAND, item);
    }

    private static void assertHolds(GameTestHelper helper, ServerPlayer player, InteractionHand hand, Item item) {
        var held = player.getItemInHand(hand);
        helper.assertTrue(held.is(item), "the player holds " + held + ", not " + item);
    }

    private static void assertCarriedLight(GameTestHelper helper, BlockPos pos, int light) {
        var state = helper.getBlockState(pos);
        helper.assertTrue(state.is(Light.CARRIED_LIGHT) && state.getValue(CarriedLightBlock.LEVEL) == light,
                "the carried light at " + pos + " is " + state + ", not " + light);
    }

    private static int heldLight(ItemStack stack) {
        return stack.getItem() instanceof HeldLightSource source ? source.heldLight(stack) : 0;
    }
}
