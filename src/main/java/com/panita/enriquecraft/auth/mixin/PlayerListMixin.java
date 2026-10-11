package com.panita.enriquecraft.auth.mixin;

import com.panita.enriquecraft.auth.AuthHooks;
import net.minecraft.network.Connection;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.server.players.PlayerList;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Lets the auth module look at a player the moment they are placed in the world, before the game sends them
 * anything, so it can tell whether they must enter a PIN. Their arrival is announced as always: the others can
 * then report a name they see trying to get in.
 */
@Mixin(PlayerList.class)
public abstract class PlayerListMixin {

    @Inject(method = "placeNewPlayer", at = @At("HEAD"))
    private void enriquecraft$checkPin(Connection connection, ServerPlayer player, CommonListenerCookie cookie,
                                       CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks != null) {
            hooks.playerJoining(connection, player);
        }
    }
}
