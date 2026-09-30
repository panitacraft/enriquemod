package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.config.CoreConfig;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormatterTest {

    private static final TextColor RED = TextColor.fromLegacyFormat(ChatFormatting.RED);

    @TempDir
    Path directory;

    private MessageFormatter formatter;

    @BeforeEach
    void createFormatter() {
        formatter = new MessageFormatter(new ConfigManager(directory).bind("core", CoreConfig.class));
    }

    private static List<Component> parts(Component component) {
        return component.toFlatList(Style.EMPTY);
    }

    @Test
    void plainTextIsKept() {
        assertEquals("Hola mundo", formatter.format(Message.plain("Hola mundo")).getString());
    }

    @Test
    void textTagsAreParsed() {
        Component component = formatter.format(Message.plain("<red>Hola</red>"));

        assertEquals("Hola", component.getString());
        assertEquals(RED, parts(component).get(0).getStyle().getColor());
    }

    @Test
    void legacyColorCodesAreParsed() {
        Component component = formatter.format(Message.plain("&cHola"));

        assertEquals("Hola", component.getString());
        assertEquals(RED, parts(component).get(0).getStyle().getColor());
    }

    @Test
    void argumentsAreInsertedAsTheyAreAndNeverParsed() {
        Component component = formatter.format(Message.plain("Hola {name}").with("name", "<red>&cx</red>"));

        assertEquals("Hola <red>&cx</red>", component.getString());
        assertTrue(parts(component).stream().noneMatch(part -> RED.equals(part.getStyle().getColor())));
    }

    @Test
    void prefixIsAddedOnlyWhenRequested() {
        assertEquals("[Enriquecraft] Hola", formatter.format(Message.plain("Hola").prefixed()).getString());
        assertFalse(formatter.format(Message.plain("Hola")).getString().contains("[Enriquecraft]"));
    }

    @Test
    void prefixComesFromTheConfig() throws IOException {
        Files.writeString(directory.resolve("enriquecraft.json5"),
                "{ \"core\": { \"messages\": { \"prefix\": \"[Mine]\" } } }");
        MessageFormatter custom = new MessageFormatter(new ConfigManager(directory).bind("core", CoreConfig.class));

        assertEquals("[Mine] Hola", custom.format(Message.plain("Hola").prefixed()).getString());
    }

    @Test
    void levelAddsItsIcon() {
        assertEquals("✖ Fallo", formatter.format(Message.error("Fallo")).getString());
        assertEquals("✔ Listo", formatter.format(Message.success("Listo")).getString());
    }

    @Test
    void emptyTextProducesAnEmptyComponent() {
        assertEquals("", formatter.format(Message.plain("")).getString());
    }
}
