// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import static io.github._5thlayer.atouchofdarkness.gametest.DarkFixture.*;

import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.DarkCues;
import io.github._5thlayer.atouchofdarkness.dark.DarkState;
import io.github._5thlayer.atouchofdarkness.dark.GracePeriod;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.locale.Language;
import net.minecraft.sounds.SoundEvent;

/**
 * The Dark's cues, as the server sends them to the player's client: a sound on entering the Dark, a
 * warning 2 s before each Strike and a sound for each Strike, every one with a subtitle, and the Dark
 * state the vignette follows. Each test stands on the {@link DarkFixture}. Every test's name starts
 * {@code dark_cues_}.
 */
final class CueTests {

    private CueTests() {
    }

    static void register(ATouchOfDarknessGameTests.Registrar tests) {
        tests.test("dark_cues_enter_then_warn_2_s_before_each_strike", AFTER_FIRST + 205, CueTests::enterWarnStrike);
        tests.test("dark_cues_light_before_the_warning_silences_it_and_clears_the_client", QUIET + 5, CueTests::litBeforeTheWarning);
    }

    /** Two Strikes in the Dark at light 0: entering is heard once, and each Strike 2 s after its warning. */
    private static void enterWarnStrike(GameTestHelper helper) {
        var player = caged(helper, 0);
        var inbox = ClientInbox.of(helper, player);
        helper.runAtTickTime(AFTER_FIRST + 200, () -> {
            assertSubtitled(helper);
            var entered = inbox.heard(DarkCues.ENTER.get());
            var warned = inbox.heard(DarkCues.WARNING.get());
            var struck = inbox.heard(DarkCues.STRIKE.get());
            helper.assertTrue(entered.size() == 1 && entered.getFirst() <= 2,
                    "entering the Dark was heard at ticks " + entered + ", not once as the test began");
            helper.assertTrue(struck.size() == 2, "the Strike was heard at ticks " + struck + ", not twice");
            helper.assertTrue(warned.size() == 2, "the warning was heard at ticks " + warned + ", not twice");
            for (int i = 0; i < 2; i++) {
                helper.assertTrue(struck.get(i) - warned.get(i) == GracePeriod.WARNING_LEAD,
                        "Strike " + (i + 1) + " was heard at tick " + struck.get(i) + ", its warning at " + warned.get(i));
            }
            leave(player);
            helper.succeed();
        });
    }

    /**
     * Halfway through the Grace Period, the client has the player in the Dark at half; Lit at 150 ticks,
     * before the warning at 160, they hear neither it nor a Strike, the client has them out of the Dark,
     * and once refilled the Grace Period reads empty.
     */
    private static void litBeforeTheWarning(GameTestHelper helper) {
        var player = caged(helper, 0);
        var inbox = ClientInbox.of(helper, player);
        helper.runAtTickTime(100, () -> assertDarkState(helper, inbox, true, 0.5F));
        helper.runAtTickTime(150, () -> light(helper, 15));
        helper.runAtTickTime(170, () -> assertDarkState(helper, inbox, false, 0.75F));
        helper.runAtTickTime(QUIET, () -> {
            assertDarkState(helper, inbox, false, 0F);
            helper.assertTrue(inbox.heard(DarkCues.WARNING.get()).isEmpty(), "the warning was heard at ticks " + inbox.heard(DarkCues.WARNING.get()));
            helper.assertTrue(inbox.heard(DarkCues.STRIKE.get()).isEmpty(), "the Strike was heard at ticks " + inbox.heard(DarkCues.STRIKE.get()));
            assertTaken(helper, player, QUIET, 0F);
            leave(player);
            helper.succeed();
        });
    }

    /** Within a few ticks' drain, as the test's ticks and the Dark's need not line up. */
    private static void assertDarkState(GameTestHelper helper, ClientInbox inbox, boolean inTheDark, float drained) {
        DarkState state = inbox.lastDarkState();
        helper.assertTrue(state != null && state.inTheDark() == inTheDark && Math.abs(state.drained() - drained) <= 0.03F,
                "the client was sent " + state + ", not " + (inTheDark ? "in the Dark" : "out of it") + " at " + drained + " drained");
    }

    /** Each cue's sound names a subtitle in {@code sounds.json}, and the language has it. */
    private static void assertSubtitled(GameTestHelper helper) {
        JsonObject sounds;
        try (var in = ATouchOfDarkness.class.getResourceAsStream("/assets/" + ATouchOfDarkness.MOD_ID + "/sounds.json")) {
            helper.assertTrue(in != null, "the Pillar has no sounds.json");
            sounds = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
        for (SoundEvent sound : List.of(DarkCues.ENTER.get(), DarkCues.WARNING.get(), DarkCues.STRIKE.get())) {
            var entry = sounds.getAsJsonObject(sound.location().getPath());
            String subtitle = entry == null || !entry.has("subtitle") ? null : entry.get("subtitle").getAsString();
            helper.assertTrue(subtitle != null && Language.getInstance().has(subtitle),
                    sound.location() + " has the subtitle " + subtitle + ", which the language lacks");
        }
    }
}
