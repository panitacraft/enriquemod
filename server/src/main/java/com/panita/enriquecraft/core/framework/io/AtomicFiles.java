package com.panita.enriquecraft.core.framework.io;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Writes files so that a crash can never leave one half written: the text goes to a temporary
 * sibling file first, which then replaces the target in one step.
 */
public final class AtomicFiles {

    private static final String TEMPORARY_SUFFIX = ".tmp";

    private AtomicFiles() {
    }

    /**
     * Writes UTF-8 text to a file, creating missing parent directories.
     *
     * @throws IOException if the file cannot be written; the previous content is then left untouched
     */
    public static void write(Path file, String text) throws IOException {
        Files.createDirectories(file.getParent());
        Path temporary = file.resolveSibling(file.getFileName() + TEMPORARY_SUFFIX);
        Files.writeString(temporary, text, StandardCharsets.UTF_8);
        try {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
