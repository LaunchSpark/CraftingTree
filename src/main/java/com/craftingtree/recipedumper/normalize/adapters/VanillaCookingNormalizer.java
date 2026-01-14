package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;

public final class VanillaCookingNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 1000;

    @Override
    public String getName() {
        return "vanilla:cooking";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        Recipe<?> recipe = ctx.recipe();
        return recipe instanceof AbstractCookingRecipe;
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        AbstractCookingRecipe recipe = (AbstractCookingRecipe) ctx.recipe();
        normalized.processing.time_ticks = recipe.getCookingTime();
        JsonElement rawJson = ctx.rawJson();
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredient")) {
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("ingredient"), ctx, normalized, false));
            }
            NormalizerUtils.parseResult(object, normalized.outputs, normalized, ctx, false);
            return normalized;
        }
        if (!recipe.getIngredients().isEmpty()) {
            Ingredient ingredient = recipe.getIngredients().get(0);
            normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
        }
        normalized.outputs.add(NormalizerUtils.fromItemStackOutput(recipe.getResultItem(ctx.registryAccess())));
        return normalized;
    }
}
