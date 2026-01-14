package com.craftingtree.recipedumper.model;

import java.util.ArrayList;
import java.util.List;

public class NormalizedProcessing {
    public Integer time_ticks;
    public Integer energy;
    public String heat;
    public List<String> notes;

    public NormalizedProcessing() {
        this.time_ticks = null;
        this.energy = null;
        this.heat = null;
        this.notes = new ArrayList<>();
    }
}
