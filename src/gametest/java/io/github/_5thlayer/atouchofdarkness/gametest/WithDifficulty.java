// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.gametest;

import com.mojang.serialization.MapCodec;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.Difficulty;

/**
 * A test environment whose batch runs on one difficulty, set back after it. The difficulty is the
 * server's, so no test on another can run beside it.
 */
record WithDifficulty(Difficulty difficulty) implements TestEnvironmentDefinition<Difficulty> {

    static final MapCodec<WithDifficulty> CODEC = Difficulty.CODEC.fieldOf("difficulty").xmap(WithDifficulty::new, WithDifficulty::difficulty);

    @Override
    public Difficulty setup(ServerLevel level) {
        var before = level.getDifficulty();
        level.getServer().setDifficulty(difficulty, true);
        return before;
    }

    @Override
    public void teardown(ServerLevel level, Difficulty before) {
        level.getServer().setDifficulty(before, true);
    }

    @Override
    public MapCodec<WithDifficulty> codec() {
        return CODEC;
    }
}
