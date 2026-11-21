package com.panita.enriquecraft.auth.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserWhiteList;
import net.minecraft.server.players.UserWhiteListEntry;
import net.minecraft.util.StringUtil;

import java.time.Duration;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * What staff do to the whitelist, mode aware. An entry carries the way its player logs in in its UUID
 * (see {@link AuthMode#of}), so adding a player means choosing the right UUID: the real one of a Microsoft
 * account, or the offline one of a name nobody proves.
 */
public final class WhitelistAdmin {

    /** The entry to keep for a player staff want to add. */
    public sealed interface Resolution {
        record Entry(NameAndId entry) implements Resolution {
        }

        /** Premium was asked for, but no Microsoft account has the name. */
        record NotPremium(String name) implements Resolution {
        }

        /** Mojang could not say whether an account has the name, and staff did not say which mode it is. */
        record LookupFailed(String name) implements Resolution {
        }

        /** The name cannot belong to any player. */
        record InvalidName(String name) implements Resolution {
        }
    }

    /** What putting an entry in the whitelist did. */
    public enum Change {
        ADDED, REPLACED, UNCHANGED
    }

    /**
     * How to change the whitelist so that it holds an entry.
     *
     * @param replaced the entries of the same name with another UUID, which are removed
     * @param add      whether the entry must be added
     */
    record Plan(List<NameAndId> replaced, boolean add) {
        Change change() {
            if (!add) {
                return Change.UNCHANGED;
            }
            return replaced.isEmpty() ? Change.ADDED : Change.REPLACED;
        }
    }

    private static final Duration LOOKUP_WAIT = Duration.ofSeconds(5);

    private final PremiumLookup lookup;

    public WhitelistAdmin(PremiumLookup lookup) {
        this.lookup = lookup;
    }

    /**
     * Decides the entry for a player. The name must be exactly as staff typed it: an offline UUID depends on
     * the capitalization, and the game itself lowercases the names it does not know, so it cannot be asked.
     *
     * @param name      the name staff typed
     * @param requested the mode staff asked for; empty means detect it
     */
    public Resolution resolve(String name, Optional<AuthMode> requested) {
        if (name.isEmpty() || !StringUtil.isValidPlayerName(name)) {
            return new Resolution.InvalidName(name);
        }
        if (requested.equals(Optional.of(AuthMode.CRACKED))) {
            return new Resolution.Entry(NameAndId.createOffline(name));
        }
        return switch (ask(name)) {
            case PremiumLookup.Result.Found found -> new Resolution.Entry(found.profile());
            case PremiumLookup.Result.Missing missing -> requested.isPresent()
                    ? new Resolution.NotPremium(name) : new Resolution.Entry(NameAndId.createOffline(name));
            case PremiumLookup.Result.Unavailable unavailable -> new Resolution.LookupFailed(name);
        };
    }

    private PremiumLookup.Result ask(String name) {
        try {
            return lookup.lookup(name).get(LOOKUP_WAIT.toSeconds(), TimeUnit.SECONDS);
        } catch (Exception e) {
            return new PremiumLookup.Result.Unavailable();
        }
    }

    /** Puts the entry in the whitelist, replacing the entries of the same name that have another UUID. */
    public Change apply(PlayerList players, NameAndId entry) {
        UserWhiteList list = players.getWhiteList();
        Plan plan = plan(list.getEntries().stream().map(UserWhiteListEntry::getUser).toList(), entry);
        plan.replaced().forEach(list::remove);
        if (plan.add()) {
            list.add(new UserWhiteListEntry(entry));
        }
        return plan.change();
    }

    static Plan plan(Collection<NameAndId> existing, NameAndId entry) {
        List<NameAndId> sameName = existing.stream().filter(other -> other.name().equalsIgnoreCase(entry.name())).toList();
        if (sameName.stream().anyMatch(other -> other.id().equals(entry.id()))) {
            return new Plan(List.of(), false);
        }
        return new Plan(sameName, true);
    }

    /** Takes a player out of the whitelist by name, whatever their mode. Returns the entries removed. */
    public List<NameAndId> remove(PlayerList players, String name) {
        UserWhiteList list = players.getWhiteList();
        List<NameAndId> removed = list.getEntries().stream().map(UserWhiteListEntry::getUser)
                .filter(user -> user.name().equalsIgnoreCase(name)).toList();
        removed.forEach(list::remove);
        return removed;
    }

    /** The entry a name has, if any, and so its mode. */
    public Optional<NameAndId> find(PlayerList players, String name) {
        return players.getWhiteList().getEntries().stream().map(UserWhiteListEntry::getUser)
                .filter(user -> user.name().equalsIgnoreCase(name)).findFirst();
    }
}
