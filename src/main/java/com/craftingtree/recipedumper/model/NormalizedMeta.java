package com.craftingtree.recipedumper.model;

import java.util.ArrayList;
import java.util.List;

public class NormalizedMeta {
    public String origin;
    public String raw_path;
    public String serializer;
    public List<String> warnings;

    public NormalizedMeta() {
        this.origin = "runtime";
        this.raw_path = "";
        this.serializer = "";
        this.warnings = new ArrayList<>();
    }
}
