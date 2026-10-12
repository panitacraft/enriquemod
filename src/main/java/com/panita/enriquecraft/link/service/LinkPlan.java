package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import net.minecraft.server.players.NameAndId;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * What has to change in the whitelist for it to match the list of the Panita API. It compares the list with the
 * whitelist itself, not only with the previous list, so a change that did not happen (a player was online, Mojang
 * did not answer) or that staff undid by hand is found again the next time. The previous list is only used for
 * what the whitelist cannot say: who left the list, and who changed their name.
 * <p>
 * Pure logic over names and UUIDs; nothing here touches the server.
 *
 * @param actions    what to do, in the order of the list
 * @param unchanged  how many players of the list already have the right entry
 * @param duplicates players left out because another player of the list has the same name
 * @param leaving    how many players of the previous list are no longer in the new one
 */
public record LinkPlan(List<Action> actions, int unchanged, List<LinkedPlayer> duplicates, int leaving) {

    public sealed interface Action {
        /** The player has no entry. */
        record Add(LinkedPlayer player) implements Action {
        }

        /** The player has an entry, but not the right one: the other mode, or another capitalization of an offline name. */
        record Replace(LinkedPlayer player, NameAndId existing) implements Action {
        }

        /** The player changed their name since the previous list. */
        record Rename(LinkedPlayer player, String previousName) implements Action {
        }

        /** The player left the list, and the entry the server gave them is still there. */
        record Remove(LinkedPlayer player, NameAndId entry) implements Action {
        }
    }

    /**
     * @param previous  the list that was applied last, empty the first time
     * @param desired   the list the API gave now
     * @param whitelist the entries the whitelist has now
     */
    public static LinkPlan compute(List<LinkedPlayer> previous, List<LinkedPlayer> desired, Collection<NameAndId> whitelist) {
        Map<String, NameAndId> entries = new HashMap<>();
        whitelist.forEach(entry -> entries.putIfAbsent(key(entry.name()), entry));
        Map<Integer, LinkedPlayer> before = new HashMap<>();
        previous.forEach(player -> before.put(player.userId(), player));

        List<LinkedPlayer> kept = new ArrayList<>();
        List<LinkedPlayer> duplicates = new ArrayList<>();
        Set<String> names = new HashSet<>();
        for (LinkedPlayer player : desired) {
            (names.add(key(player.ign())) ? kept : duplicates).add(player);
        }

        List<Action> actions = new ArrayList<>();
        int unchanged = 0;
        for (LinkedPlayer player : kept) {
            LinkedPlayer old = before.get(player.userId());
            NameAndId entry = entries.get(key(player.ign()));
            if (old != null && !old.ign().equals(player.ign())) {
                actions.add(new Action.Rename(player, old.ign()));
            } else if (entry == null) {
                actions.add(new Action.Add(player));
            } else if (isRightEntry(player, entry)) {
                unchanged++;
            } else {
                actions.add(new Action.Replace(player, entry));
            }
        }

        Set<Integer> ids = new HashSet<>();
        desired.forEach(player -> ids.add(player.userId()));
        int leaving = 0;
        for (LinkedPlayer gone : previous) {
            if (ids.contains(gone.userId())) {
                continue;
            }
            leaving++;
            NameAndId entry = entries.get(key(gone.ign()));
            // The name may have passed to another player of the list, whose entry must stay.
            if (entry != null && !names.contains(key(gone.ign()))) {
                actions.add(new Action.Remove(gone, entry));
            }
        }
        return new LinkPlan(List.copyOf(actions), unchanged, List.copyOf(duplicates), leaving);
    }

    /**
     * Whether a list that removes this many players should be applied. A list that takes most of the players away is
     * more likely a mistake than a real change, and removing them would kick people out for good.
     */
    public static boolean removesTooMany(int previousCount, int leaving, int maxPercent) {
        return previousCount > 0 && (long) leaving * 100 > (long) previousCount * maxPercent;
    }

    /**
     * A premium name keeps the UUID Mojang gave it, which cannot be told from here, so the mode is all that can be
     * compared. An offline UUID depends on the exact spelling, so that must match too.
     */
    private static boolean isRightEntry(LinkedPlayer player, NameAndId entry) {
        if (player.mode() == AuthMode.PREMIUM) {
            return AuthMode.of(entry) == AuthMode.PREMIUM;
        }
        return entry.equals(NameAndId.createOffline(player.ign()));
    }

    private static String key(String name) {
        return name.toLowerCase(Locale.ROOT);
    }
}
