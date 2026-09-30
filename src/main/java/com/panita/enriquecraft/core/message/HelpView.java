package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.framework.command.CommandEntry;
import com.panita.enriquecraft.core.service.HelpService;
import net.minecraft.commands.CommandSourceStack;

import java.util.Optional;

/**
 * Shows help to a command sender: the list of commands they may run, or the usage of one command.
 */
public final class HelpView {

    private final Messenger messenger;
    private final HelpService helpService;

    public HelpView(Messenger messenger, HelpService helpService) {
        this.messenger = messenger;
        this.helpService = helpService;
    }

    public void sendOverview(CommandSourceStack source) {
        messenger.send(source, Message.plain(Messages.Help.HEADER).prefixed());
        for (CommandEntry entry : helpService.visibleEntries(source)) {
            messenger.send(source, Message.plain(Messages.Help.ENTRY)
                    .with("command", entry.displayPath())
                    .with("description", entry.spec().description()));
        }
    }

    /**
     * Shows the usage of a top-level command, or an error when the sender has no such command.
     *
     * @return whether the command was found
     */
    public boolean sendUsage(CommandSourceStack source, String literal) {
        Optional<CommandEntry> entry = helpService.findTopLevel(source, literal);
        if (entry.isEmpty()) {
            messenger.send(source, Message.error(Messages.Help.UNKNOWN_COMMAND).prefixed().with("command", literal));
            return false;
        }
        messenger.send(source, Message.plain(Messages.Help.USAGE_HEADER)
                .with("command", entry.get().displayPath())
                .with("description", entry.get().spec().description()));
        for (String usage : helpService.usages(source, entry.get())) {
            messenger.send(source, Message.plain(Messages.Help.USAGE_LINE).with("usage", usage));
        }
        return true;
    }
}
