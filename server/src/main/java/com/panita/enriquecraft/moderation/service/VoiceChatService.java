package com.panita.enriquecraft.moderation.service;

import com.mojang.brigadier.Command;
import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.moderation.config.ModerationConfig;
import com.panita.enriquecraft.moderation.message.VoiceChatView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;

import java.util.List;
import java.util.function.Function;

/**
 * Silences and frees the voice chat through LuckPerms. There is no voice chat mod for this game version to ask, so the mod only changes the
 * permission the voice chat checks and leaves the rest to it. The commands run as the console, which leaves the changes in its log, and
 * LuckPerms is told apart from a server without it by whether its command exists.
 */
public final class VoiceChatService {

    private static final String LUCKPERMS_COMMAND = "lp";

    private final ModerationConfig config;
    private final AuthConfig auth;
    private final VoiceChatView view;

    public VoiceChatService(ModerationConfig config, AuthConfig auth, VoiceChatView view) {
        this.config = config;
        this.auth = auth;
        this.view = view;
    }

    public int muteAll(CommandSourceStack source) {
        List<String> staff = staff();
        return run(source, commands().muteAll(staff), () -> view.mutedAll(source, staff.size()));
    }

    public int unmuteAll(CommandSourceStack source) {
        return run(source, commands().unmuteAll(staff()), () -> view.unmutedAll(source));
    }

    public int mute(CommandSourceStack source, String player) {
        return forPlayer(source, player, commands -> commands.mute(player), view::muted);
    }

    public int unmute(CommandSourceStack source, String player) {
        return forPlayer(source, player, commands -> commands.unmute(player), view::unmuted);
    }

    private int forPlayer(CommandSourceStack source, String player, Function<VoiceChatCommands, List<String>> commands,
                          PlayerReport report) {
        if (!VoiceChatCommands.isPlayerName(player)) {
            view.invalidPlayer(source);
            return 0;
        }
        return run(source, commands.apply(commands()), () -> report.send(source, player));
    }

    private int run(CommandSourceStack source, List<String> commands, Runnable report) {
        MinecraftServer server = source.getServer();
        if (server.getCommands().getDispatcher().getRoot().getChild(LUCKPERMS_COMMAND) == null) {
            view.luckPermsMissing(source);
            return 0;
        }
        CommandSourceStack console = server.createCommandSourceStack();
        commands.forEach(command -> server.getCommands().performPrefixedCommand(console, command));
        report.run();
        return Command.SINGLE_SUCCESS;
    }

    /** Read at each use: a reload may have changed them. */
    private VoiceChatCommands commands() {
        return new VoiceChatCommands(config.voiceChatGroup.get(), config.voiceChatSpeakPermission.get());
    }

    private List<String> staff() {
        return auth.staffOnlyPlayers.get();
    }

    @FunctionalInterface
    private interface PlayerReport {
        void send(CommandSourceStack source, String player);
    }
}
