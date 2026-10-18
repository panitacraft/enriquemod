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

    private static boolean anyItalic(Component line) {
        return line.getStyle().isItalic() || line.getSiblings().stream().anyMatch(ReplyViewTest::anyItalic);
    }

    @Test
    void theLineSaysWhoseMessageIsAnsweredAndNothingElse() {
        assertEquals("(responde a Javi)", view.line(new InFrame.Reply("Javi")).getString());
    }

    @Test
    void theLineIsItalicAndTheNameIsPlainText() {
        Component line = view.line(new InFrame.Reply("<red>Javi &c"));

        assertEquals("(responde a <red>Javi &c)", line.getString());
        assertTrue(anyItalic(line));
    }

    @Test
    void thePrefixOfAMemberIsWhatTheAdministratorWroteWithNothingAddedAround() {
        assertEquals("[Discord]", formatter.format(Message.plain("<blue>[Discord]")).getString());
        assertEquals("\uE000", formatter.format(Message.plain("\uE000")).getString());
    }
}
