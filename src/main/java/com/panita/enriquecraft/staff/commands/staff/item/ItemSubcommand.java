package com.panita.enriquecraft.staff.commands.staff.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.commands.staff.StaffCommand;
import com.panita.enriquecraft.staff.gui.CustomItemsMenu;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff item}: opens the menu of saved custom items. The subcommands save, remove and
 * describe them.
 */
@CommandSpec(name = "item", parent = StaffCommand.class, description = StaffMessages.Items.DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class ItemSubcommand implements ModCommand {

    private final PlayerOnly playerOnly;
    private final MenuFactory menuFactory;
    private final CustomItemService service;
    private final CustomItemView view;

    public ItemSubcommand(PlayerOnly playerOnly, MenuFactory menuFactory, CustomItemService service,
                          CustomItemView view) {
        this.playerOnly = playerOnly;
        this.menuFactory = menuFactory;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(playerOnly.executes(this::openMenu));
    }

    private int openMenu(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        new CustomItemsMenu(menuFactory, service, view).open(player);
        return Command.SINGLE_SUCCESS;
    }
}
