package com.panita.enriquecraft.chat.message;

import com.panita.enriquecraft.core.message.PointerClick;
import com.panita.enriquecraft.core.message.StyledWrap;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

import java.util.Optional;
import java.util.function.IntSupplier;

/**
 * What pointing at a rank in chat shows: its title and, under it, its description. Both are finished text that keeps its own colors
 * and styles, so the title is not tied to the color of the rank or to the rank itself, and the description is gray where it sets no
 * color. A click that does nothing makes the game show a pointing cursor ({@link PointerClick}).
 */
public final class RankTooltip {

    private final IntSupplier width;

    /**
     * @param width how many characters a line of the description may have
     */
    public RankTooltip(IntSupplier width) {
        this.width = width;
    }

    /**
     * @param title       the title; blank shows none
     * @param description what the rank is, broken into lines that are never too wide; blank shows none
     * @return empty when there is neither a title nor a description, so there is nothing to show
     */
    public Optional<Style> styleOf(Component title, Component description) {
        boolean titled = !title.getString().isBlank();
        boolean described = !description.getString().isBlank();
        if (!titled && !described) {
            return Optional.empty();
        }
        MutableComponent tooltip = Component.empty();
        if (titled) {
            tooltip.append(title);
        }
        if (described) {
            tooltip.append(Component.literal(titled ? "\n" : "")).append(StyledWrap.wrap(description, width.getAsInt()).copy().withStyle(ChatFormatting.GRAY));
        }
        return Optional.of(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(tooltip)).withClickEvent(PointerClick.NOTHING));
    }
}
