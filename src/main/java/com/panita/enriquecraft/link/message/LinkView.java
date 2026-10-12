package com.panita.enriquecraft.link.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.Component;

/**
 * The texts the link module shows to players. Presentation only.
 */
public final class LinkView {

    private final MessageFormatter formatter;

    public LinkView(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    /** The disconnect screen of a player whose account was deactivated while they were online. */
    public Component deactivated() {
        return formatter.format(Message.plain(LinkMessages.Kick.DEACTIVATED));
    }
}
