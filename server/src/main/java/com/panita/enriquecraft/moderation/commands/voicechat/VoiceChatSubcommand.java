package com.panita.enriquecraft.moderation.commands.voicechat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.commands.enriquecraft.EnriquecraftCommand;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/** {@code /enriquecraft voicechat}: groups the commands that silence and free the voice chat. It does nothing on its own. */
@CommandSpec(name = "voicechat", parent = EnriquecraftCommand.class, description = ModerationMessages.VoiceChat.DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class VoiceChatSubcommand implements ModCommand {

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        // Only its subcommands do anything
    }
}
