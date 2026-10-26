// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * The Torch (GLOSSARY.md), and the soul Torch: crafted burning, never placed, and burning only while
 * held, by the time of day, twice as fast in rain under the open sky. Underwater, or burnt out, it
 * becomes a Spent Torch, unless it was a Spent Torch relit, which burns away to nothing. Its burn shows
 * as an item bar once it has started.
 */
public final class TorchItem extends Item implements HeldLightSource {

    private final int light;

    /** How many times the config's burn time a fresh one burns: 2 for the soul Torch. */
    private final int burnFactor;

    private final int barColor;

    public TorchItem(Properties properties, int light, int burnFactor, int barColor) {
        super(properties);
        this.light = light;
        this.burnFactor = burnFactor;
        this.barColor = barColor;
    }

    @Override
    public int heldLight(ItemStack stack) {
        return light;
    }

    @Override
    public ItemStack burnHeld(ItemStack stack, ServerPlayer player, long elapsed) {
        if (eyesUnderWater(player)) {
            return Light.SPENT_TORCH.toStack();
        }
        if (elapsed == 0) {
            return stack;
        }
        var burning = stack.get(Light.BURNING);
        if (burning == null) {
            burning = Burning.fresh(LightConfig.TORCH_BURN_TIME.getAsInt() * burnFactor);
        }
        // Above the eyes, as the carried light there hides the sky from its own block.
        int rate = player.level().isRainingAt(BlockPos.containing(player.getEyePosition()).above()) ? Burn.IN_RAIN : Burn.STILL;
        int left = Burn.burn(burning.left(), elapsed, rate);
        if (left == 0) {
            return burning.relit() ? ItemStack.EMPTY : Light.SPENT_TORCH.toStack();
        }
        stack.set(Light.BURNING, burning.withLeft(left));
        return stack;
    }

    /** Whether the player's eyes are below the surface of water, as vanilla's own check, which only a player's own tick updates. */
    private static boolean eyesUnderWater(Player player) {
        var level = player.level();
        var eyes = BlockPos.containing(player.getEyePosition());
        var fluid = level.getFluidState(eyes);
        return fluid.is(FluidTags.WATER) && player.getEyeY() < eyes.getY() + fluid.getHeight(level, eyes);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(Light.BURNING);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var burning = stack.get(Light.BURNING);
        return burning == null ? MAX_BAR_WIDTH : Mth.clamp(Math.round((float) MAX_BAR_WIDTH * burning.left() / burning.full()), 0, MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return barColor;
    }

    /** Not each tick its burn changes. */
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
