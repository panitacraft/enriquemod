package com.panita.enriquecraft.core.framework.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SnbtStoreTest {

    record Note(String text, int number, long big, double ratio, byte flag) {
        static final Codec<Note> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Note::text),
                Codec.INT.fieldOf("number").forGetter(Note::number),
                Codec.LONG.fieldOf("big").forGetter(Note::big),
                Codec.DOUBLE.fieldOf("ratio").forGetter(Note::ratio),
                Codec.BYTE.fieldOf("flag").forGetter(Note::flag)
        ).apply(instance, Note::new));
    }

    private static final Codec<List<Note>> NOTES = Note.CODEC.listOf();

    @TempDir
    Path directory;

    private SnbtStore<List<Note>> store(String path) {
        SnbtStore<List<Note>> store = new SnbtStore<>(path, NOTES, List.of());
        store.load(directory, NbtOps.INSTANCE);
        return store;
    }

    private static Note note(String text, int number) {
        return new Note(text, number, 7L, 1.5, (byte) 1);
    }

    @Test
    void missingFileGivesTheEmptyValueAndCreatesNothing() {
        SnbtStore<List<Note>> store = store("notes.snbt");

        assertEquals(List.of(), store.get());
        assertFalse(Files.exists(directory.resolve("notes.snbt")));
    }

    @Test
    void setWritesTheFileAndAFreshStoreReadsItBack() {
        SnbtStore<List<Note>> store = store("notes.snbt");
        List<Note> notes = List.of(note("hola", 1), note("adiós <b> \"q\"", 2));

        store.set(notes);

        assertEquals(notes, store("notes.snbt").get());
    }

    @Test
    void everyNumberTypeSurvivesTheFile() {
        SnbtStore<List<Note>> store = store("notes.snbt");
        Note typed = new Note("t", 5, 5_000_000_000L, 0.1, (byte) -3);

        store.set(List.of(typed));

        assertEquals(List.of(typed), store("notes.snbt").get());
    }

    @Test
    void fileIsReadableTextWithTypedNumbers() throws IOException {
        SnbtStore<List<Note>> store = store("notes.snbt");

        store.set(List.of(new Note("a<b>c", 5, 7L, 1.5, (byte) 1)));

        String text = Files.readString(directory.resolve("notes.snbt"));
        assertTrue(text.contains("a<b>c"), text);
        assertTrue(text.contains("7L"), "a long keeps its L: " + text);
        assertTrue(text.contains("1b"), "a byte keeps its b: " + text);
        assertTrue(text.contains("1.5d"), "a double keeps its d: " + text);
    }

    @Test
    void setCreatesMissingDirectories() {
        SnbtStore<List<Note>> store = store("deep/er/notes.snbt");

        store.set(List.of(note("x", 1)));

        assertTrue(Files.exists(directory.resolve("deep/er/notes.snbt")));
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        SnbtStore<List<Note>> store = store("notes.snbt");

        store.set(List.of(note("x", 1)));

        try (var files = Files.list(directory)) {
            assertEquals(List.of("notes.snbt"), files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void brokenSnbtIsMovedAsideNotOverwritten() throws IOException {
        Path file = directory.resolve("notes.snbt");
        Files.writeString(file, "[ { text: ");

        SnbtStore<List<Note>> store = store("notes.snbt");

        assertEquals(List.of(), store.get());
        assertFalse(Files.exists(file));
        assertEquals("[ { text: ", Files.readString(directory.resolve("notes.snbt.broken")));
    }

    @Test
    void contentTheCodecRejectsIsMovedAsideToo() throws IOException {
        Files.writeString(directory.resolve("notes.snbt"), "[ { text: 5, number: \"x\" } ]");

        SnbtStore<List<Note>> store = store("notes.snbt");

        assertEquals(List.of(), store.get());
        assertTrue(Files.exists(directory.resolve("notes.snbt.broken")));
    }

    @Test
    void afterABrokenFileWasSetAsideANewChangeWritesAFreshFile() throws IOException {
        Files.writeString(directory.resolve("notes.snbt"), "not snbt at all {{");
        SnbtStore<List<Note>> store = store("notes.snbt");

        store.set(List.of(note("new", 1)));

        assertEquals(List.of(note("new", 1)), store("notes.snbt").get());
        assertEquals("not snbt at all {{", Files.readString(directory.resolve("notes.snbt.broken")));
    }

    @Test
    void loadingAgainRereadsTheFile() throws IOException {
        SnbtStore<List<Note>> store = store("notes.snbt");
        store.set(List.of(note("old", 1)));
        Files.writeString(directory.resolve("notes.snbt"),
                "[ { text: \"edited\", number: 9, big: 1L, ratio: 2.0d, flag: 0b } ]");

        store.load(directory, NbtOps.INSTANCE);

        assertEquals(List.of(new Note("edited", 9, 1L, 2.0, (byte) 0)), store.get());
    }

    @Test
    void settingBeforeLoadingIsAProgrammingError() {
        SnbtStore<List<Note>> store = new SnbtStore<>("notes.snbt", NOTES, List.of());

        assertThrows(IllegalStateException.class, () -> store.set(List.of()));
    }
}
