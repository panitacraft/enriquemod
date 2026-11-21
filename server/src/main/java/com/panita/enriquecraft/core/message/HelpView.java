package com.panita.enriquecraft.core.message;

import com.panita.enriquecraft.core.framework.command.CommandEntry;
import com.panita.enriquecraft.core.service.HelpService;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

import java.util.Optional;

/**
 * Shows help to a command sender: the list of commands they may run, or the usage of one command.
 */
public final class HelpView {

    private final Messenger messenger;
    private final MessageFormatter formatter;
    private final HelpService helpService;

    public HelpView(Messenger messenger, MessageFormatter formatter, HelpService helpService) {
        this.formatter = formatter;
        this.messenger = messenger;
        this.helpService = helpService;
    }

    public void sendOverview(CommandSourceStack source) {
        messenger.send(source, Message.plain(Messages.Help.HEADER).prefixed());
        for (CommandEntry entry : helpService.visibleEntries(source)) {
            messenger.send(source, Message.plain(Messages.Help.ENTRY)
                    .with("command", entry.displayPath())
                    .with("description", description(entry)));
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
                .with("description", description(entry.get())));
        for (String usage : helpService.usages(source, entry.get())) {
            messenger.send(source, Message.plain(Messages.Help.USAGE_LINE).with("usage", usage));
        }
        return true;
    }

    /** What a command is for, as the administrator wants it said: the description is the default text of a message. */
    private Component description(CommandEntry entry) {
        return formatter.format(Message.plain(entry.spec().description()));
    }
}
