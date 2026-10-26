// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.client;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.lampad.Lampad;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;

/** The Lampad's renderer and model, and her Votive's screen. */
@EventBusSubscriber(modid = ATouchOfDarkness.MOD_ID, value = Dist.CLIENT)
public final class LampadClient {

    private LampadClient() {
    }

    @SubscribeEvent
    static void registerRenderers(EntityRenderersEvent.RegisterRenderers event) {
        event.registerEntityRenderer(Lampad.LAMPAD.get(), LampadRenderer::new);
    }

    @SubscribeEvent
    static void registerLayers(EntityRenderersEvent.RegisterLayerDefinitions event) {
        event.registerLayerDefinition(LampadRenderer.LAYER, LampadModel::createBodyLayer);
    }

    @SubscribeEvent
    static void registerScreens(RegisterMenuScreensEvent event) {
        event.register(Lampad.VOTIVE_MENU.get(), VotiveScreen::new);
    }
}
