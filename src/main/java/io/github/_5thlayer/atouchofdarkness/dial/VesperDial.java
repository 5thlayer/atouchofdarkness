// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

import io.github._5thlayer.atouchof.meter.Meter;
import io.github._5thlayer.atouchof.meter.MeterState;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.Dark;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Vesper Dial (GLOSSARY.md): a gold instrument set around a Nightbrand which, while in either of
 * a player's hands, shows them the light meter {@code atouchofdarkness:light}, drawn by the Runtime
 * (suite ADR-0005). Each tick it hands the meter the light at their eyes as the Lit check measures it,
 * and whether that check finds them Lit, as a {@link LightReading}; in a dimension the Dark doesn't
 * reach, Lit. It shows nothing of the Grace Period or the Strikes, has no trend, gives no light and
 * never wears out.
 */
public final class VesperDial {

    public static final Identifier METER_ID = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "light");

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);

    public static final DeferredItem<Item> VESPER_DIAL = ITEMS.registerSimpleItem("vesper_dial", properties -> properties.stacksTo(1));

    private static Meter meter;

    private VesperDial() {
    }

    /** Registers the Dial and its meter, from the mod's constructor, as the Runtime requires. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        // In LightReading's order of states.
        meter = Meter.register(METER_ID, LightReading.MAX, state(""), state("_dark"));
        NeoForge.EVENT_BUS.addListener(VesperDial::tick);
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (carried(player)) {
                var reading = LightReading.of(Dark.lightAtEyes(player), !Dark.reaches(player) || Dark.isLit(player));
                meter.set(player, reading.value(), reading.state(), 0);
            }
        }
    }

    /** Whether the player holds a Dial in their main or off hand. */
    public static boolean carried(ServerPlayer player) {
        for (var hand : InteractionHand.values()) {
            if (player.getItemInHand(hand).is(VESPER_DIAL)) {
                return true;
            }
        }
        return false;
    }

    /** The icons of a state: {@code hud/light_full}, {@code _half} and {@code _empty}, with {@code _dark} after them In the Dark. */
    private static MeterState state(String suffix) {
        return new MeterState(sprite("full" + suffix), sprite("half" + suffix), sprite("empty" + suffix));
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "hud/light_" + name);
    }
}
