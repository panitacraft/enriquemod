package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.profile.message.EmblemTooltip;
import eu.pb4.placeholders.api.PlaceholderResult;
import eu.pb4.placeholders.api.Placeholders;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import java.util.List;

/**
 * The placeholders that put a player's emblems around their name, for every text that resolves them (the lines of chat, and mods
 * that read Placeholder API's, such as a tab list mod): {@code %enriquecraft:emblems_system%} and
 * {@code %enriquecraft:emblems_lore%}. Each emblem is its character in its color, and pointing at it shows its tooltip. They are
 * placeholders, not part of the chat code, because the administrator's format decides where they stand relative to the rank.
 * <p>
 * Each one brings its own space, after the system emblems and before the lore ones, so a player with none leaves no gap in the
 * line.
 */
public final class EmblemPlaceholders {

    private final ProfileService profiles;
    private final EmblemTooltip tooltip;

    public EmblemPlaceholders(ProfileService profiles, EmblemTooltip tooltip) {
        this.profiles = profiles;
        this.tooltip = tooltip;
    }

    public void register() {
        Placeholders.registerServer(id("emblems_system"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(system(profiles.emblemsOf(context.serverPlayer().getUUID()).system()))
                : PlaceholderResult.invalid("No player"));
        Placeholders.registerServer(id("emblems_lore"), (context, argument) -> context.hasServerPlayer()
                ? PlaceholderResult.value(lore(profiles.emblemsOf(context.serverPlayer().getUUID()).chosen()))
                : PlaceholderResult.invalid("No player"));
    }

    /** The characters each followed by a space, so the text that comes next stands apart. */
    Component system(List<EmblemView> emblems) {
        MutableComponent text = Component.empty();
        for (EmblemView emblem : emblems) {
            text.append(tooltip.glyph(emblem)).append(Component.literal(" "));
        }
        return text;
    }

    /** The characters each preceded by a space, so they stand apart from the name before them. */
    Component lore(List<EmblemView> emblems) {
        MutableComponent text = Component.empty();
        for (EmblemView emblem : emblems) {
            text.append(Component.literal(" ")).append(tooltip.glyph(emblem));
        }
        return text;
    }

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(Enriquecraft.MOD_ID, path);
    }
}
