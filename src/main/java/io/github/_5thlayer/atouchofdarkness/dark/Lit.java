// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;

/**
 * The Lit rule (GLOSSARY.md): the light at a player's eyes is at or above the threshold, or their eyes
 * see the sky of a dimension tagged {@link #SKY_ALWAYS_LIT}, or a full moon's night sky with no rain.
 * Night Vision changes nothing here. It is worked out on asking: the light engine's visible data changes
 * only between ticks, so every listener in a tick reads the same light, whatever order they run in.
 */
public final class Lit {

    /** The dimension types the Dark reaches: by default the Overworld's, the Nether's and the End's. */
    public static final TagKey<DimensionType> DIMENSIONS = TagKey.create(Registries.DIMENSION_TYPE, id("dark"));

    /**
     * The dimension types whose open sky always keeps a player Lit, whatever the light at their eyes: by
     * default the End's.
     */
    public static final TagKey<DimensionType> SKY_ALWAYS_LIT = TagKey.create(Registries.DIMENSION_TYPE, id("sky_always_lit"));

    /**
     * One player's reading of the Lit rule.
     *
     * @param light the light at their eyes, 0 to 15: the sky's dimmed for the hour and the weather, or
     *              the block light there, whichever is brighter, carried light included
     * @param lit whether they are Lit, whatever their game mode and dimension
     * @param reached whether the Dark reaches their dimension at all
     * @param inTheDark whether they are in the Dark: reached, neither in Creative nor in Spectator, and
     *                  not Lit. The Pillar's one meaning of it, which everything the Dark does reads.
     */
    public record Reading(int light, boolean lit, boolean reached, boolean inTheDark) {

        /** Whether they are Lit where the Dark reaches, and so always in a dimension it doesn't. */
        public boolean litWhereReached() {
            return !reached || lit;
        }
    }

    private Lit() {
    }

    /** The player's reading now. */
    public static Reading reading(ServerPlayer player) {
        var level = player.level();
        var eyes = eyes(player);
        boolean lit = isLit(level, eyes);
        boolean reached = level.dimensionTypeRegistration().is(DIMENSIONS);
        boolean inTheDark = reached && !player.isCreative() && !player.isSpectator() && !lit;
        return new Reading(lightAt(level, eyes), lit, reached, inTheDark);
    }

    /** Whether eyes at this block would be Lit, as a player's are: for anything that isn't a player. */
    public static boolean isLit(ServerLevel level, BlockPos eyes) {
        if (lightAt(level, eyes) >= DarkConfig.LIT_THRESHOLD.getAsInt()) {
            return true;
        }
        // A dimension with no sky, such as the Nether, has no sky light, so it is never seen there.
        if (!level.canSeeSky(eyes)) {
            return false;
        }
        return level.dimensionTypeRegistration().is(SKY_ALWAYS_LIT) || underAFullMoon(level, eyes);
    }

    /** The block at the player's eyes, which the Lit rule reads and carried light lights. */
    public static BlockPos eyes(ServerPlayer player) {
        return BlockPos.containing(player.getEyePosition());
    }

    private static int lightAt(ServerLevel level, BlockPos eyes) {
        return level.getMaxLocalRawBrightness(eyes);
    }

    /** Night, a full moon, and no rain: a dimension with a fixed time, such as the End, has no night. */
    private static boolean underAFullMoon(ServerLevel level, BlockPos eyes) {
        return level.isDarkOutside() && !level.isRaining()
                && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, eyes) == MoonPhase.FULL_MOON;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, path);
    }
}
