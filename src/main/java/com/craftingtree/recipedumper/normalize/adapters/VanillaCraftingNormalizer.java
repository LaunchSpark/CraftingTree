package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;

public final class VanillaCraftingNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 1000;

    @Override
    public String getName() {
        return "vanilla:crafting";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        Recipe<?> recipe = ctx.recipe();
        return recipe instanceof ShapedRecipe || recipe instanceof ShapelessRecipe;
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        Recipe<?> recipe = ctx.recipe();
        JsonElement rawJson = ctx.rawJson();
        if (recipe instanceof ShapedRecipe shaped) {
            normalizeShaped(shaped, rawJson, normalized, ctx);
            return normalized;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            normalizeShapeless(shapeless, rawJson, normalized, ctx);
            return normalized;
        }
        ctx.warn(normalized, "VanillaCraftingNormalizer: recipe was not crafting type.");
        return normalized;
    }

    private void normalizeShaped(ShapedRecipe recipe,
                                 JsonElement rawJson,
                                 NormalizedRecipe normalized,
                                 RecipeContext ctx) {
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("key") && object.has("pattern")) {
                JsonObject key = object.getAsJsonObject("key");
                JsonArray pattern = object.getAsJsonArray("pattern");
                for (JsonElement rowElement : pattern) {
                    String row = rowElement.getAsString();
                    for (char symbol : row.toCharArray()) {
                        if (symbol == ' ') {
                            continue;
                        }
                        JsonElement ingredientJson = key.get(String.valueOf(symbol));
                        normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(ingredientJson, ctx, normalized, false));
                    }
                }
            }
            NormalizerUtils.parseResult(object, normalized.outputs, normalized, ctx, false);
            return;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
        }
        normalized.outputs.add(NormalizerUtils.fromItemStackOutput(recipe.getResultItem(ctx.registryAccess())));
    }

    private void normalizeShapeless(ShapelessRecipe recipe,
                                    JsonElement rawJson,
                                    NormalizedRecipe normalized,
                                    RecipeContext ctx) {
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredients")) {
                JsonArray ingredients = object.getAsJsonArray("ingredients");
                for (JsonElement ingredient : ingredients) {
                    normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(ingredient, ctx, normalized, false));
                }
            }
            NormalizerUtils.parseResult(object, normalized.outputs, normalized, ctx, false);
            return;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
        }
        normalized.outputs.add(NormalizerUtils.fromItemStackOutput(recipe.getResultItem(ctx.registryAccess())));
    }
}
