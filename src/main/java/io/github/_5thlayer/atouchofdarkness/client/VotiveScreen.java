// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.client;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.lampad.VotiveMenu;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;

/** A Votive's screen, a hopper's size: its fuel, Nightbrand and gold slots, each marked with what it takes. */
public final class VotiveScreen extends AbstractContainerScreen<VotiveMenu> {

    private static final Identifier TEXTURE = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "textures/gui/container/votive.png");

    public VotiveScreen(VotiveMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title, 176, 133);
        inventoryLabelY = imageHeight - 94;
    }

    @Override
    public void extractBackground(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float a) {
        super.extractBackground(graphics, mouseX, mouseY, a);
        int x = (width - imageWidth) / 2;
        int y = (height - imageHeight) / 2;
        graphics.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0F, 0F, imageWidth, imageHeight, 256, 256);
    }
}
