package com.panita.enriquecraft.core.framework.message;

import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.staff.message.StaffMessages;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageCatalogTest {

    /** Two groups of texts, the first of them the variables. */
    @SuppressWarnings("unused")
    static final class Sample {
        public static final class Variables {
            public static final String PREFIX = "[P]";
            public static final String SEPARATOR = "|";
        }

        public static final class Greeting {
            public static final String HELLO_THERE = "Hola {name}, bienvenido a {server}";
            public static final String BYE = "Adiós";
            static final String NOT_PUBLIC = "hidden";
            public static final int NOT_TEXT = 4;
        }
    }

    @SuppressWarnings("unused")
    static final class Clashing {
        public static final class A {
            public static final String ONE = "same";
        }

        public static final class B {
            public static final String TWO = "same";
        }
    }

    @TempDir
    Path directory;

    private MessageCatalog catalogWith(String file) throws IOException {
        if (file != null) {
            Files.writeString(directory.resolve("enriquecraft-messages.json5"), file);
        }
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("sample", Sample.class);
        return catalog;
    }

    private String fileText() throws IOException {
        return Files.readString(directory.resolve("enriquecraft-messages.json5"));
    }

    @Test
    void defaultsAreUsedAndTheFileListsEveryTextWithItsArguments() throws IOException {
        MessageCatalog catalog = catalogWith(null);

        assertEquals("Adiós", catalog.resolve(Sample.Greeting.BYE));
        String file = fileText();
        assertTrue(file.contains("\"helloThere\""));
        assertTrue(file.contains("\"bye\""));
        assertTrue(file.contains("Filled in by the mod: {name}, {server}."));
        assertFalse(file.contains("hidden"));
        assertFalse(file.contains("notText"));
    }

    @Test
    void aTextTheAdministratorChangedReplacesItsDefaultWherever() throws IOException {
        MessageCatalog catalog = catalogWith("{ \"sample\": { \"greeting\": { \"bye\": \"Hasta luego\" } } }");

        assertEquals("Hasta luego", catalog.resolve(Sample.Greeting.BYE));
        assertEquals(Sample.Greeting.HELLO_THERE, catalog.resolve(Sample.Greeting.HELLO_THERE));
    }

    @Test
    void aTextThatIsNotAConstantIsReturnedAsItIs() throws IOException {
        assertEquals("anything", catalogWith(null).resolve("anything"));
    }

    @Test
    void aTextCanBeEmpty() throws IOException {
        MessageCatalog catalog = catalogWith("{ \"sample\": { \"greeting\": { \"bye\": \"\" } } }");

        assertEquals("", catalog.resolve(Sample.Greeting.BYE));
    }

    @Test
    void variablesAreReplacedBetweenDoubleBraces() throws IOException {
        MessageCatalog catalog = catalogWith("{ \"sample\": { \"variables\": { \"prefix\": \"[X]\" } } }");

        assertEquals("[X] and | text", catalog.expand("{{PREFIX}} and {{SEPARATOR}} text"));
        assertEquals("no variables {here}", catalog.expand("no variables {here}"));
    }

    @Test
    void aReloadPicksUpAChangedFile() throws IOException {
        MessageCatalog catalog = catalogWith(null);

        Files.writeString(directory.resolve("enriquecraft-messages.json5"),
                "{ \"sample\": { \"greeting\": { \"bye\": \"Chao\" } } }");
        ConfigReport report = catalog.reload();

        assertTrue(report.isClean());
        assertEquals("Chao", catalog.resolve(Sample.Greeting.BYE));
    }

    @Test
    void aWrongTypeFallsBackToTheDefaultAndIsReportedWithTheFilePrefix() throws IOException {
        MessageCatalog catalog = catalogWith("{ \"sample\": { \"greeting\": { \"bye\": 4 } } }");

        assertEquals("Adiós", catalog.resolve(Sample.Greeting.BYE));
        ConfigReport report = catalog.reload();
        assertEquals("messages:sample.greeting.bye", report.issues().get(0).path());
    }

    @Test
    void constantsWithTheSameTextShareOneSettingThatSaysWhereElseItIsUsed() throws IOException {
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("clash", Clashing.class);

        String file = fileText();
        assertEquals(1, file.split("\"a\"", -1).length - 1);
        assertFalse(file.contains("\"b\""));
        assertTrue(file.contains("Also used for: b.two."));
    }

    @Test
    void theTextsOfEveryModuleCanBeRegisteredTogether() {
        MessageCatalog catalog = new MessageCatalog(directory);

        catalog.register("core", Messages.class);
        catalog.register("staff", StaffMessages.class);
        catalog.register("auth", AuthMessages.class);
    }

    @Test
    void keysAreCamelCased() {
        assertEquals("enterTitle", MessageCatalog.lowerCamel("ENTER_TITLE"));
        assertEquals("pin", MessageCatalog.lowerFirst("Pin"));
        assertEquals("pinFlow", MessageCatalog.lowerFirst("PinFlow"));
    }
}
