package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.InFrame;
import net.minecraft.network.chat.Component;

/**
 * The line shown before a Discord reply, which says whose message it answers. What the answered message said is plain text
 * (inserted as a finished component, so nothing in it is read as formatting) and is cut short, since it is only context.
 */
public final class ReplyView {

    private static final int SHOWN_TEXT = 100;

    private final MessageFormatter formatter;

    public ReplyView(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    /** @param sender the replying member as the chat line names them, with the prefix and the hover */
    public Component line(Component sender, InFrame.Reply reply) {
        String text = reply.text().length() <= SHOWN_TEXT ? reply.text() : reply.text().substring(0, SHOWN_TEXT - 1) + "…";
        return formatter.format(Message.plain(DiscordMessages.Delivery.REPLY)
                .with("sender", sender).with("author", reply.author()).with("message", text));
    }
}
