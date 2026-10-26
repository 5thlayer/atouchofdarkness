// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.util.List;
import java.util.UUID;

import com.mojang.authlib.GameProfile;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.nightbrand.Nightbrand;
import io.github._5thlayer.atouchofdarkness.nightbrand.Darkening;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.neoforged.neoforge.common.util.FakePlayer;
import org.jspecify.annotations.Nullable;

/**
 * The Nightbrand: a single Spent Torch in a real player's hand soaks the Dark up while they are in it,
 * over three Grace Periods, keeping its progress between dips, and becomes a Nightbrand, which gives no
 * light. Each player darkens one a day. Tests of a player in the Dark stand on the {@link DarkFixture};
 * a test that moves the clock or the config runs in a batch of its own. Every test's name starts
 * {@code nightbrand_}.
 */
final class NightbrandTests {

    /** The darkening at the defaults: three Grace Periods of 10 s. */
    private static final int DARKENING = 600;

    /** Ticks either side of the darkening the tests look, as the player's first tick in the Dark comes a tick or two late. */
    private static final int MARGIN = 10;

    /** The ticks a dip's spell of light lasts. */
    private static final int DIP = 100;

    /** The darkening with {@link WithDarkConfig}'s 2 s Grace Period. */
    private static final int SHORT_DARKENING = 3 * WithDarkConfig.SET.gracePeriod();

    private static final int DAY = 24000;

