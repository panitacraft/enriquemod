package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.chat.text.ChatCapability;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.server.level.ServerPlayer;

import java.util.EnumSet;
import java.util.Set;

/** What a player may do in chat, asked of the permission manager once for each capability. */
public final class ChatPermissions {

    private ChatPermissions() {
    }

    public static Set<ChatCapability> capabilitiesOf(ServerPlayer player) {
        Set<ChatCapability> granted = EnumSet.noneOf(ChatCapability.class);
        for (ChatCapability capability : ChatCapability.values()) {
            if (Permissions.check(player, capability.node(), capability.fallback())) {
                granted.add(capability);
            }
        }
        return granted;
    }
}
