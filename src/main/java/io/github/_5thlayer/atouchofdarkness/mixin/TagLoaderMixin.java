// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.mixin;

import java.util.List;
import java.util.Map;

import io.github._5thlayer.atouchofdarkness.light.Unlit;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagLoader;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Empties {@code atouchofdarkness:unlit} as the server builds the block tags, whatever the packs put in
 * it, while the switch has brought vanilla's light back. Tags can't be conditional, and the client is
 * sent the tag as built, so it agrees with the server whatever its own config says.
 */
@Mixin(TagLoader.class)
public abstract class TagLoaderMixin<T> {

    @Shadow
    @Final
    private String directory;

    @Inject(method = "build(Ljava/util/Map;)Ljava/util/Map;", at = @At("RETURN"))
    private void atouchofdarkness$emptyUnlit(Map<Identifier, List<TagLoader.EntryWithSource>> builders, CallbackInfoReturnable<Map<Identifier, List<T>>> callback) {
        if (Unlit.vanillaLight() && directory.equals(Registries.tagsDirPath(Registries.BLOCK))) {
            callback.getReturnValue().replace(Unlit.TAG.location(), List.of());
        }
    }
}
