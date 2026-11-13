package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * What pointing at the head of a player in chat shows: whether their account is premium, when that is known, and a click that writes
 * a private message to them. The click is what makes the game show a pointing cursor, since it only does so over text that can be
 * clicked; it only fills the chat box, it never sends anything.
 */
public final class HeadTooltip {

    private final MessageFormatter formatter;

    public HeadTooltip(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param name the real name of the player, which {@code /msg} takes
     * @param mode how the player logs in, when it is known
     */
    public Style styleOf(String name, Optional<AuthMode> mode) {
        Style style = Style.EMPTY.withClickEvent(new ClickEvent.SuggestCommand("/msg " + name + " "));
        return mode.map(known -> style.withHoverEvent(new HoverEvent.ShowText(formatter.format(Message.plain(
                known == AuthMode.PREMIUM ? ChatMessages.Head.PREMIUM : ChatMessages.Head.NO_PREMIUM))))).orElse(style);
    }
}
