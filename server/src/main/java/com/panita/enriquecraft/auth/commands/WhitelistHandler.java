package com.panita.enriquecraft.auth.commands;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.message.WhitelistView;
import com.panita.enriquecraft.auth.service.PlayerDataMigration;
import com.panita.enriquecraft.auth.service.WhitelistAdmin;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Runs the whitelist commands that the auth module takes over from the game: {@code /whitelist add} and
 * {@code remove}, and {@code /auth mode}. It reads what staff typed and delegates, as a command does; it is
 * not a {@code ModCommand} because the commands themselves are the game's, rewired to it by a mixin.
 * <p>
 * Names are taken exactly as typed: the game lowercases the names it does not know, and an offline UUID
 * depends on the capitalization.
 */
public final class WhitelistHandler {

    private final WhitelistAdmin admin;
    private final PlayerDataMigration migration;
    private final WhitelistView view;

    public WhitelistHandler(WhitelistAdmin admin, PlayerDataMigration migration, WhitelistView view) {
        this.admin = admin;
        this.migration = migration;
        this.view = view;
    }

    /**
     * Adds players to the whitelist, detecting how each logs in unless staff say so.
     *
     * @return how many players were added or changed
     */
    public int add(CommandSourceStack source, Collection<String> names, Optional<AuthMode> requested) {
        int changed = 0;
        for (String name : names) {
            changed += addOne(source, name, requested) ? 1 : 0;
        }
        return changed;
    }

    /** Switches a player who is already in the whitelist to the other mode. */
    public int setMode(CommandSourceStack source, String name, AuthMode mode) {
        if (admin.find(source.getServer().getPlayerList(), name).isEmpty()) {
            view.notListed(source, name);
            return 0;
        }
        return add(source, List.of(name), Optional.of(mode));
    }

    private boolean addOne(CommandSourceStack source, String name, Optional<AuthMode> requested) {
        switch (admin.resolve(name, requested)) {
            case WhitelistAdmin.Resolution.InvalidName invalid -> view.invalidName(source, invalid.name());
            case WhitelistAdmin.Resolution.NotPremium notPremium -> view.notPremium(source, notPremium.name());
            case WhitelistAdmin.Resolution.LookupFailed failed -> view.lookupFailed(source, failed.name());
            case WhitelistAdmin.Resolution.Entry found -> {
                return apply(source, found.entry());
            }
        }
        return false;
    }

    private boolean apply(CommandSourceStack source, NameAndId entry) {
        MinecraftServer server = source.getServer();
        PlayerList players = server.getPlayerList();
        Optional<NameAndId> previous = admin.find(players, entry.name());
        AuthMode mode = AuthMode.of(entry);
        boolean switching = previous.isPresent() && !previous.get().id().equals(entry.id());
        // The game saves a player's data when they leave, so what is copied must come after that.
        if (switching && players.getPlayer(previous.get().id()) != null) {
            view.mustBeOffline(source, entry.name());
            return false;
        }
        WhitelistAdmin.Change change = admin.apply(players, entry);
        switch (change) {
            case UNCHANGED -> view.unchanged(source, entry.name(), mode);
            case ADDED -> view.added(source, entry.name(), mode);
            case REPLACED -> view.replaced(source, entry.name(), mode,
                    migration.copy(server::getWorldPath, previous.orElseThrow().id(), entry.id()));
        }
        return change != WhitelistAdmin.Change.UNCHANGED;
    }

    /**
     * Takes players out of the whitelist by name, which finds them whatever their mode.
     *
     * @return how many players were removed
     */
    public int remove(CommandSourceStack source, Collection<String> names) {
        MinecraftServer server = source.getServer();
        int removed = 0;
        for (String name : names) {
            if (admin.remove(server.getPlayerList(), name).isEmpty()) {
                view.notListed(source, name);
            } else {
                view.removed(source, name);
                removed++;
            }
        }
        if (removed > 0) {
            server.kickUnlistedPlayers();
        }
        return removed;
    }
}
