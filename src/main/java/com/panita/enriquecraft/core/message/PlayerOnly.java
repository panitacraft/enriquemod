package com.panita.enriquecraft.core.message;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * Guards commands that only make sense for a player, such as those that use the player's position
 * or inventory. When the console (or a command block) runs one, it gets a notice instead.
 */
public final class PlayerOnly {

    private final Messenger messenger;

    public PlayerOnly(Messenger messenger) {
        this.messenger = messenger;
    }

    /**
     * Wraps an action so it runs only when a player runs the command.
     */
    public Command<CommandSourceStack> executes(PlayerAction action) {
        return context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player == null) {
                messenger.send(context.getSource(), Message.error(Messages.Command.PLAYERS_ONLY).prefixed());
                return 0;
            }
            return action.run(context, player);
        };
    }

    @FunctionalInterface
    public interface PlayerAction {

        /** Runs the command for the player; returns the command's result, as Brigadier expects. */
        int run(CommandContext<CommandSourceStack> context, ServerPlayer player) throws CommandSyntaxException;
    }
}
