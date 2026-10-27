// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Set;

import io.github._5thlayer.atouchofdarkness.dark.Dark;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import org.jspecify.annotations.Nullable;

/**
 * A hurt Lampad's retreat: to the nearest spot in her Votive's range where she would stand in the Dark,
 * not Lit, and there she heals 1 health every 40 ticks until she is whole, soul sparks rising from her
 * at each. Once in the Dark within her range she heals where she is. With no such spot in range she waits where she
 * stands, healing none, until one is. It holds her still, so she tends nothing meanwhile.
 */
final class HealGoal extends Goal {

    /** Ticks in the Dark to each health she heals. */
    private static final int HEAL_INTERVAL = 40;

    /** Ticks between her looks for a spot in the Dark while she is Lit. */
    private static final int LOOK_INTERVAL = 20;

    /** The spots in the Dark nearest her, as the crow flies, that she looks for a path to. */
    private static final int CANDIDATES = 256;

    private final LampadEntity lampad;
    private @Nullable BlockPos spot;
    private int nextLook;
    private int healIn;

    HealGoal(LampadEntity lampad) {
        this.lampad = lampad;
        setFlags(EnumSet.of(Flag.MOVE));
    }

    @Override
    public boolean canUse() {
        return !lampad.isDormant() && lampad.isHurt();
    }

    @Override
    public void start() {
        spot = null;
        nextLook = 0;
    }

    @Override
    public void stop() {
        lampad.getNavigation().stop();
        lampad.setHealing(false);
        spot = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var level = (ServerLevel) lampad.level();
        var votive = lampad.votive();
        boolean inTheDark = votive != null && TendedLamps.inRange(votive, lampad.blockPosition())
                && !Dark.isLit(level, BlockPos.containing(lampad.getEyePosition()));
        if (inTheDark) {
            // Counted from the tick she is in the Dark, so each health comes a whole 40 ticks after.
            if (!lampad.isHealing()) {
                lampad.getNavigation().stop();
                lampad.setHealing(true);
                healIn = HEAL_INTERVAL;
            } else if (--healIn <= 0) {
                healIn = HEAL_INTERVAL;
                lampad.heal(1F);
                level.sendParticles(ParticleTypes.SOUL, lampad.getX(), lampad.getY() + 0.8, lampad.getZ(), 3, 0.2, 0.3, 0.2, 0.01);
            }
            return;
        }
        lampad.setHealing(false);
        boolean needsSpot = spot == null || lampad.getNavigation().isDone() || Dark.isLit(level, spot.above());
        if (needsSpot && lampad.tickCount >= nextLook) {
            nextLook = lampad.tickCount + LOOK_INTERVAL;
            spot = walkToTheDark(level);
        }
    }

    /** Sets off for the nearest spot in range she can reach where she would stand in the Dark; it, or null, standing still. */
    private @Nullable BlockPos walkToTheDark(ServerLevel level) {
        var votive = lampad.votive();
        if (votive == null) {
            return null;
        }
        var spots = new ArrayList<BlockPos>();
        for (var pos : TendedLamps.range(votive)) {
            if (level.isLoaded(pos) && standsOn(level, pos) && !Dark.isLit(level, pos.above())) {
                spots.add(pos.immutable());
            }
        }
        // The nearest by path of the nearest few, as the crow flies.
        var here = lampad.blockPosition();
        spots.sort(Comparator.comparingDouble(pos -> pos.distSqr(here)));
        var path = lampad.getNavigation().createPath(Set.copyOf(spots.subList(0, Math.min(CANDIDATES, spots.size()))), 0);
        if (path != null && path.canReach()) {
            lampad.getNavigation().moveTo(path, 1.0);
            return path.getTarget();
        }
        lampad.getNavigation().stop();
        return null;
    }

    /** Whether she can stand here: two blocks clear over a floor. */
    private static boolean standsOn(ServerLevel level, BlockPos pos) {
        var below = pos.below();
        return level.getBlockState(pos).getCollisionShape(level, pos).isEmpty()
                && level.getBlockState(pos.above()).getCollisionShape(level, pos.above()).isEmpty()
                && level.getBlockState(below).isFaceSturdy(level, below, Direction.UP);
    }
}
