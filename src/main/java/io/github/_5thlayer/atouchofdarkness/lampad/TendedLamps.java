// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import io.github._5thlayer.atouchofdarkness.light.LampBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jspecify.annotations.Nullable;

/**
 * The placed Lamps a Votive's Lampad tends: those within 16 blocks of it either way across, 32 in all,
 * and 8 up or down, in loaded chunks, that are nearer it than any other Votive with a Lampad in whose
 * range they are, a tie going to the Votive first in block order. A Votive with no Lampad claims none. Carried Lamps and Campfires are never among them.
 */
final class TendedLamps {

    static final int ACROSS = 16;
    static final int UP_DOWN = 8;

    private TendedLamps() {
    }

    /** Its Lamps, nearest it first. */
    static List<BlockPos> of(ServerLevel level, BlockPos votive) {
        var votives = positionsOf(level, votive, 2 * ACROSS, 2 * UP_DOWN,
                entity -> entity instanceof VotiveBlockEntity other && other.hasLampad());
        return positionsOf(level, votive, ACROSS, UP_DOWN, LampBlockEntity.class::isInstance).stream()
                .filter(lamp -> votive.equals(nearestVotive(lamp, votives)))
                .sorted(nearestFirst(votive))
                .toList();
    }

    private static boolean inRange(BlockPos votive, BlockPos pos) {
        return within(votive, pos, ACROSS, UP_DOWN);
    }

    private static boolean within(BlockPos centre, BlockPos pos, int across, int upDown) {
        return Math.abs(pos.getX() - centre.getX()) <= across && Math.abs(pos.getZ() - centre.getZ()) <= across
                && Math.abs(pos.getY() - centre.getY()) <= upDown;
    }

    private static @Nullable BlockPos nearestVotive(BlockPos lamp, List<BlockPos> votives) {
        return votives.stream().filter(votive -> inRange(votive, lamp)).min(nearestFirst(lamp)).orElse(null);
    }

    private static Comparator<BlockPos> nearestFirst(BlockPos from) {
        return Comparator.comparingDouble((BlockPos pos) -> pos.distSqr(from)).thenComparing(Comparator.naturalOrder());
    }

    /** Where the block entities that match are within so far of the centre, in chunks that are loaded. */
    private static List<BlockPos> positionsOf(ServerLevel level, BlockPos centre, int across, int upDown, Predicate<BlockEntity> kind) {
        var found = new ArrayList<BlockPos>();
        var chunks = level.getChunkSource();
        for (int x = SectionPos.blockToSectionCoord(centre.getX() - across); x <= SectionPos.blockToSectionCoord(centre.getX() + across); x++) {
            for (int z = SectionPos.blockToSectionCoord(centre.getZ() - across); z <= SectionPos.blockToSectionCoord(centre.getZ() + across); z++) {
                var chunk = chunks.getChunkNow(x, z);
                if (chunk == null) {
                    continue;
                }
                for (var entry : chunk.getBlockEntities().entrySet()) {
                    var pos = entry.getKey();
                    if (kind.test(entry.getValue()) && within(centre, pos, across, upDown)) {
                        found.add(pos);
                    }
                }
            }
        }
        return found;
    }
}
