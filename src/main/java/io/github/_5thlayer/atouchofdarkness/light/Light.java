// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.component.DataComponentType;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.Unit;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.NoteBlockInstrument;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.attachment.IAttachmentHolder;
import net.neoforged.neoforge.event.BlockEntityTypeAddBlocksEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * The Pillar's Light Sources (GLOSSARY.md), which all burn out: the Torch, the soul Torch and the Spent
 * Torch they become, their {@link Burning} and the {@link CarriedLight} a held one gives; and the Campfire
 * and the soul Campfire, placed, with their {@link Fuel}; and the Lamp and the soul Lamp, carried or placed.
 */
public final class Light {

    /** A Torch's light, and the soul Torch's. */
    public static final int TORCH_LIGHT = 14;
    public static final int SOUL_TORCH_LIGHT = 10;

    /** A Lamp's light while it is on with fuel, and the soul Lamp's. */
    public static final int LAMP_LIGHT = 15;
    public static final int SOUL_LAMP_LIGHT = 10;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS =
            DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, ATouchOfDarkness.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Burning>> BURNING = COMPONENTS.registerComponentType("burning",
            builder -> builder.persistent(Burning.CODEC).networkSynchronized(Burning.STREAM_CODEC));

    /** A carried Lamp's fuel, in ticks. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Integer>> LAMP_FUEL = COMPONENTS.registerComponentType("lamp_fuel",
            builder -> builder.persistent(ExtraCodecs.NON_NEGATIVE_INT).networkSynchronized(ByteBufCodecs.VAR_INT));
    /** A carried Lamp that is on, with fuel; its item model shows it lit. */
    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Unit>> LAMP_ON = COMPONENTS.registerComponentType("lamp_on",
            builder -> builder.persistent(Unit.CODEC).networkSynchronized(Unit.STREAM_CODEC));

    public static final DeferredItem<TorchItem> TORCH = ITEMS.registerItem("torch",
            properties -> new TorchItem(properties, TORCH_LIGHT, 1, 0xFFB040), properties -> properties.stacksTo(1));
    public static final DeferredItem<TorchItem> SOUL_TORCH = ITEMS.registerItem("soul_torch",
            properties -> new TorchItem(properties, SOUL_TORCH_LIGHT, 2, 0x60D8E8), properties -> properties.stacksTo(1));
    public static final DeferredItem<SpentTorchItem> SPENT_TORCH = ITEMS.registerItem("spent_torch", SpentTorchItem::new);

    /** Saved with its block entity; one not yet given any holds what the block is placed with. */
    public static final Supplier<AttachmentType<Fuel>> FUEL = ATTACHMENTS.register("fuel",
            () -> AttachmentType.builder(Light::placedFuel).serialize(Fuel.CODEC).build());

    public static final DeferredBlock<FuelledCampfireBlock> CAMPFIRE = BLOCKS.registerBlock("campfire", FuelledCampfireBlock::campfire,
            Light::campfire);
    public static final DeferredBlock<FuelledCampfireBlock> SOUL_CAMPFIRE = BLOCKS.registerBlock("soul_campfire", FuelledCampfireBlock::soul,
            Light::campfire);
    public static final DeferredItem<BlockItem> CAMPFIRE_ITEM = ITEMS.registerSimpleBlockItem(CAMPFIRE);
    public static final DeferredItem<BlockItem> SOUL_CAMPFIRE_ITEM = ITEMS.registerSimpleBlockItem(SOUL_CAMPFIRE);

    public static final DeferredBlock<LampBlock> LAMP = BLOCKS.registerBlock("lamp", LampBlock::new, properties -> lamp(properties, LAMP_LIGHT));
    public static final DeferredBlock<LampBlock> SOUL_LAMP = BLOCKS.registerBlock("soul_lamp", LampBlock::new, properties -> lamp(properties, SOUL_LAMP_LIGHT));
    public static final DeferredItem<LampItem> LAMP_ITEM = ITEMS.registerItem("lamp",
            properties -> new LampItem(LAMP.get(), properties, LAMP_LIGHT, 1, 0xFFB040), properties -> properties.stacksTo(1).useBlockDescriptionPrefix());
    public static final DeferredItem<LampItem> SOUL_LAMP_ITEM = ITEMS.registerItem("soul_lamp",
            properties -> new LampItem(SOUL_LAMP.get(), properties, SOUL_LAMP_LIGHT, 2, 0x60D8E8), properties -> properties.stacksTo(1).useBlockDescriptionPrefix());
    public static final Supplier<BlockEntityType<LampBlockEntity>> LAMP_ENTITY = BLOCK_ENTITIES.register("lamp",
            () -> new BlockEntityType<>(LampBlockEntity::new, LAMP.get(), SOUL_LAMP.get()));

    /** No item: nothing places it but {@link CarriedLight}. */
    public static final DeferredBlock<CarriedLightBlock> CARRIED_LIGHT = BLOCKS.registerBlock("carried_light", CarriedLightBlock::new,
            properties -> properties.replaceable().noCollision().noOcclusion().noLootTable()
                    .strength(-1F, 3600000.8F).pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(CarriedLightBlock.LEVEL)));

    private Light() {
    }

    /** Vanilla's campfire's, but for the light, which its fuel sets, and the loot table, which a full copy would take too. */
    private static BlockBehaviour.Properties campfire(BlockBehaviour.Properties properties) {
        return properties.mapColor(MapColor.PODZOL).instrument(NoteBlockInstrument.BASS).strength(2F).sound(SoundType.WOOD).noOcclusion().ignitedByLava()
                .lightLevel(state -> state.getValue(FuelledCampfireBlock.LIT) ? state.getValue(FuelledCampfireBlock.LIGHT) : 0);
    }

    /** Vanilla's lantern's, but for the light, which it gives only while lit. */
    private static BlockBehaviour.Properties lamp(BlockBehaviour.Properties properties, int light) {
        return properties.mapColor(MapColor.METAL).forceSolidOn().strength(3.5F).sound(SoundType.LANTERN).noOcclusion().pushReaction(PushReaction.DESTROY)
                .lightLevel(state -> state.getValue(LampBlock.LIT) ? light : 0);
    }

    private static Fuel placedFuel(IAttachmentHolder holder) {
        return holder instanceof BlockEntity entity && entity.getBlockState().getBlock() instanceof FuelledCampfireBlock campfire
                ? Fuel.unclocked(campfire.placedFuel()) : Fuel.unclocked(0);
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ATTACHMENTS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        CarriedLight.register(modBus);
        // Vanilla's block entity, so the Campfires cook and render as vanilla's do.
        modBus.addListener((BlockEntityTypeAddBlocksEvent event) -> event.modify(BlockEntityType.CAMPFIRE, CAMPFIRE.get(), SOUL_CAMPFIRE.get()));
    }
}
