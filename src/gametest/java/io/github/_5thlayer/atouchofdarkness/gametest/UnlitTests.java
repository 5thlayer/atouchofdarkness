// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.settleLight;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import io.github._5thlayer.atouchofdarkness.light.Unlit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.AmethystClusterBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CandleCakeBlock;
import net.minecraft.world.level.block.CaveVines;
import net.minecraft.world.level.block.CopperBulbBlock;
import net.minecraft.world.level.block.GlowLichenBlock;
import net.minecraft.world.level.block.RedStoneOreBlock;
import net.minecraft.world.level.block.RedStoneWireBlock;
import net.minecraft.world.level.block.RedstoneLampBlock;
import net.minecraft.world.level.block.RedstoneTorchBlock;
import net.minecraft.world.level.block.RedstoneWallTorchBlock;
import net.minecraft.world.level.block.SeaPickleBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

/**
 * Vanilla's cheap light put out: its recipes gone, and every Unlit block, the Pillar's and a pack's,
 * reading 0 block light, until the {@code vanillaLight} switch brings both back, in a batch of its own.
 * The kept lights keep theirs. Every test's name starts {@code unlit_}.
 */
final class UnlitTests {

    /** Where each block is placed in turn, on the platform's floor. */
    private static final BlockPos AT = new BlockPos(4, 1, 4);

