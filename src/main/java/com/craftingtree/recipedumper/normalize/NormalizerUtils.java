package com.craftingtree.recipedumper.normalize;

import com.craftingtree.recipedumper.model.NormalizedIngredient;
import com.craftingtree.recipedumper.model.NormalizedOutput;
import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public final class NormalizerUtils {
    private NormalizerUtils() {
    }

    public static List<NormalizedIngredient> parseIngredientJson(JsonElement element,
                                                                  RecipeContext ctx,
                                                                  NormalizedRecipe normalized,
                                                                  boolean warnUnknown) {
        if (element == null || element.isJsonNull()) {
            return Collections.emptyList();
        }
        if (element.isJsonArray()) {
            List<NormalizedIngredient> options = new ArrayList<>();
            for (JsonElement option : element.getAsJsonArray()) {
                options.addAll(parseIngredientJson(option, ctx, normalized, warnUnknown));
            }
            if (options.isEmpty()) {
                return Collections.emptyList();
            }
            return List.of(NormalizedIngredient.anyOf(options, 1));
        }
        if (element.isJsonPrimitive()) {
            String value = element.getAsString();
            return List.of(NormalizedIngredient.item(value, 1));
        }
        if (!element.isJsonObject()) {
            if (warnUnknown) {
                ctx.warn(normalized, "HeuristicNormalizer: unknown ingredient " + element);
                return List.of(NormalizedIngredient.unknown("__unknown__"));
            }
            return List.of(NormalizedIngredient.unknown(element.toString()));
        }
        JsonObject obj = element.getAsJsonObject();
        if (obj.has("item")) {
            return List.of(NormalizedIngredient.item(optString(obj, "item"), optInt(obj, "count", 1)));
        }
        if (obj.has("tag")) {
            String tag = optString(obj, "tag");
            if (tag.startsWith("#")) {
                tag = tag.substring(1);
            }
            return List.of(NormalizedIngredient.tag(tag, optInt(obj, "count", 1)));
        }
        if (obj.has("items")) {
            JsonArray items = obj.getAsJsonArray("items");
            List<NormalizedIngredient> options = new ArrayList<>();
            for (JsonElement item : items) {
                options.add(NormalizedIngredient.item(item.getAsString(), 1));
            }
            return List.of(NormalizedIngredient.anyOf(options, optInt(obj, "count", 1)));
        }
        if (warnUnknown) {
            ctx.warn(normalized, "HeuristicNormalizer: unknown ingredient " + obj);
            return List.of(NormalizedIngredient.unknown("__unknown__"));
        }
        return List.of(NormalizedIngredient.unknown(obj.toString()));
    }

    public static void parseResult(JsonObject object,
                                   List<NormalizedOutput> outputs,
                                   NormalizedRecipe normalized,
                                   RecipeContext ctx,
                                   boolean warnUnknown) {
        if (object == null || outputs == null) {
            return;
        }
        if (object.has("result")) {
            parseResultEntry(object.get("result"), outputs, normalized, ctx, warnUnknown);
        }
    }

    public static void parseResultEntry(JsonElement result,
                                        List<NormalizedOutput> outputs,
                                        NormalizedRecipe normalized,
                                        RecipeContext ctx,
                                        boolean warnUnknown) {
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
        if (warnUnknown) {
            ctx.warn(normalized, "HeuristicNormalizer: unable to parse result " + result);
        }
    }

    public static NormalizedIngredient fromIngredient(Ingredient ingredient) {
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

    public static NormalizedIngredient fromItemStack(ItemStack stack) {
        ItemLike itemLike = stack.getItem();
        ResourceLocation id = Optional.ofNullable(BuiltInRegistries.ITEM.getKey(itemLike.asItem()))
                .orElse(new ResourceLocation("minecraft", "air"));
        return NormalizedIngredient.item(id.toString(), stack.getCount());
    }

    public static NormalizedOutput fromItemStackOutput(ItemStack stack) {
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
