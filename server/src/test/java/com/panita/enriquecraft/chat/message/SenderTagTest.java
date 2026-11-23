package com.panita.enriquecraft.chat.message;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SenderTagTest {

    @Test
    void takesEverythingBeforeTheMessageUpToTheLastPlaceholder() {
        assertEquals("%enriquecraft:emblems_system%%enriquecraft:rank%{sender}%luckperms:suffix%%enriquecraft:emblems_lore%",
                SenderTag.of(ChatMessages.Format.GLOBAL));
    }

    @Test
    void leavesOutTheSeparatorAndTheStyleOfTheMessage() {
        assertEquals("[%luckperms:prefix%] {sender}", SenderTag.of("[%luckperms:prefix%] {sender}<gray>: <white>{message}"));
    }

    @Test
    void keepsTheStylesInsideTheTag() {
        assertEquals("<red>{sender}</red>%x:y%", SenderTag.of("<red>{sender}</red>%x:y%<dark_gray> » {message}"));
    }

    @Test
    void usesJustTheSenderWhenTheFormatHasNoSenderBeforeTheMessage() {
        assertEquals("{sender}", SenderTag.of("{message} %enriquecraft:rank%"));
        assertEquals("{sender}", SenderTag.of("no placeholders {message}"));
    }

    @Test
    void readsAFormatWithoutAMessageWhole() {
        assertEquals("%a:b%{sender}", SenderTag.of("%a:b%{sender} text"));
    }
}
