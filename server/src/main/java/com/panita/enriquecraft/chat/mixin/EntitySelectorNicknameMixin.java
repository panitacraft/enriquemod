package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.commands.arguments.selector.EntitySelector;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Lets a nickname stand for the player in every command argument that asks for a player by name, since the nickname is the
 * name their clients suggest. The real name keeps working.
 */
@Mixin(EntitySelector.class)
public abstract class EntitySelectorNicknameMixin {

    @Redirect(method = {"findEntities", "findPlayers"},
            at = @At(value = "INVOKE",
                    target = "Lnet/minecraft/server/players/PlayerList;getPlayerByName(Ljava/lang/String;)Lnet/minecraft/server/level/ServerPlayer;"))
    private ServerPlayer enriquecraft$findByNicknameToo(PlayerList players, String name) {
        ServerPlayer player = players.getPlayerByName(name);
        ChatHooks hooks = ChatHooks.get();
        return player != null || hooks == null ? player : hooks.playerWithNickname(players, name);
    }
}
