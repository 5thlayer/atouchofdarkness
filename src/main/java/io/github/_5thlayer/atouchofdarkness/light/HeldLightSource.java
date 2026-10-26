// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;

/**
 * An item that is a carried Light Source: it burns only in a player's main or off hand, and lights the
 * world there as a placed one would (ADR 0002). Anywhere else, in the inventory, a chest or on the
 * ground, it is paused and gives no light.
 */
public interface HeldLightSource {

    /** The light the stack gives while held, 0 for none. */
    int heldLight(ItemStack stack);

    /**
     * Burns the stack in the player's hand as the time of day moved on {@code elapsed} ticks, and returns
     * what the hand holds after: the same stack, or what it burns out into.
     */
    ItemStack burnHeld(ItemStack stack, ServerPlayer player, long elapsed);
}
