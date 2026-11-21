package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.network.chat.Component;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GameEventsListenerTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void anAdvancementIsReportedByItsTitleWithoutThePlayerOrTheBrackets() {
        Component message = Component.translatable("chat.type.advancement.task", Component.literal("Javithor360"),
                Component.literal("[").append(Component.literal("Diamonds!")).append("]"));

        assertEquals("Diamonds!", GameEventsListener.advancementTitle(message));
    }

    @Test
    void aTitleThatHasNoBracketsIsKept() {
        Component message = Component.translatable("chat.type.advancement.goal", Component.literal("Javithor360"), Component.literal("Stone Age"));

        assertEquals("Stone Age", GameEventsListener.advancementTitle(message));
    }

    @Test
    void aMessageOfAnotherShapeIsSentWhole() {
        assertEquals("Javithor360 did something", GameEventsListener.advancementTitle(Component.literal("Javithor360 did something")));
        Component oneArgument = Component.translatable("chat.type.advancement.task", "Only one");
        assertEquals(oneArgument.getString(), GameEventsListener.advancementTitle(oneArgument));
    }
}
