package com.panita.enriquecraft.auth.commands.pin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.AdminView;
import com.panita.enriquecraft.auth.message.AuthMessages;
import com.panita.enriquecraft.auth.service.PinChanges;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;

/**
 * {@code /pin change}: opens the PIN screen to change the player's PIN. The current PIN is asked first, so
 * someone who borrows an open game cannot lock the owner out.
 */
@CommandSpec(name = "change", parent = PinCommand.class, description = AuthMessages.Commands.CHANGE_DESCRIPTION)
public final class PinChangeSubcommand implements ModCommand {

    private final PlayerOnly playerOnly;
    private final PinChanges changes;
    private final AdminView view;

    public PinChangeSubcommand(PlayerOnly playerOnly, PinChanges changes, AdminView view) {
        this.playerOnly = playerOnly;
        this.changes = changes;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::change));
    }

    private int change(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        if (!source.getServer().isDedicatedServer()) {
            view.notAServer(source);
            return 0;
        }
        if (AuthMode.of(player.getUUID(), player.getGameProfile().name()) == AuthMode.PREMIUM) {
            view.premiumHasNoPin(source);
            return 0;
        }
        switch (changes.begin(player)) {
            case NO_PIN -> view.noPinYet(source);
            case BLOCKED -> view.changeBlocked(source);
            case OPENED -> {
                return Command.SINGLE_SUCCESS;
            }
        }
        return 0;
    }
}
