// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.Lit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import org.jspecify.annotations.Nullable;

/**
 * Carried light (ADR 0002): every game tick, each player's held Light Sources burn by the time of day
 * that passed, and a player holding one that burns has its light at their eye block, in a
 * {@link CarriedLightBlock} that follows their block position and is removed behind them. With one in
 * each hand the brighter wins, and two carriers at one block share the brighter light. Nothing else
 * carries light: not an item on the ground, nor a mob.
 */
public final class CarriedLight {

    private static final long NO_CLOCK = Long.MIN_VALUE;

    /**
     * Ticks of the clock a held Light Source's burn gathers before it is written to the stack, so the
     * hand is sent to the client once a second rather than every tick. A jump of the clock, such as
     * sleep, is written at once.
     */
    private static final int BURN_STEP = 20;

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ATouchOfDarkness.MOD_ID);

    /** Never saved nor copied: a player who joins or respawns starts the clock afresh, with no light placed. */
    private static final Supplier<AttachmentType<Carrier>> CARRIER = ATTACHMENTS.register("carrier",
            () -> AttachmentType.builder(Carrier::new).build());

    private static final class Carrier {
        /** The time of day the player was last ticked at. */
        long clock = NO_CLOCK;
        /** Ticks of the clock since then that their held Light Sources haven't yet burned. */
        long unburnt;
        /** What the player's hands held last tick, which owe the unburnt ticks if they have left them. */
        ItemStack main = ItemStack.EMPTY;
        ItemStack off = ItemStack.EMPTY;
        /** Where the player's light was placed last tick, if anywhere, in whichever level they were in. */
        @Nullable GlobalPos lit;
    }

    private CarriedLight() {
    }

    static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        NeoForge.EVENT_BUS.addListener(CarriedLight::tick);
        Lit.countCarried(CarriedLight::heldLight);
    }

    /** Whether a player in the level carries light to this block now, which keeps a {@link CarriedLightBlock} there. */
    static boolean carriedTo(ServerLevel level, BlockPos pos) {
        return level.players().stream().anyMatch(player -> heldLight(player) > 0 && Lit.eyes(player).equals(pos));
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) {
            return;
        }
        long now = level.getOverworldClockTime();
        Map<BlockPos, Integer> wanted = new HashMap<>();
        for (ServerPlayer player : level.players()) {
            var carrier = player.getData(CARRIER);
            long elapsed = carrier.clock == NO_CLOCK ? 0 : Burn.elapsed(carrier.clock, now);
            carrier.clock = now;
            // A Spectator's hands, and the dead's, are paused as an inventory is.
            if (!player.isAlive() || player.isSpectator()) {
                carrier.unburnt = 0;
                continue;
            }
            VanillaLights.convert(player);
            carrier.unburnt += elapsed;
            settleSwappedOut(player, carrier);
            long toBurn = carrier.unburnt >= BURN_STEP ? carrier.unburnt : 0;
            carrier.unburnt -= toBurn;
            burnHeld(player, toBurn);
            carrier.main = player.getMainHandItem();
            carrier.off = player.getOffhandItem();
            int light = heldLight(player);
            if (light > 0) {
                wanted.merge(Lit.eyes(player), light, Math::max);
            }
        }
        for (ServerPlayer player : level.players()) {
            var carrier = player.getData(CARRIER);
            var lit = carrier.lit;
            boolean stillWanted = lit != null && lit.dimension() == level.dimension() && wanted.containsKey(lit.pos());
            if (lit != null && !stillWanted) {
                // From the level they left, too, on a change of dimension.
                var litLevel = level.getServer().getLevel(lit.dimension());
                if (litLevel != null) {
                    remove(litLevel, lit.pos());
                }
            }
            carrier.lit = null;
        }
        wanted.forEach((pos, light) -> {
            if (place(level, pos, light)) {
                level.players().stream().filter(player -> heldLight(player) > 0 && Lit.eyes(player).equals(pos))
                        .forEach(player -> player.getData(CARRIER).lit = GlobalPos.of(level.dimension(), pos));
            }
        });
    }

    /**
     * Once a stack has left the player's hands since last tick, what they held then pays the burn it owes,
     * wherever it went, and the count starts afresh for what they hold now: swapping a Light Source out
     * before its burn is written never dodges it, nor does one swapped in pay for another's.
     */
    private static void settleSwappedOut(ServerPlayer player, Carrier carrier) {
        var was = List.of(carrier.main, carrier.off);
        boolean swapped = was.stream().anyMatch(stack -> stack != player.getMainHandItem() && stack != player.getOffhandItem());
        if (!swapped || carrier.unburnt == 0) {
            return;
        }
        for (var stack : was) {
            if (stack.getItem() instanceof HeldLightSource source) {
                source.settle(stack, player, carrier.unburnt);
            }
        }
        carrier.unburnt = 0;
    }

    private static void burnHeld(ServerPlayer player, long elapsed) {
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof HeldLightSource source) {
                var after = source.burnHeld(stack, player, elapsed);
                if (after != stack) {
                    player.setItemInHand(hand, after);
                }
            }
        }
    }

    /** The light the player carries: the brighter of their hands', and none for a Spectator or the dead. */
    private static int heldLight(ServerPlayer player) {
        if (!player.isAlive() || player.isSpectator()) {
            return 0;
        }
        int light = 0;
        for (var hand : InteractionHand.values()) {
            var stack = player.getItemInHand(hand);
            if (stack.getItem() instanceof HeldLightSource source) {
                light = Math.max(light, source.heldLight(stack));
            }
        }
        return light;
    }

    /** Puts the light at the block, into air or a water source only; true if it is there. */
    private static boolean place(ServerLevel level, BlockPos pos, int light) {
        var state = level.getBlockState(pos);
        var block = Light.CARRIED_LIGHT.get();
        if (state.is(block)) {
            if (state.getValue(CarriedLightBlock.LEVEL) != light) {
                level.setBlockAndUpdate(pos, state.setValue(CarriedLightBlock.LEVEL, light));
            }
            return true;
        }
        boolean water = state.is(Blocks.WATER) && state.getFluidState().isSource();
        if (!state.isAir() && !water) {
            return false;
        }
        level.setBlockAndUpdate(pos, block.defaultBlockState().setValue(CarriedLightBlock.LEVEL, light).setValue(CarriedLightBlock.WATERLOGGED, water));
        return true;
    }

    private static void remove(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        if (state.is(Light.CARRIED_LIGHT)) {
            level.setBlockAndUpdate(pos, CarriedLightBlock.emptied(state));
        }
    }
}
