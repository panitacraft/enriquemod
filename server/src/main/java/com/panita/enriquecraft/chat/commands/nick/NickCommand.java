package com.panita.enriquecraft.chat.commands.nick;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panita.enriquecraft.chat.ChatPermissions;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.message.NicknameView;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.chat.service.ClientNames;
import com.panita.enriquecraft.chat.service.KnownPlayers;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /nick <nickname>} and {@code /nick off}: a player's own nickname. {@code /nick other <player> <nickname|off>}
 * is for those who may change the nickname of someone else.
 */
@CommandSpec(name = "nick", description = ChatMessages.Nickname.NICK_DESCRIPTION)
public final class NickCommand implements ModCommand {

    private static final String OFF = "off";
    private static final String OTHER = "other";
    private static final String PLAYER_ARGUMENT = "player";
    private static final String NICKNAME_ARGUMENT = "nickname";

    private final PlayerOnly playerOnly;
    private final NicknameService nicknames;
    private final NicknameView view;
    private final ClientNames clientNames;
    private final KnownPlayers knownPlayers;

    public NickCommand(PlayerOnly playerOnly, NicknameService nicknames, NicknameView view, ClientNames clientNames,
                       KnownPlayers knownPlayers) {
        this.playerOnly = playerOnly;
        this.nicknames = nicknames;
        this.view = view;
        this.clientNames = clientNames;
        this.knownPlayers = knownPlayers;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.literal(OFF).executes(playerOnly.executes((context, player) -> clear(context.getSource(), player, false))))
                .then(Commands.literal(OTHER)
                        .requires(ChatPermissions::canChangeOthersNickname)
                        .then(Commands.argument(PLAYER_ARGUMENT, EntityArgument.player())
                                .then(Commands.literal(OFF).executes(context -> clear(context.getSource(),
                                        EntityArgument.getPlayer(context, PLAYER_ARGUMENT), true)))
                                .then(Commands.argument(NICKNAME_ARGUMENT, StringArgumentType.greedyString())
                                        .executes(this::setOther))))
                .then(Commands.argument(NICKNAME_ARGUMENT, StringArgumentType.greedyString())
                        .executes(playerOnly.executes(this::setOwn)));
    }

    private int setOwn(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        return set(context, player, false);
    }

    private int setOther(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        return set(context, EntityArgument.getPlayer(context, PLAYER_ARGUMENT), true);
    }

    private int set(CommandContext<CommandSourceStack> context, ServerPlayer target, boolean other) {
        CommandSourceStack source = context.getSource();
        NicknameService.Outcome outcome = nicknames.set(target.getUUID(), target.getGameProfile().name(),
                StringArgumentType.getString(context, NICKNAME_ARGUMENT), ChatPermissions.canColorNickname(source),
                ChatPermissions.canBypassNicknameRules(source), knownPlayers.namesOn(source.getServer()));
        if (outcome.status() != NicknameService.Status.CHANGED) {
            view.rejected(source, outcome);
            return 0;
        }
        clientNames.refresh(target);
        if (other) {
            view.changedOther(source, target.getGameProfile().name(), nicknames.nicknameOf(target.getUUID()).orElseThrow());
        } else {
            view.changed(source, nicknames.nicknameOf(target.getUUID()).orElseThrow());
        }
        return Command.SINGLE_SUCCESS;
    }

    private int clear(CommandSourceStack source, ServerPlayer target, boolean other) {
        if (nicknames.clear(target.getUUID()).status() == NicknameService.Status.NOT_SET) {
            if (other) {
                view.notSetOther(source, target.getGameProfile().name());
            } else {
                view.notSet(source);
            }
            return 0;
        }
        clientNames.refresh(target);
        if (other) {
            view.clearedOther(source, target.getGameProfile().name());
        } else {
            view.cleared(source);
        }
        return Command.SINGLE_SUCCESS;
    }
}
