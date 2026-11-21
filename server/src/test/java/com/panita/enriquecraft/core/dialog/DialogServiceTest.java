package com.panita.enriquecraft.core.dialog;

import net.minecraft.nbt.CompoundTag;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The token, pace and content rules of a reply, without a real player: the service decides on ids. */
class DialogServiceTest {

    private static final long SECOND = 1_000_000_000L;

    private final AtomicLong clock = new AtomicLong(5 * SECOND);
    private final DialogTokens tokens = new DialogTokens(clock::get, Duration.ofMinutes(1).toNanos());
    private final DialogService service = new DialogService(clock::get, tokens);
    private final UUID player = UUID.randomUUID();

    private DialogTokens.Open open(UUID who) {
        return tokens.issue(who, DialogTesting.spec(), (p, button, values) -> {
        });
    }

    private Optional<DialogService.Reply> answer(UUID who, String token) {
        CompoundTag reply = DialogTesting.reply();
        reply.putString("token", token);
        return service.take(who, DialogTesting.payload(reply), "test");
    }

    @Test
    void takesTheReplyOfTheOpenDialogOnce() {
        DialogTokens.Open open = open(player);

        Optional<DialogService.Reply> reply = answer(player, open.token());

        assertTrue(reply.isPresent());
        assertEquals("save", reply.get().button());
        clock.addAndGet(SECOND);
        assertTrue(answer(player, open.token()).isEmpty(), "a reply cannot be repeated");
    }

    @Test
    void ignoresAReplyWhenThePlayerHasNoDialog() {
        assertTrue(answer(player, "anything").isEmpty());
    }

    @Test
    void aTokenOnlyWorksForThePlayerItWasIssuedTo() {
        DialogTokens.Open mine = open(player);
        open(UUID.randomUUID());

        assertTrue(answer(UUID.randomUUID(), mine.token()).isEmpty());
        assertTrue(answer(player, mine.token()).isPresent());
    }

    @Test
    void aReplacedDialogLosesItsToken() {
        DialogTokens.Open first = open(player);
        DialogTokens.Open second = open(player);

        assertNotEquals(first.token(), second.token());
        assertTrue(answer(player, first.token()).isEmpty());
        // The late reply of the old dialog does not end the new one.
        clock.addAndGet(SECOND);
        assertTrue(answer(player, second.token()).isPresent());
    }

    @Test
    void aDialogEndsAfterItsTimeLimit() {
        DialogTokens.Open open = open(player);

        clock.addAndGet(Duration.ofMinutes(1).toNanos());

        assertTrue(answer(player, open.token()).isEmpty());
        assertTrue(tokens.current(player).isEmpty());
    }

    @Test
    void forgettingAPlayerEndsTheirDialog() {
        DialogTokens.Open open = open(player);

        service.forget(player);

        assertTrue(answer(player, open.token()).isEmpty());
    }

    @Test
    void repliesCloseTogetherAreDropped() {
        DialogTokens.Open open = open(player);

        assertTrue(answer(player, "old").isEmpty());
        // Right after another reply: dropped, and the dialog stays.
        assertTrue(answer(player, open.token()).isEmpty());
        assertTrue(tokens.current(player).isPresent());
        clock.addAndGet(SECOND);
        assertTrue(answer(player, open.token()).isPresent());
    }

    @Test
    void anInvalidReplyEndsTheDialog() {
        DialogTokens.Open open = open(player);
        CompoundTag invalid = DialogTesting.reply();
        invalid.putString("token", open.token());
        invalid.putString("extra", "x");

        assertTrue(service.take(player, DialogTesting.payload(invalid), "test").isEmpty());
        assertFalse(tokens.current(player).isPresent());
    }

    @Test
    void aStaleReplyKeepsTheCurrentDialog() {
        DialogTokens.Open open = open(player);

        assertTrue(answer(player, "old").isEmpty());
        assertTrue(tokens.current(player).isPresent());
        clock.addAndGet(SECOND);
        assertTrue(answer(player, open.token()).isPresent());
    }
}
