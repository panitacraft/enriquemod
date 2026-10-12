package com.panita.enriquecraft.link.client;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

/**
 * The part of the Panita API the server reads: who may play. The future never fails; every problem is a
 * {@link Result.Failed}, so callers have one place to decide what to do when the list cannot be had.
 */
public interface PanitaApi {

    sealed interface Result {
        /** The current list, with the tag that identifies this version of it. */
        record Players(List<LinkedPlayer> players, Optional<String> etag) implements Result {
        }

        /** The list has not changed since the version the caller already has. */
        record NotModified() implements Result {
        }

        /** The list could not be had. The reason is for the console and for staff, never for players. */
        record Failed(String reason) implements Result {
        }
    }

    /**
     * @param etag the tag of the version the caller has; the API answers {@link Result.NotModified} if it is still current
     */
    CompletableFuture<Result> players(Optional<String> etag);
}
