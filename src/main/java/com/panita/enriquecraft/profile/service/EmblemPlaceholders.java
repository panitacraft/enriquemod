package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.core.art.InlineArts;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.stream.Stream;

/**
 * The placeholders that put a player's emblems around their name, for every text that resolves them (the lines of chat, and mods
 * that read Placeholder API's, such as a tab list mod): {@code %enriquecraft:emblems_system%} and
 * {@code %enriquecraft:emblems_lore%}. The system emblem is the prank level, which goes before the rank; the lore emblems are the
 * ones the player chose, which go after the name. Each emblem is its character in its color, and pointing at it shows its tooltip.
 * They are placeholders, not part of the chat code, because the administrator's format decides where they stand relative to the
 * rank.
 * <p>
 * Each one brings its own space, after the system emblem and before the lore ones, so a player with none leaves no gap in the line.
 */
public final class EmblemPlaceholders {

    private final ProfileService profiles;
    private final EmblemTooltip tooltip;
    private final InlineArts arts;

    public EmblemPlaceholders(ProfileService profiles, EmblemTooltip tooltip, InlineArts arts) {
        this.profiles = profiles;
        this.tooltip = tooltip;
        this.arts = arts;
    }

    public void register() {
        Placeholders.registerServer(id("emblems_system"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(system(profiles.emblemsOf(context.serverPlayer()).prank()))
                : PlaceholderResult.invalid("No player"));
        Placeholders.registerServer(id("emblems_lore"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(lore(profiles.emblemsOf(context.serverPlayer())))
                : PlaceholderResult.invalid("No player"));
    }

    /** The emblem followed by a space, so the text that comes next stands apart. */
    public Component system(EmblemView emblem) {
        return Component.empty().append(drawn(emblem)).append(Component.literal(" "));
    }

    /**
     * The emblems the player chose, then a stand-in for each place they have not filled, each preceded by a space so they stand apart
     * from the name before them.
     */
    public Component lore(PlayerEmblems emblems) {
        MutableComponent text = Component.empty();
        for (EmblemView emblem : Stream.concat(emblems.chosen().stream(), emblems.empty().stream()).toList()) {
            text.append(Component.literal(" ")).append(drawn(emblem));
        }
        return text;
    }

    /**
     * The emblem as its character, or, for a client that draws art, as its image when it has one. Both show the tooltip when pointed at:
     * the hover is on the whole, so the image has it too.
     */
    private Component drawn(EmblemView emblem) {
        Component glyph = tooltip.glyph(emblem);
        if (emblem.iconUrl().isEmpty()) {
            return glyph;
        }
        return arts.wrap(glyph, emblem.iconUrl().get()).copy().withStyle(tooltip.pointedStyle(emblem));
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
    }
}
