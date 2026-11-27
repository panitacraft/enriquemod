package com.panita.enriquecraft.chat.mixin;

import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.ServerScoreboard;
import net.minecraft.world.scores.PlayerScoreEntry;
import net.minecraft.world.scores.ScoreHolder;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Tells the clients the scores of a player under the name they know the player by. A client looks up the health shown in the tab list by
 * the name in the player's entry, which is the nickname for a player who has one ({@code PlayerInfoEntryMixin}), so a score sent under the
 * real name would never be found and the hearts would be missing. The scores the server keeps stay under the real name.
 */
@Mixin(ServerScoreboard.class)
public abstract class ServerScoreboardMixin {

    @Shadow
    @Final
    private MinecraftServer server;

    @Redirect(method = {"onScoreChanged", "onPlayerRemoved", "onPlayerScoreRemoved"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/ScoreHolder;getScoreboardName()Ljava/lang/String;"))
    private String enriquecraft$sendUnderTheShownName(ScoreHolder holder) {
        return shown(holder.getScoreboardName());
    }

    @Redirect(method = "getStartTrackingPackets",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/scores/PlayerScoreEntry;owner()Ljava/lang/String;"))
    private String enriquecraft$listUnderTheShownName(PlayerScoreEntry entry) {
        return shown(entry.owner());
    }

    private String shown(String owner) {
        ChatHooks hooks = ChatHooks.get();
        return hooks == null ? owner : hooks.scoreOwnerShownToClients(server.getPlayerList(), owner);
    }
}
