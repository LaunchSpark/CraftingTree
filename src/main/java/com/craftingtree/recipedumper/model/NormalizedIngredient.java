package com.craftingtree.recipedumper.model;

import java.util.ArrayList;
import java.util.List;

public class NormalizedIngredient {
    public String kind;
    public String id;
    public int count;
    public List<NormalizedIngredient> options;

    public NormalizedIngredient() {
        this.kind = "unknown";
        this.id = "";
        this.count = 1;
        this.options = new ArrayList<>();
    }

    public static NormalizedIngredient item(String id, int count) {
        NormalizedIngredient ingredient = new NormalizedIngredient();
        ingredient.kind = "item";
        ingredient.id = id;
        ingredient.count = count;
        return ingredient;
    }

    public static NormalizedIngredient tag(String id, int count) {
        NormalizedIngredient ingredient = new NormalizedIngredient();
        ingredient.kind = "tag";
        ingredient.id = id;
        ingredient.count = count;
        return ingredient;
    }

    public static NormalizedIngredient anyOf(List<NormalizedIngredient> options, int count) {
        NormalizedIngredient ingredient = new NormalizedIngredient();
        ingredient.kind = "any_of";
        ingredient.options = options;
        ingredient.count = count;
        return ingredient;
    }

    public static NormalizedIngredient unknown(String note) {
        NormalizedIngredient ingredient = new NormalizedIngredient();
        ingredient.kind = "unknown";
        ingredient.id = note == null ? "" : note;
        return ingredient;
    }
}
