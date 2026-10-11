package com.panita.enriquecraft.chat;

import com.panita.enriquecraft.Enriquecraft;
import com.panita.enriquecraft.chat.text.ChatCapability;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.EnumSet;
import java.util.Set;

/** What a player may do in chat, asked of the permission manager. The channels have their own nodes, in the config. */
public final class ChatPermissions {

    private static final String ROOT = Enriquecraft.MOD_ID + ".chat.nick";

    /** Colors and styles in a nickname. */
    public static final String NICK_COLOR = ROOT + ".color";
    /** A nickname that ignores the wait between changes and may be one another player has. */
    public static final String NICK_BYPASS = ROOT + ".bypass";
    /** Changing the nickname of another player. */
    public static final String NICK_OTHERS = ROOT + ".others";

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

    public static boolean canColorNickname(CommandSourceStack source) {
        return Permissions.check(source, NICK_COLOR, PermissionLevel.ADMINS);
    }

    public static boolean canBypassNicknameRules(CommandSourceStack source) {
        return Permissions.check(source, NICK_BYPASS, PermissionLevel.ADMINS);
    }

    public static boolean canChangeOthersNickname(CommandSourceStack source) {
        return Permissions.check(source, NICK_OTHERS, PermissionLevel.ADMINS);
    }
}
