// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** A Votive's screen: its fuel, Nightbrand and gold slots in a row, as a hopper's, above the player's inventory. */
public final class VotiveMenu extends AbstractContainerMenu {

    /** Where the first slot is drawn, and the player's inventory. */
    public static final int SLOTS_X = 62;
    public static final int SLOTS_Y = 20;
    public static final int INVENTORY_X = 8;
    public static final int INVENTORY_Y = 51;

    private static final int SIZE = 3;

    private final Container votive;

    /** The client's, which the server fills. */
    public VotiveMenu(int containerId, Inventory inventory) {
        this(containerId, inventory, new SimpleContainer(SIZE));
    }

    VotiveMenu(int containerId, Inventory inventory, Container votive) {
        super(Lampad.VOTIVE_MENU.get(), containerId);
        checkContainerSize(votive, SIZE);
        this.votive = votive;
        var level = inventory.player.level();
        for (int slot = 0; slot < SIZE; slot++) {
            int index = slot;
            addSlot(new Slot(votive, slot, SLOTS_X + slot * 18, SLOTS_Y) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    return VotiveBlockEntity.takes(index, stack, level);
                }
            });
        }
        addStandardInventorySlots(inventory, INVENTORY_X, INVENTORY_Y);
    }

    @Override
    public boolean stillValid(Player player) {
        return votive.stillValid(player);
    }

    /** Out to the inventory, or in to the slot that takes it. */
    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        var slot = slots.get(index);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        var stack = slot.getItem();
        var moved = stack.copy();
        boolean done = index < SIZE ? moveItemStackTo(stack, SIZE, slots.size(), true) : moveItemStackTo(stack, 0, SIZE, false);
        if (!done) {
            return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) {
            slot.setByPlayer(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return moved;
    }
}
