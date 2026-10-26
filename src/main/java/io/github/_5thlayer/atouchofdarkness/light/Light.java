// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import io.github._5thlayer.atouchofdarkness.ATouchOfDarkness;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * The Pillar's Light Sources (GLOSSARY.md), which all burn out: the Torch, the soul Torch and the Spent
 * Torch they become, their {@link Burning} and the {@link CarriedLight} a held one gives.
 */
public final class Light {

    /** A Torch's light, and the soul Torch's. */
    public static final int TORCH_LIGHT = 14;
    public static final int SOUL_TORCH_LIGHT = 10;

    private static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(ATouchOfDarkness.MOD_ID);
    private static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, ATouchOfDarkness.MOD_ID);

    public static final DeferredHolder<DataComponentType<?>, DataComponentType<Burning>> BURNING = COMPONENTS.registerComponentType("burning",
            builder -> builder.persistent(Burning.CODEC).networkSynchronized(Burning.STREAM_CODEC));

    public static final DeferredItem<TorchItem> TORCH = ITEMS.registerItem("torch",
            properties -> new TorchItem(properties, TORCH_LIGHT, 1, 0xFFB040), properties -> properties.stacksTo(1));
    public static final DeferredItem<TorchItem> SOUL_TORCH = ITEMS.registerItem("soul_torch",
            properties -> new TorchItem(properties, SOUL_TORCH_LIGHT, 2, 0x60D8E8), properties -> properties.stacksTo(1));
    public static final DeferredItem<SpentTorchItem> SPENT_TORCH = ITEMS.registerItem("spent_torch", SpentTorchItem::new);

    /** No item: nothing places it but {@link CarriedLight}. */
    public static final DeferredBlock<CarriedLightBlock> CARRIED_LIGHT = BLOCKS.registerBlock("carried_light", CarriedLightBlock::new,
            properties -> properties.replaceable().noCollision().noOcclusion().noLootTable()
                    .strength(-1F, 3600000.8F).pushReaction(PushReaction.DESTROY)
                    .lightLevel(state -> state.getValue(CarriedLightBlock.LEVEL)));

    private Light() {
    }

    public static void register(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        BLOCKS.register(modBus);
        CarriedLight.register(modBus);
    }
}
