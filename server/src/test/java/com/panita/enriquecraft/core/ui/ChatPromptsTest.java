package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatPromptsTest {

    private static final long SECOND = 1_000_000_000L;

    @TempDir
    Path directory;

    private final AtomicLong clock = new AtomicLong();
    private final UUID player = UUID.randomUUID();
    private final List<String> answers = new ArrayList<>();
    private int cancels;
    private ChatPrompts prompts;

    @BeforeEach
    void createPrompts() {
        prompts = new ChatPrompts(MinecraftTestSupport.messenger(directory), clock::get);
    }

    private void expect() {
        prompts.expect(player, answers::add, () -> cancels++);
    }

    @Test
    void theNextMessageIsTheAnswerAndIsNotBroadcast() {
        expect();

        assertTrue(prompts.answer(player, "  base norte "));

        assertEquals(List.of("base norte"), answers);
    }

    @Test
    void aMessageWithoutAPromptIsOrdinaryChat() {
        assertFalse(prompts.answer(player, "hola"));
        assertTrue(answers.isEmpty());
    }

    @Test
    void aPromptIsAnsweredOnlyOnce() {
        expect();
        prompts.answer(player, "uno");

        assertFalse(prompts.answer(player, "dos"));

        assertEquals(List.of("uno"), answers);
    }

    @Test
    void cancelBacksOutInAnyCase() {
        expect();

        assertTrue(prompts.answer(player, "Cancelar"));

        assertEquals(1, cancels);
        assertTrue(answers.isEmpty());
    }

    @Test
    void aPromptNobodyAnsweredExpiresAndChatGoesBackToNormal() {
        expect();
        clock.addAndGet(61 * SECOND);

        assertFalse(prompts.answer(player, "hola"));

        assertTrue(answers.isEmpty());
        assertEquals(0, cancels);
    }

    @Test
    void aPromptStillWaitsJustBeforeItExpires() {
        expect();
        clock.addAndGet(59 * SECOND);

        assertTrue(prompts.answer(player, "a tiempo"));
    }

    @Test
    void forgettingAPlayerDropsTheirPrompt() {
        expect();

        prompts.forget(player);

        assertFalse(prompts.answer(player, "hola"));
    }

    @Test
    void aPromptOfOnePlayerDoesNotTakeTheMessagesOfAnother() {
        expect();

        assertFalse(prompts.answer(UUID.randomUUID(), "hola"));
        assertTrue(prompts.answer(player, "respuesta"));
    }

    @Test
    void aFailingAnswerDoesNotEscape() {
        prompts.expect(player, text -> {
            throw new IllegalStateException("boom");
        }, () -> { });

        assertTrue(prompts.answer(player, "algo"));
    }
}
