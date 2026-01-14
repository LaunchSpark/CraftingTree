package com.craftingtree.recipedumper.model;

import java.util.ArrayList;
import java.util.List;

public class NormalizedRecipe {
    public int schema_version = 1;
    public String id;
    public String type;
    public String group;
    public String category;
    public List<NormalizedIngredient> inputs;
    public List<NormalizedOutput> outputs;
    public List<NormalizedOutput> byproducts;
    public NormalizedProcessing processing;
    public List<String> conditions;
    public NormalizedMeta meta;

    public NormalizedRecipe() {
        this.id = "";
        this.type = "";
        this.group = "";
        this.category = "";
        this.inputs = new ArrayList<>();
        this.outputs = new ArrayList<>();
        this.byproducts = new ArrayList<>();
        this.processing = new NormalizedProcessing();
        this.conditions = new ArrayList<>();
        this.meta = new NormalizedMeta();
    }
}
