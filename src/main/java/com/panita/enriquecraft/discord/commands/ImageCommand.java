package com.panita.enriquecraft.discord.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandPermissions;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.discord.message.DiscordMessages;
import com.panita.enriquecraft.discord.message.ImageView;
import com.panita.enriquecraft.discord.protocol.MediaEntry;
import com.panita.enriquecraft.discord.service.ImageMaps;
import com.panita.enriquecraft.discord.service.MediaTokens;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.List;
import java.util.Optional;

/**
 * {@code /imagen <token>}: what the label of an image in the chat runs when it is clicked. The token stands for an image the
 * server put in the chat; it is not meant to be typed.
 */
@CommandSpec(name = "imagen", description = DiscordMessages.Image.COMMAND_DESCRIPTION, clickable = true)
public final class ImageCommand implements ModCommand {

    private static final String TOKEN_ARGUMENT = "token";
    private static final List<String> PATH = List.of("imagen");

    private final PlayerOnly playerOnly;
    private final MediaTokens tokens;
    private final ImageMaps images;
    private final ImageView view;

    public ImageCommand(PlayerOnly playerOnly, MediaTokens tokens, ImageMaps images, ImageView view) {
        this.playerOnly = playerOnly;
        this.tokens = tokens;
        this.images = images;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(TOKEN_ARGUMENT, StringArgumentType.word()).executes(playerOnly.executes(this::show)));
    }

    private int show(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        if (!CommandPermissions.canUse(context.getSource(), PATH, PermissionLevel.ALL)) {
            view.notAllowed(player);
            return 0;
        }
        String token = StringArgumentType.getString(context, TOKEN_ARGUMENT);
        // Only a token the server issued means anything; whatever else is typed here is never looked at, let alone run
        Optional<MediaEntry> entry = MediaTokens.isToken(token) ? tokens.find(token) : Optional.empty();
        if (entry.isEmpty()) {
            view.expired(player);
            return 0;
        }
        images.show(context.getSource().getServer(), player, entry.get());
        return Command.SINGLE_SUCCESS;
    }
}
