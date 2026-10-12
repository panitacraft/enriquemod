package com.panita.enriquecraft.link.service;

import java.util.ArrayList;
import java.util.List;

/**
 * What applying a list did, for staff and for the console.
 *
 * @param added            players put in the whitelist
 * @param replaced         players whose entry was switched to the right mode or spelling
 * @param renamed          players whose entry now has their new name
 * @param removed          players taken out because they left the list
 * @param unchanged        players that already had the right entry
 * @param waiting          players whose change waits until they are offline, so their data is copied whole
 * @param waitingRenameIds the Panita ids of the players of {@code waiting} that were to be renamed
 * @param problems         players that could not be handled, and why
 */
public record LinkReport(int added, int replaced, int renamed, int removed, int unchanged, List<String> waiting,
                         List<Integer> waitingRenameIds, List<Problem> problems) {

    public enum Reason {
        /** No Microsoft account has the name, though the member said it is one. */
        NOT_PREMIUM,
        /** Mojang could not be asked; the player is tried again next time. */
        LOOKUP_FAILED,
        /** The name cannot belong to a player. */
        INVALID_NAME,
        /** Another player of the list has the same name. */
        DUPLICATE_NAME
    }

    public record Problem(String name, Reason reason) {
    }

    public boolean changedAnything() {
        return added + replaced + renamed + removed > 0;
    }

    /** Collects the outcome of the actions of one sync. */
    static final class Builder {
        private int added;
        private int replaced;
        private int renamed;
        private int removed;
        private final int unchanged;
        private final List<String> waiting = new ArrayList<>();
        private final List<Integer> waitingRenameIds = new ArrayList<>();
        private final List<Problem> problems = new ArrayList<>();

        Builder(int unchanged) {
            this.unchanged = unchanged;
        }

        void added() {
            added++;
        }

        void replaced() {
            replaced++;
        }

        void renamed() {
            renamed++;
        }

        void removed() {
            removed++;
        }

        void waiting(String name) {
            waiting.add(name);
        }

        void waitingRename(String name, int userId) {
            waiting.add(name);
            waitingRenameIds.add(userId);
        }

        void problem(String name, Reason reason) {
            problems.add(new Problem(name, reason));
        }

        LinkReport build() {
            return new LinkReport(added, replaced, renamed, removed, unchanged, List.copyOf(waiting),
                    List.copyOf(waitingRenameIds), List.copyOf(problems));
        }
    }
}
