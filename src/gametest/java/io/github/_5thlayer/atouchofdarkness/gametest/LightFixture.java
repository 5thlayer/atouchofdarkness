// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.List;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.light.LampBlock;
import io.github._5thlayer.atouchofdarkness.light.PlacedFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/**
 * The shared fixture of the tests of a Light Source, placed or carried, and of what a torch becomes: a
 * crafted item, a player's click on a block, a tooltip's lines, a Spent Torch's or Nightbrand's among
 * them, and a Lamp or Campfire placed with fuel. It sets things up and reads them back, and each test
 * makes its own assertions of what it reads, save the one of a placed Lamp.
 */
final class LightFixture {

    /** A Lamp's cap at the defaults, a day, and a Campfire's, half a day; written out, so a changed default fails a test. */
    static final int LAMP_CAP = 24_000;
    static final int CAMPFIRE_CAP = 12_000;

    private LightFixture() {
    }

    /** The Pillar's crafting recipe, which must exist and take this grid of items; what it gives. */
    static ItemStack crafted(GameTestHelper helper, String recipe, int width, int height, List<Item> grid) {
        var id = ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, recipe));
        var holder = helper.getLevel().getServer().getRecipeManager().byKey(id);
        helper.assertTrue(holder.isPresent() && holder.get().value() instanceof CraftingRecipe, "no crafting recipe " + id.identifier());
        var crafting = (CraftingRecipe) holder.get().value();
        var input = CraftingInput.of(width, height, grid.stream().map(ItemStack::new).toList());
        helper.assertTrue(crafting.matches(input, helper.getLevel()), recipe + " doesn't take " + grid);
        return crafting.assemble(input);
    }

    /** The player uses the stack on the centre of this face of the block at {@code at}. */
    static void useOn(GameTestHelper helper, ServerPlayer player, ItemStack stack, BlockPos at, Direction side) {
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        var pos = helper.absolutePos(at);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos).relative(side, 0.5), side, pos, false);
        player.gameMode.useItemOn(player, helper.getLevel(), stack, InteractionHand.MAIN_HAND, hit);
    }

    /** The stack's tooltip with this flag, a line to a string. */
    static List<String> tooltip(GameTestHelper helper, ItemStack stack, TooltipFlag flag) {
        return stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, flag).stream().map(Component::getString).toList();
    }

    /** A Lamp set at the block, on if it holds any fuel. */
    static void lamp(GameTestHelper helper, Block lamp, BlockPos at, int fuel) {
        helper.setBlock(at, lamp.defaultBlockState().setValue(LampBlock.ON, fuel > 0).setValue(LampBlock.LIT, fuel > 0));
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(at), fuel);
    }

    /** A Campfire set at the block, holding this much fuel. */
    static void campfire(GameTestHelper helper, Block campfire, BlockPos at, int fuel) {
        helper.setBlock(at, campfire);
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(at), fuel);
    }

    /** The fuel the placed light at the block holds. */
    static int fuel(GameTestHelper helper, BlockPos at) {
        return PlacedFuel.left(helper.getLevel(), helper.absolutePos(at));
    }

    /** The placed Lamp is a Lamp still, on or not, with this much fuel, or any for -1. */
    static void assertLamp(GameTestHelper helper, BlockPos at, boolean lit, int fuel) {
        var state = helper.getBlockState(at);
        helper.assertTrue(state.getBlock() instanceof LampBlock, "the Lamp at " + at + " became " + state);
        int left = fuel(helper, at);
        helper.assertTrue(state.getValue(LampBlock.LIT) == lit && (fuel < 0 || left == fuel),
                "the Lamp at " + at + " is " + state + " with " + left + " fuel, not " + (lit ? "on" : "off") + " with " + fuel);
    }
}
