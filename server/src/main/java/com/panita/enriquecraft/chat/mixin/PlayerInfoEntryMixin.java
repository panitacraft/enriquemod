package com.panita.enriquecraft.chat.mixin;

import com.mojang.authlib.GameProfile;
import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * What the clients are told a player is called. A client knows another player only by this profile: it writes the name over
 * their head, suggests it when a command asks for a player and lists it in the tab list. For a player with a nickname the
 * profile carries the nickname, with the same id and skin, so all of that shows the nickname. The profile the server keeps
 * is never changed.
 */
@Mixin(targets = "net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket$Entry")
public abstract class PlayerInfoEntryMixin {

    @Redirect(method = "<init>(Lnet/minecraft/server/level/ServerPlayer;)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;getGameProfile()Lcom/mojang/authlib/GameProfile;"))
    private static GameProfile enriquecraft$nameThePlayerByTheirNickname(ServerPlayer player) {
        ChatHooks hooks = ChatHooks.get();
        GameProfile profile = player.getGameProfile();
        return hooks == null ? profile : hooks.profileShownToClients(player, profile);
    }
}
