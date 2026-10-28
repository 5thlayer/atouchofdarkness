// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.discovery;

import java.util.List;
import java.util.function.Supplier;

import io.github._5thlayer.atouchof.discovery.Discovery;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's Discoveries (suite ADR-0006), each the advancement {@code atouchofdarkness:discovery/<id>}
 * in its own tab, which the Runtime keeps as the record. What discovers each is its advancement's
 * criteria: holding a thing is vanilla's {@code inventory_changed}, summoning a Lampad vanilla's
 * {@code summoned_entity}, and taking a Strike or a Spent Torch darkening the Pillar's own triggers here.
 * The {@code discovery} config switches them all off, every thing then known.
 */
public final class Discoveries {

    /** Holding a Lamp, Soul Lamp, Campfire or Soul Campfire, which opens the page on fuelling them. */
    public static final String FUEL = "fuel";
    /** Holding a Spent Torch, which opens the page on relighting it. */
    public static final String SPENT_TORCH = "spent_torch";
    /** Holding a Spent Torch that has begun darkening, which opens the Nightbrand's page. */
    public static final String NIGHTBRAND = "nightbrand";
    /** Holding a Nightbrand, which opens the Votive's page. */
    public static final String VOTIVE = "votive";
    /** Taking a Strike, which opens the page on the Dark. */
    public static final String DARK = "dark";
    /** Summoning a Lampad, which opens her page. */
    public static final String LAMPAD = "lampad";

    public static final List<String> IDS = List.of(FUEL, SPENT_TORCH, NIGHTBRAND, VOTIVE, DARK, LAMPAD);

    private static final DeferredRegister<CriterionTrigger<?>> TRIGGERS = DeferredRegister.create(Registries.TRIGGER_TYPE, ATouchOfDarkness.MOD_ID);

    /** {@code atouchofdarkness:struck}: the Dark struck the player. */
    private static final Supplier<PlayerTrigger> STRUCK = TRIGGERS.register("struck", PlayerTrigger::new);
    /** {@code atouchofdarkness:darkening}: a Spent Torch in the player's hand darkened a tick, its first or a later one. */
    private static final Supplier<PlayerTrigger> DARKENING = TRIGGERS.register("darkening", PlayerTrigger::new);

    private static Discovery record;

    private Discoveries() {
    }

    public static void register(IEventBus modBus) {
        TRIGGERS.register(modBus);
        record = Discovery.register(modBus, ATouchOfDarkness.MOD_ID, IDS, DarkConfig.DISCOVERY::getAsBoolean);
    }

    /** The Runtime's record of the Pillar's Discoveries: what each player knows, and what this client was told. */
    public static Discovery record() {
        return record;
    }

    public static void struck(ServerPlayer player) {
        STRUCK.get().trigger(player);
    }

    public static void darkening(ServerPlayer player) {
        DARKENING.get().trigger(player);
    }
}
