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
import net.minecraft.world.level.storage.ValueOutput;

/**
 * A placed Lamp's {@link Fuel}, saved with it, which it takes from the item placed and gives back to the
 * item it drops, with its lit marker while it is on.
 */
public final class LampBlockEntity extends BlockEntity {

    private static final String FUEL = "fuel";

    private Fuel fuel = Fuel.unclocked(0);

    public LampBlockEntity(BlockPos pos, BlockState state) {
        super(Light.LAMP_ENTITY.get(), pos, state);
    }

    Fuel fuel() {
        return fuel;
    }

    /** Gives it this much fuel, burning from its next tick. */
    public void setFuel(int left) {
        fuel = Fuel.unclocked(left);
        setChanged();
    }

    public int fuelLeft() {
        return fuel.left();
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store(FUEL, Fuel.CODEC.codec(), fuel);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        fuel = input.read(FUEL, Fuel.CODEC.codec()).orElseGet(() -> Fuel.unclocked(0));
    }

    @Override
    protected void applyImplicitComponents(DataComponentGetter components) {
        super.applyImplicitComponents(components);
        fuel = Fuel.unclocked(components.getOrDefault(Light.LAMP_FUEL.get(), 0));
        // Read, so it isn't kept as the item's: the block's state says whether it is lit.
        components.get(Light.LAMP_ON.get());
    }

    @Override
    protected void collectImplicitComponents(DataComponentMap.Builder components) {
        super.collectImplicitComponents(components);
        components.set(Light.LAMP_FUEL.get(), fuel.left());
        if (getBlockState().getValue(LampBlock.LIT)) {
            components.set(Light.LAMP_ON.get(), Unit.INSTANCE);
        }
    }
}
