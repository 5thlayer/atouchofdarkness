// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.mixin;

import io.github._5thlayer.atouchofdarkness.light.Unlit;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * A vanilla block's light ends in its state's {@code getLightEmission()}, which vanilla fixes as the block
 * registers and NeoForge's light hooks fall back to, so an Unlit block reads 0 here, for the light engine
 * and everything else that asks. A mod's block that answers by its position is caught by {@link BlockStateMixin}.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateBaseMixin {

    @Inject(method = "getLightEmission()I", at = @At("RETURN"), cancellable = true)
    private void atouchofdarkness$unlit(CallbackInfoReturnable<Integer> callback) {
        int vanilla = callback.getReturnValueI();
        int emission = Unlit.emission((BlockState) (Object) this, vanilla);
        if (emission != vanilla) {
            callback.setReturnValue(emission);
        }
    }
}
