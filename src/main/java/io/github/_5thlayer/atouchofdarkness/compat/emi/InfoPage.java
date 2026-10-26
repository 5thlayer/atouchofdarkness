// SPDX-FileCopyrightText: 2026 5thlayer
// SPDX-License-Identifier: LGPL-3.0-only

package io.github._5thlayer.atouchofdarkness.compat.emi;

import java.util.List;

import dev.emi.emi.api.recipe.EmiInfoRecipe;
import dev.emi.emi.api.recipe.EmiRecipe;
import dev.emi.emi.api.recipe.EmiRecipeCategory;
import dev.emi.emi.api.recipe.VanillaEmiRecipeCategories;
import dev.emi.emi.api.stack.EmiIngredient;
import dev.emi.emi.api.stack.EmiStack;
import dev.emi.emi.api.widget.WidgetHolder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;

/**
 * EMI's information page, made only once it is first measured or drawn. EMI's own breaks its text into
 * lines as it is made, on EMI's reload thread, where the font may not bake a glyph it hasn't drawn yet:
 * 26.1 uploads it to the GPU then, which only the render thread may.
 */
final class InfoPage implements EmiRecipe {

    /** EMI's information page's width, which it never measures. */
    private static final int WIDTH = 144;

    private final List<EmiIngredient> stacks;
    private final Component text;
    private final Identifier id;
    private @Nullable EmiInfoRecipe page;

    InfoPage(List<EmiIngredient> stacks, Component text, Identifier id) {
        this.stacks = stacks;
        this.text = text;
        this.id = id;
    }

    private EmiInfoRecipe page() {
        if (page == null) {
            page = new EmiInfoRecipe(stacks, List.of(text), id);
        }
        return page;
    }

    @Override
    public EmiRecipeCategory getCategory() {
        return VanillaEmiRecipeCategories.INFO;
    }

    @Override
    public Identifier getId() {
        return id;
    }

    @Override
    public List<EmiIngredient> getInputs() {
        return stacks;
    }

    @Override
    public List<EmiStack> getOutputs() {
        return stacks.stream().flatMap(stack -> stack.getEmiStacks().stream()).toList();
    }

    @Override
    public boolean supportsRecipeTree() {
        return false;
    }

    @Override
    public int getDisplayWidth() {
        return WIDTH;
    }

    @Override
    public int getDisplayHeight() {
        return page().getDisplayHeight();
    }

    @Override
    public void addWidgets(WidgetHolder widgets) {
        page().addWidgets(widgets);
    }
}
