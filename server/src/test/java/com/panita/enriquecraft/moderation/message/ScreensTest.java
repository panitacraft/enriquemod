package com.panita.enriquecraft.moderation.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScreensTest {

    private static final Instant NOW = Instant.parse("2026-11-14T12:00:00Z");

    @TempDir
    Path directory;

    private BanView bans;
    private KickView kicks;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        Messenger messenger = MinecraftTestSupport.messenger(directory);
        Reasons reasons = new Reasons(messenger);
        bans = new BanView(messenger, reasons);
        kicks = new KickView(messenger, reasons);
    }

    @Test
    void aKickIsThePrefixAndBelowItTheReasonAndNothingElse() {
        assertEquals("Enriquecraft\nGriefing", kicks.screen(Optional.of(Component.literal("Griefing"))).getString());
    }

    @Test
    void aKickWithoutReasonSaysSoBelowThePrefix() {
        assertEquals("Enriquecraft\nSin motivo especificado", kicks.screen(Optional.empty()).getString());
    }

    @Test
    void aPermanentBanSaysSoAndGivesTheReason() {
        assertEquals("Enriquecraft\nEstás baneado permanentemente de este servidor.\nMotivo: Hacks",
                bans.screen(Optional.empty(), Optional.of("Hacks"), NOW).getString());
    }

    @Test
    void aTemporaryBanSaysHowLongItStillLasts() {
        Instant end = NOW.plus(Duration.ofDays(2).plusHours(3));
        assertEquals("Enriquecraft\nEstás baneado de este servidor por 2d3h.\nMotivo: Spam",
                bans.screen(Optional.of(end), Optional.of("Spam"), NOW).getString());
    }

    @Test
    void aBanWithoutReasonSaysSo() {
        assertEquals("Enriquecraft\nEstás baneado permanentemente de este servidor.\nMotivo: Sin motivo especificado",
                bans.screen(Optional.empty(), Optional.empty(), NOW).getString());
    }
}
