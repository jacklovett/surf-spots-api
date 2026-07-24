package com.lovettj.surfspotsapi.db;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Guards against two Flyway scripts sharing the same version number.
 * That failure aborts API startup and is easy to miss when guessing the next Vn.
 */
class FlywayMigrationVersionUniquenessTest {

    private static final Pattern VERSIONED_MIGRATION =
            Pattern.compile("^V(\\d+)__.*\\.sql$");

    @Test
    void eachMigrationVersionShouldBeUnique() throws IOException {
        Path migrationDirectory = Paths.get("src/main/resources/db/migration");
        assertTrue(
                Files.isDirectory(migrationDirectory),
                "Missing migration directory: " + migrationDirectory.toAbsolutePath());

        Map<String, List<String>> filesByVersion = new TreeMap<>();
        try (Stream<Path> paths = Files.list(migrationDirectory)) {
            paths.map(path -> path.getFileName().toString())
                    .filter(fileName -> fileName.endsWith(".sql"))
                    .forEach(
                            fileName -> {
                                Matcher matcher = VERSIONED_MIGRATION.matcher(fileName);
                                assertTrue(
                                        matcher.matches(),
                                        "Unexpected migration filename (expected V{n}__name.sql): "
                                                + fileName);
                                filesByVersion
                                        .computeIfAbsent(matcher.group(1), key -> new ArrayList<>())
                                        .add(fileName);
                            });
        }

        List<String> duplicateVersions =
                filesByVersion.entrySet().stream()
                        .filter(entry -> entry.getValue().size() > 1)
                        .map(entry -> "V" + entry.getKey() + " -> " + entry.getValue())
                        .toList();

        assertTrue(
                duplicateVersions.isEmpty(),
                "Duplicate Flyway migration versions:\n" + String.join("\n", duplicateVersions));
    }
}
