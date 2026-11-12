package com.panita.enriquecraft.profile.message;

import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.core.message.TextWrap;
import com.panita.enriquecraft.profile.service.EmblemView;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.IntSupplier;

/**
 * How an emblem is described wherever it can be pointed at: its character and its title in the emblem's color, then its description
 * from the API on lines that are never too wide. The chat hover, the name and details of a chest item and the client's screens all
 * show these same lines, so an emblem reads the same everywhere.
 */
public final class EmblemTooltip {

    private final IntSupplier width;
    private final InlineArts arts;

    /**
     * @param width how many characters a line of the description may have
     */
    public EmblemTooltip(IntSupplier width, InlineArts arts) {
        this.width = width;
        this.arts = arts;
    }

    /**
     * The title line: the character, a space and the title, in the emblem's color. A client that draws art sees the image of the emblem
     * in place of the character.
     */
    public Component title(EmblemView emblem) {
        int color = emblem.look().textColor().getValue();
        Component icon = Component.literal(emblem.look().icon()).withColor(color);
        Component drawn = emblem.iconUrl().map(url -> arts.wrap(icon, url)).orElse(icon);
        return Component.empty().append(drawn).append(Component.literal(" " + emblem.title()).withColor(color));
    }

    /** The name tag that stands for the emblem in a menu: its image, for a client that draws art. */
    public ItemStack icon(EmblemView emblem) {
        ItemStack tag = new ItemStack(Items.NAME_TAG);
        return emblem.iconUrl().map(url -> arts.item(tag, url)).orElse(tag);
    }

    /** Tells a player who draws art what the images of these emblems are, before a screen or a line that shows their titles. */
    public void announce(ServerPlayer player, Collection<EmblemView> emblems) {
        arts.announce(List.of(player), Component.empty().append(emblems.stream().map(this::title).reduce(Component.empty(), (all, title) -> all.copy().append(title))));
    }

    /** A note of the menus on short lines, in gray, like a description. */
    public List<Component> note(String text) {
        List<Component> lines = new ArrayList<>();
        TextWrap.lines(text, width.getAsInt()).forEach(line -> lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY)));
        return lines;
    }

    /** The description on short lines, in gray; none when the emblem has no description. */
    public List<Component> description(EmblemView emblem) {
        List<Component> lines = new ArrayList<>();
        emblem.description().ifPresent(text -> TextWrap.lines(text, width.getAsInt())
                .forEach(line -> lines.add(Component.literal(line).withStyle(ChatFormatting.GRAY))));
        return lines;
    }

    /** The whole tooltip as one text with line breaks, which is what a hover event holds. */
    public Component hover(EmblemView emblem) {
        MutableComponent text = title(emblem).copy();
        for (Component line : description(emblem)) {
            text.append(Component.literal("\n")).append(line);
        }
        return text;
    }

    /** What shows the tooltip when something is pointed at. */
    public HoverEvent hoverEvent(EmblemView emblem) {
        return new HoverEvent.ShowText(hover(emblem));
    }

    /** The character of the emblem in its color, which shows the tooltip when pointed at. */
    public Component glyph(EmblemView emblem) {
        return Component.literal(emblem.look().icon()).withColor(emblem.look().textColor().getValue())
                .withStyle(style -> style.withHoverEvent(hoverEvent(emblem)));
    }
}
