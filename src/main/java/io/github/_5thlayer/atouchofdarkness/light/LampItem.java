// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.Unit;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickAction;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * The Lamp (GLOSSARY.md), and the soul Lamp, carried: its fuel the data component
 * {@code atouchofdarkness:lamp_fuel}, and the marker {@code atouchofdarkness:lamp_on} while it is on with fuel. It
 * burns only while held and on, by the time of day, rain or not, and runs dry into a dark Lamp until
 * fuelled. Used in the air it turns on or off; placed, its block keeps its fuel. Fuel clicked onto it in
 * an inventory with the right button adds ten times its burn time, up to its cap. Its fuel shows as an
 * item bar. One crafted has none.
 */
public final class LampItem extends BlockItem implements HeldLightSource {

    private final int light;

    /** How many times the config's cap it holds: 2 for the soul Lamp. */
    private final int capFactor;

    private final int barColor;

    public LampItem(LampBlock block, Properties properties, int light, int capFactor, int barColor) {
        super(block, properties);
        this.light = light;
        this.capFactor = capFactor;
        this.barColor = barColor;
    }

    /** The fuel it holds at most, from the config. */
    int cap() {
        return LightConfig.LAMP_CAP.getAsInt() * capFactor;
    }

    /** A Lamp full of fuel, and on. */
    public ItemStack full() {
        var stack = new ItemStack(this);
        stack.set(Light.LAMP_FUEL, cap());
        stack.set(Light.LAMP_ON, Unit.INSTANCE);
        return stack;
    }

    static int fuel(ItemStack stack) {
        return stack.getOrDefault(Light.LAMP_FUEL, 0);
    }

    static boolean lit(ItemStack stack) {
        return stack.has(Light.LAMP_ON);
    }

    @Override
    public int heldLight(ItemStack stack) {
        return lit(stack) ? light : 0;
    }

    @Override
    public ItemStack burnHeld(ItemStack stack, ServerPlayer player, long elapsed) {
        settle(stack, player, elapsed);
        return stack;
    }

    @Override
    public void settle(ItemStack stack, ServerPlayer player, long elapsed) {
        if (elapsed == 0 || !lit(stack)) {
            return;
        }
        int left = Burn.burn(fuel(stack), elapsed, Burn.STILL);
        stack.set(Light.LAMP_FUEL, left);
        if (left == 0) {
            stack.remove(Light.LAMP_ON);
        }
    }

    /** Turns it off, or on if it has fuel. */
    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var stack = player.getItemInHand(hand);
        if (lit(stack)) {
            stack.remove(Light.LAMP_ON);
        } else if (fuel(stack) > 0) {
            stack.set(Light.LAMP_ON, Unit.INSTANCE);
        } else {
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    /**
     * Fuel clicked onto it with the right button, as onto a bundle, one at a time: it adds ten times its
     * burn time, up to the cap, the rest lost, and lights a dry Lamp. A full one takes none. Tools, which a
     * furnace burns too, are never fuel here.
     */
    @Override
    public boolean overrideOtherStackedOnMe(ItemStack stack, ItemStack other, Slot slot, ClickAction action, Player player, SlotAccess carried) {
        int burnTime = action == ClickAction.SECONDARY ? HandFuel.burnTime(other, player.level()) : 0;
        if (burnTime <= 0) {
            return false;
        }
        int left = fuel(stack);
        if (left >= cap()) {
            return true;
        }
        stack.set(Light.LAMP_FUEL, Burn.refuel(left, burnTime, cap()));
        if (left == 0) {
            stack.set(Light.LAMP_ON, Unit.INSTANCE);
        }
        // A bucket of lava leaves its bucket.
        var remainder = other.getCraftingRemainder();
        other.shrink(1);
        if (remainder != null) {
            if (other.isEmpty()) {
                carried.set(remainder.create());
            } else {
                player.getInventory().placeItemBackInInventory(remainder.create());
            }
        }
        return true;
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Mth.clamp(Math.round((float) MAX_BAR_WIDTH * fuel(stack) / cap()), 0, MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    /** Not each second its fuel changes. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /** Nor does mining with one in hand start over. */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }
}
