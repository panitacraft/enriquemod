package com.panita.enriquecraft.chat.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.chat.channel.ChannelDefinition;
import com.panita.enriquecraft.chat.channel.ChannelRegistry;
import com.panita.enriquecraft.chat.channel.ChannelSelection;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.message.ChatView;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;

import java.util.Collection;

/**
 * {@code /chat [channel]}: says which channel the player writes in, or changes it.
 */
@CommandSpec(name = "chat", description = ChatMessages.Channel.CHAT_DESCRIPTION)
public final class ChatCommand implements ModCommand {

    private static final String CHANNEL_ARGUMENT = "channel";

    private final PlayerOnly playerOnly;
    private final ChannelRegistry channels;
    private final ChannelSelection selection;
    private final ChatView view;

    public ChatCommand(PlayerOnly playerOnly, ChannelRegistry channels, ChannelSelection selection, ChatView view) {
        this.playerOnly = playerOnly;
        this.channels = channels;
        this.selection = selection;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::show))
                .then(Commands.argument(CHANNEL_ARGUMENT, StringArgumentType.word())
                        .suggests(CommandSuggestions.forSource(this::namesFor))
                        .executes(playerOnly.executes(this::change)));
    }

    private int show(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        view.current(player, selection.current(player));
        return Command.SINGLE_SUCCESS;
    }

    private int change(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        String name = StringArgumentType.getString(context, CHANNEL_ARGUMENT);
        ChannelDefinition channel = channels.find(name).orElse(null);
        if (channel == null) {
            view.unknownChannel(context.getSource(), name);
            return 0;
        }
        if (!channel.canUse(player)) {
            view.channelDenied(player, channel);
            return 0;
        }
        selection.select(player, channel);
        view.switched(player, channel);
        return Command.SINGLE_SUCCESS;
    }

    /** The names of the channels the one who is typing may use. */
    private Collection<String> namesFor(CommandSourceStack source) {
        ServerPlayer player = source.getPlayer();
        return channels.all().stream()
                .filter(channel -> player == null || channel.canUse(player))
                .flatMap(channel -> channel.names().stream())
                .toList();
    }
}
