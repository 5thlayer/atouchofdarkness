// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.client;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.lampad.LampadEntity;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.layers.ItemInHandLayer;
import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;
import net.minecraft.resources.Identifier;

/** The Lampad, with the fuel she carries in her hand, and as a grey stone statue while dormant. */
public final class LampadRenderer extends MobRenderer<LampadEntity, LampadRenderState, LampadModel> {

    public static final ModelLayerLocation LAYER = new ModelLayerLocation(id("lampad"), "main");

    private static final Identifier TEXTURE = id("textures/entity/lampad/lampad.png");
    private static final Identifier STATUE = id("textures/entity/lampad/lampad_statue.png");

    public LampadRenderer(EntityRendererProvider.Context context) {
        super(context, new LampadModel(context.bakeLayer(LAYER)), 0.4F);
        addLayer(new ItemInHandLayer<>(this));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, path);
    }

    @Override
    public Identifier getTextureLocation(LampadRenderState state) {
        return state.dormant ? STATUE : TEXTURE;
    }

    @Override
    public LampadRenderState createRenderState() {
        return new LampadRenderState();
    }

    @Override
    public void extractRenderState(LampadEntity entity, LampadRenderState state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        ArmedEntityRenderState.extractArmedEntityRenderState(entity, state, itemModelResolver, partialTicks);
        state.dormant = entity.isDormant();
    }
}
