package com.panita.enriquecraft.link.service;

/**
 * How a sync ended.
 */
public sealed interface SyncOutcome {

    /**
     * The whitelist was brought in line with the list.
     *
     * @param report what was done
     * @param fresh  whether the API sent a list, or said that the one the server has is still current
     */
    record Applied(LinkReport report, boolean fresh) implements SyncOutcome {
    }

    /** The new list would remove so many players that it was not applied. */
    record Refused(int leaving, int linked, int maxPercent) implements SyncOutcome {
    }

    /** The list could not be had. The reason is for the console, never for players. */
    record Failed(String reason) implements SyncOutcome {
    }

    /** There is no service key, so the API cannot be asked. */
    record NoKey() implements SyncOutcome {
    }

    /** Another sync was still running. */
    record Busy() implements SyncOutcome {
    }
}
