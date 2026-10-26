// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import io.github._5thlayer.atouchofdarkness.light.LampBlock;
import io.github._5thlayer.atouchofdarkness.light.TorchItem;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LevelEvent;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

/**
 * A Lampad summoned as vanilla's carved pumpkin builds a golem: a Lamp of either kind standing on a gold
 * block, or on a Votive with no Lampad, kindled with a Torch of either kind, becomes a Lampad, and the
 * gold becomes her Votive. A Votive that has a Lampad summons none, and the Torch is kept.
 */
final class Summoning {

    private Summoning() {
    }

    static void kindle(PlayerInteractEvent.RightClickBlock event) {
        if (!(event.getLevel() instanceof ServerLevel level) || !(event.getItemStack().getItem() instanceof TorchItem)) {
            return;
        }
        var pos = event.getPos();
        var state = level.getBlockState(pos);
        var below = pos.below();
        var base = level.getBlockState(below);
        boolean gold = base.is(Blocks.GOLD_BLOCK);
        boolean freeVotive = level.getBlockEntity(below) instanceof VotiveBlockEntity votive && !votive.hasLampad();
        if (!(state.getBlock() instanceof LampBlock) || !gold && !freeVotive) {
            return;
        }
        summon(level, pos, state, below, gold, event.getEntity());
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS_SERVER);
    }

    private static void summon(ServerLevel level, BlockPos pos, BlockState lampState, BlockPos below, boolean gold, Player player) {
        var lamp = new ItemStack(lampState.getBlock());
        var lampEntity = level.getBlockEntity(pos);
        if (lampEntity != null) {
            lamp.applyComponents(lampEntity.collectComponents());
        }
        level.removeBlock(pos, false);
        level.levelEvent(LevelEvent.PARTICLES_DESTROY_BLOCK, pos, Block.getId(lampState));
        if (gold) {
            level.setBlockAndUpdate(below, Lampad.VOTIVE.get().defaultBlockState());
        }
        var lampad = Lampad.LAMPAD.get().create(level, EntitySpawnReason.TRIGGERED);
        if (lampad == null || !(level.getBlockEntity(below) instanceof VotiveBlockEntity votive)) {
            return;
        }
        lampad.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, player.getYRot() + 180F, 0F);
        lampad.summonedAt(below, lamp);
        level.addFreshEntity(lampad);
        votive.bind(lampad.getUUID());
        level.playSound(null, pos, SoundEvents.COPPER_GOLEM_SPAWN, SoundSource.NEUTRAL, 1F, 1F);
    }
}
