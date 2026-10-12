package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.core.message.Timestamps;
import com.panita.enriquecraft.discord.service.BridgeConnection;
import net.minecraft.commands.CommandSourceStack;

/**
 * What the discord module tells staff. Presentation only.
 */
public final class DiscordView {

    private final Messenger messenger;

    public DiscordView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void status(CommandSourceStack source, BridgeConnection.Status status) {
        switch (status.state()) {
            case OFF -> send(source, offTemplate(status));
            case CONNECTING -> send(source, DiscordMessages.Status.CONNECTING);
            case WAITING -> send(source, DiscordMessages.Status.WAITING);
            case INCOMPATIBLE -> send(source, DiscordMessages.Status.INCOMPATIBLE);
            case CONNECTED -> {
                messenger.send(source, Message.plain(DiscordMessages.Status.CONNECTED)
                        .with("since", status.since().map(Timestamps::dateTime).orElse("?")).prefixed());
                if (status.channels().isEmpty()) {
                    send(source, DiscordMessages.Status.NO_CHANNELS);
                } else {
                    messenger.send(source, Message.plain(DiscordMessages.Status.CHANNELS)
                            .with("channels", String.join(", ", status.channels())).prefixed());
                }
            }
        }
        if (!status.url().isBlank()) {
            messenger.send(source, Message.plain(DiscordMessages.Status.ADDRESS).with("url", status.url()).prefixed());
        }
        messenger.send(source, Message.plain(DiscordMessages.Status.FRAMES).with("sent", (int) status.sent())
                .with("received", (int) status.received()).with("dropped", (int) status.dropped()).prefixed());
        status.lastError().ifPresent(error ->
                messenger.send(source, Message.plain(DiscordMessages.Status.LAST_ERROR).with("error", error).prefixed()));
    }

    private static String offTemplate(BridgeConnection.Status status) {
        if (!status.enabled()) {
            return DiscordMessages.Status.OFF_DISABLED;
        }
        if (status.url().isBlank()) {
            return DiscordMessages.Status.OFF_NO_URL;
        }
        return status.keyFound() ? DiscordMessages.Status.OFF_IDLE : DiscordMessages.Status.OFF_NO_KEY;
    }

    public void reconnecting(CommandSourceStack source) {
        send(source, DiscordMessages.Reconnect.STARTED);
    }

    private void send(CommandSourceStack source, String template) {
        messenger.send(source, Message.plain(template).prefixed());
    }
}
