package com.panita.enriquecraft.chat.commands.nick;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.chat.data.ChatProfile;
import com.panita.enriquecraft.chat.message.ChatMessages;
import com.panita.enriquecraft.chat.message.NicknameView;
import com.panita.enriquecraft.chat.service.NicknameService;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /realname <nickname>}: says whose nickname it is, so staff can always tell who is behind a name.
 */
@CommandSpec(name = "realname", description = ChatMessages.Nickname.REAL_NAME_DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class RealNameCommand implements ModCommand {

    private static final String NICKNAME_ARGUMENT = "nickname";

    private final NicknameService nicknames;
    private final NicknameView view;

    public RealNameCommand(NicknameService nicknames, NicknameView view) {
        this.nicknames = nicknames;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NICKNAME_ARGUMENT, StringArgumentType.greedyString()).executes(this::find));
    }

    private int find(CommandContext<CommandSourceStack> context) {
        String text = StringArgumentType.getString(context, NICKNAME_ARGUMENT);
        ChatProfile profile = nicknames.findByNickname(text).orElse(null);
        if (profile == null) {
            view.nobodyCalled(context.getSource(), text);
            return 0;
        }
        view.realName(context.getSource(), nicknames.nicknameOf(profile.id()).orElseThrow(), profile.name());
        return Command.SINGLE_SUCCESS;
    }
}
