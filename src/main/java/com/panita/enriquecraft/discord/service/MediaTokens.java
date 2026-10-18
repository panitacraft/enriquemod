package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.MediaEntry;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * The images a player can still ask to see. A label in the chat does not carry the address in a command, only a short token
 * that stands for it here, so what a click can ask for is always something the server itself put in the chat, and it stops
 * working after a while. Not stored anywhere: a restart forgets them.
 */
public final class MediaTokens {

    private record Issued(MediaEntry entry, Instant expires) {
    }

    private static final String DIGITS = "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final int LENGTH = 8;

    private final Supplier<Instant> clock;
    private final Duration lifetime;
    private final int capacity;
    private final SecureRandom random = new SecureRandom();
    private final Map<String, Issued> issued = new LinkedHashMap<>();

    public MediaTokens(Supplier<Instant> clock, Duration lifetime, int capacity) {
        this.clock = clock;
        this.lifetime = lifetime;
        this.capacity = capacity;
    }

    /** A new token for the image; the oldest ones are forgotten when there are too many. */
    public synchronized String issue(MediaEntry entry) {
        Instant now = clock.get();
        issued.values().removeIf(old -> old.expires().isBefore(now));
        while (issued.size() >= capacity) {
            issued.remove(issued.keySet().iterator().next());
        }
        String token;
        do {
            StringBuilder builder = new StringBuilder(LENGTH);
            for (int index = 0; index < LENGTH; index++) {
                builder.append(DIGITS.charAt(random.nextInt(DIGITS.length())));
            }
            token = builder.toString();
        } while (issued.containsKey(token));
        issued.put(token, new Issued(entry, now.plus(lifetime)));
        return token;
    }

    /** The image of a token that exists and has not expired. */
    public synchronized Optional<MediaEntry> find(String token) {
        Issued found = issued.get(token);
        return found == null || found.expires().isBefore(clock.get()) ? Optional.empty() : Optional.of(found.entry());
    }
}
