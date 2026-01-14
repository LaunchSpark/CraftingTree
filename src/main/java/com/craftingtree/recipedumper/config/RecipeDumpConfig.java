package com.craftingtree.recipedumper.config;

import net.minecraftforge.common.ForgeConfigSpec;

public final class RecipeDumpConfig {
    public static final ForgeConfigSpec SPEC;
    public static final ForgeConfigSpec.ConfigValue<String> DUMP_PATH;
    public static final ForgeConfigSpec.BooleanValue PRETTY_PRINT;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();
        builder.push("recipe_dump");
        DUMP_PATH = builder.comment("Folder name under the game directory where dumps are written")
                .define("dump_path", "recipe_dump");
        PRETTY_PRINT = builder.comment("Pretty print JSON output")
                .define("pretty_print", true);
        builder.pop();
        SPEC = builder.build();
    }

    private RecipeDumpConfig() {
    }
}
