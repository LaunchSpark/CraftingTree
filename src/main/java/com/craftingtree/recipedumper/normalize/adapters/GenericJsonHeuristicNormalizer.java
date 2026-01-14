package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedIngredient;
import com.craftingtree.recipedumper.model.NormalizedOutput;
import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.world.item.crafting.Ingredient;

import java.util.ArrayList;
import java.util.List;

public final class GenericJsonHeuristicNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 0;

    @Override
    public String getName() {
        return "generic:heuristic";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        return true;
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        JsonElement rawJson = ctx.rawJson();
        boolean parsedInputs = false;
        boolean parsedOutputs = false;
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            List<NormalizedIngredient> inputs = parseInputs(object, ctx, normalized);
            if (!inputs.isEmpty()) {
                normalized.inputs.addAll(inputs);
                parsedInputs = true;
            }
            OutputParseResult outputs = parseOutputs(object, ctx, normalized);
            if (!outputs.outputs.isEmpty() || !outputs.byproducts.isEmpty()) {
                normalized.outputs.addAll(outputs.outputs);
                normalized.byproducts.addAll(outputs.byproducts);
                parsedOutputs = true;
            }
        }
        if (!parsedInputs) {
            for (Ingredient ingredient : ctx.recipe().getIngredients()) {
                normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient));
            }
        }
        if (!parsedOutputs) {
            normalized.outputs.add(NormalizerUtils.fromItemStackOutput(ctx.recipe().getResultItem(ctx.registryAccess())));
        }
        if (normalized.inputs.isEmpty() && normalized.outputs.isEmpty()) {
            ctx.warn(normalized, "HeuristicNormalizer: could not parse inputs/outputs.");
        }
        return normalized;
    }

    private List<NormalizedIngredient> parseInputs(JsonObject object, RecipeContext ctx, NormalizedRecipe normalized) {
        if (object.has("ingredients") && object.get("ingredients").isJsonArray()) {
            return parseIngredientArray(object.get("ingredients"), ctx, normalized);
        }
        if (object.has("ingredient")) {
            return new ArrayList<>(NormalizerUtils.parseIngredientJson(object.get("ingredient"), ctx, normalized, true));
        }
        if (object.has("input")) {
            JsonElement input = object.get("input");
            if (input.isJsonArray()) {
                return parseIngredientArray(input, ctx, normalized);
            }
            return new ArrayList<>(NormalizerUtils.parseIngredientJson(input, ctx, normalized, true));
        }
        if (object.has("inputs") && object.get("inputs").isJsonArray()) {
            return parseIngredientArray(object.get("inputs"), ctx, normalized);
        }
        return List.of();
    }

    private List<NormalizedIngredient> parseIngredientArray(JsonElement element, RecipeContext ctx, NormalizedRecipe normalized) {
        List<NormalizedIngredient> inputs = new ArrayList<>();
        for (JsonElement ingredient : element.getAsJsonArray()) {
            inputs.addAll(NormalizerUtils.parseIngredientJson(ingredient, ctx, normalized, true));
        }
        return inputs;
    }

    private OutputParseResult parseOutputs(JsonObject object, RecipeContext ctx, NormalizedRecipe normalized) {
        OutputParseResult result = new OutputParseResult();
        if (object.has("result")) {
            JsonElement output = object.get("result");
            addOutputsFromElement(output, result, ctx, normalized);
            return result;
        }
        if (object.has("results") && object.get("results").isJsonArray()) {
            for (JsonElement output : object.getAsJsonArray("results")) {
                addOutputsFromElement(output, result, ctx, normalized);
            }
            return result;
        }
        if (object.has("output")) {
            JsonElement output = object.get("output");
            addOutputsFromElement(output, result, ctx, normalized);
            return result;
        }
        if (object.has("outputs") && object.get("outputs").isJsonArray()) {
            for (JsonElement output : object.getAsJsonArray("outputs")) {
                addOutputsFromElement(output, result, ctx, normalized);
            }
        }
        return result;
    }

    private void addOutputsFromElement(JsonElement output,
                                       OutputParseResult outputs,
                                       RecipeContext ctx,
                                       NormalizedRecipe normalized) {
        if (output == null || output.isJsonNull()) {
            return;
        }
        if (output.isJsonPrimitive()) {
            outputs.outputs.add(NormalizedOutput.item(output.getAsString(), 1));
            return;
        }
        if (output.isJsonArray()) {
            for (JsonElement element : output.getAsJsonArray()) {
                addOutputsFromElement(element, outputs, ctx, normalized);
            }
            return;
        }
        if (!output.isJsonObject()) {
            ctx.warn(normalized, "HeuristicNormalizer: unknown output " + output);
            outputs.outputs.add(NormalizedOutput.unknown("__unknown__"));
            return;
        }
        JsonObject obj = output.getAsJsonObject();
        if (obj.has("item")) {
            String item = obj.get("item").getAsString();
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            NormalizedOutput out = NormalizedOutput.item(item, count);
            if (obj.has("chance")) {
                outputs.byproducts.add(out);
                ctx.warn(normalized, "HeuristicNormalizer: output chance=" + obj.get("chance") + " stored as byproduct.");
            } else {
                outputs.outputs.add(out);
            }
            return;
        }
        ctx.warn(normalized, "HeuristicNormalizer: unknown output " + obj);
        outputs.outputs.add(NormalizedOutput.unknown("__unknown__"));
    }

    private static class OutputParseResult {
        private final List<NormalizedOutput> outputs = new ArrayList<>();
        private final List<NormalizedOutput> byproducts = new ArrayList<>();
    }
}
