package com.craftingtree.recipedumper.dump;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.util.JsonUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.mojang.serialization.JsonOps;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeManager;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;

import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class RecipeDumpService {
    public static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor(runnable -> {
        Thread thread = new Thread(runnable, "recipe-dump-worker");
        thread.setDaemon(true);
        return thread;
    });

    public enum DumpMode {
        ALL,
        RECIPES,
        TAGS
    }

    private RecipeDumpService() {
    }

    public static RecipeDumpSummary dumpAll(MinecraftServer server, Path outputRoot, DumpMode mode, boolean prettyPrint) {
        int recipeCount = 0;
        int tagCount = 0;
        List<String> warnings = new ArrayList<>();
        List<String> errors = new ArrayList<>();
        List<RecipeIndexEntry> indexEntries = new ArrayList<>();

        if (mode == DumpMode.ALL || mode == DumpMode.RECIPES) {
            RecipeManager recipeManager = server.getRecipeManager();
            Map<ResourceLocation, Recipe<?>> recipes = collectRecipes(recipeManager);
            recipeCount = recipes.size();

            for (Map.Entry<ResourceLocation, Recipe<?>> entry : recipes.entrySet()) {
                ResourceLocation id = entry.getKey();
                Recipe<?> recipe = entry.getValue();
                JsonElement rawJson = JsonNull.INSTANCE;
                boolean rawWritten = false;

                try {
                    RecipeSerializer<?> serializer = recipe.getSerializer();
                    JsonElement encoded = serializer.codec().encodeStart(JsonOps.INSTANCE, recipe)
                            .getOrThrow(false, errors::add);
                    rawJson = encoded;
                    Path rawPath = outputRoot.resolve("raw/data/")
                            .resolve(id.getNamespace())
                            .resolve("recipes/")
                            .resolve(id.getPath() + ".json");
                    JsonUtil.writeJson(rawPath, rawJson, prettyPrint);
                    rawWritten = true;
                } catch (Exception ex) {
                    errors.add("Failed to encode recipe " + id + ": " + ex.getMessage());
                    rawJson = null;
                }

                NormalizedRecipe normalized = RecipeNormalizer.normalize(id, recipe, rawJson);
                if (!rawWritten) {
                    normalized.meta.warnings.add("Raw JSON not available for this recipe.");
                }
                if (!normalized.meta.warnings.isEmpty()) {
                    for (String warning : normalized.meta.warnings) {
                        warnings.add(id + ": " + warning);
                    }
                }

                Path normalizedPath = outputRoot.resolve("normalized/recipes/")
                        .resolve(id.getNamespace())
                        .resolve(id.getPath() + ".json");
                try {
                    JsonUtil.writeJson(normalizedPath, JsonUtil.createGson(prettyPrint).toJsonTree(normalized), prettyPrint);
                } catch (Exception ex) {
                    errors.add("Failed to write normalized recipe " + id + ": " + ex.getMessage());
                }

                indexEntries.add(new RecipeIndexEntry(id.toString(), normalized.type,
                        normalized.meta.raw_path,
                        "normalized/recipes/" + id.getNamespace() + "/" + id.getPath() + ".json"));
            }
        }

        if (mode == DumpMode.ALL || mode == DumpMode.TAGS) {
            TagDumpResult tagDump = TagDumpService.dumpItemTags(server, outputRoot, prettyPrint);
            tagCount = tagDump.tagCount();
            warnings.addAll(tagDump.warnings());
            errors.addAll(tagDump.errors());
        }

        if (mode == DumpMode.ALL || mode == DumpMode.RECIPES) {
            RecipeIndex index = new RecipeIndex();
            index.recipes = indexEntries.stream()
                    .sorted(Comparator.comparing(entry -> entry.id))
                    .toList();
            try {
                JsonUtil.writeJson(outputRoot.resolve("index.json"), JsonUtil.createGson(prettyPrint).toJsonTree(index), prettyPrint);
            } catch (Exception ex) {
                errors.add("Failed to write index.json: " + ex.getMessage());
            }
        }

        try {
            DocsGenerator.generate(outputRoot, prettyPrint);
        } catch (Exception ex) {
            warnings.add("Failed to generate docs: " + ex.getMessage());
        }

        DumpMeta meta = new DumpMeta();
        meta.generated_at = Instant.now().toString();
        meta.minecraft = server.getServerVersion();
        meta.loader = "forge";
        meta.counts = Map.of("recipes", recipeCount, "tags", tagCount);
        meta.errors = errors;
        meta.warnings = warnings;

        try {
            JsonUtil.writeJson(outputRoot.resolve("meta.json"), JsonUtil.createGson(prettyPrint).toJsonTree(meta), prettyPrint);
        } catch (Exception ex) {
            errors.add("Failed to write meta.json: " + ex.getMessage());
        }

        return new RecipeDumpSummary(outputRoot, recipeCount, tagCount, warnings.size(), errors.size());
    }

    public static ResourceLocation resolveRecipeTypeId(Recipe<?> recipe) {
        RecipeType<?> type = recipe.getType();
        ResourceLocation id = BuiltInRegistries.RECIPE_TYPE.getKey(type);
        return id == null ? new ResourceLocation("minecraft", "unknown") : id;
    }

    public static ResourceLocation resolveSerializerId(Recipe<?> recipe) {
        RecipeSerializer<?> serializer = recipe.getSerializer();
        ResourceLocation id = BuiltInRegistries.RECIPE_SERIALIZER.getKey(serializer);
        return id == null ? new ResourceLocation("minecraft", "unknown") : id;
    }

    private static Map<ResourceLocation, Recipe<?>> collectRecipes(RecipeManager manager) {
        Map<ResourceLocation, Recipe<?>> recipes = new LinkedHashMap<>();
        manager.getRecipes().forEach((type, map) -> recipes.putAll(map));
        return recipes;
    }

    public record TagDumpResult(int tagCount, List<String> warnings, List<String> errors) {
    }

    public static class TagDumpService {
        public static TagDumpResult dumpItemTags(MinecraftServer server, Path outputRoot, boolean prettyPrint) {
            List<String> warnings = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            int count = 0;
            try {
                Registry<Item> itemRegistry = server.registryAccess().registryOrThrow(Registries.ITEM);
                for (HolderSet.Named<Item> named : itemRegistry.getTags().toList()) {
                    TagKey<Item> key = named.key();
                    List<String> values = named.stream()
                            .map(holder -> Optional.ofNullable(itemRegistry.getKey(holder.value()))
                                    .map(ResourceLocation::toString)
                                    .orElse("minecraft:air"))
                            .sorted()
                            .toList();
                    JsonObject tagJson = new JsonObject();
                    tagJson.addProperty("schema_version", 1);
                    tagJson.addProperty("id", key.location().toString());
                    tagJson.add("values", JsonUtil.createGson(prettyPrint).toJsonTree(values));
                    Path tagPath = outputRoot.resolve("tags/items/")
                            .resolve(key.location().getNamespace())
                            .resolve(key.location().getPath() + ".json");
                    JsonUtil.writeJson(tagPath, tagJson, prettyPrint);
                    count++;
                }
            } catch (Exception ex) {
                errors.add("Failed to dump item tags: " + ex.getMessage());
            }
            return new TagDumpResult(count, warnings, errors);
        }
    }

    private static class DumpMeta {
        public String generated_at;
        public String minecraft;
        public String loader;
        public Map<String, Integer> counts;
        public List<String> errors;
        public List<String> warnings;
    }

    private static class RecipeIndex {
        public int schema_version = 1;
        public List<RecipeIndexEntry> recipes = List.of();
    }

    private static class RecipeIndexEntry {
        public String id;
        public String type;
        public String raw_path;
        public String normalized_path;

        public RecipeIndexEntry(String id, String type, String raw_path, String normalized_path) {
            this.id = id;
            this.type = type;
            this.raw_path = raw_path;
            this.normalized_path = normalized_path;
        }
    }
}
