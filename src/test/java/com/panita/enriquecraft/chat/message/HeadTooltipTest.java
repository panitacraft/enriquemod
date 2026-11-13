package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

class HeadTooltipTest {

    @TempDir
    Path directory;

    private HeadTooltip tooltip;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        MessageCatalog catalog = new MessageCatalog(directory);
        catalog.register("chat", ChatMessages.class);
        tooltip = new HeadTooltip(new MessageFormatter(catalog));
    }

    private static String hover(Style style) {
        return assertInstanceOf(HoverEvent.ShowText.class, style.getHoverEvent()).value().getString();
    }

    @Test
    void aPremiumAccountSaysSo() {
        assertEquals("Cuenta premium", hover(tooltip.styleOf("Alex", Optional.of(AuthMode.PREMIUM))));
    }

    @Test
    void anAccountThatIsNotPremiumSaysThatToo() {
        assertEquals("Cuenta no premium", hover(tooltip.styleOf("Alex", Optional.of(AuthMode.CRACKED))));
    }

    @Test
    void anAccountOfUnknownKindSaysNothingOfIt() {
        assertNull(tooltip.styleOf("Alex", Optional.empty()).getHoverEvent());
    }

    @Test
    void theHeadCanBeClickedToWriteAMessageButNeverRunsAnything() {
        ClickEvent.SuggestCommand click = assertInstanceOf(ClickEvent.SuggestCommand.class,
                tooltip.styleOf("Alex", Optional.of(AuthMode.PREMIUM)).getClickEvent());

        assertEquals("/msg Alex ", click.command());
    }
}
