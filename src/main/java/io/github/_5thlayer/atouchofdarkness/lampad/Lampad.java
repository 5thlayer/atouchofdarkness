// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.lampad;

import java.util.function.Supplier;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.transfer.item.WorldlyContainerWrapper;

/**
 * The Lampad (GLOSSARY.md), a {@link LampadEntity}, and her Votive, a {@link VotiveBlock}: summoned
 * together by {@link Summoning} from a Lamp on gold kindled with a Torch. Her spawn egg makes one with
 * no Votive, who stands dormant.
 */
public final class Lampad {

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.Entities ENTITIES = DeferredRegister.createEntities(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, ATouchOfDarkness.MOD_ID);

    /** The copper golem's size, near enough. */
    public static final DeferredHolder<EntityType<?>, EntityType<LampadEntity>> LAMPAD = ENTITIES.registerEntityType("lampad", LampadEntity::new,
            MobCategory.MISC, builder -> builder.sized(0.5F, 1.2F).eyeHeight(1.0F).clientTrackingRange(10));
    public static final DeferredItem<SpawnEggItem> LAMPAD_SPAWN_EGG = ITEMS.registerItem("lampad_spawn_egg", SpawnEggItem::new,
            properties -> properties.spawnEgg(LAMPAD.get()));

    /** A gold block's, which it was. */
    public static final DeferredBlock<VotiveBlock> VOTIVE = BLOCKS.registerBlock("votive", VotiveBlock::new,
            properties -> properties.mapColor(MapColor.GOLD).requiresCorrectToolForDrops().strength(3F, 6F).sound(SoundType.METAL));
    public static final DeferredItem<BlockItem> VOTIVE_ITEM = ITEMS.registerSimpleBlockItem(VOTIVE);
    public static final Supplier<BlockEntityType<VotiveBlockEntity>> VOTIVE_ENTITY = BLOCK_ENTITIES.register("votive",
            () -> new BlockEntityType<>(VotiveBlockEntity::new, VOTIVE.get()));
    public static final Supplier<MenuType<VotiveMenu>> VOTIVE_MENU = MENUS.register("votive",
            () -> new MenuType<>(VotiveMenu::new, FeatureFlags.VANILLA_SET));

    private Lampad() {
    }

    public static void register(IEventBus modBus) {
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        ENTITIES.register(modBus);
        MENUS.register(modBus);
        modBus.addListener((EntityAttributeCreationEvent event) -> event.put(LAMPAD.get(), LampadEntity.createAttributes().build()));
        // Other mods' pipes, through the item handler; vanilla's hoppers see the container itself.
        modBus.addListener((RegisterCapabilitiesEvent event) ->
                event.registerBlockEntity(Capabilities.Item.BLOCK, VOTIVE_ENTITY.get(), WorldlyContainerWrapper::new));
        NeoForge.EVENT_BUS.addListener(Summoning::kindle);
    }
}
