package com.panita.enriquecraft.core.commands.enriquecraft;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.panita.enriquecraft.core.framework.command.CommandSpec;
import com.panita.enriquecraft.core.framework.command.ModCommand;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import com.panita.enriquecraft.core.framework.message.MessageCatalog;
import com.panita.enriquecraft.core.framework.reload.ReloadHooks;
import com.panita.enriquecraft.core.message.ConfigReportView;
import com.panita.enriquecraft.core.message.Messages;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.permissions.PermissionLevel;

/**
 * {@code /enriquecraft reload}: re-reads the config file and the messages file and shows what was wrong with them,
 * if anything, then lets the modules that follow a reload do their part.
 */
@CommandSpec(name = "reload", parent = EnriquecraftCommand.class, description = Messages.Reload.DESCRIPTION,
        access = PermissionLevel.ADMINS)
public final class ReloadSubcommand implements ModCommand {

    private final ConfigManager configManager;
    private final MessageCatalog messages;
    private final ConfigReportView reportView;
    private final ReloadHooks hooks;

    public ReloadSubcommand(ConfigManager configManager, MessageCatalog messages, ConfigReportView reportView, ReloadHooks hooks) {
        this.configManager = configManager;
        this.messages = messages;
        this.reportView = reportView;
        this.hooks = hooks;
    }

    @Override
    public void configure(LiteralArgumentBuilder<CommandSourceStack> builder) {
        builder.executes(this::execute);
    }

    private int execute(CommandContext<CommandSourceStack> context) {
        ConfigReport report = configManager.reload().and(messages.reload());
        reportView.send(context.getSource(), report);
        hooks.fire(context.getSource());
        return report.hasSyntaxError() ? 0 : Command.SINGLE_SUCCESS;
    }
}
