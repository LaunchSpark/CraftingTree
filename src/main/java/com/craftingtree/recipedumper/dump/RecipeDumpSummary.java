package com.craftingtree.recipedumper.dump;

import java.nio.file.Path;

public final class RecipeDumpSummary {
    private final Path outputRoot;
    private final int recipeCount;
    private final int tagCount;
    private final int warningCount;
    private final int errorCount;

    public RecipeDumpSummary(Path outputRoot, int recipeCount, int tagCount, int warningCount, int errorCount) {
        this.outputRoot = outputRoot;
        this.recipeCount = recipeCount;
        this.tagCount = tagCount;
        this.warningCount = warningCount;
        this.errorCount = errorCount;
    }

    public boolean hasFailures() {
        return errorCount > 0;
    }

    public String toUserMessage() {
        return "Recipe dump complete. Recipes=" + recipeCount
                + ", Tags=" + tagCount
                + ", Warnings=" + warningCount
                + ", Errors=" + errorCount
                + ", Output=" + outputRoot.toAbsolutePath();
    }
}
