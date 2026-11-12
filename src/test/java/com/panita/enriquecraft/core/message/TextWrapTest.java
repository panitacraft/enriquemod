package com.panita.enriquecraft.core.message;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TextWrapTest {

    @Test
    void aShortTextStaysOnOneLine() {
        assertEquals(List.of("hola mundo"), TextWrap.lines("hola mundo", 40));
    }

    @Test
    void aLongTextBreaksAtSpacesAndNoLineIsLongerThanAsked() {
        List<String> lines = TextWrap.lines("Emblema que se entrega a quien ayudo a construir el spawn del servidor", 20);

        assertEquals(List.of("Emblema que se", "entrega a quien", "ayudo a construir el", "spawn del servidor"), lines);
        assertTrue(lines.stream().allMatch(line -> line.length() <= 20));
    }

    @Test
    void theLineBreaksOfTheTextAreKept() {
        assertEquals(List.of("uno", "dos tres", "", "cuatro"), TextWrap.lines("uno\ndos tres\n\ncuatro", 40));
    }

    @Test
    void aWordLongerThanALineIsCut() {
        assertEquals(List.of("abcde", "fghij", "k"), TextWrap.lines("abcdefghijk", 5));
        assertEquals(List.of("a", "abcde", "fg"), TextWrap.lines("a abcdefg", 5));
    }

    @Test
    void extraSpacesAreCollapsed() {
        assertEquals(List.of("a b c"), TextWrap.lines("  a   b  c ", 40));
    }

    @Test
    void aLineHoldsAtLeastACharacter() {
        assertThrows(IllegalArgumentException.class, () -> TextWrap.lines("a", 0));
    }
}
