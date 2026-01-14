package com.craftingtree.recipedumper.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public final class JsonUtil {
    private JsonUtil() {
    }

    public static Gson createGson(boolean prettyPrint) {
        GsonBuilder builder = new GsonBuilder()
                .disableHtmlEscaping();
        if (prettyPrint) {
            builder.setPrettyPrinting();
        }
        return builder.create();
    }

    public static void writeJson(Path path, JsonElement element, boolean prettyPrint) throws IOException {
        Files.createDirectories(path.getParent());
        Gson gson = createGson(prettyPrint);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(element, writer);
        }
    }

    public static void writeJson(Path path, Object value, boolean prettyPrint) throws IOException {
        Files.createDirectories(path.getParent());
        Gson gson = createGson(prettyPrint);
        try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
            gson.toJson(value, writer);
        }
    }
}
