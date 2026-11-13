package com.panita.enriquecraft.chat.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.chat.message.HeadTooltip;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.Optional;
import java.util.function.BooleanSupplier;

/**
 * The head of the player a chat line starts with. It is an {@code object} text component, which any client draws inline at the
 * size of the text with no resource pack. It carries the player's own game profile, read at each line, so it is the skin the server
 * shows them (SkinsRestorer puts the skin a player chose in the profile's {@code textures}, and replaces the profile when they
 * change it) and never a lookup by name, which a player without a Microsoft account could not answer.
 * <p>
 * Only the {@code textures} property travels, without its signature: the client does not check it to draw a head, and it is
 * about a kilobyte that would otherwise repeat in every line to every reader.
 * <p>
 * Someone who is not online, such as the author of a Discord message, has the skin the server last saw on them ({@link SkinMemory}),
 * and when it never saw them the client is left to look the name up, which finds the skin of a Microsoft account.
 * <p>
 * Pointing at a head says whether the account is premium ({@link HeadTooltip}).
 */
public final class ChatHeads {

    private static final String TEXTURES = "textures";

    private final BooleanSupplier enabled;
    private final SkinMemory skins;
    private final HeadTooltip tooltip;

    public ChatHeads(BooleanSupplier enabled, SkinMemory skins, HeadTooltip tooltip) {
        this.enabled = enabled;
        this.skins = skins;
        this.tooltip = tooltip;
    }

    /** The head and the space after it, which start a line; empty when heads are turned off. */
    public Optional<Component> of(ServerPlayer player) {
        return enabled.getAsBoolean() ? Optional.of(of(player.getGameProfile(), modeOf(player.getGameProfile()))) : Optional.empty();
    }

    /**
     * The head of the player with this name, who may not be online: the skin they have now, the one last seen of them, or else the
     * one the client finds by name. Empty when heads are turned off.
     *
     * @param modeAway how the player logs in, as far as is known without them being online
     */
    public Optional<Component> of(MinecraftServer server, String name, Optional<AuthMode> modeAway) {
        if (!enabled.getAsBoolean()) {
            return Optional.empty();
        }
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        if (online != null) {
            return Optional.of(of(online.getGameProfile(), modeOf(online.getGameProfile())));
        }
        Style style = tooltip.styleOf(name, modeAway);
        return Optional.of(skins.profileOf(name).map(profile -> head(ResolvableProfile.createResolved(slim(profile)), style))
                .orElseGet(() -> head(ResolvableProfile.createUnresolved(name), style)));
    }

    /** What pointing at the head of that player shows, for a line that carries the head inside something of its own. */
    public Style tooltipStyle(MinecraftServer server, String name, Optional<AuthMode> modeAway) {
        ServerPlayer online = server.getPlayerList().getPlayerByName(name);
        return tooltip.styleOf(name, online != null ? modeOf(online.getGameProfile()) : modeAway);
    }

    private Component of(GameProfile profile, Optional<AuthMode> mode) {
        return head(ResolvableProfile.createResolved(slim(profile)), tooltip.styleOf(profile.name(), mode));
    }

    private static Optional<AuthMode> modeOf(GameProfile profile) {
        return Optional.of(AuthMode.of(profile.id(), profile.name()));
    }

    static Component of(GameProfile profile) {
        return head(ResolvableProfile.createResolved(slim(profile)), Style.EMPTY);
    }

    private static Component head(ResolvableProfile profile, Style style) {
        MutableComponent head = MutableComponent.create(new ObjectContents(new PlayerSprite(profile, true), Optional.empty())).withStyle(style);
        return Component.empty().append(head).append(Component.literal(" "));
    }

    /** The profile with only its skin, and without the signature of it. */
    static GameProfile slim(GameProfile profile) {
        ImmutableMultimap.Builder<String, Property> textures = ImmutableMultimap.builder();
        profile.properties().get(TEXTURES).stream().findFirst().ifPresent(property -> textures.put(TEXTURES, new Property(TEXTURES, property.value())));
        return new GameProfile(profile.id(), profile.name(), new PropertyMap(textures.build()));
    }
}
