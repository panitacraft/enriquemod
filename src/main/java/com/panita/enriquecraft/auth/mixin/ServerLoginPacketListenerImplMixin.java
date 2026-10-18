package com.panita.enriquecraft.auth.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.authlib.GameProfile;
import com.panita.enriquecraft.auth.AuthHooks;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.service.LoginProfiles;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.login.ServerboundHelloPacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerLoginPacketListenerImpl;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes the server ask for a Microsoft login only from the players who have an account, instead of from
 * everyone or no one. When a player says their name, the decision is made first (it may take a moment,
 * so the hello is parked and replayed); then vanilla runs as usual, with its "does this server
 * authenticate players" question answered for this connection alone, and a player without an account is
 * let in under the spelling of their name that the whitelist has, since the offline UUID depends on it.
 */
@Mixin(ServerLoginPacketListenerImpl.class)
public abstract class ServerLoginPacketListenerImplMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Shadow
    @Final
    private Connection connection;

    @Unique
    private volatile @Nullable AuthMode enriquecraft$mode;

    @Unique
    private volatile @Nullable String enriquecraft$name;

    @Unique
    private boolean enriquecraft$deciding;

    @Shadow
    private @Nullable GameProfile authenticatedProfile;

    @Shadow
    public abstract void handleHello(ServerboundHelloPacket packet);

    @Shadow
    public abstract void disconnect(Component reason);

    @Inject(method = "handleHello", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$decideMode(ServerboundHelloPacket packet, CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        // A singleplayer world has its own rules, and a decided hello is the replay.
        if (hooks == null || !server.isDedicatedServer() || enriquecraft$mode != null) {
            return;
        }
        callback.cancel();
        if (enriquecraft$deciding) {
            return;
        }
        enriquecraft$deciding = true;
        hooks.admit(server, connection, packet.name(), (mode, name) -> {
            enriquecraft$name = name;
            enriquecraft$mode = mode;
            handleHello(packet);
        }, this::disconnect);
    }

    /**
     * The game builds the player from the profile it authenticated and only sends the client the one the login finishes with,
     * so a skin that a mod put on that one would show while connecting and be gone once the player is in.
     */
    @Inject(method = "finishLoginAndWaitForClient", at = @At("HEAD"))
    private void enriquecraft$keepTheProfileTheLoginFinishedWith(GameProfile profile, CallbackInfo callback) {
        authenticatedProfile = LoginProfiles.carry(authenticatedProfile, profile);
    }

    @WrapOperation(method = "handleHello",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/server/MinecraftServer;usesAuthentication()Z"))
    private boolean enriquecraft$authenticateOnlyPremium(MinecraftServer instance, Operation<Boolean> original) {
        AuthMode mode = enriquecraft$mode;
        return mode == null ? original.call(instance) : mode == AuthMode.PREMIUM;
    }

    @WrapOperation(method = "handleHello",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/core/UUIDUtil;createOfflineProfile(Ljava/lang/String;)Lcom/mojang/authlib/GameProfile;"))
    private GameProfile enriquecraft$offlineUnderTheWhitelistsSpelling(String typed, Operation<GameProfile> original) {
        String name = enriquecraft$name;
        return original.call(name == null ? typed : name);
    }
}
