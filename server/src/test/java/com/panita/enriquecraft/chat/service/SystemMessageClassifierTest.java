package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.chat.service.SystemMessageClassifier.Kind;
import com.panita.enriquecraft.chat.service.SystemMessageClassifier.Recognized;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SystemMessageClassifierTest {

    private final SystemMessageClassifier classifier = new SystemMessageClassifier();

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    /** A name as the game writes the one of a player: it shows one text and inserts the real name. */
    private static Component name(String shown, String real) {
        return Component.literal(shown).withStyle(style -> style.withInsertion(real));
    }

    @Test
    void joinIsRecognizedWithTheRealNameWhateverIsShown() {
        Recognized recognized = classifier.recognize(Component.translatable("multiplayer.player.joined", name("Pepa", "MrUnused"))).orElseThrow();

        assertEquals(Kind.JOINED, recognized.kind());
        assertEquals("MrUnused", recognized.playerName());
    }

    @Test
    void joinUnderANewNameKeepsTheOldOne() {
        Recognized recognized = classifier.recognize(
                Component.translatable("multiplayer.player.joined.renamed", name("New", "New"), "Old")).orElseThrow();

        assertEquals(Kind.JOINED_RENAMED, recognized.kind());
        assertEquals(Optional.of("Old"), recognized.oldName());
    }

    @Test
    void leaveIsRecognized() {
        assertEquals(Kind.LEFT, classifier.recognize(Component.translatable("multiplayer.player.left", name("A", "A"))).orElseThrow().kind());
    }

    @Test
    void everyKindOfDeathIsRecognized() {
        for (String key : new String[]{"death.attack.mob", "death.fell.accident.generic", "death.attack.player.item", "death.attack.generic"}) {
            Recognized recognized = classifier.recognize(Component.translatable(key, name("A", "Real"))).orElseThrow();

            assertEquals(Kind.DIED, recognized.kind(), key);
            assertEquals("Real", recognized.playerName(), key);
        }
    }

    @Test
    void everyKindOfAdvancementIsRecognized() {
        for (String key : new String[]{"chat.type.advancement.task", "chat.type.advancement.goal", "chat.type.advancement.challenge"}) {
            assertEquals(Kind.ADVANCED, classifier.recognize(Component.translatable(key, name("A", "A"), Component.literal("x"))).orElseThrow().kind(), key);
        }
    }

    @Test
    void theOriginalMessageIsKeptForTheClientToTranslate() {
        Component death = Component.translatable("death.attack.mob", name("A", "A"), Component.literal("Zombie"));

        assertEquals(death, classifier.recognize(death).orElseThrow().message());
    }

    @Test
    void otherMessagesAreNotRecognized() {
        assertTrue(classifier.recognize(Component.literal("multiplayer.player.joined")).isEmpty());
        assertTrue(classifier.recognize(Component.translatable("chat.type.text", name("A", "A"), "hi")).isEmpty());
        assertTrue(classifier.recognize(Component.translatable("commands.message.display.incoming", name("A", "A"))).isEmpty());
        assertTrue(classifier.recognize(Component.translatable("deathly.hallows", name("A", "A"))).isEmpty());
    }

    @Test
    void messageWithoutAPlayerNameIsNotRecognized() {
        assertTrue(classifier.recognize(Component.translatable("multiplayer.player.joined")).isEmpty());
        assertTrue(classifier.recognize(Component.translatable("multiplayer.player.joined", "text")).isEmpty());
        assertTrue(classifier.recognize(Component.translatable("multiplayer.player.joined", Component.literal("no insertion"))).isEmpty());
    }
}
