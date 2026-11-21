package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Makes the name the game shows for a player, in death messages, advancements, private messages and every other line that
 * names them, their nickname. Everything around the name stays the game's: the team color, the tooltip and the click that
 * starts a private message to the real name.
 */
@Mixin(Player.class)
public abstract class PlayerDisplayNameMixin {

    @Redirect(method = "getDisplayName",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/player/Player;getName()Lnet/minecraft/network/chat/Component;"))
    private Component enriquecraft$showTheNickname(Player player) {
        ChatHooks hooks = ChatHooks.get();
        Component nickname = hooks == null ? null : hooks.nicknameOf(player);
        return nickname != null ? nickname : player.getName();
    }
}
