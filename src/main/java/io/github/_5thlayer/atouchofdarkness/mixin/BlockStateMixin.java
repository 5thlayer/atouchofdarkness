// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.mixin;

import io.github._5thlayer.atouchofdarkness.light.Unlit;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.extensions.IBlockStateExtension;
import org.spongepowered.asm.mixin.Mixin;

/**
 * The light engine asks a state for its light through NeoForge's {@link IBlockStateExtension}, which asks the
 * block; a mod's block may answer by the position without its state's {@code getLightEmission()}, so an
 * Unlit one reads 0 here too. The extension's own default, written into {@link BlockState} over it.
 */
@Mixin(BlockState.class)
public abstract class BlockStateMixin implements IBlockStateExtension {

    @Override
    public int getLightEmission(BlockGetter level, BlockPos pos) {
        var state = (BlockState) (Object) this;
        return Unlit.emission(state, state.getBlock().getLightEmission(state, level, pos));
    }
}
