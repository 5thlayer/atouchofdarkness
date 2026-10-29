// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;
import java.util.function.BooleanSupplier;

import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.github._5thlayer.atouchofdarkness.lampad.Lampad;
import io.github._5thlayer.atouchofdarkness.lampad.LampadEntity;
import io.github._5thlayer.atouchofdarkness.lampad.VotiveBlockEntity;
import io.github._5thlayer.atouchofdarkness.light.LampBlock;
import io.github._5thlayer.atouchofdarkness.light.LampItem;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.PlacedFuel;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestEnvironmentDefinition.Weather;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.entity.HopperBlockEntity;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.TagValueOutput;

/**
 * The Lampad: summoned from a Lamp on gold kindled with a Torch, the gold her Votive, and tending the
 * placed Lamps around it from what the Votive holds. What she does is read from what a player sees: a
 * light's fuel, the Votive's contents, whether she stands dormant. Every test's name starts
 * {@code lampad_}.
 */
final class LampadTests {

    private LampadTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("lampad_a_torch_kindles_a_lamp_on_gold_into_her_and_her_votive_one_to_a_votive", 10, LampadTests::summons);
        tests.test("lampad_votive_slots_take_only_fuel_a_nightbrand_and_gold_and_hoppers_fill_all_three", 120, LampadTests::votiveSlots);
        tests.test("lampad_counts_lamps_32_across_and_8_up_or_down_each_to_the_nearest_votive_a_nugget_per_8", 40, LampadTests::counts);
        tests.test("lampad_gives_no_light_and_is_never_struck", QUIET + 5, LampadTests::unlitUnstruck);
        tests.in("lampad_unloaded", KeepsClock.INSTANCE)
                .test("lampad_does_nothing_while_unloaded_and_catches_nothing_up", 600, LampadTests::unloaded);
        tests.in("lampad_binding", WithLampadConfig.INSTANCE)
                .test("lampad_binding_fades_to_a_statue_and_each_nightbrand_from_her_votive_renews_it", 260, LampadTests::binding);
        tests.test("lampad_feeds_the_paid_lamps_nearest_her_votive_a_whole_item_each_never_a_campfire_nor_a_carried_lamp", 2000, LampadTests::tends);
        tests.test("lampad_has_12_health_and_mobs_treat_her_as_a_passive_mob", 80, LampadTests::passive);
        tests.in("lampad_healing", new AtNight(MoonPhase.WANING_GIBBOUS, Weather.Type.CLEAR), WithLampadConfig.INSTANCE)
                .test("lampad_hurt_heals_in_the_dark_tending_nothing_till_whole_or_waits_where_lit_her_binding_fading", 1200, LampadTests::heals);
        tests.test("lampad_a_broken_votive_spills_and_leaves_a_statue_for_good_and_death_or_a_broken_statue_drops_her_lamp_and_nightbrand",
                60, LampadTests::lost);
    }

    /**
     * A Lamp and a soul Lamp, each on a gold block, kindled with a Torch: each becomes a Lampad and its
     * gold a Votive. A second Lamp kindled on the first Votive stays a Lamp; once that Lampad is dead, it
     * becomes a Lampad too.
     */
    private static void summons(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var votive = new BlockPos(2, 1, 2);
        var soulVotive = new BlockPos(6, 1, 2);
        for (var gold : new BlockPos[] {votive, soulVotive}) {
            helper.setBlock(gold, Blocks.GOLD_BLOCK);
        }
        LampTests.lamp(helper, Light.LAMP.get(), votive.above(), 1000);
        LampTests.lamp(helper, Light.SOUL_LAMP.get(), soulVotive.above(), 0);
        LampTests.useOn(helper, player, Light.TORCH.toStack(), votive.above());
        LampTests.useOn(helper, player, Light.SOUL_TORCH.toStack(), soulVotive.above());
        for (var at : new BlockPos[] {votive, soulVotive}) {
            helper.assertBlockPresent(Lampad.VOTIVE.get(), at);
            helper.assertBlockNotPresent(Light.LAMP.get(), at.above());
            helper.assertBlockNotPresent(Light.SOUL_LAMP.get(), at.above());
        }
        assertLampads(helper, 2);

        var first = helper.getEntities(Lampad.LAMPAD.get(), votive.above(), 1.5).getFirst();
        LampTests.lamp(helper, Light.LAMP.get(), votive.above(), 1000);
        LampTests.useOn(helper, player, Light.TORCH.toStack(), votive.above());
        helper.assertBlockPresent(Light.LAMP.get(), votive.above());
        assertLampads(helper, 2);
        first.kill(helper.getLevel());
        LampTests.useOn(helper, player, Light.TORCH.toStack(), votive.above());
        helper.assertBlockNotPresent(Light.LAMP.get(), votive.above());
        assertLampads(helper, 2);
        leave(player);
        helper.succeed();
    }

    /**
     * A hopper above a Votive holding coal, a Nightbrand, gold nuggets, cobblestone and a bucket of lava,
     * and one beside it holding coal, put the coal, the Nightbrand and the gold in its three slots and
     * keep the rest. No slot takes another's items.
     */
    private static void votiveSlots(GameTestHelper helper) {
        var at = new BlockPos(4, 1, 4);
        helper.setBlock(at, Lampad.VOTIVE.get());
        helper.setBlock(at.above(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.DOWN));
        helper.setBlock(at.west(), Blocks.HOPPER.defaultBlockState().setValue(HopperBlock.FACING, Direction.EAST));
        var above = helper.getBlockEntity(at.above(), HopperBlockEntity.class);
        above.setItem(0, new ItemStack(Items.COAL, 2));
        above.setItem(1, Nightbrand.NIGHTBRAND.toStack());
        above.setItem(2, new ItemStack(Items.GOLD_NUGGET, 3));
        above.setItem(3, new ItemStack(Items.COBBLESTONE));
        above.setItem(4, new ItemStack(Items.LAVA_BUCKET));
        helper.getBlockEntity(at.west(), HopperBlockEntity.class).setItem(0, new ItemStack(Items.COAL));
        var votive = helper.getBlockEntity(at, VotiveBlockEntity.class);
        var coal = new ItemStack(Items.COAL);
        var nugget = new ItemStack(Items.GOLD_NUGGET);
        var nightbrand = Nightbrand.NIGHTBRAND.toStack();
        helper.assertFalse(votive.canPlaceItem(VotiveBlockEntity.FUEL, nugget) || votive.canPlaceItem(VotiveBlockEntity.FUEL, nightbrand),
                "the fuel slot takes gold or a Nightbrand");
        helper.assertFalse(votive.canPlaceItem(VotiveBlockEntity.NIGHTBRAND, coal) || votive.canPlaceItem(VotiveBlockEntity.NIGHTBRAND, nugget),
                "the Nightbrand slot takes coal or gold");
        helper.assertFalse(votive.canPlaceItem(VotiveBlockEntity.GOLD, coal) || votive.canPlaceItem(VotiveBlockEntity.GOLD, nightbrand),
                "the gold slot takes coal or a Nightbrand");
        helper.runAtTickTime(100, () -> {
            assertHolds(helper, votive, VotiveBlockEntity.FUEL, Items.COAL, 3);
            assertHolds(helper, votive, VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.get(), 1);
            assertHolds(helper, votive, VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 3);
            helper.assertTrue(above.getItem(3).is(Items.COBBLESTONE) && above.getItem(4).is(Items.LAVA_BUCKET),
                    "the hopper above kept " + above.getItem(3) + " and " + above.getItem(4));
            helper.succeed();
        });
    }

    /**
     * A Lampad summoned on gold, her Votive then given 20 coal, a Nightbrand and a single gold nugget, among
     * nine dry Lamps, a soul Campfire with room for a coal and a player holding a dry Lamp. The nugget
     * pays for 8 of the 9 Lamps, and she puts a coal into each of those 8 nearest the Votive that a whole
     * coal fits, its 16000 ticks: the one that's off stays off, the one that's on shines, and one nearly
     * full gets none. Long after, the farthest Lamp, the Campfire and the carried Lamp still have none.
     * A second nugget put in her Votive then pays for the farthest Lamp, and she feeds it.
     */
    private static void tends(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, new BlockPos(7, 1, 6), player);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.LAMP_ITEM.toStack());
        var votive = new BlockPos(0, 1, 0);
        var onDry = new BlockPos(0, 1, 2);
        var nearlyFull = new BlockPos(2, 1, 2);
        var farthest = new BlockPos(8, 1, 8);
        var campfire = new BlockPos(1, 1, 1);
        var fed = List.of(new BlockPos(2, 1, 0), onDry, new BlockPos(4, 1, 0), new BlockPos(0, 1, 4), new BlockPos(4, 1, 2),
                new BlockPos(2, 1, 4), new BlockPos(4, 1, 4));
        for (var lamp : fed) {
            LampTests.lamp(helper, Light.LAMP.get(), lamp, 0);
        }
        helper.setBlock(onDry, Light.LAMP.get().defaultBlockState().setValue(LampBlock.ON, true));
        LampTests.lamp(helper, Light.LAMP.get(), farthest, 0);
        helper.setBlock(nearlyFull, Light.LAMP.get());
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(nearlyFull), LampTests.CAP - 1000);
        helper.setBlock(campfire, Light.SOUL_CAMPFIRE.get());
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(campfire), 2000);
        var votiveEntity = summon(helper, player, votive);
        votiveEntity.setItem(VotiveBlockEntity.FUEL, new ItemStack(Items.COAL, 20));
        votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        votiveEntity.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET));

        var fedAt = new long[] {-1};
        helper.onEachTick(() -> {
            if (fedAt[0] < 0) {
                if (fed.stream().allMatch(lamp -> PlacedFuel.left(helper.getLevel(), helper.absolutePos(lamp)) > 0)) {
                    fedAt[0] = helper.getTick();
                }
            } else if (helper.getTick() == fedAt[0] + 200) {
                for (var lamp : fed) {
                    int left = PlacedFuel.left(helper.getLevel(), helper.absolutePos(lamp));
                    helper.assertTrue(lamp.equals(onDry) ? left > 15_000 && left <= 16_000 : left == 16_000,
                            "the Lamp at " + lamp + " holds " + left + ", not a coal's 16000");
                }
                helper.assertTrue(helper.getBlockState(onDry).getValue(LampBlock.LIT), "the Lamp on but dry doesn't shine once fed");
                LampTests.assertPlaced(helper, new BlockPos(2, 1, 0), false, 16_000);
                LampTests.assertPlaced(helper, nearlyFull, false, LampTests.CAP - 1000);
                LampTests.assertPlaced(helper, farthest, false, 0);
                var fire = PlacedFuel.left(helper.getLevel(), helper.absolutePos(campfire));
                helper.assertTrue(fire <= 2000, "the Lampad fed the Campfire, to " + fire);
                helper.assertTrue(player.getMainHandItem().getOrDefault(Light.LAMP_FUEL, 0) == 0, "the Lampad fed a carried Lamp");
                assertHolds(helper, votiveEntity, VotiveBlockEntity.FUEL, Items.COAL, 20 - fed.size());
                assertHolds(helper, votiveEntity, VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 0);
                votiveEntity.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET));
            } else if (helper.getTick() > fedAt[0] + 200 && PlacedFuel.left(helper.getLevel(), helper.absolutePos(farthest)) == 16_000) {
                assertHolds(helper, votiveEntity, VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 0);
                leave(player);
                helper.succeed();
            }
        });
    }

    /**
     * Far above the platform, clear of every other test, two Lampads' Votives 12 blocks apart, each with
     * 5 gold nuggets and a Nightbrand, among Lamps. Nine are the first's: at the edges of her range, 16
     * blocks across either way and 8 up or down, near her, and one as near the other Votive, which goes to
     * the first in block order. Eight are the other's: nearer it, or in its range alone. Lamps a block past
     * the first's range are neither's, and Campfires never count. The first's renewal takes 2 nuggets, the
     * other's 1.
     */
    private static void counts(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var votive = new BlockPos(4, 40, 4);
        var other = votive.east(12);
        var hers = List.of(votive.west(16), votive.north(16), votive.above(8), votive.below(8), votive.west(16).north(16).above(8),
                votive.east(2), votive.south(2), votive.west(2).north(2), votive.east(6));
        var others = List.of(other.west(4), other.east(4), other.south(3), other.north(3), other.above(8).south(), other.below(8).south(),
                other.east(10), other.east(16));
        // Apart, so none is the block a lantern in the air would need to stand or hang on.
        var neither = List.of(votive.west(17), votive.north(17), votive.above(9).east(), votive.below(9).east());
        // Kept loaded, as a player near would keep them: only Lamps in loaded chunks count.
        var chunks = forceLoaded(helper, votive.west(17).north(17), other.east(16).south(3));
        for (var lamp : hers) {
            helper.setBlock(lamp, Light.LAMP.get());
        }
        for (var lamp : others) {
            helper.setBlock(lamp, Light.SOUL_LAMP.get());
        }
        for (var lamp : neither) {
            helper.setBlock(lamp, Light.LAMP.get());
        }
        helper.setBlock(votive.south(4), Light.CAMPFIRE.get());
        helper.setBlock(other.north(5), Light.SOUL_CAMPFIRE.get());
        var votiveEntities = List.of(summon(helper, player, votive), summon(helper, player, other));
        for (var votiveEntity : votiveEntities) {
            votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
            votiveEntity.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET, 5));
        }
        // Once both Lampads have ticked.
        helper.succeedWhen(() -> {
            assertHolds(helper, votiveEntities.get(0), VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.get(), 0);
            assertHolds(helper, votiveEntities.get(1), VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.get(), 0);
            assertHolds(helper, votiveEntities.get(0), VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 3);
            assertHolds(helper, votiveEntities.get(1), VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 4);
            chunks.forEach(chunk -> helper.getLevel().setChunkForced(chunk.x(), chunk.z(), false));
            leave(player);
        });
    }

    /**
     * Forces the chunks from one corner to the other loaded, and holds the server until each ticks its
     * entities; those it forced. The game-test server ticks unthrottled while a forced chunk loads off its
     * thread, so the hold is in wall time, as vanilla's own for a test's structure is, not in ticks.
     */
    private static List<ChunkPos> forceLoaded(GameTestHelper helper, BlockPos from, BlockPos to) {
        var level = helper.getLevel();
        var low = ChunkPos.containing(helper.absolutePos(from));
        var high = ChunkPos.containing(helper.absolutePos(to));
        var forced = new ArrayList<ChunkPos>();
        for (int x = Math.min(low.x(), high.x()); x <= Math.max(low.x(), high.x()); x++) {
            for (int z = Math.min(low.z(), high.z()); z <= Math.max(low.z(), high.z()); z++) {
                if (level.setChunkForced(x, z, true)) {
                    forced.add(new ChunkPos(x, z));
                }
            }
        }
        BooleanSupplier ticking = () -> {
            for (int x = Math.min(low.x(), high.x()); x <= Math.max(low.x(), high.x()); x++) {
                for (int z = Math.min(low.z(), high.z()); z <= Math.max(low.z(), high.z()); z++) {
                    if (!level.isPositionEntityTicking(new ChunkPos(x, z).getWorldPosition())) {
                        return false;
                    }
                }
            }
            return true;
        };
        var deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (!ticking.getAsBoolean() && System.nanoTime() < deadline) {
            if (!level.getChunkSource().pollTask()) {
                LockSupport.parkNanos(100_000);
            }
        }
        helper.assertTrue(ticking.getAsBoolean(), "the forced chunks tick no entities after 10 s");
        return forced;
    }

    /**
     * A Lampad bound to her Votive, shut in the Dark's cage: she gives no light, and once a Strike would
     * have landed on a player there she has taken nothing.
     */
    private static void unlitUnstruck(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND.east(3), player);
        cage(helper, STAND);
        var votiveEntity = summon(helper, player, STAND.below());
        votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        var lampad = helper.getEntities(Lampad.LAMPAD.get(), STAND, 1.0).getFirst();
        helper.runAtTickTime(QUIET, () -> {
            settleLight(helper, STAND);
            var at = helper.absolutePos(STAND);
            int light = helper.getLevel().getMaxLocalRawBrightness(at);
            helper.assertTrue(light == 0, "the Lampad's block has light " + light);
            helper.assertFalse(lampad.isDormant(), "the Lampad is dormant");
            helper.assertTrue(lampad.getHealth() == lampad.getMaxHealth(), "the Lampad took " + (lampad.getMaxHealth() - lampad.getHealth()));
            leave(player);
            helper.succeed();
        });
    }

    /**
     * A Lampad bound by a Nightbrand, a full Lamp that's off beside her Votive, is unloaded with her
     * chunk, saved, while the Lamp is emptied, coal put in her Votive, and the clock moves on 8 days, past
     * her week's binding, as a long absence would. She feeds nothing while away. Loaded again from her save, she isn't dormant, as she
     * caught none of those days up, and she feeds the Lamp.
     */
    private static void unloaded(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var votive = new BlockPos(1, 1, 1);
        var lamp = new BlockPos(4, 1, 1);
        helper.setBlock(lamp, Light.LAMP.get());
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(lamp), LampTests.CAP);
        var votiveEntity = summon(helper, player, votive);
        votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        votiveEntity.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET));
        var registries = helper.getLevel().registryAccess();
        var saved = new CompoundTag[1];
        helper.runAtTickTime(3, () -> {
            var lampad = helper.getEntities(Lampad.LAMPAD.get()).getFirst();
            helper.assertFalse(lampad.isDormant(), "the Lampad is dormant with a Nightbrand in her Votive");
            var output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
            lampad.saveWithoutId(output);
            saved[0] = output.buildResult();
            lampad.remove(Entity.RemovalReason.UNLOADED_TO_CHUNK);
            PlacedFuel.set(helper.getLevel(), helper.absolutePos(lamp), 0);
            votiveEntity.setItem(VotiveBlockEntity.FUEL, new ItemStack(Items.COAL, 4));
            KeepsClock.advance(helper, 8 * 24_000);
        });
        var reloaded = new LampadEntity[1];
        helper.runAtTickTime(100, () -> {
            LampTests.assertPlaced(helper, lamp, false, 0);
            reloaded[0] = Lampad.LAMPAD.get().create(helper.getLevel(), EntitySpawnReason.LOAD);
            reloaded[0].load(TagValueInput.create(ProblemReporter.DISCARDING, registries, saved[0]));
            helper.getLevel().addFreshEntity(reloaded[0]);
        });
        helper.onEachTick(() -> {
            if (reloaded[0] == null) {
                return;
            }
            helper.assertFalse(reloaded[0].isDormant(), "the Lampad caught her absence up and stands dormant");
            if (PlacedFuel.left(helper.getLevel(), helper.absolutePos(lamp)) == 16_000) {
                leave(player);
                helper.succeed();
            }
        });
    }

    /**
     * With a binding of 5 s, a Lampad summoned on bare gold stands dormant at once. Two Nightbrands put in
     * her Votive wake her, and she takes one, holding one at a time; once her binding has faded she takes
     * the other and keeps working, and once that one's has she stands dormant again. A third wakes her.
     */
    private static void binding(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var votiveEntity = summon(helper, player, new BlockPos(2, 1, 2));
        var lampad = helper.getEntities(Lampad.LAMPAD.get()).getFirst();
        helper.runAtTickTime(3, () -> {
            helper.assertTrue(lampad.isDormant(), "a Lampad with no Nightbrand isn't dormant");
            votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, new ItemStack(Nightbrand.NIGHTBRAND.get(), 2));
        });
        helper.runAtTickTime(5, () -> {
            helper.assertFalse(lampad.isDormant(), "a Nightbrand didn't wake the Lampad");
            assertHolds(helper, votiveEntity, VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.get(), 1);
        });
        helper.runAtTickTime(5 + WithLampadConfig.BINDING + 20, () -> {
            helper.assertFalse(lampad.isDormant(), "the second Nightbrand didn't renew the Lampad's binding");
            assertHolds(helper, votiveEntity, VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.get(), 0);
        });
        helper.runAtTickTime(5 + 2 * WithLampadConfig.BINDING + 20, () -> {
            helper.assertTrue(lampad.isDormant(), "the Lampad's binding didn't fade");
            votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        });
        helper.runAtTickTime(5 + 2 * WithLampadConfig.BINDING + 22, () -> {
            helper.assertFalse(lampad.isDormant(), "a Nightbrand didn't wake the dormant Lampad");
            leave(player);
            helper.succeed();
        });
    }

    /**
     * A Lampad bound by a Nightbrand, a husk beside her and no player: the husk never takes her for its
     * target, as it would a villager, and she keeps her 12 health. Its blow still hurts her.
     */
    private static void passive(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, STAND, player);
        var votiveEntity = summon(helper, player, new BlockPos(2, 1, 2));
        votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        leave(player);
        var lampad = helper.getEntities(Lampad.LAMPAD.get()).getFirst();
        var husk = helper.spawn(EntityType.HUSK, new BlockPos(4, 1, 2));
        helper.onEachTick(() -> helper.assertFalse(husk.getTarget() == lampad, "the husk took the Lampad for its target"));
        helper.runAtTickTime(60, () -> {
            helper.assertTrue(lampad.getMaxHealth() == 12F && lampad.getHealth() == 12F,
                    "the Lampad has " + lampad.getHealth() + " of " + lampad.getMaxHealth() + " health, not 12");
            husk.doHurtTarget(helper.getLevel(), lampad);
            helper.assertTrue(lampad.getHealth() < 12F, "the husk's blow didn't hurt the Lampad");
            husk.discard();
            helper.succeed();
        });
    }

    /**
     * At night under a waning moon, where all the open sky is in the Dark, with a binding of 5 s. On the
     * platform a Lampad stands Lit by a light block beside her Votive, which holds coal and Nightbrands
     * enough to outlast the test, a dry Lamp by it, and is hurt by 2: she goes into the Dark, out of the
     * light block's reach, and heals there, 1 health 40 ticks after she gets there and another 40 ticks
     * later, feeding the Lamp only once she is whole. High above, on a ledge a light block lights end to
     * end, another with one Nightbrand, hurt the same, waits where she stands, healing none and feeding
     * her dry Lamp nothing, until her binding fades and she stands dormant.
     */
    private static void heals(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, new BlockPos(4, 1, 0), player);
        var votive = new BlockPos(1, 1, 1);
        var lamp = new BlockPos(0, 1, 2);
        helper.setBlock(new BlockPos(0, 2, 0), Blocks.LIGHT);
        var ledge = new BlockPos(4, 40, 4);
        for (var side : Direction.Plane.HORIZONTAL) {
            helper.setBlock(ledge.relative(side), Blocks.STONE);
        }
        helper.setBlock(ledge.above(3), Blocks.LIGHT);
        var ledgeLamp = ledge.east().above();
        helper.setBlock(ledgeLamp, Light.LAMP.get());
        settleLight(helper, ledge.above(2));
        var healer = summon(helper, player, votive);
        helper.setBlock(lamp, Light.LAMP.get());
        settleLight(helper, votive.above(2));
        var waiter = summon(helper, player, ledge);
        leave(player);
        var lampad = helper.getEntities(Lampad.LAMPAD.get(), votive.above(), 1.0).getFirst();
        var waiting = helper.getEntities(Lampad.LAMPAD.get(), ledge.above(), 1.0).getFirst();
        var waitingAt = waiting.position();
        var level = helper.getLevel();
        for (var hurt : List.of(lampad, waiting)) {
            hurt.hurtServer(level, level.damageSources().generic(), 2F);
        }
        // A binding to outlast her walk to the Dark and back, however long it takes.
        healer.setItem(VotiveBlockEntity.NIGHTBRAND, new ItemStack(Nightbrand.NIGHTBRAND.get(), 10));
        healer.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET, 10));
        healer.setItem(VotiveBlockEntity.FUEL, new ItemStack(Items.COAL));
        waiter.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        waiter.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET));
        waiter.setItem(VotiveBlockEntity.FUEL, new ItemStack(Items.COAL));

        // Read as events, each heal against the one before, so no check hangs on the tick she gets there.
        var healedAt = new long[] {-1};
        var health = new float[] {lampad.getHealth()};
        var wasHealing = new boolean[1];
        var waited = new boolean[1];
        helper.onEachTick(() -> {
            long tick = helper.getTick();
            int fed = PlacedFuel.left(helper.getLevel(), helper.absolutePos(lamp));
            helper.assertTrue(fed == 0 || !lampad.isHurt(), "the Lampad fed a Lamp while hurt, " + lampad.getHealth() + " health");
            if (lampad.isHealing()) {
                helper.assertFalse(Dark.isLit(level, BlockPos.containing(lampad.getEyePosition())), "the Lampad heals where it is Lit");
                if (!wasHealing[0]) {
                    healedAt[0] = tick;
                }
            }
            wasHealing[0] = lampad.isHealing();
            if (lampad.getHealth() != health[0]) {
                helper.assertTrue(lampad.getHealth() == health[0] + 1F, "the Lampad went from " + health[0] + " to " + lampad.getHealth() + " health");
                helper.assertTrue(healedAt[0] >= 0 && tick - healedAt[0] == 40,
                        "the Lampad healed " + (tick - healedAt[0]) + " ticks after she came into the Dark or last healed, not 40");
                health[0] = lampad.getHealth();
                healedAt[0] = tick;
            }
            if (!waited[0]) {
                helper.assertTrue(waiting.getHealth() == 10F, "the Lampad with no Dark in range healed to " + waiting.getHealth());
                helper.assertTrue(waiting.position().distanceTo(waitingAt) < 0.5, "the Lampad with no Dark in range walked off");
                LampTests.assertPlaced(helper, ledgeLamp, false, 0);
                waited[0] = waiting.isDormant() && tick > 50;
            }
            if (waited[0] && fed == 16_000) {
                helper.succeed();
            }
        });
    }

    /**
     * Two Lampads, each with a Nightbrand: one summoned from a Lamp with 5000 fuel, her Votive given coal,
     * Nightbrands and gold, of which she takes a nugget for a full Lamp in her range, and the other from a
     * soul Lamp with 3000. The first's Votive, broken with a pickaxe, drops itself and spills the coal, the
     * Nightbrand and the gold left in it, and she stands dormant, nor wakes when a Votive with a
     * Nightbrand is set there again. A player's blow breaks her statue, which drops the Lamp with its 5000
     * and her Nightbrand, and no gold. The other, killed, drops the soul Lamp with its 3000 and her Nightbrand.
     */
    private static void lost(GameTestHelper helper) {
        var player = survivalPlayer(helper);
        standAt(helper, new BlockPos(8, 1, 0), player);
        var votive = new BlockPos(2, 1, 2);
        var soulVotive = new BlockPos(6, 1, 6);
        helper.setBlock(new BlockPos(0, 1, 0), Light.LAMP.get());
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(new BlockPos(0, 1, 0)), LampTests.CAP);
        var votiveEntity = summonFrom(helper, player, votive, Light.LAMP.get(), 5000);
        var soulVotiveEntity = summonFrom(helper, player, soulVotive, Light.SOUL_LAMP.get(), 3000);
        votiveEntity.setItem(VotiveBlockEntity.FUEL, new ItemStack(Items.COAL, 5));
        votiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, new ItemStack(Nightbrand.NIGHTBRAND.get(), 2));
        votiveEntity.setItem(VotiveBlockEntity.GOLD, new ItemStack(Items.GOLD_NUGGET, 3));
        soulVotiveEntity.setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        var lampad = helper.getEntities(Lampad.LAMPAD.get(), votive.above(), 1.0).getFirst();
        var soulLampad = helper.getEntities(Lampad.LAMPAD.get(), soulVotive.above(), 1.0).getFirst();
        var level = helper.getLevel();
        helper.runAtTickTime(5, () -> {
            helper.assertFalse(lampad.isDormant() || soulLampad.isDormant(), "a Lampad with a Nightbrand is dormant");
            assertHolds(helper, votiveEntity, VotiveBlockEntity.GOLD, Items.GOLD_NUGGET, 2);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.IRON_PICKAXE));
            player.gameMode.destroyBlock(helper.absolutePos(votive));
            assertDropped(helper, Lampad.VOTIVE_ITEM.get(), 1, Items.COAL, 5, Nightbrand.NIGHTBRAND.get(), 1, Items.GOLD_NUGGET, 2);
        });
        helper.runAtTickTime(7, () -> {
            helper.assertTrue(lampad.isDormant(), "the Lampad of a broken Votive isn't dormant");
            helper.setBlock(votive, Lampad.VOTIVE.get());
            helper.getBlockEntity(votive, VotiveBlockEntity.class).setItem(VotiveBlockEntity.NIGHTBRAND, Nightbrand.NIGHTBRAND.toStack());
        });
        helper.runAtTickTime(20, () -> {
            helper.assertTrue(lampad.isDormant(), "a Votive set again woke the Lampad of the broken one");
            player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
            player.attack(lampad);
            helper.assertFalse(lampad.isAlive(), "a player's blow didn't break the Lampad's statue");
            assertDropped(helper, Light.LAMP_ITEM.get(), 5000, Nightbrand.NIGHTBRAND.get(), 1, Items.GOLD_NUGGET, 0);
            soulLampad.hurtServer(level, level.damageSources().generic(), 100F);
            helper.assertFalse(soulLampad.isAlive(), "the soul Lampad survived");
            assertDropped(helper, Light.SOUL_LAMP_ITEM.get(), 3000, Nightbrand.NIGHTBRAND.get(), 1, Items.GOLD_NUGGET, 0);
            leave(player);
            helper.succeed();
        });
    }

    /**
     * The items lying in the test are these, item then count in pairs, with a Lamp's count read as its
     * fuel; they are taken up after.
     */
    private static void assertDropped(GameTestHelper helper, Object... expected) {
        var items = helper.getEntities(EntityType.ITEM);
        for (int i = 0; i < expected.length; i += 2) {
            var item = (Item) expected[i];
            int want = (int) expected[i + 1];
            var of = items.stream().map(ItemEntity::getItem).filter(stack -> stack.is(item)).toList();
            int got = item instanceof LampItem
                    ? of.size() == 1 ? of.getFirst().getOrDefault(Light.LAMP_FUEL, 0) : -of.size()
                    : of.stream().mapToInt(ItemStack::getCount).sum();
            helper.assertTrue(got == want, "dropped " + of + ", not " + want + (item instanceof LampItem ? " fuel in one " : " ") + item);
        }
        items.forEach(Entity::discard);
    }

    /** A Lampad summoned by the player from a Lamp of this kind, off with this much fuel, on a gold block at {@code at}; her Votive. */
    private static VotiveBlockEntity summonFrom(GameTestHelper helper, ServerPlayer player, BlockPos at, Block lamp, int fuel) {
        helper.setBlock(at, Blocks.GOLD_BLOCK);
        helper.setBlock(at.above(), lamp);
        PlacedFuel.set(helper.getLevel(), helper.absolutePos(at.above()), fuel);
        LampTests.useOn(helper, player, Light.TORCH.toStack(), at.above());
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        return helper.getBlockEntity(at, VotiveBlockEntity.class);
    }

    /** A Lampad summoned by the player from a Lamp on a gold block at {@code at}; her Votive. */
    static VotiveBlockEntity summon(GameTestHelper helper, ServerPlayer player, BlockPos at) {
        return summonFrom(helper, player, at, Light.LAMP.get(), 0);
    }

    /** The Votive's slot holds so many of the item. */
    static void assertHolds(GameTestHelper helper, VotiveBlockEntity votive, int slot, Item item, int count) {
        var stack = votive.getItem(slot);
        helper.assertTrue(count == 0 ? stack.isEmpty() : stack.is(item) && stack.getCount() == count,
                "the Votive's slot " + slot + " holds " + stack + ", not " + count + " " + item);
    }

    /** So many Lampads are alive in the test. */
    private static void assertLampads(GameTestHelper helper, int count) {
        var alive = helper.getEntities(Lampad.LAMPAD.get()).stream().filter(LampadEntity::isAlive).count();
        helper.assertTrue(alive == count, alive + " Lampads, not " + count);
    }
}
