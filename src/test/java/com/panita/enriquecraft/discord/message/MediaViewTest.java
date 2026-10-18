package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import com.panita.enriquecraft.discord.service.MediaTokens;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.URI;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
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
    private MediaTokens tokens;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("discord", DiscordMessages.class);
        tokens = new MediaTokens(Instant::now, Duration.ofMinutes(10), 10);
        view = new MediaView(new MessageFormatter(catalog), tokens);
    }

    private static MediaEntry image(String name, boolean spoiler) {
        return new MediaEntry(URL, name, "image/png", 250_880, OptionalInt.of(800), OptionalInt.of(600), spoiler);
    }

    private static String hoverOf(Component component) {
        HoverEvent.ShowText hover = assertInstanceOf(HoverEvent.ShowText.class, component.getStyle().getHoverEvent());
        return hover.value().getString();
    }

    @Test
    void anImageIsTheNameInBracketsThatShowsItOnAMapAndALinkToTheOriginal() {
        Component line = view.link(image("foto.png", false));
        Component label = line.getSiblings().get(0);
        Component original = line.getSiblings().get(1);

        assertEquals("[foto.png] (Link)", line.getString());
        String command = assertInstanceOf(ClickEvent.RunCommand.class, label.getStyle().getClickEvent()).command();
        assertTrue(command.startsWith("/imagen "), command);
        assertEquals("foto.png", tokens.find(command.substring("/imagen ".length())).orElseThrow().name());
        assertTrue(hoverOf(label).contains("Haz clic para verla en un mapa"));
        assertTrue(hoverOf(label).contains("800x600, 245 KB"));
        assertEquals(URI.create(URL), assertInstanceOf(ClickEvent.OpenUrl.class, original.getStyle().getClickEvent()).uri());
        assertTrue(hoverOf(original).contains("original"));
    }

    @Test
    void theCommandNeverCarriesTheAddress() {
        Component label = view.link(image("foto.png", false)).getSiblings().get(0);

        assertFalse(assertInstanceOf(ClickEvent.RunCommand.class, label.getStyle().getClickEvent()).command().contains("discordapp"));
    }

    @Test
    void aSpoilerDoesNotSayItsNameButStillShowsOnAMap() {
        Component line = view.link(image("secreto.png", true));
        Component label = line.getSiblings().get(0);

        assertEquals("[spoiler] (Link)", line.getString());
        assertFalse(hoverOf(label).contains("secreto"));
        assertTrue(hoverOf(label).contains("spoiler"));
        assertInstanceOf(ClickEvent.RunCommand.class, label.getStyle().getClickEvent());
    }

    @Test
    void theNameOfTheFileIsNeverReadAsFormatting() {
        Component line = view.link(image("<red>hola</red>&c.png", false));

        assertEquals("[<red>hola</red>&c.png] (Link)", line.getString());
    }

    @Test
    void eachImageGetsItsOwnToken() {
        String first = ((ClickEvent.RunCommand) view.link(image("a.png", false)).getSiblings().get(0).getStyle().getClickEvent()).command();
        String second = ((ClickEvent.RunCommand) view.link(image("a.png", false)).getSiblings().get(0).getStyle().getClickEvent()).command();

        assertFalse(first.equals(second));
    }
}
