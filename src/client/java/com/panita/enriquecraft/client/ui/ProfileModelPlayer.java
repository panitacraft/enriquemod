package com.panita.enriquecraft.client.ui;

import com.mojang.authlib.GameProfile;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.RemotePlayer;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;

import java.util.function.Supplier;

/**
 * A player that exists only to be drawn in a screen, with the skin of a profile the server sent: the game finds it from the profile's
 * `textures` property, which is where SkinsRestorer puts the skin a player chose. It is never added to the world.
 */
final class ProfileModelPlayer extends RemotePlayer {

    private final Supplier<PlayerSkinRenderCache.RenderInfo> skin;

    ProfileModelPlayer(ClientLevel level, ResolvableProfile profile) {
        super(level, profile.partialProfile());
        // A lookup gives the default skin until the real one is ready, and then the real one, so the model fills in by itself
        this.skin = Minecraft.getInstance().playerSkinRenderCache().createLookup(profile);
    }

    GameProfile profile() {
        return getGameProfile();
    }

    @Override
    public PlayerSkin getSkin() {
        return skin.get().playerSkin();
    }
}
