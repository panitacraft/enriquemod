package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.util.OptionalInt;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaViewTest {

    private static final String URL = "https://cdn.discordapp.com/attachments/1/2/foto.png?ex=68f1";

    @TempDir
    Path directory;

    private MediaView view;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("discord", DiscordMessages.class);
        view = new MediaView(new MessageFormatter(catalog));
    }

    private static MediaEntry image(String name, boolean spoiler) {
        return new MediaEntry(URL, name, "image/png", 250_880, OptionalInt.of(800), OptionalInt.of(600), spoiler);
    }

    private static String hoverOf(Component component) {
        HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class, component.getStyle().getHoverEvent());
        return hover.value().getString();
    }

    @Test
    void anImageIsALabelWithItsNameThatOpensTheAddressAndTellsWhatItIsWhenHovered() {
        Component link = view.link(image("foto.png", false));

        assertEquals("[Imagen: foto.png]", link.getString());
        assertEquals(URI.create(URL), assertInstanceOf(ClickEvent.OpenUrl.class, link.getStyle().getClickEvent()).uri());
        String hover = hoverOf(link);
        assertTrue(hover.contains("Haz clic para abrir la imagen"));
        assertTrue(hover.contains("800x600, 245 KB"));
    }

    @Test
    void aSpoilerDoesNotSayItsNameButStillOpens() {
        Component link = view.link(image("secreto.png", true));

        assertEquals("[Imagen oculta]", link.getString());
        assertFalse(hoverOf(link).contains("secreto"));
        assertTrue(hoverOf(link).contains("spoiler"));
        assertInstanceOf(ClickEvent.OpenUrl.class, link.getStyle().getClickEvent());
    }

    @Test
    void theNameOfTheFileIsNeverReadAsFormatting() {
        Component link = view.link(image("<red>hola</red>&c.png", false));

        assertEquals("[Imagen: <red>hola</red>&c.png]", link.getString());
    }
}
