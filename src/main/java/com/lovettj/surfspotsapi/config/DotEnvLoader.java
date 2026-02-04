package com.lovettj.surfspotsapi.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Loads a .env file from the current working directory and sets each KEY=VALUE
 * as a system property so Spring's ${VAR} placeholders resolve (e.g. in application.yml).
 * Skips empty lines and # comments. Does nothing if .env is missing.
 */
public final class DotEnvLoader {

    private static final Pattern ENV_LINE = Pattern.compile("^([A-Za-z_][A-Za-z0-9_]*)=(.*)$");

    public static void load() {
        Path envPath = Paths.get(System.getProperty("user.dir")).resolve(".env");
        if (!Files.isRegularFile(envPath)) {
            return;
        }
        try {
            List<String> lines = Files.readAllLines(envPath);
            for (String line : lines) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue;
                }
                var matcher = ENV_LINE.matcher(trimmed);
                if (matcher.matches()) {
                    String key = matcher.group(1);
                    String value = matcher.group(2).trim();
                    if (value.length() >= 2 && value.startsWith("\"") && value.endsWith("\"")) {
                        value = value.substring(1, value.length() - 1).replace("\\\"", "\"");
                    }
                    System.setProperty(key, value);
                }
            }
        } catch (IOException ignored) {
            // .env is optional; ignore read errors
        }
    }
}
