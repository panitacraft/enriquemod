package com.panita.enriquecraft.core.framework.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConfigManagerTest {

    public static final class SampleConfig implements ModConfig {
        public final ConfigValue<String> prefix;
        public final ConfigValue<Integer> limit;
        public final ConfigValue<Boolean> enabled;

        public SampleConfig(ConfigSectionBuilder builder) {
            prefix = builder.string("messages.prefix", "<red>[X]</red>", "Shown before messages.\nSupports tags.",
                    text -> !text.isBlank(), "must not be blank");
            limit = builder.intRange("limits.max", 10, 1, 100, "Maximum.");
            enabled = builder.bool("enabled", true, "Turns the feature on.");
        }
    }

    public static final class OtherConfig implements ModConfig {
        public final ConfigValue<Boolean> active;

        public OtherConfig(ConfigSectionBuilder builder) {
            active = builder.bool("active", false, "Other flag.");
        }
    }

    public static final class WrongConstructorConfig implements ModConfig {
        public WrongConstructorConfig() {
        }
    }

    @TempDir
    Path directory;

    private Path file;

    @BeforeEach
    void locateFile() {
        file = directory.resolve(ConfigManager.FILE_NAME);
    }

    private void write(String text) throws IOException {
        Files.writeString(file, text, StandardCharsets.UTF_8);
    }

    private String read() throws IOException {
        return Files.readString(file, StandardCharsets.UTF_8);
    }

    @Test
    void missingFileIsCreatedWithDefaultsAndComments() throws IOException {
        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals("<red>[X]</red>", config.prefix.get());
        assertEquals(10, config.limit.get());
        assertTrue(config.enabled.get());
        String text = read();
        assertTrue(text.contains("// Shown before messages."));
        assertTrue(text.contains("// Supports tags."));
        assertTrue(text.contains("// Allowed: must not be blank"));
        assertTrue(text.contains("// Allowed: a whole number from 1 to 100"));
        assertTrue(text.contains("// Default: \"<red>[X]</red>\""));
        assertTrue(text.contains("\"prefix\": \"<red>[X]</red>\""));
        assertTrue(text.contains("\"max\": 10"));
    }

    @Test
    void createdFileReadsBackWithoutBeingRewritten() throws IOException {
        new ConfigManager(directory).bind("core", SampleConfig.class);
        String first = read();

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals("<red>[X]</red>", config.prefix.get());
        assertEquals(first, read());
    }

    @Test
    void validValuesAreReadAndTheFileIsLeftAlone() throws IOException {
        String text = """
                {
                  "core": {
                    "messages": { "prefix": "Hello" },
                    "limits": { "max": 25 },
                    "enabled": false
                  }
                }
                """;
        write(text);

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals("Hello", config.prefix.get());
        assertEquals(25, config.limit.get());
        assertFalse(config.enabled.get());
        assertEquals(text, read());
    }

    @Test
    void commentsWrittenByAnAdministratorAreAccepted() throws IOException {
        write("""
                // my notes
                {
                  "core": {
                    /* block comment */
                    "messages": { "prefix": "Hello" },
                    "limits": { "max": 25 },
                    "enabled": false // inline
                  }
                }
                """);

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals("Hello", config.prefix.get());
        assertEquals(25, config.limit.get());
        assertFalse(config.enabled.get());
    }

    @Test
    void trailingCommaInAnObjectIsASyntaxErrorAndLeavesTheFileAlone() throws IOException {
        String text = "{ \"core\": { \"enabled\": false, } }";
        write(text);
        ConfigManager manager = new ConfigManager(directory);

        SampleConfig config = manager.bind("core", SampleConfig.class);

        assertTrue(config.enabled.get());
        assertEquals(ConfigIssue.Kind.SYNTAX_ERROR, manager.reload().issues().get(0).kind());
        assertEquals(text, read());
    }

    @Test
    void invalidValuesFallBackToDefaultsAndAreReported() throws IOException {
        write("""
                {
                  "core": {
                    "messages": { "prefix": "   " },
                    "limits": { "max": 500 },
                    "enabled": "yes"
                  }
                }
                """);
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);

        ConfigReport report = manager.reload();

        assertEquals("<red>[X]</red>", config.prefix.get());
        assertEquals(10, config.limit.get());
        assertTrue(config.enabled.get());
        assertEquals(List.of(
                new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, "core.messages.prefix", "must not be blank"),
                new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, "core.limits.max", "must be between 1 and 100"),
                new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, "core.enabled", "must be true or false")),
                report.issues());
    }

    @Test
    void missingKeyIsAddedWhileAnInvalidValueIsKeptAsWritten() throws IOException {
        write("""
                {
                  "core": {
                    "messages": { "prefix": "Hello" },
                    "limits": { "max": 500 }
                  }
                }
                """);

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals(10, config.limit.get());
        String text = read();
        assertTrue(text.contains("\"max\": 500"), "the invalid value must stay so the administrator can fix it");
        assertTrue(text.contains("\"enabled\": true"), "the missing key must be added");
        assertTrue(text.contains("// Turns the feature on."));
        assertTrue(text.contains("\"prefix\": \"Hello\""));
    }

    @Test
    void unknownKeyIsReportedAndKeptAfterTheDeclaredValues() throws IOException {
        write("""
                {
                  "core": {
                    "messages": { "prefix": "Hello", "prefx": "typo" },
                    "limits": { "max": 25 }
                  }
                }
                """);
        ConfigManager manager = new ConfigManager(directory);
        manager.bind("core", SampleConfig.class);

        ConfigReport report = manager.reload();

        assertEquals(List.of(new ConfigIssue(ConfigIssue.Kind.UNKNOWN_KEY, "core.messages.prefx", "no module declares this key")),
                report.issues());
        String text = read();
        assertTrue(text.contains("\"prefx\": \"typo\""));
        assertTrue(text.indexOf("\"prefix\"") < text.indexOf("\"prefx\""));
    }

    @Test
    void sectionThatIsNotAnObjectAtStartupIsRebuiltWithDefaults() throws IOException {
        write("{ \"core\": 5 }");

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals(10, config.limit.get());
        assertTrue(read().contains("\"max\": 10"));
    }

    @Test
    void sectionThatBecomesANonObjectIsReportedOnReload() throws IOException {
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);
        write("{ \"core\": 5 }");

        ConfigReport report = manager.reload();

        assertEquals(List.of(new ConfigIssue(ConfigIssue.Kind.INVALID_VALUE, "core", "must be an object")),
                report.issues());
        assertEquals(10, config.limit.get());
    }

    @Test
    void brokenSyntaxAtStartupUsesDefaultsAndNeverTouchesTheFile() throws IOException {
        String broken = "{ \"core\": { \"messages\": ";
        write(broken);

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals("<red>[X]</red>", config.prefix.get());
        assertEquals(broken, read());
    }

    @Test
    void rootThatIsNotAnObjectCountsAsBrokenSyntax() throws IOException {
        write("[1, 2, 3]");
        ConfigManager manager = new ConfigManager(directory);
        manager.bind("core", SampleConfig.class);

        ConfigReport report = manager.reload();

        assertEquals(ConfigIssue.Kind.SYNTAX_ERROR, report.issues().get(0).kind());
        assertEquals("[1, 2, 3]", read());
    }

    @Test
    void blankFileIsTreatedAsEmpty() throws IOException {
        write("  \n ");

        SampleConfig config = new ConfigManager(directory).bind("core", SampleConfig.class);

        assertEquals(10, config.limit.get());
        assertTrue(read().contains("\"max\": 10"));
    }

    @Test
    void reloadPicksUpChanges() throws IOException {
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);

        write("""
                { "core": { "messages": { "prefix": "Changed" }, "limits": { "max": 40 }, "enabled": false } }
                """);
        ConfigReport report = manager.reload();

        assertTrue(report.isClean());
        assertFalse(report.hasSyntaxError());
        assertEquals("Changed", config.prefix.get());
        assertEquals(40, config.limit.get());
        assertFalse(config.enabled.get());
    }

    @Test
    void removedKeyGoesBackToItsDefaultOnReload() throws IOException {
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);
        write("""
                { "core": { "messages": { "prefix": "Changed" }, "limits": { "max": 40 }, "enabled": false } }
                """);
        manager.reload();

        write("""
                { "core": { "messages": { "prefix": "Changed" }, "limits": { "max": 40 } } }
                """);
        manager.reload();

        assertTrue(config.enabled.get());
        assertTrue(read().contains("\"enabled\": true"));
    }

    @Test
    void reloadWithBrokenSyntaxKeepsThePreviousValuesAndTheFile() throws IOException {
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);
        write("""
                { "core": { "messages": { "prefix": "Changed" }, "limits": { "max": 40 }, "enabled": true } }
                """);
        manager.reload();
        String broken = "{ \"core\": { oops";
        write(broken);

        ConfigReport report = manager.reload();

        assertEquals(1, report.issues().size());
        assertEquals(ConfigIssue.Kind.SYNTAX_ERROR, report.issues().get(0).kind());
        assertTrue(report.hasSyntaxError());
        assertEquals("Changed", config.prefix.get());
        assertEquals(40, config.limit.get());
        assertEquals(broken, read());
    }

    @Test
    void fixingABrokenFileAndReloadingRecovers() throws IOException {
        write("{ broken");
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig config = manager.bind("core", SampleConfig.class);

        write("""
                { "core": { "messages": { "prefix": "Fixed" }, "limits": { "max": 40 }, "enabled": true } }
                """);
        ConfigReport report = manager.reload();

        assertTrue(report.isClean());
        assertEquals("Fixed", config.prefix.get());
    }

    @Test
    void sectionsOfModulesThatAreNotLoadedSurviveARewrite() throws IOException {
        write("""
                {
                  "future": { "a": 1, "b": [1, 2] },
                  "core": { "messages": { "prefix": "Hello" } }
                }
                """);

        new ConfigManager(directory).bind("core", SampleConfig.class);

        String text = read();
        assertTrue(text.contains("\"future\""));
        assertTrue(text.contains("\"a\": 1"));
        assertTrue(text.contains("\"b\": ["));
        assertTrue(text.contains("\"enabled\": true"));
    }

    @Test
    void modulesShareTheFileWithoutLosingEachOthersValues() throws IOException {
        write("""
                { "core": { "messages": { "prefix": "Hello" }, "limits": { "max": 25 }, "enabled": false } }
                """);
        ConfigManager manager = new ConfigManager(directory);
        SampleConfig core = manager.bind("core", SampleConfig.class);

        OtherConfig other = manager.bind("other", OtherConfig.class);

        assertFalse(other.active.get());
        assertEquals("Hello", core.prefix.get());
        String text = read();
        assertTrue(text.contains("\"core\""));
        assertTrue(text.contains("\"other\""));
        assertTrue(text.contains("\"prefix\": \"Hello\""));
        assertTrue(text.contains("\"active\": false"));
        assertEquals("Hello", new ConfigManager(directory).bind("core", SampleConfig.class).prefix.get());
    }

    @Test
    void bindingTheSameSectionTwiceFails() {
        ConfigManager manager = new ConfigManager(directory);
        manager.bind("core", SampleConfig.class);

        assertThrows(IllegalStateException.class, () -> manager.bind("core", SampleConfig.class));
    }

    @Test
    void configClassMustTakeASectionBuilder() {
        ConfigManager manager = new ConfigManager(directory);

        assertThrows(IllegalStateException.class, () -> manager.bind("core", WrongConstructorConfig.class));
    }

    @Test
    void noTemporaryFileIsLeftBehind() throws IOException {
        new ConfigManager(directory).bind("core", SampleConfig.class);

        try (var files = Files.list(directory)) {
            assertEquals(List.of(ConfigManager.FILE_NAME), files.map(path -> path.getFileName().toString()).toList());
        }
    }
}
