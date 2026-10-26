// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.compat.emi;

import java.util.List;
import java.util.stream.Stream;

import dev.emi.emi.api.EmiEntrypoint;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;
import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.light.Burn;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.TimeLeft;
import io.github._5thlayer.atouchofdarkness.light.Unlit;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ItemLike;

/**
 * The Pillar in EMI: its recipes are vanilla's crafting, which EMI shows unaided, so the plugin adds
 * only information pages for what no recipe shows: fuelling a placed light by hand, relighting a Spent
 * Torch, vanilla's lights becoming the Pillar's own, and vanilla's cheap light having no recipe.
 *
 * <p>Found by EMI's entrypoint scan and named nowhere else, so EMI stays a soft dependency.
 */
@EmiEntrypoint
public final class DarknessEmiPlugin implements EmiPlugin {

    @Override
    public void register(EmiRegistry registry) {
        // The caps from this side's config, as the tooltips read it.
        info(registry, "fuel", stacks(Light.CAMPFIRE_ITEM, Light.SOUL_CAMPFIRE_ITEM, Light.LAMP_ITEM, Light.SOUL_LAMP_ITEM), Burn.FUEL_FACTOR,
                TimeLeft.clock(Light.CAMPFIRE.get().cap()), TimeLeft.clock(Light.SOUL_CAMPFIRE.get().cap()),
                TimeLeft.clock(Light.LAMP.get().cap()), TimeLeft.clock(Light.SOUL_LAMP.get().cap()));
        info(registry, "relight", stacks(Light.SPENT_TORCH));
        info(registry, "vanilla_lights", Stream.concat(
                Stream.of(EmiIngredient.of(ItemTags.LANTERNS)),
                stacks(Items.TORCH, Items.SOUL_TORCH, Items.CAMPFIRE, Items.SOUL_CAMPFIRE, Items.SOUL_LANTERN,
                        Light.TORCH, Light.SOUL_TORCH, Light.CAMPFIRE_ITEM, Light.SOUL_CAMPFIRE_ITEM, Light.LAMP_ITEM, Light.SOUL_LAMP_ITEM)));
        // Each gone recipe's item, which its id names, waxing from honeycomb too.
        info(registry, "no_recipe", Unlit.RECIPES.stream().map(id -> id.withPath(path -> path.replace("_from_honeycomb", ""))).distinct().sorted()
                .flatMap(id -> BuiltInRegistries.ITEM.getOptional(id).stream()).map(EmiStack::of));
    }

    /** A page on these stacks, its text the translation {@code emi.atouchofdarkness.info.<name>}. */
    private static void info(EmiRegistry registry, String name, Stream<? extends EmiIngredient> stacks, Object... args) {
        List<EmiIngredient> ingredients = stacks.map(EmiIngredient.class::cast).toList();
        // EMI's own ids start with a slash for a recipe no data pack holds.
        var id = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "/info/" + name);
        registry.addRecipe(new EmiInfoRecipe(ingredients, List.of(Component.translatable("emi.atouchofdarkness.info." + name, args)), id));
    }

    private static Stream<EmiStack> stacks(ItemLike... items) {
        return Stream.of(items).map(EmiStack::of);
    }
}
