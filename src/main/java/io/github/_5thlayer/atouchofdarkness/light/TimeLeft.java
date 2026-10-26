// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.light;

import java.util.Locale;

import net.minecraft.ChatFormatting;
import net.minecraft.SharedConstants;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;

/** A countdown in an advanced tooltip (F3+H), or Jade's: the ticks left, as m:ss. */
public final class TimeLeft {

    private TimeLeft() {
    }

    /** The line, from a translation that takes the m:ss. */
    static Component line(String key, int ticks) {
        return Component.translatable(key, clock(ticks)).withStyle(ChatFormatting.DARK_GRAY);
    }

    /** The ticks as m:ss, a part second counted whole. */
    public static String clock(int ticks) {
        int seconds = Mth.positiveCeilDiv(ticks, SharedConstants.TICKS_PER_SECOND);
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
