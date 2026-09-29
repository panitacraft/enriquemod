package com.panita.enriquecraft.command;

import com.panita.enriquecraft.Enriquecraft;
import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.function.Predicate;

/**
 * Derives permission nodes from command paths and checks them. Nodes are never declared by hand:
 * {@code /enriquecraft info} uses {@code enriquecraft.command.enriquecraft.info}.
 */
public final class CommandPermissions {

    private static final String ROOT = Enriquecraft.MOD_ID + ".command";

    private CommandPermissions() {
    }

    public static String node(List<String> path) {
        return ROOT + "." + String.join(".", path);
    }

    /** Brigadier requirement that also hides the command from tab completion when it is not allowed. */
    public static Predicate<CommandSourceStack> requirement(List<String> path, PermissionLevel fallback) {
        return Permissions.require(node(path), fallback);
    }

    public static boolean canUse(CommandSourceStack source, List<String> path, PermissionLevel fallback) {
        return Permissions.check(source, node(path), fallback);
    }
}
