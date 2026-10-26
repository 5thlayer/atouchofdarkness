// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import java.util.Locale;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.Dark;
import io.github._5thlayer.atouchofdarkness.dark.DarkConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * In the {@code client} run only, shows each player once a second on the action bar whether they are
 * Lit, the light at their eyes against the threshold, what the sky over them is, and how long until
 * the next Strike, so the Dark can be tried by walking into it. Like {@link DevPacks}, it lives in the
 * {@code gametest} source set, which the jar leaves out (libworks ADR 0004).
 */
@EventBusSubscriber(modid = ATouchOfDarkness.MOD_ID)
final class DevReadout {

    private static final boolean CLIENT_RUN = System.getProperty("libworks.run", "").equals("client");
    private static final int TICKS_PER_SECOND = 20;

    private DevReadout() {
    }

    @SubscribeEvent
    static void tick(PlayerTickEvent.Post event) {
        if (!CLIENT_RUN || !(event.getEntity() instanceof ServerPlayer player) || player.tickCount % TICKS_PER_SECOND != 0) {
            return;
        }
        var level = player.level();
        if (!level.dimensionTypeRegistration().is(Dark.DIMENSIONS)) {
            player.sendSystemMessage(Component.literal("No Dark in this dimension"), true);
            return;
        }
        var eyes = BlockPos.containing(player.getEyePosition());
        String text = String.format(Locale.ROOT, "%s · light %d/%d · %s · Strike in %.1f s",
                Dark.isLit(player) ? "Lit" : "Dark", level.getMaxLocalRawBrightness(eyes), DarkConfig.LIT_THRESHOLD.getAsInt(),
                sky(player, eyes), (float) Dark.ticksToStrike(player) / TICKS_PER_SECOND);
        player.sendSystemMessage(Component.literal(text), true);
    }

    /** Roofed, or the open sky: by day, or the night's moon, and rain. */
    private static String sky(ServerPlayer player, BlockPos eyes) {
        var level = player.level();
        if (!level.canSeeSky(eyes)) {
            return "roofed";
        }
        if (level.dimensionTypeRegistration().is(Dark.SKY_ALWAYS_LIT)) {
            return "sky always lit";
        }
        String open = level.isDarkOutside()
                ? level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, eyes).getSerializedName().replace('_', ' ')
                : "day";
        return "open sky, " + open + (level.isRaining() ? ", rain" : "");
    }
}
