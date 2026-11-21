package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.profile.client.Birthday;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BirthdayViewTest {

    @Test
    void everyMonthHasItsOwnTemplateThatTakesTheDay() {
        List<String> templates = IntStream.rangeClosed(1, 12).mapToObj(month -> BirthdayView.date(new Birthday(month, 18)).template()).toList();

        assertEquals(12, templates.stream().distinct().count());
        assertTrue(templates.stream().allMatch(template -> template.contains("{day}")));
    }

    @Test
    void theDayIsPassedAsAnArgument() {
        Message august = BirthdayView.date(new Birthday(8, 18));

        assertEquals("{day} de agosto", august.template());
        assertEquals("18", august.arguments().get("day").getString());
    }
}
