// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageScaling;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.common.damagesource.IScalingFunction;

/**
 * How difficulty scales a Strike: nothing on Peaceful, half on Easy, all on Normal and half again on
 * Hard, so the default 10 deals 0 / 5 / 10 / 15 before armour. Vanilla's own scaling gives Easy half
 * plus one, so the Strike's damage type names this one, a {@link DamageScaling} that
 * {@code META-INF/enumextensions.json} adds from {@link Proxy}.
 */
public final class StrikeScaling {

    private StrikeScaling() {
    }

    /**
     * The damage on a difficulty, given by its {@link Difficulty#getId() id}: 0 for Peaceful up to 3 for
     * Hard. An id, as the JUnit tests run with no Minecraft on the class path.
     */
    public static float scale(float base, int difficulty) {
        return base * switch (difficulty) {
            case 0 -> 0F;
            case 1 -> 0.5F;
            case 2 -> 1F;
            case 3 -> 1.5F;
            default -> throw new IllegalArgumentException("no difficulty has the id " + difficulty);
        };
    }

    /** What NeoForge reads as {@link DamageScaling} loads. */
    public static final class Proxy {

        public static final EnumProxy<DamageScaling> STRIKE = new EnumProxy<>(DamageScaling.class,
                ATouchOfDarkness.MOD_ID + ":strike",
                (IScalingFunction) (source, target, amount, difficulty) -> scale(amount, difficulty.getId()));

        private Proxy() {
        }
    }
}
