package com.panita.enriquecraft.staff.commands.staff;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff}: root of the staff tools. Running it alone shows what it can do.
 */
@CommandSpec(name = "staff", description = StaffMessages.Staff.DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class StaffCommand implements ModCommand {

    private static final String NAME = "staff";

    private final HelpView helpView;

    public StaffCommand(HelpView helpView) {
        this.helpView = helpView;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            helpView.sendUsage(context.getSource(), NAME);
            return Command.SINGLE_SUCCESS;
        });
    }
}
