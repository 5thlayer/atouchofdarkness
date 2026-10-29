// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.function.Consumer;

import io.github._5thlayer.atouchofdarkness.dark.Lit;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

/**
 * The Torch (GLOSSARY.md), and the soul Torch: crafted burning, never placed, and burning only while
 * held, by the time of day, twice as fast in rain under the open sky. Underwater, or burnt out, it
 * becomes a Spent Torch, unless it was a Spent Torch relit, which is gone. Its burn shows
 * as an item bar once it has started, and as a countdown in its advanced tooltip.
 */
public final class TorchItem extends Item implements HeldLightSource {

    private static final String HINT = "item.atouchofdarkness.torch.hint";
    private static final String BURN_LEFT = "item.atouchofdarkness.torch.burn_left";

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
        var burning = stack.get(Light.BURNING);
        // Burnt down to nothing out of hand, it goes out once held again.
        if (eyesUnderWater(player) || burning != null && burning.left() == 0) {
            return goneOut(burning);
        }
        if (elapsed == 0) {
            return stack;
        }
        var burnt = burnt(stack, player, elapsed);
        if (burnt.left() == 0) {
            return goneOut(burnt);
        }
        stack.set(Light.BURNING, burnt);
        return stack;
    }

    @Override
    public void settle(ItemStack stack, ServerPlayer player, long elapsed) {
        stack.set(Light.BURNING, burnt(stack, player, elapsed));
    }

    /** Its burn once the clock has moved on this far in the player's hand, twice as fast in rain under the open sky. */
    private Burning burnt(ItemStack stack, ServerPlayer player, long elapsed) {
        var burning = stack.getOrDefault(Light.BURNING, Burning.fresh(freshBurn()));
        // Above the eyes, as the carried light there hides the sky from its own block.
        int rate = player.level().isRainingAt(Lit.eyes(player).above()) ? Burn.IN_RAIN : Burn.STILL;
        return burning.withLeft(Burn.burn(burning.left(), elapsed, rate));
    }

    /** A fresh one's burn, from the config: on a client, its own. */
    private int freshBurn() {
        return LightConfig.TORCH_BURN_TIME.getAsInt() * burnFactor;
    }

    /** A Spent Torch, or nothing for one already relit, so relighting is never endless light. */
    private static ItemStack goneOut(@Nullable Burning burning) {
        return burning != null && burning.relit() ? ItemStack.EMPTY : Light.SPENT_TORCH.toStack();
    }

    /** Whether the player's eyes are below the surface of water, as vanilla's own check, which only a player's own tick updates. */
    private static boolean eyesUnderWater(ServerPlayer player) {
        var level = player.level();
        var eyes = Lit.eyes(player);
        var fluid = level.getFluidState(eyes);
        return fluid.is(FluidTags.WATER) && player.getEyeY() < eyes.getY() + fluid.getHeight(level, eyes);
    }

    /** That it burns only while held, and with advanced tooltips (F3+H) the time of day it has left, as m:ss. */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(HINT).withStyle(ChatFormatting.GRAY));
        if (flag.isAdvanced()) {
            var burning = stack.get(Light.BURNING);
            builder.accept(TimeLeft.line(BURN_LEFT, burning == null ? freshBurn() : burning.left()));
        }
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(Light.BURNING);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var burning = stack.get(Light.BURNING);
        return burning == null ? MAX_BAR_WIDTH : Burn.bar(burning.left(), burning.full(), MAX_BAR_WIDTH);
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
