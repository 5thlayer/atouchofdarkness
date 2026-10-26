// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's torches, which a player can only loot, trade for, or still have: one in a player's inventory
 * becomes a fresh Torch, soul to soul, each to its own slot since Torches don't stack, and any that don't
 * fit drop at their feet. One placed in the world stays lit, and broken drops a Spent Torch, by the
 * loot tables {@code data/minecraft/loot_table/blocks/torch.json} and {@code soul_torch.json}.
 */
final class VanillaTorches {

    private VanillaTorches() {
    }

    static void convert(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var torch = torchFor(stack);
            if (torch == null) {
                continue;
            }
            int count = stack.getCount();
            inventory.setItem(slot, new ItemStack(torch));
            for (int more = 1; more < count; more++) {
                inventory.placeItemBackInInventory(new ItemStack(torch));
            }
        }
    }

    private static @Nullable Item torchFor(ItemStack stack) {
        if (stack.is(Items.TORCH)) {
            return Light.TORCH.get();
        }
        return stack.is(Items.SOUL_TORCH) ? Light.SOUL_TORCH.get() : null;
    }
}
