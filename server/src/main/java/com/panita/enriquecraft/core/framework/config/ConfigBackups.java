package com.panita.enriquecraft.core.framework.config;

import com.panita.enriquecraft.core.framework.io.AtomicFiles;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

/**
 * Keeps a copy of a config file before the mod rewrites it, which it does to add the settings a new version brings (a rewrite
 * drops the comments the administrator wrote and normalizes the layout, though every value is kept). The copies go in a folder beside
 * the file, named after it and the moment, and only the newest few of each file are kept.
 */
final class ConfigBackups {

    static final String DIRECTORY = "enriquecraft-backups";
    static final int KEPT = 10;

    private static final String EXTENSION = ".json5";
    private static final DateTimeFormatter STAMP = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private final Path directory;
    private final String stem;
    private final Pattern ownFiles;

    /**
     * @param file the config file whose copies these are
     */
    ConfigBackups(Path file) {
        this.directory = file.resolveSibling(DIRECTORY);
        String name = file.getFileName().toString();
        this.stem = name.endsWith(EXTENSION) ? name.substring(0, name.length() - EXTENSION.length()) : name;
        this.ownFiles = Pattern.compile(Pattern.quote(stem) + "-\\d{8}-\\d{6}(-\\d+)?" + Pattern.quote(EXTENSION));
    }

    /**
     * Writes the text as a new copy and removes the oldest copies beyond the limit.
     *
     * @return the file the copy was written to
     */
    Path save(String text, LocalDateTime now) throws IOException {
        String base = stem + "-" + STAMP.format(now);
        Path target = directory.resolve(base + EXTENSION);
        // Two copies in the same second must not replace each other
        for (int number = 2; Files.exists(target); number++) {
            target = directory.resolve(base + "-" + number + EXTENSION);
        }
        AtomicFiles.write(target, text);
        prune();
        return target;
    }

    /** Without the extension, so a copy made in the same second as another ({@code -2}) sorts after it. */
    private static String withoutExtension(Path path) {
        String name = path.getFileName().toString();
        return name.substring(0, name.length() - EXTENSION.length());
    }

    private void prune() throws IOException {
        try (Stream<Path> files = Files.list(directory)) {
            List<Path> copies = files.filter(path -> ownFiles.matcher(path.getFileName().toString()).matches())
                    .sorted(Comparator.comparing(ConfigBackups::withoutExtension).reversed())
                    .toList();
            for (Path old : copies.stream().skip(KEPT).toList()) {
                Files.deleteIfExists(old);
            }
        }
    }
}
