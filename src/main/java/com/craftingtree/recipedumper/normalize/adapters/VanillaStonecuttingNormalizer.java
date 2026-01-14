package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;

public final class VanillaStonecuttingNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 1000;

    @Override
    public String getName() {
        return "vanilla:stonecutting";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        Recipe<?> recipe = ctx.recipe();
        return recipe instanceof StonecutterRecipe || recipe instanceof SingleItemRecipe;
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        Recipe<?> recipe = ctx.recipe();
        JsonElement rawJson = ctx.rawJson();
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredient")) {
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("ingredient"), ctx, normalized, false));
            }
            NormalizerUtils.parseResult(object, normalized.outputs, normalized, ctx, false);
            return normalized;
        }
        if (recipe instanceof SingleItemRecipe single) {
            Ingredient ingredient = single.getIngredients().isEmpty() ? Ingredient.EMPTY : single.getIngredients().get(0);
            if (!ingredient.isEmpty()) {
                normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
            }
            normalized.outputs.add(NormalizerUtils.fromItemStackOutput(single.getResultItem(ctx.registryAccess())));
            return normalized;
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            Ingredient ingredient = stonecutter.getIngredients().isEmpty() ? Ingredient.EMPTY : stonecutter.getIngredients().get(0);
            if (!ingredient.isEmpty()) {
                normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
            }
            normalized.outputs.add(NormalizerUtils.fromItemStackOutput(stonecutter.getResultItem(ctx.registryAccess())));
        }
        return normalized;
    }
}
