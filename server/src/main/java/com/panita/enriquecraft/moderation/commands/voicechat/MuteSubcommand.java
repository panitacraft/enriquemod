package com.panita.enriquecraft.moderation.commands.voicechat;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.moderation.message.ModerationMessages;
import com.panita.enriquecraft.moderation.service.VoiceChatService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/** {@code /enriquecraft voicechat mute <player|all>}: the player, or everybody but the staff, cannot speak in the voice chat. */
@CommandSpec(name = "mute", parent = VoiceChatSubcommand.class, description = ModerationMessages.VoiceChat.MUTE_DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class MuteSubcommand implements ModCommand {

    private final VoiceChatService voiceChat;

    public MuteSubcommand(VoiceChatService voiceChat) {
        this.voiceChat = voiceChat;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        VoiceChatTargets.attach(builder, voiceChat::muteAll, voiceChat::mute);
    }
}
