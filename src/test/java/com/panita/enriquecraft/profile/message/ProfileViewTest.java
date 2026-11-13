package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.channel.BossBarChannel;
import com.panita.enriquecraft.core.message.channel.TitleChannel;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProfileViewTest {

    @TempDir
    Path directory;

    private ProfileView view;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("profile", ProfileMessages.class);
        view = new ProfileView(new Messenger(new MessageFormatter(catalog), new TitleChannel(), new BossBarChannel()));
    }

    @Test
    void discordIsToldWhoChangedToWhichLevelInPlainWords() {
        assertEquals("Alex cambió su nivel de Pranks a Pranks+: Nivel Tres.",
                view.plainAnnouncement(Component.literal("Alex"), "Pranks+: Nivel Tres"));
    }

    @Test
    void whatIsWrittenInANameOrATitleIsNeverReadAsFormatting() {
        assertEquals("<red>Al &c cambió su nivel de Pranks a <b>x</b>.",
                view.plainAnnouncement(Component.literal("<red>Al &c"), "<b>x</b>"));
    }
}
