// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * The Votive (GLOSSARY.md), the gold block a Lampad was summoned on: its {@link VotiveBlockEntity} holds
 * her fuel, her Nightbrand and her gold, which a hand puts in through its screen and a hopper from any
 * side. Broken, it drops itself and spills what it holds, and its Lampad stands dormant for good. Placed
 * again it is a Votive with no Lampad, which a Lamp on it kindled with a Torch summons, as gold does.
 */
public final class VotiveBlock extends BaseEntityBlock {

    public static final MapCodec<VotiveBlock> CODEC = simpleCodec(VotiveBlock::new);

    public VotiveBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<VotiveBlock> codec() {
        return CODEC;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new VotiveBlockEntity(pos, state);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof VotiveBlockEntity votive) {
            player.openMenu(votive);
        }
        return InteractionResult.SUCCESS;
    }
}
