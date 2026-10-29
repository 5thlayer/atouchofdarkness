// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dial;

import java.util.Arrays;

import io.github._5thlayer.atouchof.meter.Meter;
import io.github._5thlayer.atouchof.meter.MeterState;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.Lit;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
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
 * out of 15, and as its {@link LightState} whether that check finds them Lit; in a dimension the Dark
 * doesn't reach, Lit. Once the Dial is in neither hand the meter is cleared, and no longer drawn. It
 * shows nothing of the Grace Period or the Strikes, has no trend, gives no light and never wears out.
 */
public final class VesperDial {

    public static final Identifier METER_ID = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "light");

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);

    public static final DeferredItem<Item> VESPER_DIAL = ITEMS.registerSimpleItem("vesper_dial", properties -> properties.stacksTo(1));

    /** The light that fills the meter's row, vanilla's brightest. */
    private static final float MAX_LIGHT = 15F;

    /** The trend band the meter always shows: none, so it draws no chevrons. */
    private static final int NO_TREND = 0;

    private static Meter meter;

    private VesperDial() {
    }

    /** Registers the Dial and its meter, from the mod's constructor, as the Runtime requires. */
    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        var states = Arrays.stream(LightState.values()).map(VesperDial::state).toArray(MeterState[]::new);
        meter = Meter.register(METER_ID, MAX_LIGHT, states);
        NeoForge.EVENT_BUS.addListener(VesperDial::tick);
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level)) {
            return;
        }
        for (ServerPlayer player : level.players()) {
            if (player.isHolding(VESPER_DIAL.get())) {
                var reading = Lit.reading(player);
                meter.set(player, reading.light(), LightState.of(reading.litWhereReached()).ordinal(), NO_TREND);
            } else {
                // Told to the client only if it was drawing the meter.
                meter.clear(player);
            }
        }
    }

    /** The icons of a state: {@code hud/light_full}, {@code _half} and {@code _empty}, each followed by the state's suffix. */
    private static MeterState state(LightState state) {
        var suffix = state.suffix();
        return new MeterState(sprite("full" + suffix), sprite("half" + suffix), sprite("empty" + suffix));
    }

    private static Identifier sprite(String name) {
        return Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "hud/light_" + name);
    }
}
