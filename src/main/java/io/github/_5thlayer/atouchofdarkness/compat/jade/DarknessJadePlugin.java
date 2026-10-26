// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.compat.jade;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import io.github._5thlayer.atouchofdarkness.light.FuelledCampfireBlock;
import io.github._5thlayer.atouchofdarkness.light.LampBlock;
import io.github._5thlayer.atouchofdarkness.light.LampBlockEntity;
import io.github._5thlayer.atouchofdarkness.light.Light;
import io.github._5thlayer.atouchofdarkness.light.TimeLeft;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.entity.CampfireBlockEntity;
import org.jspecify.annotations.Nullable;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaCommonRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.StreamServerDataProvider;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;

/**
 * A placed Light Source's fuel in Jade, which only the server knows: a Campfire's time left at the
 * still rate and the light it gives now, or "Out" once dry; a Lamp's "On" or "Off", then its time left,
 * or "Empty" once dry. Shown always, not only with advanced tooltips.
 *
 * <p>Found by Jade's annotation scan and named nowhere else, so Jade stays a soft dependency.
 */
@WailaPlugin
public final class DarknessJadePlugin implements IWailaPlugin {

    private static final Identifier FUEL = Identifier.fromNamespaceAndPath(ATouchOfDarkness.MOD_ID, "fuel");

    private static final String FUEL_LEFT = "atouchofdarkness.jade.fuel_left";
    private static final String LIGHT = "atouchofdarkness.jade.light";
    private static final String OUT = "atouchofdarkness.jade.out";
    private static final String ON = "atouchofdarkness.jade.on";
    private static final String OFF = "atouchofdarkness.jade.off";
    private static final String EMPTY = "atouchofdarkness.jade.empty";

    @Override
    public void register(IWailaCommonRegistration registration) {
        registration.registerBlockDataProvider(Server.INSTANCE, FuelledCampfireBlock.class);
        registration.registerBlockDataProvider(Server.INSTANCE, LampBlock.class);
    }

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(Client.INSTANCE, FuelledCampfireBlock.class);
        registration.registerBlockComponent(Client.INSTANCE, LampBlock.class);
    }

    /** The server's half: the ticks of fuel the placed light has left. Names no client type. */
    private enum Server implements StreamServerDataProvider<BlockAccessor, Integer> {
        INSTANCE;

        @Override
        public @Nullable Integer streamData(BlockAccessor accessor) {
            return switch (accessor.getBlockEntity()) {
                case LampBlockEntity lamp -> lamp.fuelLeft();
                case CampfireBlockEntity campfire when accessor.getBlock() instanceof FuelledCampfireBlock -> campfire.getData(Light.FUEL).left();
                case null, default -> null;
            };
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, Integer> streamCodec() {
            return ByteBufCodecs.VAR_INT.cast();
        }

        @Override
        public Identifier getUid() {
            return FUEL;
        }
    }

    /** The client's half: the lines, from the fuel sent and the block's state. Client-only. */
    private enum Client implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            Server.INSTANCE.decodeFromData(accessor).ifPresent(left -> {
                var state = accessor.getBlockState();
                if (accessor.getBlock() instanceof LampBlock) {
                    tooltip.add(Component.translatable(state.getValue(LampBlock.ON) ? ON : OFF));
                    tooltip.add(left > 0 ? fuelLeft(left) : Component.translatable(EMPTY));
                } else if (left > 0) {
                    tooltip.add(fuelLeft(left));
                    tooltip.add(Component.translatable(LIGHT, FuelledCampfireBlock.light(state)));
                } else {
                    tooltip.add(Component.translatable(OUT));
                }
            });
        }

        private static Component fuelLeft(int left) {
            return Component.translatable(FUEL_LEFT, TimeLeft.clock(left));
        }

        @Override
        public Identifier getUid() {
            return FUEL;
        }
    }
}
