// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.compat.emi;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.stream.Stream;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dial.VesperDial;
import io.github._5thlayer.atouchofdarkness.discovery.Discoveries;
import io.github._5thlayer.atouchofdarkness.lampad.Lampad;
import io.github._5thlayer.atouchofdarkness.light.Burn;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import io.github._5thlayer.atouchofdarkness.light.TimeLeft;
import io.github._5thlayer.atouchofdarkness.light.Unlit;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;
import org.jspecify.annotations.Nullable;

/**
 * The Pillar in EMI: its recipes are vanilla's crafting, which EMI shows unaided, so the plugin adds
 * only information pages for what no recipe shows: fuelling a placed light by hand, relighting a Spent
 * Torch, vanilla's lights becoming the Pillar's own, the Nightbrand darkened and the Votive made by
 * summoning a Lampad rather than crafted, the Dark, the Lampad, and vanilla's cheap light having no
 * recipe. Each page on a thing reads as undiscovered until the player has discovered it (suite ADR-0006);
 * the two on the rules of the world, vanilla's lights and their missing recipes, always show.
 *
 * <p>Found by EMI's entrypoint scan and named nowhere else, so EMI stays a soft dependency.
 */
@EmiEntrypoint
public final class DarknessEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // The caps from this side's config, as the tooltips read it.
        info(registry, "fuel", Discoveries.FUEL, stacks(Light.CAMPFIRE_ITEM, Light.SOUL_CAMPFIRE_ITEM, Light.LAMP_ITEM, Light.SOUL_LAMP_ITEM),
                Burn.FUEL_FACTOR, TimeLeft.clock(Light.CAMPFIRE.get().cap()), TimeLeft.clock(Light.SOUL_CAMPFIRE.get().cap()),
                TimeLeft.clock(Light.LAMP.get().cap()), TimeLeft.clock(Light.SOUL_LAMP.get().cap()));
        info(registry, "relight", Discoveries.SPENT_TORCH, stacks(Light.SPENT_TORCH));
        info(registry, "vanilla_lights", null, Stream.concat(
                Stream.of(EmiIngredient.of(ItemTags.LANTERNS)),
                stacks(Items.TORCH, Items.SOUL_TORCH, Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.SOUL_LANTERN,
                        Light.TORCH, Light.SOUL_TORCH, Light.CAMPFIRE_ITEM, Light.SOUL_CAMPFIRE_ITEM, Light.LAMP_ITEM, Light.SOUL_LAMP_ITEM)));
        info(registry, "nightbrand", Discoveries.NIGHTBRAND, stacks(Nightbrand.NIGHTBRAND, Light.SPENT_TORCH), TimeLeft.clock(Nightbrand.fullDarkening()));
        info(registry, "votive", Discoveries.VOTIVE, stacks(Lampad.VOTIVE_ITEM, Items.GOLD_BLOCK, Light.LAMP_ITEM, Light.SOUL_LAMP_ITEM));
        info(registry, "dark", Discoveries.DARK, stacks(Light.TORCH, Light.SOUL_TORCH, Light.LAMP_ITEM, VesperDial.VESPER_DIAL));
        info(registry, "lampad", Discoveries.LAMPAD, stacks(Lampad.VOTIVE_ITEM, Nightbrand.NIGHTBRAND, Light.LAMP_ITEM),
                LightConfig.LAMPAD_LAMPS_PER_NUGGET.getAsInt(), Math.ceilDiv(LightConfig.LAMPAD_BINDING.getAsInt(), LightConfig.DAY));
        // Each gone recipe's item, which its id names, waxing from honeycomb too.
        info(registry, "no_recipe", null, Unlit.RECIPES.stream().map(id -> id.withPath(path -> path.replace("_from_honeycomb", ""))).distinct().sorted()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream()).map(EmiStack::of));
    }

    /**
     * A page on these stacks, its text the translation {@code emi.atouchofdarkness.info.<name>}, which
     * reads as undiscovered until the player has discovered {@code discovery}; with none, it always shows.
     */
    private static void info(EmiRegistry registry, String name, @Nullable String discovery, Stream<? extends EmiIngredient> stacks, Object... args) {
        List<EmiIngredient> ingredients = stacks.map(EmiIngredient.class::cast).toList();
        // EMI's own ids start with a slash for a recipe no data pack holds.
        var id = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "/info/" + name);
        BooleanSupplier known = discovery == null ? () -> true : () -> Discoveries.record().isKnownOnClient(discovery);
        registry.addRecipe(new InfoPage(ingredients, Component.translatable("emi.atouchofdarkness.info." + name, args), known, id));
    }

    private static Stream<EmiStack> stacks(ItemLike... items) {
        return Stream.of(items).map(EmiStack::of);
    }
}
