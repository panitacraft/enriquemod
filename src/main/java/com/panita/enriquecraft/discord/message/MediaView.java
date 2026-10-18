package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import com.panita.enriquecraft.discord.service.MediaLinks;
import com.panita.enriquecraft.discord.service.MediaTokens;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;

import java.net.URI;

/**
 * How an image of a Discord message shows in the game chat: a label that any client can read, which shows the image on a map
 * when clicked, followed by a link to the original (the game asks the player to confirm before it opens a link). A spoiler does
 * not say its name. Presentation only; the caller has already checked the address.
 */
public final class MediaView {

    private final MessageFormatter formatter;
    private final MediaTokens tokens;

    public MediaView(MessageFormatter formatter, MediaTokens tokens) {
        this.formatter = formatter;
        this.tokens = tokens;
    }

    public Component link(MediaEntry entry) {
        Component label;
        Component hover;
        if (entry.spoiler()) {
            label = formatter.format(Message.plain(DiscordMessages.Media.LABEL_SPOILER));
            hover = formatter.format(Message.plain(DiscordMessages.Media.HOVER_SPOILER));
        } else {
            label = formatter.format(Message.plain(DiscordMessages.Media.LABEL).with("name", entry.name()));
            hover = formatter.format(Message.plain(DiscordMessages.Media.HOVER).with("details", MediaLinks.details(entry)));
        }
        String command = "/imagen " + tokens.issue(entry);
        URI address = URI.create(entry.url());
        Component link = formatter.format(Message.plain(DiscordMessages.Media.LINK)).copy().withStyle(style -> style
                .withClickEvent(new ClickEvent.OpenUrl(address))
                .withHoverEvent(new HoverEvent.ShowText(formatter.format(Message.plain(DiscordMessages.Media.LINK_HOVER)))));
        return Component.empty()
                .append(label.copy().withStyle(style -> style.withClickEvent(new ClickEvent.RunCommand(command)).withHoverEvent(new HoverEvent.ShowText(hover))))
                .append(link);
    }
}
