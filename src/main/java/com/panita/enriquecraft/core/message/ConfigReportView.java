package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.framework.config.ConfigIssue;
import com.panita.enriquecraft.core.framework.config.ConfigReport;
import net.minecraft.commands.CommandSourceStack;

/**
 * Shows the outcome of a config reload to a command sender. Only the paths of the problems are
 * shown; the detailed reasons are in the console log.
 */
public final class ConfigReportView {

    private final Messenger messenger;

    public ConfigReportView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void send(CommandSourceStack source, ConfigReport report) {
        if (report.isClean()) {
            messenger.send(source, Message.success(Messages.Reload.SUCCESS).prefixed());
            return;
        }
        if (report.hasSyntaxError()) {
            messenger.send(source, Message.error(Messages.Reload.SYNTAX_ERROR).prefixed());
            return;
        }
        messenger.send(source, Message.warning(Messages.Reload.WITH_ISSUES).prefixed().with("count", report.issues().size()));
        for (ConfigIssue issue : report.issues()) {
            String template = issue.kind() == ConfigIssue.Kind.UNKNOWN_KEY
                    ? Messages.Reload.UNKNOWN_KEY
                    : Messages.Reload.INVALID_VALUE;
            messenger.send(source, Message.plain(template).with("path", issue.path()));
        }
    }
}
