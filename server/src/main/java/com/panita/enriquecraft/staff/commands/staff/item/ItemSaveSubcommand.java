package com.panita.enriquecraft.staff.commands.staff.item;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.message.PlayerOnly;
import com.panita.enriquecraft.staff.message.CustomItemView;
import com.panita.enriquecraft.staff.message.StaffMessages;
import com.panita.enriquecraft.staff.service.CustomItemService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.permissions.PermissionLevel;
import net.minecraft.world.item.ItemStack;

import java.time.Instant;

/**
 * {@code /staff item save <name>}: saves the item in the main hand as a custom item and marks the
 * held item too.
 */
@CommandSpec(name = "save", parent = ItemSubcommand.class, description = StaffMessages.Items.SAVE_DESCRIPTION,
        access = PermissionLevel.GAMEMASTERS)
public final class ItemSaveSubcommand implements ModCommand {

    private static final String NAME_ARGUMENT = "name";

    private final PlayerOnly playerOnly;
    private final CustomItemService service;
    private final CustomItemView view;

    public ItemSaveSubcommand(PlayerOnly playerOnly, CustomItemService service, CustomItemView view) {
        this.playerOnly = playerOnly;
        this.service = service;
        this.view = view;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.then(Commands.argument(NAME_ARGUMENT, StringArgumentType.word())
                .executes(playerOnly.executes(this::save)));
    }

    private int save(CommandContext<CommandSourceStack> context, ServerPlayer player) {
        CommandSourceStack source = context.getSource();
        String name = StringArgumentType.getString(context, NAME_ARGUMENT);
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) {
            view.emptyHand(source);
            return 0;
        }
        CustomItemService.AddResult result = service.save(held, name, player.getUUID(),
                player.getName().getString(), Instant.now());
        if (result != CustomItemService.AddResult.ADDED) {
            view.rejected(source, name, result);
            return 0;
        }
        view.saved(source, service.find(name).orElseThrow());
        return Command.SINGLE_SUCCESS;
    }
}
