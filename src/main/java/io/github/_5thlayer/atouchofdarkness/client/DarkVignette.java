// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.client;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.dark.DarkState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterGuiLayersEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;
import net.neoforged.neoforge.client.network.event.RegisterClientPayloadHandlersEvent;

/**
 * The screen's edge darkening as the player's Grace Period drains, from nothing when it is full to
 * vanilla's vignette at its darkest once it has run out, and clearing over a second in light. It
 * follows the {@link DarkState} the server sends, and shows no number: the Vesper Dial's meter on the
 * Runtime's HUD shows the light and whether the player is Lit, and nothing of the Grace Period.
 */
@EventBusSubscriber(modid = ATouchOfDarkness.MOD_ID, value = Dist.CLIENT)
public final class DarkVignette {

    private static final Identifier LAYER = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "dark_vignette");
    private static final Identifier VIGNETTE = Identifier.withDefaultNamespace("textures/misc/vignette.png");

    /** How far the vignette moves in a tick: from its darkest to clear in a second. */
    private static final float STEP = 1F / 20;

    private static DarkState state = DarkState.CLEAR;

    /** How dark the vignette is, 0 to 1, this tick and the last, so a frame between them can blend. */
    private static float depth;
    private static float lastDepth;

    private DarkVignette() {
    }

    @SubscribeEvent
    static void registerPayloadHandler(RegisterClientPayloadHandlersEvent event) {
        event.register(DarkState.TYPE, (received, context) -> state = received);
    }

    @SubscribeEvent
    static void registerLayer(RegisterGuiLayersEvent event) {
        event.registerAbove(VanillaGuiLayers.CAMERA_OVERLAYS, LAYER, DarkVignette::render);
    }

    @SubscribeEvent
    static void tick(ClientTickEvent.Post event) {
        lastDepth = depth;
        depth = Mth.approach(depth, state.inTheDark() ? state.drained() : 0F, STEP);
    }

    @SubscribeEvent
    static void loggingOut(ClientPlayerNetworkEvent.LoggingOut event) {
        state = DarkState.CLEAR;
        depth = 0F;
        lastDepth = 0F;
    }

    /** Vanilla's vignette texture, whose pipeline darkens the screen by the colour's brightness. */
    private static void render(GuiGraphicsExtractor graphics, DeltaTracker deltaTracker) {
        float shown = Mth.lerp(deltaTracker.getGameTimeDeltaPartialTick(false), lastDepth, depth);
        if (shown <= 0F) {
            return;
        }
        int width = graphics.guiWidth();
        int height = graphics.guiHeight();
        graphics.blit(RenderPipelines.VIGNETTE, VIGNETTE, 0, 0, 0F, 0F, width, height, width, height, ARGB.colorFromFloat(1F, shown, shown, shown));
    }
}
