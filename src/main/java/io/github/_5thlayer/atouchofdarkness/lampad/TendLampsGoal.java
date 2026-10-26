// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;

import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.PlacedFuel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

/**
 * A Lampad's round, as a copper golem carries items between chests: to her Votive for one fuel item,
 * then to the nearest Lamp she tends that the whole of it fits, and in with it, a sound at each end and
 * a burst of flame at the Lamp. She sets out only when some Lamp has room for what her Votive holds,
 * or for what she still carries.
 */
final class TendLampsGoal extends Goal {

    /** How far from her eyes she reaches into a Votive or a Lamp, the copper golem's reach into a chest. */
    private static final double REACH = 3.0;

    /** Ticks between her looks for a Lamp to feed while idle. */
    private static final int LOOK_INTERVAL = 20;

    /** Times she paths again to where she is going before she gives it up. */
    private static final int TRIES = 4;

    /** Ticks a Lamp she couldn't reach is passed over. */
    private static final int UNREACHABLE_TICKS = 400;

    private final LampadEntity lampad;
    private final Map<BlockPos, Long> unreachable = new HashMap<>();
    private int nextLook;
    private @Nullable BlockPos lamp;
    private int tries;
    private boolean done;

    TendLampsGoal(LampadEntity lampad) {
        this.lampad = lampad;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    @Override
    public boolean canUse() {
        if (lampad.isDormant() || lampad.tickCount < nextLook) {
            return false;
        }
        nextLook = lampad.tickCount + LOOK_INTERVAL;
        lamp = lampToFeed(carried().isEmpty() ? votiveFuel() : carried());
        return lamp != null;
    }

    @Override
    public boolean canContinueToUse() {
        return !done && !lampad.isDormant() && lamp != null;
    }

    @Override
    public void start() {
        done = false;
        tries = 0;
        moveTo(destination());
    }

    @Override
    public void stop() {
        lampad.getNavigation().stop();
        lamp = null;
    }

    @Override
    public boolean requiresUpdateEveryTick() {
        return true;
    }

    @Override
    public void tick() {
        var destination = destination();
        if (destination == null) {
            done = true;
            return;
        }
        lampad.getLookControl().setLookAt(Vec3.atCenterOf(destination));
        if (lampad.getEyePosition().closerThan(Vec3.atCenterOf(destination), REACH)) {
            if (carried().isEmpty()) {
                takeFuel();
            } else {
                feed();
            }
        } else if (lampad.getNavigation().isDone()) {
            if (++tries > TRIES) {
                if (!carried().isEmpty()) {
                    unreachable.put(lamp, lampad.level().getGameTime() + UNREACHABLE_TICKS);
                }
                done = true;
            } else {
                moveTo(destination);
            }
        }
    }

    /** Her Votive while her hand is empty, else the Lamp. */
    private @Nullable BlockPos destination() {
        return carried().isEmpty() ? lampad.votive() : lamp;
    }

    /** One fuel item from her Votive into her hand, then off to the Lamp it fits. */
    private void takeFuel() {
        var votive = lampad.votiveEntity();
        lamp = votive == null ? null : lampToFeed(votive.peekFuel());
        if (lamp == null) {
            done = true;
            return;
        }
        lampad.setItemInHand(InteractionHand.MAIN_HAND, votive.takeFuel());
        lampad.swing(InteractionHand.MAIN_HAND);
        lampad.playSound(SoundEvents.ALLAY_ITEM_TAKEN, 1F, 1F);
        tries = 0;
        moveTo(lamp);
    }

    /** The whole item into the Lamp, or, if it no longer fits, on to another it does. */
    private void feed() {
        var level = (ServerLevel) lampad.level();
        var fuel = carried();
        if (!PlacedFuel.fits(level, lamp, fuel)) {
            lamp = lampToFeed(fuel);
            tries = 0;
            if (lamp == null) {
                done = true;
            } else {
                moveTo(lamp);
            }
            return;
        }
        PlacedFuel.take(level, lamp, fuel);
        fuel.shrink(1);
        lampad.swing(InteractionHand.MAIN_HAND);
        fedCues(level, lamp);
        done = true;
    }

    /**
     * What a player sees and hears of a Lamp fed: the allay's chime of an item given and the crackle of
     * fuel taking, with a burst of flame, a soul Lamp's blue, rising from the Lamp.
     */
    private void fedCues(ServerLevel level, BlockPos lamp) {
        lampad.playSound(SoundEvents.ALLAY_ITEM_GIVEN, 1F, 1F);
        level.playSound(null, lamp, SoundEvents.FIRECHARGE_USE, lampad.getSoundSource(), 0.8F, 1.2F);
        var flame = level.getBlockState(lamp).is(Light.SOUL_LAMP) ? ParticleTypes.SOUL_FIRE_FLAME : ParticleTypes.FLAME;
        var centre = Vec3.atCenterOf(lamp);
        level.sendParticles(flame, centre.x, centre.y + 0.2, centre.z, 12, 0.2, 0.25, 0.2, 0.02);
        level.sendParticles(ParticleTypes.SMOKE, centre.x, centre.y + 0.5, centre.z, 4, 0.1, 0.1, 0.1, 0.01);
    }

    /** The nearest Lamp she tends that has room for the whole of this, but those she couldn't reach of late. */
    private @Nullable BlockPos lampToFeed(ItemStack fuel) {
        if (fuel.isEmpty()) {
            return null;
        }
        var level = (ServerLevel) lampad.level();
        long now = level.getGameTime();
        unreachable.values().removeIf(until -> until <= now);
        for (var pos : lampad.tended()) {
            if (!unreachable.containsKey(pos) && PlacedFuel.fits(level, pos, fuel)) {
                return pos;
            }
        }
        return null;
    }

    private ItemStack votiveFuel() {
        var votive = lampad.votiveEntity();
        return votive == null ? ItemStack.EMPTY : votive.peekFuel();
    }

    private ItemStack carried() {
        return lampad.getMainHandItem();
    }

    private void moveTo(@Nullable BlockPos pos) {
        if (pos != null) {
            lampad.getNavigation().moveTo(lampad.getNavigation().createPath(pos, 1), 1.0);
        }
    }
}
