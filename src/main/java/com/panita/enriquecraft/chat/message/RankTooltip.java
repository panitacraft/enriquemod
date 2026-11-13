package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.MessageFormatter;
import com.panita.enriquecraft.core.message.TextWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * What pointing at a rank in chat shows, built like the tooltip of an emblem: the rank as it is drawn (its image for a client that
 * draws art, its prefix for every other) and its title, then a description. Titles and descriptions are written by the administrator
 * and accept text tags. A click that copies the title is what makes the game show a pointing cursor, since it only does so over text
 * that can be clicked; it never sends or runs anything.
 */
public final class RankTooltip {

    private final MessageFormatter formatter;
    private final InlineArts arts;
    private final IntSupplier width;

    /**
     * @param width how many characters a line of a description written as plain text may have
     */
    public RankTooltip(MessageFormatter formatter, InlineArts arts, IntSupplier width) {
        this.formatter = formatter;
        this.arts = arts;
        this.width = width;
    }

    /**
     * For a title and a description written as text, such as the meta of a LuckPerms group: the description is broken into lines that
     * are never too wide.
     *
     * @param prefix         the rank as every client draws it
     * @param iconUrl        the image of the rank, for a client that draws art
     * @param rawTitle       the title as the administrator wrote it; blank shows only the rank
     * @param rawDescription what the rank is, as the administrator wrote it; blank shows none
     * @return empty when there is neither a title nor a description, so there is nothing to show
     */
    public Optional<Style> styleOf(Component prefix, Optional<String> iconUrl, String rawTitle, String rawDescription) {
        MutableComponent description = Component.empty();
        if (!rawDescription.isBlank()) {
            boolean first = true;
            for (String line : TextWrap.lines(rawDescription.strip(), width.getAsInt())) {
                description.append(Component.literal(first ? "" : "\n")).append(formatter.format(Message.plain(line)));
                first = false;
            }
        }
        Component title = rawTitle.isBlank() ? Component.empty() : formatter.format(Message.plain(rawTitle.strip()));
        return styleOf(prefix, iconUrl, title, description);
    }

    /** For a title and a description that are already finished text; the description keeps the line breaks it has. */
    public Optional<Style> styleOf(Component prefix, Optional<String> iconUrl, Component title, Component description) {
        boolean titled = !title.getString().isBlank();
        boolean described = !description.getString().isBlank();
        if (!titled && !described) {
            return Optional.empty();
        }
        MutableComponent tooltip = Component.empty().append(iconUrl.map(url -> arts.wrap(prefix, url)).orElse(prefix));
        if (titled) {
            tooltip.append(Component.literal(" ")).append(title);
        }
        if (described) {
            tooltip.append(Component.literal("\n")).append(description.copy().withStyle(ChatFormatting.GRAY));
        }
        String copied = titled ? title.getString() : prefix.getString();
        return Optional.of(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(tooltip)).withClickEvent(new ClickEvent.CopyToClipboard(copied)));
    }
}
