package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;

public final class VanillaSmithingNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 1000;

    @Override
    public String getName() {
        return "vanilla:smithing";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        Recipe<?> recipe = ctx.recipe();
        return recipe instanceof SmithingTransformRecipe || recipe instanceof SmithingTrimRecipe;
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        Recipe<?> recipe = ctx.recipe();
        JsonElement rawJson = ctx.rawJson();
        if (recipe instanceof SmithingTransformRecipe smithingTransform) {
            normalized.processing.notes.add("smithing_transform");
            if (rawJson != null && rawJson.isJsonObject()) {
                JsonObject object = rawJson.getAsJsonObject();
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("template"), ctx, normalized, false));
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("base"), ctx, normalized, false));
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("addition"), ctx, normalized, false));
                NormalizerUtils.parseResult(object, normalized.outputs, normalized, ctx, false);
                return normalized;
            }
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTransform.getTemplate()));
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTransform.getBase()));
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTransform.getAddition()));
            normalized.outputs.add(NormalizerUtils.fromItemStackOutput(smithingTransform.getResultItem(ctx.registryAccess())));
            return normalized;
        }
        if (recipe instanceof SmithingTrimRecipe smithingTrim) {
            normalized.processing.notes.add("smithing_trim");
            if (rawJson != null && rawJson.isJsonObject()) {
                JsonObject object = rawJson.getAsJsonObject();
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("template"), ctx, normalized, false));
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("base"), ctx, normalized, false));
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(object.get("addition"), ctx, normalized, false));
                return normalized;
            }
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTrim.getTemplate()));
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTrim.getBase()));
            normalized.inputs.add(NormalizerUtils.fromIngredient(smithingTrim.getAddition()));
            return normalized;
        }
        ctx.warn(normalized, "VanillaSmithingNormalizer: recipe was not smithing type.");
        return normalized;
    }
}
