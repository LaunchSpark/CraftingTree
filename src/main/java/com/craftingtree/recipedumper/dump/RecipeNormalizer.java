package com.craftingtree.recipedumper.dump;

import com.craftingtree.recipedumper.model.NormalizedIngredient;
import com.craftingtree.recipedumper.model.NormalizedMeta;
import com.craftingtree.recipedumper.model.NormalizedOutput;
import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.SingleItemRecipe;
import net.minecraft.world.item.crafting.SmithingTransformRecipe;
import net.minecraft.world.item.crafting.SmithingTrimRecipe;
import net.minecraft.world.item.crafting.StonecutterRecipe;
import net.minecraft.world.level.ItemLike;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class RecipeNormalizer {
    private RecipeNormalizer() {
    }

    public static NormalizedRecipe normalize(ResourceLocation id, Recipe<?> recipe, JsonElement rawJson) {
        NormalizedRecipe normalized = new NormalizedRecipe();
        normalized.id = id.toString();
        normalized.type = RecipeDumpService.resolveRecipeTypeId(recipe).toString();
        normalized.group = recipe.getGroup();
        normalized.category = "";
        normalized.meta.serializer = RecipeDumpService.resolveSerializerId(recipe).toString();
        normalized.meta.raw_path = "raw/data/" + id.getNamespace() + "/recipes/" + id.getPath() + ".json";

        if (recipe instanceof ShapedRecipe shaped) {
            normalizeShaped(shaped, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof ShapelessRecipe shapeless) {
            normalizeShapeless(shapeless, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof AbstractCookingRecipe cooking) {
            normalizeCooking(cooking, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof StonecutterRecipe stonecutter) {
            normalizeStonecutting(stonecutter, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof SmithingTransformRecipe smithingTransform) {
            normalizeSmithingTransform(smithingTransform, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof SmithingTrimRecipe smithingTrim) {
            normalizeSmithingTrim(smithingTrim, rawJson, normalized);
            return normalized;
        }
        if (recipe instanceof SingleItemRecipe singleItem) {
            normalizeSingleItem(singleItem, rawJson, normalized);
            return normalized;
        }

        if (normalized.type.startsWith("create:")) {
            normalizeCreate(rawJson, normalized);
            return normalized;
        }

        normalized.meta.warnings.add("Unsupported recipe type: " + normalized.type);
        return normalized;
    }

    private static void normalizeShaped(ShapedRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
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
                        normalized.inputs.addAll(parseIngredientJson(ingredientJson));
                    }
                }
            }
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            normalized.inputs.add(fromIngredient(ingredient));
        }
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeShapeless(ShapelessRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredients")) {
                JsonArray ingredients = object.getAsJsonArray("ingredients");
                for (JsonElement ingredient : ingredients) {
                    normalized.inputs.addAll(parseIngredientJson(ingredient));
                }
            }
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        for (Ingredient ingredient : recipe.getIngredients()) {
            normalized.inputs.add(fromIngredient(ingredient));
        }
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeCooking(AbstractCookingRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        normalized.processing.time_ticks = recipe.getCookingTime();
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredient")) {
                normalized.inputs.addAll(parseIngredientJson(object.get("ingredient")));
            }
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        normalized.inputs.add(fromIngredient(recipe.getIngredients().get(0)));
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeStonecutting(StonecutterRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredient")) {
                normalized.inputs.addAll(parseIngredientJson(object.get("ingredient")));
            }
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        normalized.inputs.add(fromIngredient(recipe.getIngredients().get(0)));
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeSingleItem(SingleItemRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            if (object.has("ingredient")) {
                normalized.inputs.addAll(parseIngredientJson(object.get("ingredient")));
            }
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        normalized.inputs.add(fromIngredient(recipe.getIngredients().get(0)));
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeSmithingTransform(SmithingTransformRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        normalized.processing.notes.add("smithing_transform");
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            normalized.inputs.addAll(parseIngredientJson(object.get("template")));
            normalized.inputs.addAll(parseIngredientJson(object.get("base")));
            normalized.inputs.addAll(parseIngredientJson(object.get("addition")));
            parseResult(object, normalized.outputs, normalized.meta);
            return;
        }
        normalized.inputs.add(fromIngredient(recipe.getTemplate()));
        normalized.inputs.add(fromIngredient(recipe.getBase()));
        normalized.inputs.add(fromIngredient(recipe.getAddition()));
        normalized.outputs.add(fromItemStack(recipe.getResultItem(null)));
    }

    private static void normalizeSmithingTrim(SmithingTrimRecipe recipe, JsonElement rawJson, NormalizedRecipe normalized) {
        normalized.processing.notes.add("smithing_trim");
        if (rawJson != null && rawJson.isJsonObject()) {
            JsonObject object = rawJson.getAsJsonObject();
            normalized.inputs.addAll(parseIngredientJson(object.get("template")));
            normalized.inputs.addAll(parseIngredientJson(object.get("base")));
            normalized.inputs.addAll(parseIngredientJson(object.get("addition")));
            return;
        }
        normalized.inputs.add(fromIngredient(recipe.getTemplate()));
        normalized.inputs.add(fromIngredient(recipe.getBase()));
        normalized.inputs.add(fromIngredient(recipe.getAddition()));
    }

    private static void normalizeCreate(JsonElement rawJson, NormalizedRecipe normalized) {
        if (rawJson == null || !rawJson.isJsonObject()) {
            normalized.meta.warnings.add("Missing raw JSON for Create recipe");
            return;
        }
        JsonObject object = rawJson.getAsJsonObject();
        if (object.has("ingredients")) {
            JsonArray ingredients = object.getAsJsonArray("ingredients");
            for (JsonElement ingredient : ingredients) {
                normalized.inputs.addAll(parseIngredientJson(ingredient));
            }
        }
        if (object.has("results")) {
            JsonArray results = object.getAsJsonArray("results");
            for (JsonElement result : results) {
                parseResultEntry(result, normalized.outputs, normalized.meta);
            }
        } else if (object.has("result")) {
            parseResult(object, normalized.outputs, normalized.meta);
        }
        if (normalized.type.equals("create:sequenced_assembly")) {
            normalized.processing.notes.add("Sequenced assembly contains sub-recipes; only top-level inputs/outputs captured.");
        }
    }

    private static void parseResult(JsonObject object, List<NormalizedOutput> outputs, NormalizedMeta meta) {
        if (object == null) {
            return;
        }
        if (object.has("result")) {
            parseResultEntry(object.get("result"), outputs, meta);
        }
    }

    private static void parseResultEntry(JsonElement result, List<NormalizedOutput> outputs, NormalizedMeta meta) {
        if (result == null || result.isJsonNull()) {
            return;
        }
        if (result.isJsonPrimitive()) {
            outputs.add(NormalizedOutput.item(result.getAsString(), 1));
            return;
        }
        if (result.isJsonObject()) {
            JsonObject resultObj = result.getAsJsonObject();
            String itemId = optString(resultObj, "item");
            int count = optInt(resultObj, "count", 1);
            if (!itemId.isEmpty()) {
                outputs.add(NormalizedOutput.item(itemId, count));
                return;
            }
        }
        meta.warnings.add("Unable to parse result: " + result);
    }

    private static List<NormalizedIngredient> parseIngredientJson(JsonElement element) {
        if (element == null || element.isJsonNull()) {
            return Collections.emptyList();
        }
        if (element.isJsonArray()) {
            List<NormalizedIngredient> options = new ArrayList<>();
            for (JsonElement option : element.getAsJsonArray()) {
                options.addAll(parseIngredientJson(option));
            }
            if (options.isEmpty()) {
                return Collections.emptyList();
            }
            return List.of(NormalizedIngredient.anyOf(options, 1));
        }
        if (!element.isJsonObject()) {
            return List.of(NormalizedIngredient.unknown(element.toString()));
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("item")) {
            return List.of(NormalizedIngredient.item(optString(obj, "item"), optInt(obj, "count", 1)));
        }
        if (obj.has("tag")) {
            return List.of(NormalizedIngredient.tag(optString(obj, "tag"), optInt(obj, "count", 1)));
        }
        if (obj.has("items")) {
            JsonArray items = obj.getAsJsonArray("items");
            List<NormalizedIngredient> options = new ArrayList<>();
            for (JsonElement item : items) {
                options.add(NormalizedIngredient.item(item.getAsString(), 1));
            }
            return List.of(NormalizedIngredient.anyOf(options, optInt(obj, "count", 1)));
        }
        return List.of(NormalizedIngredient.unknown(obj.toString()));
    }

    private static NormalizedIngredient fromIngredient(Ingredient ingredient) {
        ItemStack[] stacks = ingredient.getItems();
        if (stacks.length == 1) {
            return fromItemStack(stacks[0]);
        }
        if (stacks.length > 1) {
            List<NormalizedIngredient> options = new ArrayList<>();
            for (ItemStack stack : stacks) {
                options.add(fromItemStack(stack));
            }
            return NormalizedIngredient.anyOf(options, 1);
        }
        return NormalizedIngredient.unknown("empty");
    }

    private static NormalizedIngredient fromItemStack(ItemStack stack) {
        ItemLike itemLike = stack.getItem();
        ResourceLocation id = Optional.ofNullable(BuiltInRegistries.ITEM.getKey(itemLike.asItem()))
                .orElse(new ResourceLocation("minecraft", "air"));
        return NormalizedIngredient.item(id.toString(), stack.getCount());
    }

    private static NormalizedOutput fromItemStack(ItemStack stack) {
        ItemLike itemLike = stack.getItem();
        ResourceLocation id = Optional.ofNullable(BuiltInRegistries.ITEM.getKey(itemLike.asItem()))
                .orElse(new ResourceLocation("minecraft", "air"));
        return NormalizedOutput.item(id.toString(), stack.getCount());
    }

    private static String optString(JsonObject object, String key) {
        if (object.has(key)) {
            return object.get(key).getAsString();
        }
        return "";
    }

    private static int optInt(JsonObject object, String key, int fallback) {
        if (object.has(key)) {
            return object.get(key).getAsInt();
        }
        return fallback;
    }
}
