// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.model.ArmedModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.HumanoidArm;

/**
 * The Lampad: a small robed nymph, her hair down her back, her left arm raising the Nightbrand of her
 * binding, its dark flame giving no light, and her right free for the fuel she carries. Walking, her
 * right arm and her robe swing and the dark flame bobs; dormant, she holds her statue pose, head bowed,
 * the Nightbrand still raised.
 */
public final class LampadModel extends EntityModel<LampadRenderState> implements ArmedModel<LampadRenderState> {

    /** The left arm raised forward, holding the Nightbrand aloft. */
    private static final float TORCH_RAISE = -1.9F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightArm;
    private final ModelPart leftArm;
    private final ModelPart flame;
    private final ModelPart skirt;

    public LampadModel(ModelPart root) {
        super(root);
        body = root.getChild("body");
        head = body.getChild("head");
        rightArm = body.getChild("right_arm");
        leftArm = body.getChild("left_arm");
        flame = leftArm.getChild("torch").getChild("flame");
        skirt = root.getChild("skirt");
    }

    /** 19 pixels tall, on a 64 by 64 texture. */
    public static LayerDefinition createBodyLayer() {
        var mesh = new MeshDefinition().transformed(pose -> pose.translated(0F, 24F, 0F));
        var root = mesh.getRoot();
        root.addOrReplaceChild("skirt", CubeListBuilder.create().texOffs(0, 28).addBox(-4F, 0F, -3F, 8F, 6F, 6F), PartPose.offset(0F, -6F, 0F));
        var body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 16).addBox(-3F, -6F, -2F, 6F, 6F, 4F),
                PartPose.offset(0F, -6F, 0F));
        body.addOrReplaceChild("head", CubeListBuilder.create()
                        .texOffs(0, 0).addBox(-3.5F, -7F, -3.5F, 7F, 7F, 7F)
                        .texOffs(28, 0).addBox(-3.5F, -7F, 3.5F, 7F, 8F, 1F),
                PartPose.offset(0F, -6F, 0F));
        body.addOrReplaceChild("right_arm", CubeListBuilder.create().texOffs(32, 16).addBox(-2F, -0.5F, -1F, 2F, 7F, 2F),
                PartPose.offset(-3F, -5.5F, 0F));
        var leftArm = body.addOrReplaceChild("left_arm", CubeListBuilder.create().texOffs(40, 16).addBox(0F, -0.5F, -1F, 2F, 7F, 2F),
                PartPose.offset(3F, -5.5F, 0F));
        var torch = leftArm.addOrReplaceChild("torch", CubeListBuilder.create().texOffs(48, 16).addBox(-0.5F, -6F, -0.5F, 1F, 6F, 1F),
                PartPose.offsetAndRotation(1F, 6.5F, 0F, -TORCH_RAISE, 0F, 0F));
        torch.addOrReplaceChild("flame", CubeListBuilder.create().texOffs(52, 16).addBox(-1F, -2F, -1F, 2F, 2F, 2F), PartPose.offset(0F, -6F, 0F));
        return LayerDefinition.create(mesh, 64, 64);
    }

    @Override
    public void setupAnim(LampadRenderState state) {
        super.setupAnim(state);
        leftArm.xRot = TORCH_RAISE;
        if (state.dormant) {
            head.xRot = 0.35F;
            rightArm.xRot = -0.2F;
            rightArm.zRot = 0.15F;
            return;
        }
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float swing = Mth.cos(state.walkAnimationPos * 0.6662F) * state.walkAnimationSpeed;
        boolean carrying = !state.rightHandItemState.isEmpty();
        rightArm.xRot = carrying ? -0.9F + swing * 0.2F : swing * 1.2F;
        rightArm.zRot = carrying ? 0F : 0.1F + Mth.sin(state.ageInTicks * 0.067F) * 0.05F;
        leftArm.xRot += Mth.sin(state.ageInTicks * 0.09F) * 0.05F - swing * 0.1F;
        skirt.xRot = -Math.abs(swing) * 0.15F;
        float flicker = 1F + Mth.sin(state.ageInTicks * 0.6F) * 0.12F;
        flame.xScale = flicker;
        flame.zScale = flicker;
        flame.yScale = 2F - flicker;
    }

    /** In her right hand, held out before her. */
    @Override
    public void translateToHand(LampadRenderState state, HumanoidArm arm, PoseStack poseStack) {
        root().translateAndRotate(poseStack);
        body.translateAndRotate(poseStack);
        var hand = arm == HumanoidArm.RIGHT ? rightArm : leftArm;
        hand.translateAndRotate(poseStack);
        poseStack.translate(-0.0625F, 0.375F, 0F);
        poseStack.mulPose(Axis.XP.rotationDegrees(-90F));
        poseStack.scale(0.6F, 0.6F, 0.6F);
    }
}
