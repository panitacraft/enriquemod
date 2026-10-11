package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.data.ChatProfile;
import com.panita.enriquecraft.chat.text.ChatCapability;
import com.panita.enriquecraft.chat.text.ChatTextParser;
import com.panita.enriquecraft.core.framework.data.SnbtStore;
import com.panita.enriquecraft.core.framework.data.WorldData;
import net.minecraft.network.chat.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * The nicknames: a cosmetic name that chat, the tab list and the game's own messages show instead of the real one. It
 * is never used to tell who someone is, so nothing but display reads it. Used from the server thread only.
 * <p>
 * A nickname is made of letters, digits, spaces and {@code _ . -}, so it cannot pass for a title such as {@code [Staff]}.
 * It cannot be a real name of another player or the nickname of one. Colors and styles are for those who may.
 */
public final class NicknameService {

    public enum Status { CHANGED, CLEARED, NOT_SET, TOO_SHORT, TOO_LONG, BAD_CHARACTERS, TAKEN, COOLDOWN }

    /**
     * @param remaining how long until the nickname can change, for {@link Status#COOLDOWN}; zero otherwise
     */
    public record Outcome(Status status, Duration remaining) {

        static Outcome of(Status status) {
            return new Outcome(status, Duration.ZERO);
        }
    }

    private static final Pattern ALLOWED = Pattern.compile("[\\p{L}\\p{N}_.\\-]+( [\\p{L}\\p{N}_.\\-]+)*");
    private static final Set<ChatCapability> STYLES = EnumSet.of(ChatCapability.COLOR, ChatCapability.FORMAT, ChatCapability.GRADIENT);

    /** A nickname read once: it is shown far more often than it changes. */
    private record Shown(String written, Component component, String plain) {
    }

    private final SnbtStore<List<ChatProfile>> store;
    private final ChatTextParser parser;
    private final ChatConfig config;
    private final Supplier<Instant> clock;
    private final Map<UUID, Shown> shown = new HashMap<>();

    public NicknameService(WorldData worldData, ChatTextParser parser, ChatConfig config, Supplier<Instant> clock) {
        this.store = worldData.register("chat_profiles.snbt", ChatProfile.CODEC.listOf(), List.of());
        this.parser = parser;
        this.config = config;
        this.clock = clock;
    }

    /** Remembers the name a player joined with. */
    public void touch(UUID id, String name) {
        Optional<ChatProfile> existing = profile(id);
        if (existing.isEmpty()) {
            save(ChatProfile.create(id, name));
        } else if (!existing.get().name().equals(name)) {
            save(existing.get().withName(name));
        }
    }

    public Optional<ChatProfile> profile(UUID id) {
        return store.get().stream().filter(profile -> profile.id().equals(id)).findFirst();
    }

    /** The nickname of a player, as it is shown. */
    public Optional<Component> nicknameOf(UUID id) {
        return shownOf(id).map(Shown::component);
    }

    /** The player whose nickname is this text, whatever its case and styles. */
    public Optional<ChatProfile> findByNickname(String text) {
        String wanted = text.strip();
        return store.get().stream()
                .filter(profile -> shownOf(profile.id()).map(nickname -> nickname.plain().equalsIgnoreCase(wanted)).orElse(false))
                .findFirst();
    }

    /**
     * Gives a player a nickname.
     *
     * @param written    the nickname as typed
     * @param formatting whether colors and styles in it are read, which only those who may use them are allowed
     * @param bypass     whether the cooldown and the nickname being taken are ignored
     */
    public Outcome set(UUID id, String name, String written, boolean formatting, boolean bypass) {
        touch(id, name);
        String plain = formatting ? parser.parse(written, STYLES).getString().strip() : written.strip();
        int length = plain.codePointCount(0, plain.length());
        if (length < config.nicknameMinLength.get()) {
            return Outcome.of(Status.TOO_SHORT);
        }
        if (length > config.nicknameMaxLength.get()) {
            return Outcome.of(Status.TOO_LONG);
        }
        if (!ALLOWED.matcher(plain).matches()) {
            return Outcome.of(Status.BAD_CHARACTERS);
        }
        ChatProfile profile = profile(id).orElseThrow();
        if (!bypass) {
            Duration remaining = cooldownLeft(profile);
            if (!remaining.isZero()) {
                return new Outcome(Status.COOLDOWN, remaining);
            }
            if (isTaken(plain, id)) {
                return Outcome.of(Status.TAKEN);
            }
        }
        save(profile.withNickname(Optional.of(formatting ? written.strip() : plain), Optional.of(clock.get())));
        return Outcome.of(Status.CHANGED);
    }

    /** Takes a nickname away; there is no wait for that. */
    public Outcome clear(UUID id) {
        Optional<ChatProfile> profile = profile(id).filter(found -> found.nickname().isPresent());
        if (profile.isEmpty()) {
            return Outcome.of(Status.NOT_SET);
        }
        save(profile.get().withNickname(Optional.empty(), profile.get().nicknameChangedAt()));
        return Outcome.of(Status.CLEARED);
    }

    private Duration cooldownLeft(ChatProfile profile) {
        Duration cooldown = Duration.ofSeconds(config.nicknameCooldownSeconds.get());
        return profile.nicknameChangedAt()
                .map(changed -> Duration.between(clock.get(), changed.plus(cooldown)))
                .filter(left -> !left.isNegative() && !left.isZero())
                .orElse(Duration.ZERO);
    }

    private boolean isTaken(String plain, UUID owner) {
        return store.get().stream()
                .filter(profile -> !profile.id().equals(owner))
                .anyMatch(profile -> profile.name().equalsIgnoreCase(plain)
                        || shownOf(profile.id()).map(nickname -> nickname.plain().equalsIgnoreCase(plain)).orElse(false));
    }

    private Optional<Shown> shownOf(UUID id) {
        Optional<String> written = profile(id).flatMap(ChatProfile::nickname);
        if (written.isEmpty()) {
            shown.remove(id);
            return Optional.empty();
        }
        Shown cached = shown.get(id);
        if (cached == null || !cached.written().equals(written.get())) {
            Component component = parser.parse(written.get(), STYLES);
            cached = new Shown(written.get(), component, component.getString().strip());
            shown.put(id, cached);
        }
        return Optional.of(cached);
    }

    private void save(ChatProfile changed) {
        List<ChatProfile> updated = new ArrayList<>(store.get());
        updated.removeIf(profile -> profile.id().equals(changed.id()));
        updated.add(changed);
        store.set(List.copyOf(updated));
    }
}
