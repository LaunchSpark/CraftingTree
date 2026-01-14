package com.craftingtree.recipedumper.normalize;

import com.craftingtree.recipedumper.model.NormalizedRecipe;
import com.craftingtree.recipedumper.normalize.adapters.CreateRecipeNormalizer;
import com.craftingtree.recipedumper.normalize.adapters.GenericJsonHeuristicNormalizer;
import com.craftingtree.recipedumper.normalize.adapters.VanillaCookingNormalizer;
import com.craftingtree.recipedumper.normalize.adapters.VanillaCraftingNormalizer;
import com.craftingtree.recipedumper.normalize.adapters.VanillaSmithingNormalizer;
import com.craftingtree.recipedumper.normalize.adapters.VanillaStonecuttingNormalizer;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public final class NormalizerRegistry {
    private final List<INormalizerAdapter> adapters;

    public NormalizerRegistry(List<INormalizerAdapter> adapters) {
        List<INormalizerAdapter> sorted = new ArrayList<>(adapters);
        sorted.sort(Comparator.comparingInt(INormalizerAdapter::getPriority).reversed());
        this.adapters = List.copyOf(sorted);
    }

    public static NormalizerRegistry createDefault() {
        return new NormalizerRegistry(List.of(
                new VanillaCraftingNormalizer(),
                new VanillaCookingNormalizer(),
                new VanillaStonecuttingNormalizer(),
                new VanillaSmithingNormalizer(),
                new CreateRecipeNormalizer(),
                new GenericJsonHeuristicNormalizer()
        ));
    }

    public NormalizedRecipe normalize(RecipeContext ctx) {
        for (INormalizerAdapter adapter : adapters) {
            if (!adapter.matches(ctx)) {
                continue;
            }
            try {
                NormalizedRecipe normalized = adapter.normalize(ctx);
                if (normalized != null) {
                    ctx.applyWarnings(normalized);
                    ensureDefaults(normalized);
                    return normalized;
                }
            } catch (Exception ex) {
                ctx.warn("Adapter " + adapter.getName() + " failed: " + ex.getMessage());
            }
        }
        NormalizedRecipe fallback = ctx.baseRecipe();
        ctx.warn(fallback, "NormalizerRegistry: no adapter produced output.");
        ctx.applyWarnings(fallback);
        ensureDefaults(fallback);
        return fallback;
    }

    private void ensureDefaults(NormalizedRecipe normalized) {
        if (normalized.inputs == null) {
            normalized.inputs = new ArrayList<>();
        }
        if (normalized.outputs == null) {
            normalized.outputs = new ArrayList<>();
        }
        if (normalized.byproducts == null) {
            normalized.byproducts = new ArrayList<>();
        }
        if (normalized.processing == null) {
            normalized.processing = new com.craftingtree.recipedumper.model.NormalizedProcessing();
        }
        if (normalized.meta == null) {
            normalized.meta = new com.craftingtree.recipedumper.model.NormalizedMeta();
        }
    }
}
