package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.function.Function;

/**
 * Where the game tells everyone that a player joined, left, died or made an advancement: it all ends in this method. The chat
 * module recognizes those messages and shows its own instead; any other message goes on as the game wants.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListSystemMessageMixin {

    @Shadow
    public abstract MinecraftServer getServer();

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void enriquecraft$rememberTheJoiningPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                                      CallbackInfo callback) {
        ChatHooks hooks = ChatHooks.get();
        if (hooks != null) {
            hooks.joinBegins(player);
        }
    }

    @Inject(method = "placeNewPlayer", at = @At("RETURN"))
    private void enriquecraft$forgetTheJoiningPlayer(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                                    CallbackInfo callback) {
        ChatHooks hooks = ChatHooks.get();
        if (hooks != null) {
            hooks.joinEnds();
        }
    }

    @Inject(method = "broadcastSystemMessage(Lnet/minecraft/network/chat/Component;Ljava/util/function/Function;Z)V",
            at = @At("HEAD"), cancellable = true)
    private void enriquecraft$showTheModulesLine(Component message, Function<ServerPlayer, Component> playerMessages,
                                                 boolean overlay, CallbackInfo callback) {
        ChatHooks hooks = ChatHooks.get();
        if (!overlay && hooks != null && hooks.systemMessage(getServer(), message)) {
            callback.cancel();
        }
    }
}
