package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.InFrame;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ReplyViewTest {

    @TempDir
    Path directory;

    private MessageFormatter formatter;
    private ReplyView view;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("discord", DiscordMessages.class);
        formatter = new MessageFormatter(catalog);
        view = new ReplyView(formatter);
    }

    @Test
    void theLineNamesTheReplierAndWhatTheyAnswer() {
        Component line = view.line(Component.literal("[Discord] Tez"), new InFrame.Reply("Javi", "nos vemos en la base"));

        assertEquals("[Discord] Tez (respondiendo a) \u00bb Javi: nos vemos en la base", line.getString());
    }

    @Test
    void whatTheAnsweredMessageSaysIsPlainTextAndIsCutShort() {
        Component tags = view.line(Component.literal("Tez"), new InFrame.Reply("<red>Javi", "<bold>hola &c&lmundo"));
        assertEquals("Tez (respondiendo a) \u00bb <red>Javi: <bold>hola &c&lmundo", tags.getString());

        Component longer = view.line(Component.literal("Tez"), new InFrame.Reply("Javi", "x".repeat(150)));
        assertTrue(longer.getString().endsWith("x".repeat(99) + "\u2026"));
    }

    @Test
    void thePrefixOfAMemberIsWhatTheAdministratorWroteWithNothingAddedAround() {
        assertEquals("[Discord]", formatter.format(Message.plain("<blue>[Discord]")).getString());
        assertEquals("\uE000", formatter.format(Message.plain("\uE000")).getString());
    }
}
