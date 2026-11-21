package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.service.PlayerDataMigration;
import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import net.minecraft.commands.CommandSourceStack;

/**
 * What staff are told when they change the whitelist. Presentation only.
 */
public final class WhitelistView {

    private final Messenger messenger;

    public WhitelistView(Messenger messenger) {
        this.messenger = messenger;
    }

    private static String label(AuthMode mode) {
        return mode == AuthMode.PREMIUM ? AuthMessages.Whitelist.MODE_PREMIUM : AuthMessages.Whitelist.MODE_CRACKED;
    }

    public void added(CommandSourceStack source, String name, AuthMode mode) {
        messenger.send(source, Message.success(AuthMessages.Whitelist.ADDED).with("name", name).with("mode", label(mode)).prefixed());
    }

    public void replaced(CommandSourceStack source, String name, AuthMode mode, PlayerDataMigration.Result migration) {
        String template = migration.kept() > 0 ? AuthMessages.Whitelist.REPLACED_KEPT : AuthMessages.Whitelist.REPLACED;
        messenger.send(source, Message.success(template).with("name", name).with("mode", label(mode))
                .with("copied", migration.copied()).with("kept", migration.kept()).prefixed());
    }

    public void unchanged(CommandSourceStack source, String name, AuthMode mode) {
        messenger.send(source, Message.info(AuthMessages.Whitelist.UNCHANGED).with("name", name).with("mode", label(mode)).prefixed());
    }

    public void invalidName(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Whitelist.INVALID_NAME).with("name", name).prefixed());
    }

    public void notPremium(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Whitelist.NOT_PREMIUM).with("name", name).prefixed());
    }

    public void lookupFailed(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Whitelist.LOOKUP_FAILED).with("name", name).prefixed());
    }

    public void mustBeOffline(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Whitelist.MUST_BE_OFFLINE).with("name", name).prefixed());
    }

    public void removed(CommandSourceStack source, String name) {
        messenger.send(source, Message.success(AuthMessages.Whitelist.REMOVED).with("name", name).prefixed());
    }

    public void notListed(CommandSourceStack source, String name) {
        messenger.send(source, Message.error(AuthMessages.Whitelist.NOT_LISTED).with("name", name).prefixed());
    }
}
