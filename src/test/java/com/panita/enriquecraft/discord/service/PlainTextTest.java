package com.panita.enriquecraft.discord.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PlainTextTest {

    @Test
    void takesTextTagsAndColorCodesOutOfWhatAnAdministratorWrote() {
        assertEquals("Reinicio en 5 minutos", PlainText.withoutMarkup("<red>&lReinicio</red> en <bold>5</bold> minutos"));
        assertEquals("Hola", PlainText.withoutMarkup("§aHola"));
    }

    @Test
    void takesControlCharactersOutOfALineOfChat() {
        assertEquals("hola mundo", PlainText.chat("hola\u0000 mun\u001bdo\n"));
    }

    @Test
    void cutsChatToTheProtocolLimitAndEventsToTheirs() {
        assertEquals(256, PlainText.chat("x".repeat(1000)).length());
        assertEquals(1024, PlainText.event("x".repeat(5000)).length());
        assertEquals("corto", PlainText.chat("corto"));
    }
}
