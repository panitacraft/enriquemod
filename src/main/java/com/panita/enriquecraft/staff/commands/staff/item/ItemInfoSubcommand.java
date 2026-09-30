package com.panita.enriquecraft.staff.commands.staff.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.CommandSuggestions;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.staff.data.SavedItem;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.permissions.PermissionLevel;

import java.util.Optional;

/**
 * {@code /staff item info <name>}: shows who saved a custom item, when, and what it is.
 */
@CommandSpec(name = "info", parent = ItemSubcommand.class, description = StaffMessages.Items.INFO_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class ItemInfoSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final CustomItemService service;
    private final CustomItemView view;

    public ItemInfoSubcommand(CustomItemService service, CustomItemView view) {
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .suggests(CommandSuggestions.forSource(source -> service.names()))
                .executes(this::info));
    }

    private int info(CommandContext<CommandSourceStack> context) {
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        Optional<SavedItem> item = service.find(name);
        if (item.isEmpty()) {
            view.notFound(context.getSource(), name);
            return 0;
        }
        view.info(context.getSource(), item.get());
        return Command.SINGLE_SUCCESS;
    }
}