    private NightbrandTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("nightbrand_single_spent_torch_held_in_the_dark_darkens_over_three_grace_periods", DARKENING + 2 * MARGIN, NightbrandTests::darkens);
        tests.test("nightbrand_progress_is_kept_between_dips_into_the_dark", DARKENING + DIP + 2 * MARGIN, NightbrandTests::dips);
        tests.in("nightbrand_peaceful", new WithDifficulty(Difficulty.PEACEFUL))
                .test("nightbrand_darkens_on_peaceful", DARKENING + MARGIN, NightbrandTests::onPeaceful);
        tests.in("nightbrand_daily", KeepsClock.INSTANCE, WithDarkConfig.INSTANCE)
                .test("nightbrand_one_a_day_per_player_and_the_count_resets_at_dawn", 4 * SHORT_DARKENING, NightbrandTests::oneADay);
        tests.test("nightbrand_advanced_tooltip_counts_the_darkening_down", 1, NightbrandTests::tooltips);
        tests.test("nightbrand_never_darkens_a_stack_nor_out_of_an_unlit_players_hand_and_gives_no_light", DARKENING + 2 * MARGIN, NightbrandTests::neverDarkens);
    }

    /**
     * A survival player with a Spent Torch in the main hand, and an adventure player with one in the off
     * hand, both in the Dark, and a third with one in each hand: halfway, each Spent Torch's item bar
     * is half full; short of three Grace Periods
     * each is a Spent Torch still, and past them a Nightbrand. Of the third's, only the main hand's
     * darkened, as only one torch at a time is ever darkened.
     */
    private static void darkens(GameTestHelper helper) {
        var survival = caged(helper, new BlockPos(2, 1, 2), 0);
        survival.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        var adventure = caged(helper, new BlockPos(6, 1, 2), 0);
        adventure.setGameMode(GameType.ADVENTURE);
        adventure.setItemInHand(InteractionHand.OFF_HAND, Light.SPENT_TORCH.toStack());
        var twoHanded = caged(helper, new BlockPos(4, 1, 6), 0);
        twoHanded.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        twoHanded.setItemInHand(InteractionHand.OFF_HAND, Light.SPENT_TORCH.toStack());
        helper.runAtTickTime(DARKENING / 2, () -> {
            assertHalfShown(helper, survival.getMainHandItem());
            assertHalfShown(helper, adventure.getOffhandItem());
        });
        helper.runAtTickTime(DARKENING - MARGIN, () -> {
            assertDarkening(helper, survival.getMainHandItem());
            assertDarkening(helper, adventure.getOffhandItem());
        });
        helper.runAtTickTime(DARKENING + MARGIN, () -> {
            assertNightbrand(helper, survival.getMainHandItem());
            assertNightbrand(helper, adventure.getOffhandItem());
            assertNightbrand(helper, twoHanded.getMainHandItem());
            var other = twoHanded.getOffhandItem();
            helper.assertTrue(other.is(Light.SPENT_TORCH) && !other.has(Nightbrand.DARKENING), "the Spent Torch in the other hand became " + other);
            leave(survival);
            leave(adventure);
            leave(twoHanded);
            helper.succeed();
        });
    }

    /**
     * A survival player in the Dark for half the darkening, then Lit for 5 s, then in the Dark again: their
     * Spent Torch kept its progress in the light, darkened none there, and is a Nightbrand once the two
     * spells in the Dark add up to three Grace Periods.
     */
    private static void dips(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        helper.runAtTickTime(DARKENING / 2, () -> light(helper, 15));
        helper.runAtTickTime(DARKENING / 2 + DIP, () -> {
            var darkening = player.getMainHandItem().get(Nightbrand.DARKENING);
            helper.assertTrue(darkening != null && darkening.darkened() >= DARKENING / 2 - MARGIN && darkening.darkened() <= DARKENING / 2,
                    "after the dip the Spent Torch has darkened " + darkening);
            light(helper, 0);
        });
        helper.runAtTickTime(DARKENING + MARGIN, () -> helper.assertTrue(player.getMainHandItem().is(Light.SPENT_TORCH),
                "the Spent Torch darkened in the light, and became " + player.getMainHandItem()));
        helper.runAtTickTime(DARKENING + DIP + MARGIN, () -> {
            assertNightbrand(helper, player.getMainHandItem());
            leave(player);
            helper.succeed();
        });
    }

    /** On Peaceful, where Strikes deal nothing, a survival player's Spent Torch in the Dark is a Nightbrand after three Grace Periods. */
    private static void onPeaceful(GameTestHelper helper) {
        var player = caged(helper, 0);
        player.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        helper.runAtTickTime(DARKENING + MARGIN, () -> {
            assertNightbrand(helper, player.getMainHandItem());
            leave(player);
            helper.succeed();
        });
    }

    /**
     * With a 2 s Grace Period: one player darkens a Nightbrand. Then they and another player each hold a
     * Spent Torch in the Dark: the other's becomes a Nightbrand, theirs never starts to darken. Once the
     * clock reaches the next dawn, theirs darkens into a Nightbrand too.
     */
    private static void oneADay(GameTestHelper helper) {
        var first = caged(helper, new BlockPos(2, 1, 2), 0);
        first.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        var second = caged(helper, new BlockPos(6, 1, 2), 0);
        helper.runAtTickTime(SHORT_DARKENING + MARGIN, () -> {
            assertNightbrand(helper, first.getMainHandItem());
            first.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
            second.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        });
        helper.runAtTickTime(2 * SHORT_DARKENING + 2 * MARGIN, () -> {
            assertNightbrand(helper, second.getMainHandItem());
            var again = first.getMainHandItem();
            helper.assertTrue(again.is(Light.SPENT_TORCH) && !again.has(Nightbrand.DARKENING), "a second Spent Torch the same day became " + again);
            long now = helper.getLevel().getOverworldClockTime();
            KeepsClock.advance(helper, DAY - Math.floorMod(now, DAY));
        });
        helper.runAtTickTime(3 * SHORT_DARKENING + 3 * MARGIN, () -> {
            assertNightbrand(helper, first.getMainHandItem());
            leave(first);
            leave(second);
            helper.succeed();
        });
    }

    /**
     * Spent Torches held through three Grace Periods: two in a survival player's hand in the Dark, one in a
     * chest, one on the ground, one in a zombie's hand and one in a fake player's in the Dark, and one each in
     * the hand of a Lit player and of a creative and a spectator player in the Dark. None has darkened at all.
     * A survival player in the Dark holding a Nightbrand has no light at their eyes.
     */
    private static void neverDarkens(GameTestHelper helper) {
        var stacked = caged(helper, new BlockPos(1, 1, 1), 0);
        stacked.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack(2));
        var lit = caged(helper, new BlockPos(4, 1, 1), 15);
        lit.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        var creative = caged(helper, new BlockPos(7, 1, 1), 0);
        creative.setGameMode(GameType.CREATIVE);
        creative.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        var spectator = caged(helper, new BlockPos(1, 1, 4), 0);
        spectator.setGameMode(GameType.SPECTATOR);
        spectator.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());

        var zombieAt = new BlockPos(4, 1, 4);
        cage(helper, zombieAt);
        var zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, zombieAt);
        zombie.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());

        var fakeAt = new BlockPos(7, 1, 4);
        cage(helper, fakeAt);
        var fake = new FakePlayer(helper.getLevel(), new GameProfile(UUID.randomUUID(), "nightbrand-fake-player"));
        fake.setGameMode(GameType.SURVIVAL);
        standAt(helper, fakeAt, fake);
        fake.setItemInHand(InteractionHand.MAIN_HAND, Light.SPENT_TORCH.toStack());
        // In the level's players, as a machine that adds its fake player to the world has it.
        helper.getLevel().addFreshEntity(fake);

        var chest = new BlockPos(1, 1, 7);
        helper.setBlock(chest, Blocks.CHEST);
        var chestEntity = helper.getBlockEntity(chest, ChestBlockEntity.class);
        chestEntity.setItem(0, Light.SPENT_TORCH.toStack());
        var dropped = helper.spawnItem(Light.SPENT_TORCH.get(), new BlockPos(4, 1, 7));

        var bearerAt = new BlockPos(7, 1, 7);
        var bearer = caged(helper, bearerAt, 0);
        bearer.setItemInHand(InteractionHand.MAIN_HAND, Nightbrand.NIGHTBRAND.toStack());

        helper.runAtTickTime(MARGIN, () -> {
            helper.assertTrue(helper.getLevel().getMaxLocalRawBrightness(helper.absolutePos(bearerAt.above())) == 0,
                    "a Nightbrand held in the Dark gave light " + helper.getLevel().getMaxLocalRawBrightness(helper.absolutePos(bearerAt.above())));
        });
        helper.runAtTickTime(DARKENING + MARGIN, () -> {
            var undarkened = List.of(stacked.getMainHandItem(), lit.getMainHandItem(), creative.getMainHandItem(), spectator.getMainHandItem(),
                    zombie.getMainHandItem(), fake.getMainHandItem(), chestEntity.getItem(0), dropped.getItem());
            for (var stack : undarkened) {
                helper.assertTrue(stack.is(Light.SPENT_TORCH) && !stack.has(Nightbrand.DARKENING), "a Spent Torch that never darkens became " + stack);
            }
            fake.discard();
            for (var player : List.of(stacked, lit, creative, spectator, bearer)) {
                leave(player);
            }
            helper.succeed();
        });
    }

    /**
     * A Spent Torch halfway through its darkening tells, with advanced tooltips (F3+H), the time in the Dark it
     * has left, as m:ss; without them, or before it has started, it tells nothing of it.
     */
    private static void tooltips(GameTestHelper helper) {
        var half = Light.SPENT_TORCH.toStack();
        half.set(Nightbrand.DARKENING, new Darkening(DARKENING / 2, DARKENING));
        assertDarkeningLine(helper, half, TooltipFlag.ADVANCED, "0:15 in the Dark to a Nightbrand");
        assertDarkeningLine(helper, half, TooltipFlag.NORMAL, null);
        assertDarkeningLine(helper, Light.SPENT_TORCH.toStack(), TooltipFlag.ADVANCED, null);
        helper.succeed();
    }

    private static void assertDarkeningLine(GameTestHelper helper, ItemStack stack, TooltipFlag flag, @Nullable String countdown) {
        var lines = stack.getTooltipLines(Item.TooltipContext.of(helper.getLevel()), null, flag).stream().map(Component::getString).toList();
        boolean told = lines.stream().anyMatch(text -> text.endsWith("to a Nightbrand"));
        helper.assertTrue(countdown == null ? !told : lines.contains(countdown), stack + "'s tooltip " + lines + " with " + flag + " should read " + countdown);
    }

    /** A Spent Torch most of the way darkened, which its item bar shows. */
    private static void assertDarkening(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(stack.is(Light.SPENT_TORCH), "short of its darkening the Spent Torch became " + stack);
        var darkening = stack.get(Nightbrand.DARKENING);
        helper.assertTrue(darkening != null && darkening.darkened() > DARKENING - 3 * MARGIN && darkening.darkened() < DARKENING,
                "short of its darkening the Spent Torch has darkened " + darkening);
    }

    /** Halfway through its darkening, its item bar half full, give or take the ticks it started late. */
    private static void assertHalfShown(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(stack.isBarVisible() && Math.abs(stack.getBarWidth() - Item.MAX_BAR_WIDTH / 2F) <= 1F,
                "halfway through its darkening the Spent Torch's item bar shows " + stack.getBarWidth() + " of " + Item.MAX_BAR_WIDTH);
    }

    private static void assertNightbrand(GameTestHelper helper, ItemStack stack) {
        helper.assertTrue(stack.is(Nightbrand.NIGHTBRAND) && stack.getCount() == 1, "past its darkening the Spent Torch became " + stack);
    }
}
