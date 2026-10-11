package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.data.ChatProfile;
import com.panita.enriquecraft.chat.service.NicknameService.Status;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import com.panita.enriquecraft.chat.text.EmojiCatalog;
import com.panita.enriquecraft.chat.text.LinkDetector;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NicknameServiceTest {

    private static final UUID ANA = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    private static final UUID BOB = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    @TempDir
    Path directory;

    private final AtomicReference<Instant> now = new AtomicReference<>(Instant.parse("2026-10-11T12:00:00Z"));
    private WorldData worldData;
    private ChatConfig config;
    private ChatTextParser parser;
    private NicknameService nicknames;

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    @BeforeEach
    void createService() throws Exception {
        Path world = Files.createDirectory(directory.resolve("world"));
        config = new ConfigManager(directory).bind("chat", ChatConfig.class);
        parser = new ChatTextParser(new EmojiCatalog(Map::of, Component::literal),
                new LinkDetector(() -> Component.empty()));
        worldData = new WorldData();
        nicknames = new NicknameService(worldData, parser, config, now::get);
        worldData.attach(world, MinecraftTestSupport.ops());
        nicknames.touch(ANA, "Ana");
        nicknames.touch(BOB, "Bob");
    }

    private Status set(UUID id, String name, String nickname) {
        return nicknames.set(id, name, nickname, false, false).status();
    }

    @Test
    void nicknameIsStoredAndShown() {
        assertEquals(Status.CHANGED, set(ANA, "Ana", "Anita"));

        assertEquals("Anita", nicknames.nicknameOf(ANA).orElseThrow().getString());
        assertEquals(Optional.empty(), nicknames.nicknameOf(BOB));
    }

    @Test
    void nicknameIsRememberedAcrossRestarts() {
        set(ANA, "Ana", "Anita");

        NicknameService restarted = new NicknameService(worldData, parser, config, now::get);

        assertEquals("Anita", restarted.nicknameOf(ANA).orElseThrow().getString());
    }

    @Test
    void lengthIsCheckedWithoutTheStyles() {
        assertEquals(Status.TOO_SHORT, set(ANA, "Ana", "ab"));
        assertEquals(Status.TOO_SHORT, set(ANA, "Ana", "   "));
        assertEquals(Status.TOO_LONG, set(ANA, "Ana", "a".repeat(17)));
        assertEquals(Status.CHANGED, nicknames.set(ANA, "Ana", "<red>Ana</red><bold>nita</bold>", true, false).status());
        assertEquals("Ananita", nicknames.nicknameOf(ANA).orElseThrow().getString());
    }

    @Test
    void onlyLettersDigitsAndFewSignsAreAllowed() {
        for (String bad : new String[]{"[Staff]", "Ana<3", "an@", "a  b", "<red>x", "a§b", "Ana!"}) {
            assertEquals(Status.BAD_CHARACTERS, set(ANA, "Ana", bad), bad);
        }
        for (String good : new String[]{"Ana María", "pepe_99", "A.B-C", "Ñandú"}) {
            now.updateAndGet(time -> time.plus(Duration.ofDays(1)));
            assertEquals(Status.CHANGED, set(ANA, "Ana", good), good);
        }
    }

    @Test
    void tagsAreLiteralForSomeoneWhoMayNotUseStyles() {
        assertEquals(Status.BAD_CHARACTERS, set(ANA, "Ana", "<red>Anita</red>"));
    }

    @Test
    void realNamesOfOthersAreTaken() {
        assertEquals(Status.TAKEN, set(ANA, "Ana", "bob"));
        assertEquals(Status.CHANGED, set(BOB, "Bob", "BOB"));
    }

    @Test
    void nicknamesOfOthersAreTakenWhateverTheCaseOrStyle() {
        nicknames.set(BOB, "Bob", "<red>Bobby</red>", true, false);

        assertEquals(Status.TAKEN, set(ANA, "Ana", "bobby"));
        assertEquals(Status.CHANGED, nicknames.set(ANA, "Ana", "bobby", false, true).status());
    }

    @Test
    void cooldownBlocksAChangeUntilItIsOver() {
        set(ANA, "Ana", "Anita");
        now.updateAndGet(time -> time.plusSeconds(100));

        NicknameService.Outcome outcome = nicknames.set(ANA, "Ana", "Anai", false, false);

        assertEquals(Status.COOLDOWN, outcome.status());
        assertEquals(Duration.ofSeconds(200), outcome.remaining());

        now.updateAndGet(time -> time.plusSeconds(200));
        assertEquals(Status.CHANGED, set(ANA, "Ana", "Anai"));
    }

    @Test
    void bypassSkipsTheCooldown() {
        set(ANA, "Ana", "Anita");

        assertEquals(Status.CHANGED, nicknames.set(ANA, "Ana", "Anai", false, true).status());
    }

    @Test
    void clearingTakesTheNicknameAwayWithoutWaiting() {
        set(ANA, "Ana", "Anita");

        assertEquals(Status.CLEARED, nicknames.clear(ANA).status());

        assertEquals(Optional.empty(), nicknames.nicknameOf(ANA));
        assertEquals(Status.NOT_SET, nicknames.clear(ANA).status());
    }

    @Test
    void nicknameCanBeFoundWhateverItsCaseAndStyle() {
        nicknames.set(ANA, "Ana", "&6Anita", true, false);

        assertEquals(ANA, nicknames.findByNickname(" aNITA ").orElseThrow().id());
        assertEquals(Optional.empty(), nicknames.findByNickname("Ana"));
    }

    @Test
    void changingNameIsRemembered() {
        nicknames.touch(ANA, "Anabel");

        assertEquals("Anabel", nicknames.profile(ANA).map(ChatProfile::name).orElseThrow());
        assertTrue(nicknames.profile(UUID.randomUUID()).isEmpty());
    }
}
