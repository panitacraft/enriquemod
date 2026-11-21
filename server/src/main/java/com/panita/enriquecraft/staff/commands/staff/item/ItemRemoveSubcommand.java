package com.panita.enriquecraft.staff.commands.staff.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /staff item remove <name>}: deletes a saved custom item. Items already handed out keep
 * their mark.
 */
@CommandSpec(name = "remove", parent = ItemSubcommand.class, description = StaffMessages.Items.REMOVE_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class ItemRemoveSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final CustomItemService service;
    private final CustomItemView view;

    public ItemRemoveSubcommand(CustomItemService service, CustomItemView view) {
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .suggests(CommandSuggestions.forSource(source -> service.names()))
                .executes(this::remove));
    }

    private int remove(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        if (!service.remove(name)) {
            view.notFound(context.getSource(), name);
            return 0;
        }
        view.removed(context.getSource(), name);
        return Command.SINGLE_SUCCESS;
    }
}
