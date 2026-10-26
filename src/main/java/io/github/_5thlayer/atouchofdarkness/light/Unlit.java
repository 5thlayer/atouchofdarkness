// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.ModifyRecipeJsonsEvent;

/**
 * Vanilla's cheap light put out (suite ADR-0003, ADR 0003): the recipes of vanilla's light are gone, and
 * a block in {@code atouchofdarkness:unlit} gives no light at all, for players, mobs and other mods alike.
 * The light config's {@code vanillaLight} switch brings the recipes back and empties the tag, both as the
 * server's data loads; the client is sent the tag as the server has it.
 */
public final class Unlit {

    public static final TagKey<Block> TAG = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "unlit"));

    private static final String[] COLOURS = {"white", "orange", "magenta", "light_blue", "yellow", "lime", "pink", "gray", "light_gray", "cyan",
            "purple", "blue", "brown", "green", "red", "black"};

    private static final String[] OXIDATION = {"", "exposed_", "weathered_", "oxidized_"};

    /** Vanilla's recipes for cheap light, which go while vanilla's light is out. */
    public static final Set<Identifier> RECIPES = Stream.of(
            Stream.of("torch", "soul_torch", "copper_torch", "lantern", "soul_lantern", "copper_lantern", "jack_o_lantern", "glowstone",
                    "sea_lantern", "end_rod", "redstone_lamp", "candle", "campfire", "soul_campfire"),
            Stream.of(OXIDATION).map(stage -> "waxed_" + stage + "copper_lantern_from_honeycomb"),
            Stream.of(OXIDATION).flatMap(stage -> Stream.of(stage + "copper_bulb", "waxed_" + stage + "copper_bulb",
                    "waxed_" + stage + "copper_bulb_from_honeycomb")),
            Stream.of(COLOURS).map(colour -> colour + "_candle")
    ).flatMap(ids -> ids).map(Identifier::withDefaultNamespace).collect(Collectors.toUnmodifiableSet());

    private Unlit() {
    }

    static void register() {
        NeoForge.EVENT_BUS.addListener(Unlit::removeRecipes);
    }

    /** Whether the switch has brought vanilla's light back. */
    public static boolean vanillaLight() {
        return LightConfig.VANILLA_LIGHT.getAsBoolean();
    }

    /** The light this state gives, from what vanilla gives it: none if it is Unlit. */
    public static int emission(BlockState state, int vanilla) {
        // The tag is read only for a state that gives light, since the light engine asks every block.
        return vanilla > 0 && state.is(TAG) ? 0 : vanilla;
    }

    private static void removeRecipes(ModifyRecipeJsonsEvent event) {
        if (!vanillaLight()) {
            event.getRecipeJsons().keySet().removeAll(RECIPES);
        }
    }
}
