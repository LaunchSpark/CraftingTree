package com.craftingtree.recipedumper.normalize;

import com.craftingtree.recipedumper.model.NormalizedRecipe;

public interface INormalizerAdapter {
    String getName();

    int getPriority();

    boolean matches(RecipeContext ctx);

    NormalizedRecipe normalize(RecipeContext ctx) throws Exception;
}
