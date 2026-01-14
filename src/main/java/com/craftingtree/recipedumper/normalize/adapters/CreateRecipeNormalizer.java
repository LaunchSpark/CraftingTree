package com.craftingtree.recipedumper.normalize.adapters;

import com.craftingtree.recipedumper.model.NormalizedOutput;
import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.INormalizerAdapter;
import com.craftingtree.recipedumper.normalize.NormalizerUtils;
import com.craftingtree.recipedumper.normalize.RecipeContext;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public final class CreateRecipeNormalizer implements INormalizerAdapter {
    private static final int PRIORITY = 900;

    @Override
    public String getName() {
        return "create:adapter";
    }

    @Override
    public int getPriority() {
        return PRIORITY;
    }

    @Override
    public boolean matches(RecipeContext ctx) {
        return ctx.typeId().startsWith("create:");
    }

    @Override
    public NormalizedRecipe normalize(RecipeContext ctx) {
        NormalizedRecipe normalized = ctx.baseRecipe();
        JsonElement rawJson = ctx.rawJson();
        if (rawJson == null || !rawJson.isJsonObject()) {
            ctx.warn(normalized, "CreateNormalizer: Missing raw JSON; falling back to recipe ingredients.");
            ctx.recipe().getIngredients().forEach(ingredient -> normalized.inputs.add(NormalizerUtils.fromIngredient(ingredient)));
            normalized.outputs.add(NormalizerUtils.fromItemStackOutput(ctx.recipe().getResultItem(ctx.registryAccess())));
            return normalized;
        }
        JsonObject object = rawJson.getAsJsonObject();
        readProcessingNotes(object, normalized);
        JsonElement ingredientsElement = pickInputElement(object);
        if (ingredientsElement != null) {
            if (ingredientsElement.isJsonArray()) {
                for (JsonElement ingredient : ingredientsElement.getAsJsonArray()) {
                    normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(ingredient, ctx, normalized, false));
                }
            } else {
                normalized.inputs.addAll(NormalizerUtils.parseIngredientJson(ingredientsElement, ctx, normalized, false));
            }
        }
        if (object.has("results")) {
            JsonArray results = object.getAsJsonArray("results");
            for (JsonElement result : results) {
                parseCreateResult(result, normalized, ctx);
            }
        } else if (object.has("result")) {
            NormalizerUtils.parseResultEntry(object.get("result"), normalized.outputs, normalized, ctx, false);
        }
        if ("create:sequenced_assembly".equals(ctx.typeId())) {
            normalized.processing.notes.add("sequenced_assembly: steps not flattened (see raw)");
        }
        return normalized;
    }

    private void readProcessingNotes(JsonObject object, NormalizedRecipe normalized) {
        if (object.has("processingTime")) {
            normalized.processing.time_ticks = object.get("processingTime").getAsInt();
        }
        if (object.has("heatRequirement")) {
            normalized.processing.heat = object.get("heatRequirement").getAsString();
        }
        addNoteIfPresent(object, normalized, "fluidIngredients");
        addNoteIfPresent(object, normalized, "fluidResults");
        addNoteIfPresent(object, normalized, "transitionalItem");
        addNoteIfPresent(object, normalized, "sequence");
        addNoteIfPresent(object, normalized, "loops");
        addNoteIfPresent(object, normalized, "keepHeldItem");
    }

    private void addNoteIfPresent(JsonObject object, NormalizedRecipe normalized, String key) {
        if (object.has(key)) {
            normalized.processing.notes.add("create: " + key + " present (see raw)");
        }
    }

    private JsonElement pickInputElement(JsonObject object) {
        if (object.has("ingredients")) {
            return object.get("ingredients");
        }
        if (object.has("ingredient")) {
            return object.get("ingredient");
        }
        if (object.has("input")) {
            return object.get("input");
        }
        if (object.has("inputs")) {
            return object.get("inputs");
        }
        return null;
    }

    private void parseCreateResult(JsonElement result, NormalizedRecipe normalized, RecipeContext ctx) {
        if (result == null || result.isJsonNull()) {
            return;
        }
        if (result.isJsonObject() && result.getAsJsonObject().has("chance")) {
            JsonObject obj = result.getAsJsonObject();
            NormalizedOutput output = parseOutputObject(obj);
            if (output != null) {
                normalized.byproducts.add(output);
                ctx.warn(normalized, "CreateNormalizer: output chance=" + obj.get("chance") + " stored as byproduct.");
            } else {
                ctx.warn(normalized, "CreateNormalizer: unable to parse result " + result);
            }
            return;
        }
        NormalizerUtils.parseResultEntry(result, normalized.outputs, normalized, ctx, false);
    }

    private NormalizedOutput parseOutputObject(JsonObject obj) {
        if (obj == null) {
            return null;
        }
        if (obj.has("item")) {
            String item = obj.get("item").getAsString();
            int count = obj.has("count") ? obj.get("count").getAsInt() : 1;
            return NormalizedOutput.item(item, count);
        }
        return null;
    }
}
