package com.panita.enriquecraft.staff.commands.staff;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.gui.DeathListMenu;
import com.panita.enriquecraft.staff.message.DeathInventoryView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.DeathInventoryService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.server.players.NameAndId;

import java.util.Collection;

/**
 * {@code /staff invrestore <player>}: opens the death inventories of a player, online or not, to
 * inspect them and to recover them.
 */
@CommandSpec(name = "invrestore", parent = StaffCommand.class, description = StaffMessages.Deaths.DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class InvRestoreSubcommand implements ModCommand {

    private static final String PLAYER_ARGUMENT = "player";

    private final PlayerOnly playerOnly;
    private final MenuFactory menuFactory;
    private final DeathInventoryService service;
    private final DeathInventoryView view;

    public InvRestoreSubcommand(PlayerOnly playerOnly, MenuFactory menuFactory, DeathInventoryService service,
                                DeathInventoryView view) {
        this.playerOnly = playerOnly;
        this.menuFactory = menuFactory;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(PLAYER_ARGUMENT, GameProfileArgument.gameProfile())
                .executes(playerOnly.executes(this::open)));
    }

    private int open(CommandContext<CommandSourceStack> context, ServerPlayer staff) throws CommandSyntaxException {
        Collection<NameAndId> profiles = GameProfileArgument.getGameProfiles(context, PLAYER_ARGUMENT);
        if (profiles.size() != 1) {
            view.singlePlayerRequired(context.getSource());
            return 0;
        }
        NameAndId target = profiles.iterator().next();
        new DeathListMenu(menuFactory, service, view, target.id(), target.name()).open(staff);
        return Command.SINGLE_SUCCESS;
    }
}
