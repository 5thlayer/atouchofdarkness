// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.nightbrand;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Nightbrand (GLOSSARY.md): every game tick, a single Spent Torch in either hand of a player in the
 * Dark soaks it up, the main hand's alone if both hold one, its {@link Darkening} kept between dips, until
 * it has darkened for the light config's Grace Periods and is a Nightbrand. Each player darkens one a
 * day: once they have, no torch of theirs darkens until the next dawn. A Nightbrand keeps forever and gives no light: it is no Light Source.
 */
public final class Nightbrand {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ATouchOfDarkness.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Darkening>> DARKENING = COMPONENTS.registerComponentType("darkening",
            builder -> builder.persistent(Darkening.CODEC).networkSynchronized(Darkening.STREAM_CODEC));

    public static final DeferredItem<Item> NIGHTBRAND = ITEMS.registerSimpleItem("nightbrand");

    /** The day of a player's last Nightbrand: none yet. */
    private static final long NEVER = Long.MIN_VALUE;

    /** Ticks of the time of day from one dawn to the next. */
    private static final long DAY = 24000;

    /**
     * The day, counted in dawns of the Overworld's clock, on which the player last darkened a Nightbrand.
     * Copied on death, so dying never buys a second one that day.
     */
    private static final Supplier<AttachmentType<Long>> DARKENED_ON = ATTACHMENTS.register("darkened_on",
            () -> AttachmentType.builder(() -> NEVER).serialize(Codec.LONG.fieldOf("day")).copyOnDeath().build());

    private Nightbrand() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        NeoForge.EVENT_BUS.addListener(Nightbrand::tick);
    }

    /** The full darkening the config sets now, so many Grace Periods. */
    public static int fullDarkening() {
        return Darken.full(LightConfig.NIGHTBRAND_GRACE_PERIODS.getAsInt(), DarkConfig.GRACE_PERIOD.getAsInt());
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) {
            return;
        }
        int full = fullDarkening();
        long today = Math.floorDiv(level.getOverworldClockTime(), DAY);
        for (ServerPlayer player : level.players()) {
            // A machine's fake player in the world is no one who took the risk.
            if (!player.isAlive() || player.isFakePlayer() || player.getData(DARKENED_ON) == today || !Dark.inTheDark(player)) {
                continue;
            }
            for (var hand : InteractionHand.values()) {
                var stack = player.getItemInHand(hand);
                // One alone, in one hand, so only one torch at a time is ever darkened.
                if (!stack.is(Light.SPENT_TORCH) || stack.getCount() != 1) {
                    continue;
                }
                var darkening = stack.getOrDefault(DARKENING, new Darkening(0, full)).darken(1, full);
                if (darkening.done()) {
                    player.setItemInHand(hand, NIGHTBRAND.toStack());
                    player.setData(DARKENED_ON, today);
                } else {
                    stack.set(DARKENING, darkening);
                }
                // The main hand's first: one in the other hand waits until it is put away.
                break;
            }
        }
    }
}
