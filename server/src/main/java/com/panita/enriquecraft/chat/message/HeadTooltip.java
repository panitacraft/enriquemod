package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;

import java.util.Optional;

/**
 * What pointing at the head of a player in chat shows: whether their account is premium, when that is known, and that a click opens
 * their profile. The click runs the profile command, which anyone may use and which only shows a profile, so it can never do anything
 * else; the game also needs a click on the text to show a pointing cursor.
 */
public final class HeadTooltip {

    private static final String PROFILE_COMMAND = "/profile ";

    private final MessageFormatter formatter;

    public HeadTooltip(MessageFormatter formatter) {
        this.formatter = formatter;
    }

    /**
     * @param name the real name of the player, which the profile command takes
     * @param mode how the player logs in, when it is known
     */
    public Style styleOf(String name, Optional<AuthMode> mode) {
        Component hint = formatter.format(Message.plain(ChatMessages.Head.PROFILE));
        Component hover = mode.<Component>map(known -> Component.empty()
                        .append(formatter.format(Message.plain(known == AuthMode.PREMIUM ? ChatMessages.Head.PREMIUM : ChatMessages.Head.NO_PREMIUM)))
                        .append(Component.literal("\n")).append(hint))
                .orElse(hint);
        return Style.EMPTY.withClickEvent(new ClickEvent.RunCommand(PROFILE_COMMAND + name)).withHoverEvent(new HoverEvent.ShowText(hover));
    }
}
