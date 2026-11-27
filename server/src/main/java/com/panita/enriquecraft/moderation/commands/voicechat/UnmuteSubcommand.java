package com.panita.enriquecraft.moderation.commands.voicechat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.service.VoiceChatService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/** {@code /enriquecraft voicechat unmute <player|all>}: undoes {@code mute}, for the player or for everybody. */
@CommandSpec(name = "unmute", parent = VoiceChatSubcommand.class, description = ModerationMessages.VoiceChat.UNMUTE_DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class UnmuteSubcommand implements ModCommand {

    private final VoiceChatService voiceChat;

    public UnmuteSubcommand(VoiceChatService voiceChat) {
        this.voiceChat = voiceChat;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        VoiceChatTargets.attach(builder, voiceChat::unmuteAll, voiceChat::unmute);
    }
}
