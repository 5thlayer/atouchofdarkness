// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;

/**
 * Fuel fed to a Light Source by hand: any item a furnace burns, but a tool, which keeps its own use, such
 * as a wooden shovel putting a Campfire out.
 */
final class HandFuel {

    private HandFuel() {
    }

    /** Its vanilla furnace burn time, or 0 if it isn't fuel here. */
    static int burnTime(ItemStack stack, Level level) {
        return stack.isDamageableItem() ? 0 : stack.getBurnTime(RecipeType.SMELTING, level.fuelValues());
    }

    /** Uses one up from the player's hand into the placed Light Source, leaving what it leaves, such as a bucket of lava's bucket. */
    static void feed(ItemStack stack, Level level, BlockPos pos, Player player, InteractionHand hand) {
        var remainder = stack.getCraftingRemainder();
        if (remainder == null) {
            stack.consume(1, player);
        } else {
            player.setItemInHand(hand, ItemUtils.createFilledResult(stack, player, remainder.create()));
        }
        level.playSound(null, pos, SoundEvents.FIRECHARGE_USE, SoundSource.BLOCKS, 0.5F, 1F);
    }
}
