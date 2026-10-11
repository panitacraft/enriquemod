package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.framework.message.MessageCatalog;
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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MessageFormatterTest {

    private static final TextColor RED = TextColor.fromLegacyFormat(ChatFormatting.RED);

    @TempDir
    Path directory;

    private MessageFormatter formatter;

    @BeforeEach
    void createFormatter() {
        formatter = com.panita.enriquecraft.MinecraftTestSupport.formatter(directory);
    }

    private static List<Component> parts(Component component) {
        return component.toFlatList(Style.EMPTY);
    }

    private static MessageFormatter formatterWith(Path directory, String messagesFile) throws IOException {
        Files.writeString(directory.resolve("enriquecraft-messages.json5"), messagesFile);
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("core", Messages.class);
        return new MessageFormatter(catalog);
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
    void theColorTagWithASpaceStillWorks() {
        Component component = formatter.format(Message.plain("<color #F2B134>Hola</color>"));

        assertEquals("Hola", component.getString());
        assertEquals(TextColor.parseColor("#F2B134").getOrThrow(), parts(component).get(0).getStyle().getColor());
    }

    @Test
    void gradientsAndShadowsAreParsed() {
        Component component = formatter.format(Message.plain("<b><gradient:#920000:#BA5B5B:#920000><shadow:#3A0620:1>Enri</shadow></gradient></b>"));

        assertEquals("Enri", component.getString());
        List<Component> parts = parts(component);
        assertNotNull(parts.get(0).getStyle().getColor());
        assertNotNull(parts.get(0).getStyle().getShadowColor());
        assertTrue(parts.get(0).getStyle().isBold());
        assertFalse(parts.get(0).getStyle().getColor().equals(parts.get(2).getStyle().getColor()));
    }

    @Test
    void argumentsAreInsertedAsTheyAreAndNeverParsed() {
        Component component = formatter.format(Message.plain("Hola {name}").with("name", "<red>&cx</red>"));

        assertEquals("Hola <red>&cx</red>", component.getString());
        assertTrue(parts(component).stream().noneMatch(part -> RED.equals(part.getStyle().getColor())));
    }

    @Test
    void prefixAndSeparatorAreAddedOnlyWhenRequested() {
        assertEquals("Enriquecraft » Hola", formatter.format(Message.plain("Hola").prefixed()).getString());
        assertFalse(formatter.format(Message.plain("Hola")).getString().contains("Enriquecraft"));
    }

    @Test
    void prefixAndSeparatorComeFromTheMessagesFile() throws IOException {
        MessageFormatter custom = formatterWith(directory,
                "{ \"core\": { \"variables\": { \"prefix\": \"[Mine]\", \"separator\": \"::\" } } }");

        assertEquals("[Mine] :: Hola", custom.format(Message.plain("Hola").prefixed()).getString());
    }

    @Test
    void thePrefixAndTheSeparatorCanBeWrittenInAnyTextBetweenDoubleBraces() throws IOException {
        MessageFormatter custom = formatterWith(directory,
                "{ \"core\": { \"variables\": { \"prefix\": \"[Mine]\", \"separator\": \"::\" } } }");

        assertEquals("[Mine] y ::", custom.format(Message.plain("{{PREFIX}} y {{SEPARATOR}}")).getString());
    }

    @Test
    void aTextTheAdministratorChangedIsUsedWhereverTheCodeUsesItsDefault() throws IOException {
        MessageFormatter custom = formatterWith(directory,
                "{ \"core\": { \"ping\": { \"result\": \"Latencia: {ping}\" } } }");

        assertEquals("Latencia: 12", custom.format(Message.plain(Messages.Ping.RESULT).with("ping", 12)).getString());
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
