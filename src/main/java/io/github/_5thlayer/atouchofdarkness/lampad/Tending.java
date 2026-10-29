// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.List;
import java.util.Set;

import io.github._5thlayer.atouchofdarkness.light.LightConfig;
import io.github._5thlayer.atouchofdarkness.light.PlacedFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * A Lampad's binding as it pays for her Tended Lamps: the gold nuggets it has taken, and her Claimed
 * Lamps as last scanned, nearest her Votive first, as many of which as the gold paid for are her
 * Tended Lamps. The Claimed Lamps are scanned only when she charges or renews, so between charges a
 * Lamp placed or broken can be up to a charge interval out of date. They aren't saved, so she charges
 * again on her first tick loaded.
 */
final class Tending {

    private static final String NUGGETS = "nuggets";

    /** The gold nuggets this binding has taken. */
    private int nuggets;

    /** Her Claimed Lamps at the last charge, nearest her Votive first; null until the first. */
    private @Nullable List<BlockPos> claimed;

    /**
     * Scans her Claimed Lamps afresh, and takes from her Votive the nuggets owed for more of them than this
     * binding has paid for, as far as it has them; how many it took.
     */
    int charge(ServerLevel level, VotiveBlockEntity votiveEntity) {
        claimed = ClaimedLamps.of(level, votiveEntity.getBlockPos());
        int owed = Gold.owed(claimed.size(), lampsPerNugget()) - nuggets;
        int taken = owed > 0 ? votiveEntity.takeGold(owed) : 0;
        nuggets += taken;
        return taken;
    }

    /** A new binding, which has paid nothing yet, charged; how many nuggets it took. */
    int renew(ServerLevel level, VotiveBlockEntity votiveEntity) {
        nuggets = 0;
        return charge(level, votiveEntity);
    }

    /** Her binding over with none to follow, or her Votive gone: no gold taken, and no Lamps to tend. */
    void end() {
        nuggets = 0;
        claimed = null;
    }

    /** Whether her Claimed Lamps have been scanned since she was loaded or her binding ended. */
    boolean scanned() {
        return claimed != null;
    }

    /** Her nearest Tended Lamp with room for the whole of this fuel, skipping those passed over as unreachable. */
    @Nullable BlockPos nextLamp(ServerLevel level, ItemStack fuel, Set<BlockPos> unreachable) {
        if (fuel.isEmpty() || claimed == null) {
            return null;
        }
        int paidFor = Gold.paidFor(nuggets, lampsPerNugget());
        for (var pos : claimed.subList(0, Math.min(paidFor, claimed.size()))) {
            if (!unreachable.contains(pos) && PlacedFuel.fits(level, pos, fuel)) {
                return pos;
            }
        }
        return null;
    }

    private static int lampsPerNugget() {
        return LightConfig.LAMPAD_LAMPS_PER_NUGGET.getAsInt();
    }

    void save(ValueOutput output) {
        output.putInt(NUGGETS, nuggets);
    }

    void load(ValueInput input) {
        nuggets = input.getIntOr(NUGGETS, 0);
    }
}
