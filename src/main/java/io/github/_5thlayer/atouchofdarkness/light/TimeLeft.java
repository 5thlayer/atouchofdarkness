// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** A carried Light Source's countdown in its advanced tooltip (F3+H): the time of day it has left, as m:ss. */
final class TimeLeft {

    private TimeLeft() {
    }

    /** The line, from a translation that takes the m:ss. */
    static Component line(String key, int ticks) {
        int seconds = Mth.positiveCeilDiv(ticks, SharedConstants.TICKS_PER_SECOND);
        return Component.translatable(key, String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)).withStyle(ChatFormatting.DARK_GRAY);
    }
}
