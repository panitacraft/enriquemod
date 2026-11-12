package com.panita.enriquecraft.chat.service;

import com.google.common.collect.ImmutableMultimap;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.contents.ObjectContents;
import net.minecraft.network.chat.contents.objects.PlayerSprite;
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
 */
public final class ChatHeads {

    private static final String TEXTURES = "textures";

    private final BooleanSupplier enabled;

    public ChatHeads(BooleanSupplier enabled) {
        this.enabled = enabled;
    }

    /** The head and the space after it, which start a line; empty when heads are turned off. */
    public Optional<Component> of(ServerPlayer player) {
        return enabled.getAsBoolean() ? Optional.of(of(player.getGameProfile())) : Optional.empty();
    }

    static Component of(GameProfile profile) {
        MutableComponent head = MutableComponent.create(new ObjectContents(new PlayerSprite(ResolvableProfile.createResolved(slim(profile)), true), Optional.empty()));
        return Component.empty().append(head).append(Component.literal(" "));
    }

    /** The profile with only its skin, and without the signature of it. */
    static GameProfile slim(GameProfile profile) {
        ImmutableMultimap.Builder<String, Property> textures = ImmutableMultimap.builder();
        profile.properties().get(TEXTURES).stream().findFirst().ifPresent(property -> textures.put(TEXTURES, new Property(TEXTURES, property.value())));
        return new GameProfile(profile.id(), profile.name(), new PropertyMap(textures.build()));
    }
}
