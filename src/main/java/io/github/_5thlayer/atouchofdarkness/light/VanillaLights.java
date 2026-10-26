// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/**
 * Vanilla's Light Sources, which a player can only loot, trade for, or still have: one in a player's
 * inventory becomes the Pillar's own, soul to soul. A torch becomes a fresh Torch, each to its own slot
 * since Torches don't stack, and any that don't fit drop at their feet; a lantern, copper ones too,
 * a Lamp full of fuel and on, likewise; a campfire a Campfire, stack for stack. A torch placed in the world stays lit, and broken drops a Spent Torch, by the loot
 * tables {@code data/minecraft/loot_table/blocks/torch.json} and {@code soul_torch.json}.
 */
final class VanillaLights {

    private VanillaLights() {
    }

    static void convert(ServerPlayer player) {
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            var stack = inventory.getItem(slot);
            var own = ownFor(stack);
            if (own == null) {
                continue;
            }
            int count = stack.getCount();
            int fits = Math.min(count, own.getMaxStackSize());
            inventory.setItem(slot, own.copyWithCount(fits));
            for (int more = fits; more < count; more++) {
                inventory.placeItemBackInInventory(own.copy());
            }
        }
    }

    /** One of the Pillar's own for one of vanilla's. */
    private static @Nullable ItemStack ownFor(ItemStack stack) {
        if (stack.is(Items.TORCH)) {
            return Light.TORCH.toStack();
        }
        if (stack.is(Items.SOUL_TORCH)) {
            return Light.SOUL_TORCH.toStack();
        }
        if (stack.is(Items.CAMPFIRE)) {
            return Light.CAMPFIRE_ITEM.toStack();
        }
        if (stack.is(Items.SOUL_CAMPFIRE)) {
            return Light.SOUL_CAMPFIRE_ITEM.toStack();
        }
        if (stack.is(Items.SOUL_LANTERN)) {
            return Light.SOUL_LAMP_ITEM.get().full();
        }
        return stack.is(ItemTags.LANTERNS) ? Light.LAMP_ITEM.get().full() : null;
    }
}
