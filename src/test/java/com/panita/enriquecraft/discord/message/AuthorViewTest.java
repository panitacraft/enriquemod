package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.TranslatableContents;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthorViewTest {

    @TempDir
    Path directory;

    @BeforeEach
    void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void theLineIsHeadPrankRankNameEmblemsAndTheReplyEachWithItsOwnSpace() {
        Component line = AuthorView.compose(Optional.of(Component.literal("H ")), Component.literal("P "), Component.literal("[D]"),
                "Alex", Component.literal(" a b c"), Optional.of(Component.literal("(r)")));

        assertEquals("H P [D] Alex a b c (r)", line.getString());
    }

    @Test
    void withoutAHeadOrAReplyTheLineSimplyStartsAndEndsEarlier() {
        Component line = AuthorView.compose(Optional.empty(), Component.literal("P "), Component.literal("[D]"), "Alex",
                Component.empty(), Optional.empty());

        assertEquals("P [D] Alex", line.getString());
    }

    @Test
    void theHeadIsTheSkinForAClientWithoutTheCompanionAndTheBotIconForTheCompanion() {
        Component skin = Component.literal("SKIN ");

        Component head = AuthorView.withBotHead(skin);

        TranslatableContents contents = assertInstanceOf(TranslatableContents.class, head.getContents());
        assertEquals("enriquecraft.discord_head.v1", contents.getKey());
        assertEquals("%1$s", contents.getFallback());
        // A client that does not have the key shows the first argument, the skin
        assertEquals(skin, contents.getArgs()[0]);
        Component icon = (Component) contents.getArgs()[1];
        Style glyphStyle = icon.getSiblings().get(0).getStyle();
        assertEquals(new FontDescription.Resource(AuthorView.HEAD_FONT), glyphStyle.getFont());
        assertEquals("", icon.getSiblings().get(0).getString());
        // The space after the icon is not in the font of the icon, which has no space
        assertNotEquals(glyphStyle.getFont(), icon.getSiblings().get(1).getStyle().getFont());
    }

    @Test
    void theDiscordRankIsBoldAndTheColorOfDiscord() {
        DiscordConfig config = new ConfigManager(directory).bind("discord", DiscordConfig.class);
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("discord", DiscordMessages.class);

        Component rank = new MessageFormatter(catalog).format(Message.plain(config.deliveryPrefix.get()));

        assertEquals("[DISCORD]", rank.getString());
        Style style = rank.toFlatList().stream().filter(part -> part.getString().equals("[DISCORD]")).findFirst().orElseThrow().getStyle();
        assertTrue(style.isBold());
        assertEquals(0x5662F6, style.getColor().getValue());
    }
}
