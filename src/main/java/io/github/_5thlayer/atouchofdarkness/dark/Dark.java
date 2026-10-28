// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.dark;

import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.discovery.Discoveries;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.attribute.EnvironmentAttributes;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.level.MoonPhase;
import net.minecraft.world.level.dimension.DimensionType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Dark (GLOSSARY.md): every game tick, each player in a dimension tagged
 * {@code atouchofdarkness:dark} who isn't Lit drains their Grace Period, and a Strike lands when it
 * runs out, and the player feels both through the {@link DarkCues}. Only players are ticked, and the
 * Dark never reaches Creative and Spectator players.
 */
public final class Dark {

    /** The dimension types the Dark reaches: by default the Overworld's, the Nether's and the End's. */
    public static final TagKey<DimensionType> DIMENSIONS = TagKey.create(Registries.DIMENSION_TYPE, id("dark"));

    /**
     * The dimension types whose open sky always keeps a player Lit, whatever the light at their eyes: by
     * default the End's.
     */
    public static final TagKey<DimensionType> SKY_ALWAYS_LIT = TagKey.create(Registries.DIMENSION_TYPE, id("sky_always_lit"));

    /** The Strike's damage type, {@code data/atouchofdarkness/damage_type/strike.json}. */
    public static final ResourceKey<DamageType> STRIKE = ResourceKey.create(Registries.DAMAGE_TYPE, id("strike"));

    /** Kept with the player, so logging out and back in refills nothing. */
    private static final MapCodec<GracePeriod> GRACE_PERIOD_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.INT.fieldOf("drained").forGetter(GracePeriod::drained),
            Codec.INT.fieldOf("lit_for").forGetter(GracePeriod::litFor),
            Codec.BOOL.fieldOf("spent").forGetter(GracePeriod::spent),
            Codec.INT.fieldOf("since_strike").forGetter(GracePeriod::sinceStrike)
    ).apply(instance, GracePeriod::new));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ATouchOfDarkness.MOD_ID);

    /** Not copied on death, so a respawned player starts with a full Grace Period. */
    private static final Supplier<AttachmentType<GracePeriod>> GRACE_PERIOD = ATTACHMENTS.register("grace_period",
            () -> AttachmentType.builder(GracePeriod::new).serialize(GRACE_PERIOD_CODEC).build());

    private Dark() {
    }

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
        DarkCues.register(modBus);
        NeoForge.EVENT_BUS.addListener(Dark::tick);
    }

    /**
     * Whether the player is Lit: the light at their eyes is at or above the threshold, or their eyes see
     * the sky of a dimension tagged {@link #SKY_ALWAYS_LIT}, or a full moon's night sky with no rain.
     * Night Vision changes nothing here.
     */
    public static boolean isLit(ServerPlayer player) {
        return isLit(player.level(), eyes(player));
    }

    /** Whether eyes at this block would be Lit, as a player's are. */
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

    /**
     * The light at the player's eyes, 0 to 15, as the Lit check measures it: the sky's dimmed for the
     * hour and the weather, or the block light there, whichever is brighter, carried light included.
     */
    public static int lightAtEyes(ServerPlayer player) {
        return lightAt(player.level(), eyes(player));
    }

    /**
     * Whether the player is Lit where the Dark reaches, and so always in a dimension it doesn't. Unlike
     * {@link #inTheDark}, it reads the Lit check alone, whatever the player's game mode.
     */
    public static boolean isLitWhereReached(ServerPlayer player) {
        return !reaches(player) || isLit(player);
    }

    /** The block at the player's eyes, which the Lit check reads and carried light lights. */
    public static BlockPos eyes(ServerPlayer player) {
        return BlockPos.containing(player.getEyePosition());
    }

    private static int lightAt(ServerLevel level, BlockPos eyes) {
        return level.getMaxLocalRawBrightness(eyes);
    }

    /** Whether the Dark reaches the player's dimension at all. */
    private static boolean reaches(ServerPlayer player) {
        return player.level().dimensionTypeRegistration().is(DIMENSIONS);
    }

    /** Night, a full moon, and no rain: a dimension with a fixed time, such as the End, has no night. */
    private static boolean underAFullMoon(ServerLevel level, BlockPos eyes) {
        return level.isDarkOutside() && !level.isRaining()
                && level.environmentAttributes().getValue(EnvironmentAttributes.MOON_PHASE, eyes) == MoonPhase.FULL_MOON;
    }

    /**
     * Whether the player is in the Dark: in a dimension tagged {@link #DIMENSIONS}, neither in Creative
     * nor in Spectator, and not Lit. The Pillar's one meaning of it, which everything the Dark does reads.
     */
    public static boolean inTheDark(ServerPlayer player) {
        return reaches(player) && !player.isCreative() && !player.isSpectator() && !isLit(player);
    }

    /** Ticks the player could still spend in the Dark before the next Strike lands. */
    public static int ticksToStrike(ServerPlayer player) {
        return player.getData(GRACE_PERIOD).ticksToStrike(DarkConfig.gracePeriodSettings());
    }

    private static void tick(LevelTickEvent.Post event) {
        if (!(event.getLevel() instanceof ServerLevel level) || level.players().isEmpty()) {
            return;
        }
        var settings = DarkConfig.gracePeriodSettings();
        // A copy, since a Strike can kill and so change the list.
        for (ServerPlayer player : level.players().toArray(ServerPlayer[]::new)) {
            if (!player.isAlive()) {
                continue;
            }
            boolean inTheDark = inTheDark(player);
            var gracePeriod = player.getData(GRACE_PERIOD);
            var cues = gracePeriod.tick(inTheDark, settings);
            cues.forEach(cue -> DarkCues.play(player, cue));
            if (cues.contains(GracePeriod.Cue.STRIKE)) {
                strike(level, player);
            }
            DarkCues.send(player, new DarkState(inTheDark, gracePeriod.drainedFraction(settings)));
        }
    }

    /** One Strike: the config's damage, which the damage type's scaling sets for the difficulty. */
    private static void strike(ServerLevel level, ServerPlayer player) {
        player.hurtServer(level, level.damageSources().source(STRIKE), (float) DarkConfig.STRIKE_DAMAGE.getAsDouble());
        Discoveries.struck(player);
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, path);
    }
}
