package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.MediaEntry;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.HashSet;
import java.util.OptionalInt;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MediaTokensTest {

    private Instant now = Instant.parse("2026-10-18T10:00:00Z");
    private final MediaTokens tokens = new MediaTokens(() -> now, Duration.ofMinutes(10), 3);

    private static MediaEntry image(String name) {
        return new MediaEntry("https://cdn.discordapp.com/a/" + name, name, "image/png", 1, OptionalInt.empty(), OptionalInt.empty(), false);
    }

    @Test
    void aTokenStandsForTheImageItWasIssuedFor() {
        String token = tokens.issue(image("a.png"));

        assertEquals("a.png", tokens.find(token).orElseThrow().name());
    }

    @Test
    void aTokenIsShortAndLowerCaseAndEachOneIsDifferent() {
        Set<String> seen = new HashSet<>();
        for (int index = 0; index < 50; index++) {
            MediaTokens roomy = new MediaTokens(() -> now, Duration.ofMinutes(10), 100);
            String token = roomy.issue(image("a.png"));
            assertTrue(token.matches("[a-z0-9]{8}"), token);
            seen.add(token);
        }
        assertTrue(seen.size() > 45);

        MediaTokens one = new MediaTokens(() -> now, Duration.ofMinutes(10), 100);
        Set<String> distinct = new HashSet<>();
        for (int index = 0; index < 100; index++) {
            distinct.add(one.issue(image("a.png")));
        }
        assertEquals(100, distinct.size());
    }

    @Test
    void onlyTheShapeOfATokenIsAToken() {
        assertTrue(MediaTokens.isToken("x90ygqp5"));
        for (String text : new String[]{"", "short", "toolong123", "UPPERCAS", "has space", "../../../", "a;b|c&d$", "x90ygqp5 ", "\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9\u00e9", "x90ygqp\n"}) {
            assertFalse(MediaTokens.isToken(text), text);
        }
    }

    @Test
    void aTokenOfNothingFindsNothing() {
        assertTrue(tokens.find("zzzzzzzz").isEmpty());
        assertTrue(tokens.find("").isEmpty());
        assertTrue(tokens.find("../../x").isEmpty());
    }

    @Test
    void aTokenStopsWorkingAfterItsLifetime() {
        String token = tokens.issue(image("a.png"));

        now = now.plus(Duration.ofMinutes(9));
        assertTrue(tokens.find(token).isPresent());
        now = now.plus(Duration.ofMinutes(2));
        assertTrue(tokens.find(token).isEmpty());
    }

    @Test
    void theOldestTokensAreForgottenWhenThereAreTooMany() {
        String first = tokens.issue(image("1.png"));
        String second = tokens.issue(image("2.png"));
        String third = tokens.issue(image("3.png"));
        String fourth = tokens.issue(image("4.png"));

        assertTrue(tokens.find(first).isEmpty());
        assertTrue(tokens.find(second).isPresent());
        assertTrue(tokens.find(third).isPresent());
        assertTrue(tokens.find(fourth).isPresent());
    }
}
