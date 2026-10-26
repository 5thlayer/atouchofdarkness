// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DeathMessageType;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.fml.common.asm.enumextension.EnumProxy;
import net.neoforged.neoforge.common.damagesource.IDeathMessageProvider;

/**
 * The death message of a player a Strike kills: one of several, picked at random, each naming the
 * Babau, and each with a form for a player who was fleeing a mob too. The Strike's damage type names
 * it, a {@link DeathMessageType} that {@code META-INF/enumextensions.json} adds from {@link Proxy}.
 */
public final class StrikeDeathMessage {

    /** How many there are, {@code death.attack.atouchofdarkness.strike.1} on. */
    public static final int VARIANTS = 4;

    private StrikeDeathMessage() {
    }

    /** The message numbered {@code variant}, from 1; the fleeing form when a mob had the victim's kill credit. */
    public static Component message(LivingEntity victim, int variant) {
        String key = "death.attack." + ATouchOfDarkness.MOD_ID + ".strike." + variant;
        LivingEntity fled = victim.getKillCredit();
        return fled == null
                ? Component.translatable(key, victim.getDisplayName())
                : Component.translatable(key + ".player", victim.getDisplayName(), fled.getDisplayName());
    }

    /** What NeoForge reads as {@link DeathMessageType} loads. */
    public static final class Proxy {

        public static final EnumProxy<DeathMessageType> STRIKE = new EnumProxy<>(DeathMessageType.class,
                ATouchOfDarkness.MOD_ID + ":strike",
                (IDeathMessageProvider) (victim, killingBlow, fall) -> message(victim, 1 + victim.getRandom().nextInt(VARIANTS)));

        private Proxy() {
        }
    }
}
