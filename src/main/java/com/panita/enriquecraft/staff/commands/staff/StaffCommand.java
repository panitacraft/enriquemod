package com.panita.enriquecraft.staff.commands.staff;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.HelpView;
import com.panita.enriquecraft.staff.gui.StaffMenus;
import com.panita.enriquecraft.staff.message.StaffMessages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff}: root of the staff tools. A player running it alone gets the staff menu; the console gets
 * the list of what the command can do.
 */
@CommandSpec(name = "staff", description = StaffMessages.Staff.DESCRIPTION, access = PermissionLevel.GAMEMASTERS)
public final class StaffCommand implements ModCommand {

    private static final String NAME = "staff";

    private final HelpView helpView;
    private final StaffMenus menus;

    public StaffCommand(HelpView helpView, StaffMenus menus) {
        this.helpView = helpView;
        this.menus = menus;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(context -> {
            ServerPlayer player = context.getSource().getPlayer();
            if (player != null) {
                menus.open(player);
            } else {
                helpView.sendUsage(context.getSource(), NAME);
            }
            return Command.SINGLE_SUCCESS;
        });
    }
}
