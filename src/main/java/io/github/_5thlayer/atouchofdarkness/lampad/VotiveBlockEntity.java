// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.UUID;

import io.github._5thlayer.atouchofdarkness.light.PlacedFuel;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BaseContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Votive's three slots, each taking only its own items, by hand or by hopper from any side, and none
 * taken out but by hand: fuel for the Lamps, any furnace fuel but a tool or one that leaves something
 * behind, such as a bucket of lava; the Nightbrands that renew its Lampad's binding; and the gold
 * nuggets she takes for it. It remembers its Lampad, so it summons no second one.
 */
public final class VotiveBlockEntity extends BaseContainerBlockEntity implements WorldlyContainer {

    public static final int FUEL = 0;
    public static final int NIGHTBRAND = 1;
    public static final int GOLD = 2;

    private static final int[] SLOTS = {FUEL, NIGHTBRAND, GOLD};
    private static final String LAMPAD = "lampad";
    private static final String TITLE = "container.atouchofdarkness.votive";

    private NonNullList<ItemStack> items = NonNullList.withSize(SLOTS.length, ItemStack.EMPTY);
    private @Nullable UUID lampad;

    public VotiveBlockEntity(BlockPos pos, BlockState state) {
        super(Lampad.VOTIVE_ENTITY.get(), pos, state);
    }

    /** Whether the slot takes the stack. */
    static boolean takes(int slot, ItemStack stack, Level level) {
        return switch (slot) {
            case FUEL -> PlacedFuel.isFuel(stack, level) && stack.getCraftingRemainder() == null;
            case NIGHTBRAND -> stack.is(Nightbrand.NIGHTBRAND);
            case GOLD -> stack.is(Items.GOLD_NUGGET);
            default -> false;
        };
    }

    /** Whether it has a Lampad, alive or dormant. */
    public boolean hasLampad() {
        return lampad != null;
    }

    boolean isBoundTo(UUID id) {
        return id.equals(lampad);
    }

    void bind(UUID id) {
        lampad = id;
        setChanged();
    }

    /** Forgets its Lampad, if it is this one: she is dead. */
    void release(UUID id) {
        if (isBoundTo(id)) {
            lampad = null;
            setChanged();
        }
    }

    /** One of the fuel in its slot, left there; empty if there is none. */
    ItemStack peekFuel() {
        return items.get(FUEL).copyWithCount(Math.min(items.get(FUEL).getCount(), 1));
    }

    /** One of the fuel, taken from its slot. */
    ItemStack takeFuel() {
        return removeItem(FUEL, 1);
    }

    /** One Nightbrand, taken from its slot; empty if there is none. */
    ItemStack takeNightbrand() {
        return removeItem(NIGHTBRAND, 1);
    }

    /** Up to this many gold nuggets taken; how many were. */
    int takeGold(int nuggets) {
        return nuggets <= 0 ? 0 : removeItem(GOLD, nuggets).getCount();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return level != null && takes(slot, stack, level);
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return SLOTS;
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, @Nullable Direction side) {
        return canPlaceItem(slot, stack);
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return false;
    }

    @Override
    public int getContainerSize() {
        return SLOTS.length;
    }

    @Override
    protected NonNullList<ItemStack> getItems() {
        return items;
    }

    @Override
    protected void setItems(NonNullList<ItemStack> items) {
        this.items = items;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable(TITLE);
    }

    @Override
    protected AbstractContainerMenu createMenu(int containerId, Inventory inventory) {
        return new VotiveMenu(containerId, inventory, this);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        ContainerHelper.saveAllItems(output, items);
        output.storeNullable(LAMPAD, UUIDUtil.CODEC, lampad);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        ContainerHelper.loadAllItems(input, items);
        lampad = input.read(LAMPAD, UUIDUtil.CODEC).orElse(null);
    }
}
