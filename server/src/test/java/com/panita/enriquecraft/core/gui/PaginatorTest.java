package com.panita.enriquecraft.core.gui;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaginatorTest {

    private final Paginator paginator = new Paginator(28);

    private static List<Integer> numbers(int count) {
        return IntStream.range(0, count).boxed().toList();
    }

    @Test
    void anEmptyListStillHasOnePage() {
        assertEquals(1, paginator.pageCount(0));
        assertEquals(List.of(), paginator.slice(List.of(), 0));
    }

    @Test
    void pageCountRoundsUp() {
        assertEquals(1, paginator.pageCount(28));
        assertEquals(2, paginator.pageCount(29));
        assertEquals(3, paginator.pageCount(57));
    }

    @Test
    void sliceReturnsTheRequestedPage() {
        List<Integer> entries = numbers(60);

        assertEquals(numbers(28), paginator.slice(entries, 0));
        assertEquals(IntStream.range(28, 56).boxed().toList(), paginator.slice(entries, 1));
        assertEquals(IntStream.range(56, 60).boxed().toList(), paginator.slice(entries, 2));
    }

    @Test
    void sliceClampsAPageBeyondTheEnd() {
        assertEquals(IntStream.range(56, 60).boxed().toList(), paginator.slice(numbers(60), 99));
    }

    @Test
    void clampKeepsThePageInRange() {
        assertEquals(0, paginator.clamp(-3, 60));
        assertEquals(2, paginator.clamp(9, 60));
        assertEquals(1, paginator.clamp(1, 60));
        assertEquals(0, paginator.clamp(5, 0));
    }

    @Test
    void previousAndNextFollowThePosition() {
        assertFalse(paginator.hasPrevious(0));
        assertTrue(paginator.hasPrevious(1));
        assertTrue(paginator.hasNext(0, 60));
        assertTrue(paginator.hasNext(1, 60));
        assertFalse(paginator.hasNext(2, 60));
        assertFalse(paginator.hasNext(0, 10));
    }

    @Test
    void pageSizeMustBePositive() {
        assertThrows(IllegalArgumentException.class, () -> new Paginator(0));
    }
}
