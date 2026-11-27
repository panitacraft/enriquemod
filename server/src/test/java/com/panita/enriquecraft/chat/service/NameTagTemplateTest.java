package com.panita.enriquecraft.chat.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class NameTagTemplateTest {

    @Test
    void theFormatIsCutWhereTheNameGoes() {
        NameTagTemplate template = NameTagTemplate.of("%rank%{name}%suffix%");

        assertEquals("%rank%", template.before());
        assertEquals("%suffix%", template.after());
    }

    @Test
    void aFormatWithoutTheNameIsAllBeforeIt() {
        NameTagTemplate template = NameTagTemplate.of("<gold>VIP ");

        assertEquals("<gold>VIP ", template.before());
        assertEquals("", template.after());
    }

    @Test
    void onlyTheFirstNameCounts() {
        NameTagTemplate template = NameTagTemplate.of("a{name}b{name}c");

        assertEquals("a", template.before());
        assertEquals("b{name}c", template.after());
    }
}