    private UnlitTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("unlit_vanilla_light_has_no_recipes", 1, UnlitTests::noRecipes);
        tests.test("unlit_blocks_read_0_block_light_a_packs_too", 1, UnlitTests::unlitBlocks);
        tests.test("unlit_kept_lights_keep_their_light", 1, UnlitTests::keptLights);
        tests.test("unlit_redstone_torch_still_powers_redstone", 5, UnlitTests::redstoneTorchPowers);
        tests.in("unlit_vanilla_light", WithVanillaLight.INSTANCE)
                .test("unlit_vanilla_light_switch_brings_back_the_recipes_and_the_light", 1, UnlitTests::vanillaLight);
    }

    /** Each listed recipe is absent. */
    private static void noRecipes(GameTestHelper helper) {
        var present = recipes(helper, true);
        helper.assertTrue(present.isEmpty(), "vanilla light recipes still there: " + present);
        helper.succeed();
    }

    /** Each default Unlit block, lit where it can be, and the block the game-test pack adds to the tag, reads 0. */
    private static void unlitBlocks(GameTestHelper helper) {
        var lit = new ArrayList<String>();
        unlit().keySet().forEach(state -> {
            int light = lightOf(helper, state);
            if (light != 0) {
                lit.add(state + " " + light);
            }
        });
        helper.assertTrue(lit.isEmpty(), "Unlit blocks gave light: " + lit);
        helper.succeed();
    }

    /** The lights the Pillar leaves alone, vanilla's torches and lanterns placed in the world and natural light, give vanilla's. */
    private static void keptLights(GameTestHelper helper) {
        assertLights(helper, kept());
        helper.succeed();
    }

    /** A redstone torch, which gives no light, still powers the wire beside it. */
    private static void redstoneTorchPowers(GameTestHelper helper) {
        var wire = AT.east();
        helper.setBlock(AT, Blocks.REDSTONE_TORCH);
        helper.setBlock(wire, Blocks.REDSTONE_WIRE);
        helper.succeedWhen(() -> helper.assertBlockProperty(wire, RedStoneWireBlock.POWER, 15));
    }

    /** With the switch on, every listed recipe is back, and every Unlit block, the pack's too, gives vanilla's light. */
    private static void vanillaLight(GameTestHelper helper) {
        var missing = recipes(helper, false);
        helper.assertTrue(missing.isEmpty(), "vanilla light recipes still gone: " + missing);
        assertLights(helper, unlit());
        helper.succeed();
    }

    /** The listed recipes that are present, or else absent. */
    private static List<Identifier> recipes(GameTestHelper helper, boolean present) {
        var recipes = helper.getLevel().getServer().getRecipeManager();
        return Unlit.RECIPES.stream()
                .filter(id -> recipes.byKey(ResourceKey.create(Registries.RECIPE, id)).isPresent() == present)
                .sorted()
                .toList();
    }

    private static void assertLights(GameTestHelper helper, Map<BlockState, Integer> expected) {
        var wrong = new ArrayList<String>();
        expected.forEach((state, light) -> {
            int actual = lightOf(helper, state);
            if (actual != light) {
                wrong.add(state + " " + actual + ", not " + light);
            }
        });
        helper.assertTrue(wrong.isEmpty(), "wrong light: " + wrong);
    }

    /** The block light the state reads placed alone, then gone again; none of them is checked for survival. */
    private static int lightOf(GameTestHelper helper, BlockState state) {
        helper.setBlock(AT, state);
        settleLight(helper, AT);
        int light = helper.getLevel().getBrightness(LightLayer.BLOCK, helper.absolutePos(AT));
        helper.setBlock(AT, Blocks.AIR);
        settleLight(helper, AT);
        return light;
    }

    /** Each Unlit block, lit where it can be, with vanilla's light. */
    private static Map<BlockState, Integer> unlit() {
        var states = new LinkedHashMap<BlockState, Integer>();
        states.put(Blocks.REDSTONE_TORCH.defaultBlockState().setValue(RedstoneTorchBlock.LIT, true), 7);
        states.put(Blocks.REDSTONE_WALL_TORCH.defaultBlockState().setValue(RedstoneWallTorchBlock.LIT, true), 7);
        states.put(Blocks.REDSTONE_ORE.defaultBlockState().setValue(RedStoneOreBlock.LIT, true), 9);
        states.put(Blocks.DEEPSLATE_REDSTONE_ORE.defaultBlockState().setValue(RedStoneOreBlock.LIT, true), 9);
        put(states, 7, Blocks.ENCHANTING_TABLE, Blocks.ENDER_CHEST);
        put(states, 3, Blocks.MAGMA_BLOCK);
        states.put(Blocks.GLOW_LICHEN.defaultBlockState().setValue(GlowLichenBlock.getFaceProperty(Direction.DOWN), true), 7);
        states.put(Blocks.CAVE_VINES.defaultBlockState().setValue(CaveVines.BERRIES, true), 14);
        states.put(Blocks.CAVE_VINES_PLANT.defaultBlockState().setValue(CaveVines.BERRIES, true), 14);
        states.put(Blocks.SEA_PICKLE.defaultBlockState().setValue(SeaPickleBlock.WATERLOGGED, true), 6);
        states.put(Blocks.AMETHYST_CLUSTER.defaultBlockState().setValue(AmethystClusterBlock.FACING, Direction.UP), 5);
        put(states, 4, Blocks.LARGE_AMETHYST_BUD);
        put(states, 2, Blocks.MEDIUM_AMETHYST_BUD, Blocks.FIREFLY_BUSH);
        put(states, 1, Blocks.SMALL_AMETHYST_BUD);
        put(states, 6, Blocks.SCULK_CATALYST);
        states.put(Blocks.REDSTONE_LAMP.defaultBlockState().setValue(RedstoneLampBlock.LIT, true), 15);
        bulbs(states, 15, Blocks.COPPER_BULB, Blocks.WAXED_COPPER_BULB);
        bulbs(states, 12, Blocks.EXPOSED_COPPER_BULB, Blocks.WAXED_EXPOSED_COPPER_BULB);
        bulbs(states, 8, Blocks.WEATHERED_COPPER_BULB, Blocks.WAXED_WEATHERED_COPPER_BULB);
        bulbs(states, 4, Blocks.OXIDIZED_COPPER_BULB, Blocks.WAXED_OXIDIZED_COPPER_BULB);
        for (var candle : List.of(Blocks.CANDLE, Blocks.WHITE_CANDLE, Blocks.ORANGE_CANDLE, Blocks.MAGENTA_CANDLE, Blocks.LIGHT_BLUE_CANDLE,
                Blocks.YELLOW_CANDLE, Blocks.LIME_CANDLE, Blocks.PINK_CANDLE, Blocks.GRAY_CANDLE, Blocks.LIGHT_GRAY_CANDLE, Blocks.CYAN_CANDLE,
                Blocks.PURPLE_CANDLE, Blocks.BLUE_CANDLE, Blocks.BROWN_CANDLE, Blocks.GREEN_CANDLE, Blocks.RED_CANDLE, Blocks.BLACK_CANDLE)) {
            states.put(candle.defaultBlockState().setValue(CandleBlock.LIT, true), 3);
            states.put(CandleCakeBlock.byCandle((CandleBlock) candle).setValue(CandleCakeBlock.LIT, true), 3);
        }
        // The game-test pack's, as a pack would add another mod's lamp.
        put(states, 15, Blocks.VERDANT_FROGLIGHT);
        return states;
    }

    /** Lights a pack can't make cheaply, or the Pillar leaves placed in the world, with vanilla's light. */
    private static Map<BlockState, Integer> kept() {
        var states = new LinkedHashMap<BlockState, Integer>();
        put(states, 14, Blocks.TORCH, Blocks.COPPER_TORCH, Blocks.END_ROD);
        put(states, 10, Blocks.SOUL_TORCH, Blocks.SOUL_LANTERN);
        put(states, 15, Blocks.LANTERN, Blocks.COPPER_LANTERN.unaffected(), Blocks.JACK_O_LANTERN, Blocks.GLOWSTONE, Blocks.SEA_LANTERN, Blocks.SHROOMLIGHT,
                Blocks.OCHRE_FROGLIGHT, Blocks.PEARLESCENT_FROGLIGHT, Blocks.BEACON, Blocks.LAVA, Blocks.CAMPFIRE);
        return states;
    }

    private static void put(Map<BlockState, Integer> states, int light, Block... blocks) {
        for (var block : blocks) {
            states.put(block.defaultBlockState(), light);
        }
    }

    private static void bulbs(Map<BlockState, Integer> states, int light, Block... bulbs) {
        for (var bulb : bulbs) {
            states.put(bulb.defaultBlockState().setValue(CopperBulbBlock.LIT, true), light);
        }
    }
}
