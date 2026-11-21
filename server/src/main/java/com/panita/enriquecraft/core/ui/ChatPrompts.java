package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messages;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Lets a player type a value in chat, for players whose client cannot show a text field. The next
 * chat message of the player is taken as the answer instead of being broadcast; {@code cancelar}
 * backs out, and a prompt nobody answered stops waiting after a minute so it cannot swallow chat later.
 */
public final class ChatPrompts {

    private static final Logger LOGGER = LoggerFactory.getLogger(ChatPrompts.class);

    private static final String CANCEL_WORD = "cancelar";
    private static final long EXPIRY_NANOS = 60_000_000_000L;

    private record Pending(Consumer<String> answer, Runnable cancel, long deadline) {
    }

    private final Messenger messenger;
    private final LongSupplier clock;
    private final Map<UUID, Pending> pending = new ConcurrentHashMap<>();

    public ChatPrompts(Messenger messenger) {
        this(messenger, System::nanoTime);
    }

    ChatPrompts(Messenger messenger, LongSupplier clock) {
        this.messenger = messenger;
        this.clock = clock;
    }

    /**
     * Tells the player what to type and waits for it.
     *
     * @param field  what the value is for, shown in the instruction
     * @param answer receives what the player typed
     * @param cancel runs when the player backs out
     */
    public void ask(ServerPlayer player, Component field, Consumer<String> answer, Runnable cancel) {
        messenger.send(player, Message.info(Messages.Gui.PROMPT).with("field", field));
        expect(player.getUUID(), answer, cancel);
    }

    void expect(UUID player, Consumer<String> answer, Runnable cancel) {
        pending.put(player, new Pending(answer, cancel, clock.getAsLong() + EXPIRY_NANOS));
    }

    /**
     * Offers a chat message to the prompt of its sender.
     *
     * @return whether the message was the answer, and so must not be broadcast
     */
    public boolean answer(UUID player, String message) {
        Pending prompt = pending.remove(player);
        if (prompt == null || clock.getAsLong() > prompt.deadline) {
            return false;
        }
        String reply = message.trim();
        try {
            if (reply.equalsIgnoreCase(CANCEL_WORD)) {
                prompt.cancel.run();
            } else {
                prompt.answer.accept(reply);
            }
        } catch (RuntimeException e) {
            LOGGER.error("A chat prompt failed for {}", player, e);
        }
        return true;
    }

    /** Stops waiting for a player, for example because they left. */
    public void forget(UUID player) {
        pending.remove(player);
    }
}
