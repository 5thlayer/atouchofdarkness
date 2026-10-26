// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.List;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.light.Fuel;
import io.github._5thlayer.atouchofdarkness.light.LampBlock;
import io.github._5thlayer.atouchofdarkness.light.LampBlockEntity;
import io.github._5thlayer.atouchofdarkness.light.Light;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComparatorBlock;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import net.minecraft.world.level.block.entity.ComparatorBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * The Lamp: carried or placed, turned on and off by hand or, placed, by redstone, fed fuel by hand up
 * to a day, shining while on with fuel and burning only then, rain or not, until it runs dry into a
 * dark Lamp, a comparator reading its fuel, and the Campfire's. Its carried light and its placed burn
 * are the Torch's and the Campfire's, so only what is new to it is tested here. A test that moves the
 * clock runs in a batch of its own. Every test's name starts {@code lamp_}.
 */
final class LampTests {

    /** A Lamp's cap at the defaults, a day, and the soul Lamp's. */
    static final int CAP = 24_000;
    private static final int SOUL_CAP = 2 * CAP;

    /** Ticks short of a burn the clock is moved, and then past it, as the clock also runs a tick a tick. */
    private static final int MARGIN = 100;

    private LampTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("lamp_crafted_empty_and_fuel_by_use_or_bundle_click_adds_x10_up_to_a_day", 5, LampTests::fuel);
        tests.test("lamp_held_on_keeps_its_carrier_from_being_struck", QUIET + 5, LampTests::carried);
        tests.test("lamp_vanilla_lanterns_become_full_lamps", 5, LampTests::vanillaLanterns);
        tests.test("lamp_tooltip_says_how_to_fuel_it_and_counts_its_fuel_down_with_advanced_tooltips", 1, LampTests::tooltips);
        tests.test("lamp_redstone_rise_turns_it_on_and_fall_off_and_a_hand_toggle_holds_till_the_next_change", 10, LampTests::redstone);
        tests.test("lamp_fuel_never_switches_it_off_stays_off_and_on_dry_shines_again", 5, LampTests::fuelKeepsSwitch);
        tests.test("lamp_placed_into_a_live_signal_is_placed_on", 5, LampTests::placedIntoSignal);
        tests.in("lamp_toggled", KeepsClock.INSTANCE)
                .test("lamp_empty_hand_toggles_it_and_it_doesnt_burn_while_off", 10, LampTests::toggled);
        tests.in("lamp_goes_dark", KeepsClock.INSTANCE)
                .test("lamp_placed_on_gives_15_soul_10_and_goes_dark_when_dry_also_while_unloaded", 10, LampTests::goesDark);
        tests.in("lamp_comparator", KeepsClock.INSTANCE)
                .test("lamp_and_campfire_comparator_reads_fuel_0_only_dry_and_falls_as_it_burns", 10, LampTests::comparator);
        tests.in("lamp_rain", new AtNight(MoonPhase.FULL_MOON, Weather.Type.RAIN))
                .test("lamp_rain_changes_nothing", 10, LampTests::rain);
    }

    /**
     * Each Lamp crafts from iron nuggets around its Torch, with no fuel. Placed empty and on, a stick, of
     * 100 ticks in a furnace, adds 1000 and lights it; a coal, of 1600, adds 16000, and the next fills it
     * to a day, the rest lost. In the inventory, coal clicked onto a Lamp, as onto a bundle, does the same,
 * and the Lamp placed and broken again keeps its fuel.
     */
    private static void fuel(GameTestHelper helper) {
        assertCrafts(helper, "lamp", Light.TORCH.get(), Light.LAMP_ITEM.get());
        assertCrafts(helper, "soul_lamp", Light.SOUL_TORCH.get(), Light.SOUL_LAMP_ITEM.get());

        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var lamp = new BlockPos(4, 1, 2);
        helper.setBlock(lamp, Light.LAMP.get().defaultBlockState().setValue(LampBlock.ON, true));
        useOn(helper, player, new ItemStack(Items.STICK), lamp);
        assertPlaced(helper, lamp, true, 1000);
        var coal = new ItemStack(Items.COAL, 3);
        useOn(helper, player, coal, lamp);
        assertPlaced(helper, lamp, true, 17_000);
        useOn(helper, player, coal, lamp);
        assertPlaced(helper, lamp, true, CAP);
        helper.assertTrue(coal.getCount() == 1, "fuelling took " + (3 - coal.getCount()) + " coal, not 2");

        player.setItemInHand(InteractionHand.MAIN_HAND, Light.LAMP_ITEM.toStack());
        player.containerMenu.setCarried(new ItemStack(Items.COAL, 2));
        // A left click on the first hotbar slot, in the inventory's own menu.
        player.containerMenu.clicked(36, 0, ContainerInput.PICKUP, player);
        assertCarried(helper, player.getMainHandItem(), true, 16_000);
        player.containerMenu.clicked(36, 0, ContainerInput.PICKUP, player);
        assertCarried(helper, player.getMainHandItem(), true, CAP);
        helper.assertTrue(player.containerMenu.getCarried().isEmpty(), "clicking fuel on left " + player.containerMenu.getCarried());

        var placed = new BlockPos(2, 1, 6);
        useOn(helper, player, player.getMainHandItem(), placed.below(), Direction.UP);
        assertPlaced(helper, placed, true, CAP);
        helper.getLevel().destroyBlock(helper.absolutePos(placed), true);
        var dropped = helper.getEntities(EntityType.ITEM, placed, 1.0).stream().map(ItemEntity::getItem).filter(stack -> stack.is(Light.LAMP_ITEM)).toList();
        helper.assertTrue(dropped.size() == 1, "the broken Lamp dropped " + dropped);
        assertCarried(helper, dropped.getFirst(), true, CAP);
        leave(player);
        helper.succeed();
    }

    private static void assertCrafts(GameTestHelper helper, String recipe, Item torch, Item lamp) {
        var id = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, recipe));
        var holder = helper.getLevel().getServer().getRecipeManager().byKey(id);
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "no crafting recipe " + id.identifier());
        var crafting = (CraftingRecipe) holder.get().value();
        var nugget = Items.IRON_NUGGET;
        var input = CraftingInput.of(3, 3, List.of(nugget, nugget, nugget, nugget, torch, nugget, nugget, nugget, nugget).stream().map(ItemStack::new).toList());
        helper.assertTrue(crafting.matches(input, helper.getLevel()), recipe + " doesn't take nuggets around " + torch);
        var result = crafting.assemble(input);
        helper.assertTrue(result.is(lamp) && result.getCount() == 1, recipe + " gives " + result);
        assertCarried(helper, result, false, 0);
    }

    /** A player in the Dark's cage holding a Lamp that's on is never struck. */
    private static void carried(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.LAMP_ITEM.get().full());
        finishAt(helper, player, QUIET, 0F);
    }

    /**
     * A Lamp's tooltip says how to fuel it and turn it on or off placed, and with advanced tooltips (F3+H) how long its fuel lasts: a
     * crafted one none, a full one a day, 20 min, one half full 10 min, a full soul Lamp 40 min.
     */
    private static void tooltips(GameTestHelper helper) {
        var half = Light.LAMP_ITEM.get().full();
        half.set(Light.LAMP_FUEL, CAP / 2);
        String hint = "Click fuel onto it to fill it";
        assertTooltip(helper, Light.LAMP_ITEM.get().full(), TooltipFlag.NORMAL, hint, null);
        assertTooltip(helper, Light.LAMP_ITEM.toStack(), TooltipFlag.ADVANCED, hint, "0:00 of fuel left");
        assertTooltip(helper, Light.LAMP_ITEM.get().full(), TooltipFlag.ADVANCED, hint, "20:00 of fuel left");
        assertTooltip(helper, half, TooltipFlag.ADVANCED, hint, "10:00 of fuel left");
        assertTooltip(helper, Light.SOUL_LAMP_ITEM.get().full(), TooltipFlag.ADVANCED, hint, "40:00 of fuel left");
        helper.succeed();
    }

    /** The tooltip has the hint, and the countdown or, given none, no countdown at all. */
    private static void assertTooltip(GameTestHelper helper, ItemStack stack, TooltipFlag flag, String hint, @Nullable String countdown) {
        var lines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, flag).stream().map(Component::getString).toList();
        for (var line : List.of(hint, "Right-click it placed to turn it on or off")) {
            helper.assertTrue(lines.contains(line), stack + "'s tooltip " + lines + " lacks \"" + line + "\"");
        }
        boolean counted = lines.stream().anyMatch(line -> line.endsWith("of fuel left"));
        helper.assertTrue(countdown == null ? !counted : lines.contains(countdown), stack + "'s tooltip " + lines + " with " + flag + " should read " + countdown);
    }

    /**
     * Two lanterns, a soul lantern and a copper lantern of vanilla's in a player's inventory become three
     * full Lamps and a full soul Lamp, one to a slot.
     */
    private static void vanillaLanterns(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var inventory = player.getInventory();
        inventory.setItem(9, new ItemStack(Items.LANTERN, 2));
        inventory.setItem(10, new ItemStack(Items.SOUL_LANTERN));
        inventory.setItem(11, new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace("waxed_copper_lantern"))));
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(inventory.getItem(11).is(Light.LAMP_ITEM) && inventory.countItem(Items.LANTERN) == 0 && inventory.countItem(Items.SOUL_LANTERN) == 0,
                    "vanilla lanterns are still in the inventory");
            helper.assertTrue(inventory.countItem(Light.LAMP_ITEM.get()) == 3, inventory.countItem(Light.LAMP_ITEM.get()) + " Lamps, not 3");
            for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
                var stack = inventory.getItem(slot);
                if (stack.is(Light.LAMP_ITEM)) {
                    assertCarried(helper, stack, true, CAP);
                }
            }
            assertCarried(helper, inventory.getItem(10), true, SOUL_CAP);
            leave(player);
            helper.succeed();
        });
    }

    /**
     * A placed Lamp turned off, on and off again by an empty hand, and a held one turned off by using it,
     * don't burn while the clock moves on; one held on beside them does. A dry one can't be turned on.
     */
    private static void toggled(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var placed = new BlockPos(4, 1, 2);
        lamp(helper, Light.LAMP.get(), placed, 1000);
        var dry = new BlockPos(2, 1, 4);
        lamp(helper, Light.LAMP.get(), dry, 0);
        for (boolean lit : List.of(false, true, false)) {
            useOn(helper, player, ItemStack.EMPTY, placed);
            helper.assertTrue(helper.getBlockState(placed).getValue(LampBlock.LIT) == lit, "an empty hand didn't turn the Lamp " + (lit ? "on" : "off"));
        }
        useOn(helper, player, ItemStack.EMPTY, dry);
        assertPlaced(helper, dry, false, 0);

        var off = caged(helper, new BlockPos(2, 1, 7), 0);
        off.setItemInHand(InteractionHand.MAIN_HAND, Light.LAMP_ITEM.get().full());
        off.gameMode.useItem(off, helper.getLevel(), off.getMainHandItem(), InteractionHand.MAIN_HAND);
        assertCarried(helper, off.getMainHandItem(), false, CAP);
        var on = caged(helper, new BlockPos(6, 1, 7), 0);
        on.setItemInHand(InteractionHand.MAIN_HAND, Light.LAMP_ITEM.get().full());

        helper.runAtTickTime(2, () -> KeepsClock.advance(helper, 500));
        helper.runAtTickTime(4, () -> {
            assertPlaced(helper, placed, false, 1000);
            assertCarried(helper, off.getMainHandItem(), false, CAP);
            int left = on.getMainHandItem().getOrDefault(Light.LAMP_FUEL, 0);
            helper.assertTrue(left <= CAP - 500, "a Lamp held on burned to " + left + " in 500 ticks");
            List.of(player, off, on).forEach(DarkFixture::leave);
            helper.succeed();
        });
    }

    /**
     * Fuel by hand leaves a placed Lamp on or off as it was: an empty one that's off stays off, dark, with
     * the stick's 1000, and one that's on but dry shines again. Turned on, the first shines too.
     */
    private static void fuelKeepsSwitch(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var off = new BlockPos(2, 1, 2);
        var onDry = new BlockPos(6, 1, 2);
        helper.setBlock(off, Light.LAMP.get());
        helper.setBlock(onDry, Light.LAMP.get().defaultBlockState().setValue(LampBlock.ON, true));
        useOn(helper, player, new ItemStack(Items.STICK), off);
        useOn(helper, player, new ItemStack(Items.STICK), onDry);
        assertPlaced(helper, off, false, 1000);
        assertLight(helper, off, 0);
        assertPlaced(helper, onDry, true, 1000);
        useOn(helper, player, ItemStack.EMPTY, off);
        assertPlaced(helper, off, true, 1000);
        leave(player);
        helper.succeed();
    }

    /**
     * A Lamp with fuel, off, placed by a player beside a powered block is placed on, as a vanilla door
     * placed into a live signal opens; a dry one placed so stays dark, and shines once fuelled. The fall of
     * the signal turns the first off.
     */
    private static void placedIntoSignal(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var lamp = new BlockPos(2, 1, 2);
        var dry = new BlockPos(6, 1, 2);
        helper.setBlock(lamp.east(), Blocks.REDSTONE_BLOCK);
        helper.setBlock(dry.east(), Blocks.REDSTONE_BLOCK);
        var fuelled = Light.LAMP_ITEM.toStack();
        fuelled.set(Light.LAMP_FUEL, 1000);
        useOn(helper, player, fuelled, lamp.below(), Direction.UP);
        useOn(helper, player, Light.LAMP_ITEM.toStack(), dry.below(), Direction.UP);
        assertPlaced(helper, lamp, true, 1000);
        assertPlaced(helper, dry, false, 0);
        useOn(helper, player, new ItemStack(Items.STICK), dry);
        assertPlaced(helper, dry, true, 1000);
        helper.setBlock(lamp.east(), Blocks.AIR);
        assertPlaced(helper, lamp, false, 1000);
        leave(player);
        helper.succeed();
    }

    /**
     * A placed Lamp, off with fuel, beside a block that powers and unpowers it: a rise turns it on, an
     * empty hand turns it off again and it stays off while the signal holds, a fall leaves it off, the
     * hand turns it on and it stays on, and the next fall turns it off. A dry Lamp powered stays dark
     * until it is fuelled.
     */
    private static void redstone(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var lamp = new BlockPos(4, 1, 2);
        var power = lamp.east();
        lamp(helper, Light.LAMP.get(), lamp, 1000);
        useOn(helper, player, ItemStack.EMPTY, lamp);
        assertPlaced(helper, lamp, false, -1);
        helper.setBlock(power, Blocks.REDSTONE_BLOCK);
        assertPlaced(helper, lamp, true, -1);
        useOn(helper, player, ItemStack.EMPTY, lamp);
        var dry = new BlockPos(2, 1, 4);
        lamp(helper, Light.LAMP.get(), dry, 0);
        helper.setBlock(dry.east(), Blocks.REDSTONE_BLOCK);
        assertPlaced(helper, dry, false, 0);
        helper.runAtTickTime(3, () -> {
            assertPlaced(helper, lamp, false, -1);
            helper.setBlock(power, Blocks.AIR);
            assertPlaced(helper, lamp, false, -1);
            useOn(helper, player, ItemStack.EMPTY, lamp);
            useOn(helper, player, new ItemStack(Items.STICK), dry);
            assertPlaced(helper, dry, true, 1000);
        });
        helper.runAtTickTime(5, () -> {
            assertPlaced(helper, lamp, true, -1);
            helper.setBlock(power, Blocks.REDSTONE_BLOCK);
            assertPlaced(helper, lamp, true, -1);
            helper.setBlock(power, Blocks.AIR);
            assertPlaced(helper, lamp, false, -1);
            leave(player);
            helper.succeed();
        });
    }

    /**
     * A Lamp and a soul Lamp placed on, giving 15 and 10, and a third: the first two stay loaded while the
     * clock moves on past their fuel, the third is unloaded meanwhile, saved and taken away, then loaded
     * again from its save. Each is then a Lamp still, dark and dry.
     */
    private static void goesDark(GameTestHelper helper) {
        var lamp = new BlockPos(2, 1, 2);
        var soul = new BlockPos(6, 1, 2);
        var unloaded = new BlockPos(4, 1, 6);
        lamp(helper, Light.LAMP.get(), lamp, 1000);
        lamp(helper, Light.SOUL_LAMP.get(), soul, 1000);
        lamp(helper, Light.LAMP.get(), unloaded, 1000);
        assertLight(helper, lamp, Light.LAMP_LIGHT);
        assertLight(helper, soul, Light.SOUL_LAMP_LIGHT);
        var registries = helper.getLevel().registryAccess();
        var saved = new CompoundTag[1];
        helper.runAtTickTime(2, () -> {
            saved[0] = helper.getBlockEntity(unloaded, LampBlockEntity.class).saveWithFullMetadata(registries);
            helper.setBlock(unloaded, Blocks.AIR);
            KeepsClock.advance(helper, 1000 - MARGIN);
        });
        helper.runAtTickTime(4, () -> {
            assertPlaced(helper, lamp, true, -1);
            helper.setBlock(unloaded, Light.LAMP.get().defaultBlockState().setValue(LampBlock.ON, true).setValue(LampBlock.LIT, true));
            helper.getBlockEntity(unloaded, LampBlockEntity.class)
                    .loadWithComponents(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved[0]));
            KeepsClock.advance(helper, 2 * MARGIN);
        });
        helper.runAtTickTime(6, () -> {
            for (var pos : List.of(lamp, soul, unloaded)) {
                assertPlaced(helper, pos, false, 0);
                assertLight(helper, pos, 0);
            }
            helper.succeed();
        });
    }

    /**
     * A comparator reading each of a full Lamp, a Lamp with 100 ticks of fuel and a full Campfire: 15, 1
     * and 15. Once the clock has moved on short of half the Campfire's cap, 5900 ticks, they read 12, 0
     * and 8.
     */
    private static void comparator(GameTestHelper helper) {
        var full = new BlockPos(2, 1, 2);
        var low = new BlockPos(4, 1, 2);
        var campfire = new BlockPos(6, 1, 2);
        lamp(helper, Light.LAMP.get(), full, CAP);
        lamp(helper, Light.LAMP.get(), low, 100);
        helper.setBlock(campfire, Light.CAMPFIRE.get());
        helper.getBlockEntity(campfire, CampfireBlockEntity.class).setData(Light.FUEL, Fuel.unclocked(CampfireTests.CAP));
        for (var light : List.of(full, low, campfire)) {
            helper.setBlock(light.south(), Blocks.COMPARATOR.defaultBlockState().setValue(ComparatorBlock.FACING, Direction.NORTH));
        }
        helper.runAtTickTime(4, () -> {
            assertReads(helper, full, 15);
            assertReads(helper, low, 1);
            assertReads(helper, campfire, 15);
            KeepsClock.advance(helper, CampfireTests.CAP / 2 - MARGIN);
        });
        helper.runAtTickTime(8, () -> {
            assertReads(helper, full, 12);
            assertReads(helper, low, 0);
            assertReads(helper, campfire, 8);
            helper.succeed();
        });
    }

    /** The comparator south of the light reads this from it. */
    private static void assertReads(GameTestHelper helper, BlockPos light, int signal) {
        int read = helper.getBlockEntity(light.south(), ComparatorBlockEntity.class).getOutputSignal();
        helper.assertTrue(read == signal, "a comparator reads " + read + " from " + helper.getBlockState(light) + ", not " + signal);
    }

    /**
     * At midnight in rain, a Lamp placed and one held under the open sky, each with 2000 ticks of fuel,
     * are still on once the clock has moved on more than half of that.
     */
    private static void rain(GameTestHelper helper) {
        var placed = new BlockPos(1, 1, 1);
        var stand = new BlockPos(7, 1, 7);
        // The framework roofs each test with barriers, which keep the rain off as any roof would.
        helper.setBlock(placed.above(4), Blocks.AIR);
        helper.setBlock(stand.above(4), Blocks.AIR);
        lamp(helper, Light.LAMP.get(), placed, 2000);
        var player = inTheOpen(helper, stand);
        var held = Light.LAMP_ITEM.get().full();
        held.set(Light.LAMP_FUEL, 2000);
        player.setItemInHand(InteractionHand.MAIN_HAND, held);
        helper.runAtTickTime(2, () -> KeepsClock.advance(helper, 1000 + MARGIN));
        helper.runAtTickTime(4, () -> {
            assertPlaced(helper, placed, true, -1);
            assertCarried(helper, player.getMainHandItem(), true, -1);
            leave(player);
            helper.succeed();
        });
    }

    /** A Lamp set at the block, on if it holds any fuel. */
    static void lamp(GameTestHelper helper, Block lamp, BlockPos at, int fuel) {
        helper.setBlock(at, lamp.defaultBlockState().setValue(LampBlock.ON, fuel > 0).setValue(LampBlock.LIT, fuel > 0));
        helper.getBlockEntity(at, LampBlockEntity.class).setFuel(fuel);
    }

    /** The player uses the stack on the side of the block at {@code at}. */
    static void useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos at) {
        useOn(helper, player, stack, at, Direction.SOUTH);
    }

    static void useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos at, Direction side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var pos = helper.absolutePos(at);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    /** The placed Lamp is a Lamp still, on or not, with this much fuel, or any for -1. */
    static void assertPlaced(GameTestHelper helper, BlockPos at, boolean lit, int fuel) {
        var state = helper.getBlockState(at);
        helper.assertTrue(state.getBlock() instanceof LampBlock, "the Lamp at " + at + " became " + state);
        int left = helper.getBlockEntity(at, LampBlockEntity.class).fuelLeft();
        helper.assertTrue(state.getValue(LampBlock.LIT) == lit && (fuel < 0 || left == fuel),
                "the Lamp at " + at + " is " + state + " with " + left + " fuel, not " + (lit ? "on" : "off") + " with " + fuel);
    }

    /** The carried Lamp is on or not, with this much fuel, or any for -1. */
    private static void assertCarried(GameTestHelper helper, ItemStack stack, boolean lit, int fuel) {
        int left = stack.getOrDefault(Light.LAMP_FUEL, 0);
        helper.assertTrue(stack.has(Light.LAMP_ON) == lit && (fuel < 0 || left == fuel),
                stack + " is " + (stack.has(Light.LAMP_ON) ? "on" : "off") + " with " + left + " fuel, not " + (lit ? "on" : "off") + " with " + fuel);
    }

    private static void assertLight(GameTestHelper helper, BlockPos at, int light) {
        int given = helper.getBlockState(at).getLightEmission(helper.getLevel(), helper.absolutePos(at));
        helper.assertTrue(given == light, "the Lamp at " + at + " gives " + given + ", not " + light);
    }
}
