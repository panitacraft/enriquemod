package com.panita.enriquecraft.profile.client;

import java.util.concurrent.CompletableFuture;

/**
 * The part of the Panita API the profile module reads: a player's emblems and names. The future never fails; every problem is a
 * {@link Result.Failed}, so callers have one place to decide what to do when the answer cannot be had.
 */
public interface EmblemApi {

    sealed interface Result {
        record Found(ModProfile profile) implements Result {
        }

        /** The API has no active account with that id. */
        record NotFound() implements Result {
        }

        /** The answer could not be had. The reason is for the console, never for players. */
        record Failed(String reason) implements Result {
        }
    }

    /**
     * @param userId    the Panita account
     * @param editionId the edition whose emblems are wanted
     */
    CompletableFuture<Result> profile(int userId, String editionId);
}
