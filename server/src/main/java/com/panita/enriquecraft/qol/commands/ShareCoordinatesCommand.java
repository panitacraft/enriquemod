package com.panita.enriquecraft.qol.commands;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.qol.message.QolMessages;
import com.panita.enriquecraft.qol.service.CoordinateSharing;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;

/**
 * {@code /sharecoordinates [player]}: tells everyone where the player is, or only the player given. {@code /c}, {@code /coords}
 * and {@code /coordinateshare} run the same command.
 */
@CommandSpec(name = "sharecoordinates", aliases = {"c", "coords", "coordinateshare"}, description = QolMessages.ShareCoordinates.DESCRIPTION)
public final class ShareCoordinatesCommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";

    private final PlayerOnly playerOnly;
    private final CoordinateSharing sharing;

    public ShareCoordinatesCommand(PlayerOnly playerOnly, CoordinateSharing sharing) {
        this.playerOnly = playerOnly;
        this.sharing = sharing;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes((context, player) -> sharing.withEveryone(player)))
                .then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                        .executes(playerOnly.executes((context, player) ->
                                sharing.withPlayer(player, EntityArgument.getPlayer(context, PLAYER_ARGUMENT)))));
    }
}
