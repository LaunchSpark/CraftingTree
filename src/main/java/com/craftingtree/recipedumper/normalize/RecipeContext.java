package com.craftingtree.recipedumper.normalize;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.google.gson.JsonElement;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class RecipeContext {
    private final ResourceLocation id;
    private final Recipe<?> recipe;
    private final ResourceLocation typeRl;
    private final String typeId;
    private final JsonElement rawJson;
    private final RegistryAccess registryAccess;
    private final ResourceLocation serializerId;
    private final String rawPath;
    private final Object optionalModContext;
    private final List<String> warnings = new ArrayList<>();

    public RecipeContext(ResourceLocation id,
                         Recipe<?> recipe,
                         ResourceLocation typeRl,
                         String typeId,
                         JsonElement rawJson,
                         RegistryAccess registryAccess,
                         ResourceLocation serializerId,
                         String rawPath,
                         Object optionalModContext) {
        this.id = Objects.requireNonNull(id, "id");
        this.recipe = Objects.requireNonNull(recipe, "recipe");
        this.typeRl = typeRl;
        this.typeId = typeId == null ? "" : typeId;
        this.rawJson = rawJson;
        this.registryAccess = registryAccess;
        this.serializerId = serializerId;
        this.rawPath = rawPath;
        this.optionalModContext = optionalModContext;
    }

    public ResourceLocation id() {
        return id;
    }

    public Recipe<?> recipe() {
        return recipe;
    }

    public ResourceLocation typeRl() {
        return typeRl;
    }

    public String typeId() {
        return typeId;
    }

    public JsonElement rawJson() {
        return rawJson;
    }

    public RegistryAccess registryAccess() {
        return registryAccess;
    }

    public ResourceLocation serializerId() {
        return serializerId;
    }

    public String rawPath() {
        return rawPath;
    }

    public Object optionalModContext() {
        return optionalModContext;
    }

    public void warn(String warning) {
        warnings.add(warning);
    }

    public void warn(NormalizedRecipe recipe, String warning) {
        if (recipe != null && recipe.meta != null) {
            recipe.meta.warnings.add(warning);
        } else {
            warnings.add(warning);
        }
    }

    public void applyWarnings(NormalizedRecipe recipe) {
        if (recipe == null || recipe.meta == null) {
            return;
        }
        recipe.meta.warnings.addAll(warnings);
    }

    public NormalizedRecipe baseRecipe() {
        NormalizedRecipe normalized = new NormalizedRecipe();
        normalized.id = id.toString();
        normalized.type = typeId;
        normalized.group = recipe.getGroup();
        normalized.category = "";
        normalized.meta.origin = "runtime";
        if (serializerId != null) {
            normalized.meta.serializer = serializerId.toString();
        }
        normalized.meta.raw_path = rawPath;
        return normalized;
    }
}
