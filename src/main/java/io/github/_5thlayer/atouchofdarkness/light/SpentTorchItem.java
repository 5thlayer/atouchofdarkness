// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.function.Consumer;

import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

/**
 * The Spent Torch, what a Torch burns out into, which is never placed. Used in the hand with flint and
 * steel or a fire charge in the other, or a burning Light Source, or while looking at fire, lava or a lit
 * campfire, it is relit into a Torch that burns a quarter as long as a fresh one, then is gone. Held
 * alone in the Dark it steeps toward a Nightbrand, which its item bar shows.
 */
public final class SpentTorchItem extends Item {

    private static final String HINT = "item.atouchofdarkness.spent_torch.hint";

    /** The steep's item bar, the violet of the night sky. */
    private static final int STEEP_BAR_COLOR = 0x7A4FC8;

    public SpentTorchItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        var sound = lightFrom(level, player, hand);
        if (sound == null) {
            return InteractionResult.PASS;
        }
        level.playSound(player, player.getX(), player.getEyeY(), player.getZ(), sound, SoundSource.PLAYERS, 1F, 1F);
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        var torch = Light.TORCH.toStack();
        torch.set(Light.BURNING, Burning.relit(LightConfig.TORCH_BURN_TIME.getAsInt()));
        return InteractionResult.SUCCESS.heldItemTransformedTo(ItemUtils.createFilledResult(player.getItemInHand(hand), player, torch));
    }

    /** How to relight it, which nothing in the world shows. */
    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> builder, TooltipFlag flag) {
        builder.accept(Component.translatable(HINT).withStyle(ChatFormatting.GRAY));
    }

    /** Its steep, once it has started, toward a Nightbrand. */
    @Override
    public boolean isBarVisible(ItemStack stack) {
        return stack.has(Nightbrand.STEEPING);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        var steeping = stack.get(Nightbrand.STEEPING);
        return steeping == null ? 0 : Mth.clamp(Math.round((float) MAX_BAR_WIDTH * steeping.steeped() / steeping.full()), 0, MAX_BAR_WIDTH);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return STEEP_BAR_COLOR;
    }

    /** Not each tick its steep changes. */
    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || !ItemStack.isSameItem(oldStack, newStack);
    }

    /** Nor does mining with one in hand start over. */
    @Override
    public boolean shouldCauseBlockBreakReset(ItemStack oldStack, ItemStack newStack) {
        return !ItemStack.isSameItem(oldStack, newStack);
    }

    /**
     * Takes light from what the other hand holds or what the player looks at, and returns the sound it
     * makes, or null where there is none to take. Flint and steel wears and a fire charge is used up.
     */
    private static @Nullable SoundEvent lightFrom(Level level, Player player, InteractionHand hand) {
        var otherHand = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        var other = player.getItemInHand(otherHand);
        if (other.is(Items.FLINT_AND_STEEL)) {
            other.hurtAndBreak(1, player, otherHand.asEquipmentSlot());
            return SoundEvents.FLINTANDSTEEL_USE;
        }
        if (other.is(Items.FIRE_CHARGE)) {
            other.consume(1, player);
            return SoundEvents.FIRECHARGE_USE;
        }
        if (other.getItem() instanceof HeldLightSource source && source.heldLight(other) > 0) {
            return SoundEvents.FIRE_AMBIENT;
        }
        var hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.ANY);
        if (hit.getType() == HitResult.Type.MISS) {
            return null;
        }
        var state = level.getBlockState(hit.getBlockPos());
        boolean burning = state.is(BlockTags.FIRE) || CampfireBlock.isLitCampfire(state)
                || level.getFluidState(hit.getBlockPos()).is(FluidTags.LAVA);
        return burning ? SoundEvents.FIRE_AMBIENT : null;
    }
}
