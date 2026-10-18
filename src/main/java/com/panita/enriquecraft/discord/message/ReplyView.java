package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.InFrame;
import net.minecraft.network.chat.Component;

/**
 * The note a Discord reply carries after the name of whoever replies, which says whose message it answers. The name is plain
 * text (inserted as a finished component, so nothing in it is read as formatting).
 */
public final class ReplyView {

    private final MessageFormatter formatter;

    public ReplyView(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    public Component line(InFrame.Reply reply) {
        return formatter.format(Message.plain(DiscordMessages.Delivery.REPLY_TO).with("author", reply.author()));
    }
}
