// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.util.Unit;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;

/**
 * A placed Lamp's block entity, which hands its {@link Fuel}, kept by {@link PlacedFuel} as the
 * {@link Light#FUEL} attachment, between it and the item: taken from the item placed and given back to
 * the item it drops, with its lit marker while it is on.
 */
public final class LampBlockEntity extends BlockEntity {

    /** Where 0.1.x saved its fuel, before the attachment. */
    private static final String OLD_FUEL = "fuel";

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(Light.LAMP_ENTITY.get(), pos, state);
    }

    /** Gives it this much fuel, burning from its next tick. */
    public void setFuel(int left) {
        setData(Light.FUEL, Fuel.unclocked(left));
    }

    public int fuelLeft() {
        return getData(Light.FUEL).left();
    }

    /** A Lamp saved by 0.1.x keeps its fuel, from its own key, which it isn't saved with again. Kept until 1.0. */
    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.read(OLD_FUEL, Fuel.CODEC.codec()).ifPresent(fuel -> setData(Light.FUEL, fuel));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        setData(Light.FUEL, Fuel.unclocked(components.getOrDefault(Light.LAMP_FUEL.get(), 0)));
        // Read, so it isn't kept as the item's: the block's state says whether it is lit.
        components.get(Light.LAMP_ON.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Light.LAMP_FUEL.get(), getData(Light.FUEL).left());
        if (getBlockState().getValue(LampBlock.LIT)) {
            components.set(Light.LAMP_ON.get(), Unit.INSTANCE);
        }
    }
}
