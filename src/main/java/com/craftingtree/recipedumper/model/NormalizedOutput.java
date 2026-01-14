package com.craftingtree.recipedumper.model;

public class NormalizedOutput {
    public String kind;
    public String id;
    public int count;
    public Object nbt;

    public NormalizedOutput() {
        this.kind = "unknown";
        this.id = "";
        this.count = 1;
        this.nbt = null;
    }

    public static NormalizedOutput item(String id, int count) {
        NormalizedOutput output = new NormalizedOutput();
        output.kind = "item";
        output.id = id;
        output.count = count;
        return output;
    }

    public static NormalizedOutput unknown(String note) {
        NormalizedOutput output = new NormalizedOutput();
        output.kind = "unknown";
        output.id = note == null ? "" : note;
        return output;
    }
}
