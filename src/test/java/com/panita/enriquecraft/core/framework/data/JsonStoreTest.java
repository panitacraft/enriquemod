package com.panita.enriquecraft.core.framework.data;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.mojang.serialization.codecs.RecordCodecBuilder;
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

class JsonStoreTest {

    record Note(String text, int number) {
        static final Codec<Note> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                Codec.STRING.fieldOf("text").forGetter(Note::text),
                Codec.INT.fieldOf("number").forGetter(Note::number)
        ).apply(instance, Note::new));
    }

    private static final Codec<List<Note>> NOTES = Note.CODEC.listOf();

    @TempDir
    Path directory;

    private JsonStore<List<Note>> store(String path) {
        JsonStore<List<Note>> store = new JsonStore<>(path, NOTES, List.of());
        store.load(directory, JsonOps.INSTANCE);
        return store;
    }

    @Test
    void missingFileGivesTheEmptyValueAndCreatesNothing() {
        JsonStore<List<Note>> store = store("notes.json");

        assertEquals(List.of(), store.get());
        assertFalse(Files.exists(directory.resolve("notes.json")));
    }

    @Test
    void setWritesTheFileAndAFreshStoreReadsItBack() {
        JsonStore<List<Note>> store = store("notes.json");
        List<Note> notes = List.of(new Note("hola", 1), new Note("adiós <b>", 2));

        store.set(notes);

        assertEquals(notes, store("notes.json").get());
    }

    @Test
    void fileIsReadableJsonWithoutEscapedCharacters() throws IOException {
        JsonStore<List<Note>> store = store("notes.json");

        store.set(List.of(new Note("a<b>c", 1)));

        String text = Files.readString(directory.resolve("notes.json"));
        assertTrue(text.contains("a<b>c"), text);
    }

    @Test
    void setCreatesMissingDirectories() {
        JsonStore<List<Note>> store = store("deep/er/notes.json");

        store.set(List.of(new Note("x", 1)));

        assertTrue(Files.exists(directory.resolve("deep/er/notes.json")));
    }

    @Test
    void leavesNoTemporaryFileBehind() throws IOException {
        JsonStore<List<Note>> store = store("notes.json");

        store.set(List.of(new Note("x", 1)));

        try (var files = Files.list(directory)) {
            assertEquals(List.of("notes.json"), files.map(path -> path.getFileName().toString()).toList());
        }
    }

    @Test
    void brokenJsonIsMovedAsideNotOverwritten() throws IOException {
        Path file = directory.resolve("notes.json");
        Files.writeString(file, "[ { \"text\": ");

        JsonStore<List<Note>> store = store("notes.json");

        assertEquals(List.of(), store.get());
        assertFalse(Files.exists(file));
        assertEquals("[ { \"text\": ", Files.readString(directory.resolve("notes.json.broken")));
    }

    @Test
    void contentTheCodecRejectsIsMovedAsideToo() throws IOException {
        Path file = directory.resolve("notes.json");
        Files.writeString(file, "[ { \"text\": 5, \"number\": \"x\" } ]");

        JsonStore<List<Note>> store = store("notes.json");

        assertEquals(List.of(), store.get());
        assertTrue(Files.exists(directory.resolve("notes.json.broken")));
    }

    @Test
    void afterABrokenFileWasSetAsideANewChangeWritesAFreshFile() throws IOException {
        Files.writeString(directory.resolve("notes.json"), "not json at all");
        JsonStore<List<Note>> store = store("notes.json");

        store.set(List.of(new Note("new", 1)));

        assertEquals(List.of(new Note("new", 1)), store("notes.json").get());
        assertEquals("not json at all", Files.readString(directory.resolve("notes.json.broken")));
    }

    @Test
    void loadingAgainRereadsTheFile() throws IOException {
        JsonStore<List<Note>> store = store("notes.json");
        store.set(List.of(new Note("old", 1)));
        Files.writeString(directory.resolve("notes.json"), "[ { \"text\": \"edited\", \"number\": 9 } ]");

        store.load(directory, JsonOps.INSTANCE);

        assertEquals(List.of(new Note("edited", 9)), store.get());
    }

    @Test
    void settingBeforeLoadingIsAProgrammingError() {
        JsonStore<List<Note>> store = new JsonStore<>("notes.json", NOTES, List.of());

        assertThrows(IllegalStateException.class, () -> store.set(List.of()));
    }
}
