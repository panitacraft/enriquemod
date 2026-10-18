package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.PlayerInfo;

import java.util.concurrent.CompletableFuture;

/** Hands a screenshot to the bot, which posts it to Discord and says where it ended up. Behind an interface so a fake stands in for the bot. */
public interface MediaUploader {

    /** Where the bot posts the image: to the channel's webhook under the player's name, or to its private channel only to get an address. */
    enum Destination {
        DISCORD("discord"),
        MEDIA("media");

        private final String id;

        Destination(String id) {
            this.id = id;
        }

        public String id() {
            return id;
        }
    }

    /** @param channel the id of a chat channel the bot bridges */
    record Request(PlayerInfo player, String channel, String caption, Destination destination, byte[] jpeg) {
    }

    /** What the bot answered: the address of the posted image and the message to delete it with. */
    record Uploaded(String url, String type, long size, int width, int height, String messageId) {
    }

    /** Why the bot did not take the image, with the code the bot gave. */
    enum Failure {
        BAD_REQUEST,
        AUTH,
        CHANNEL,
        TOO_LARGE,
        TYPE,
        RATE,
        UNAVAILABLE
    }

    final class UploadException extends Exception {
        private final Failure failure;

        public UploadException(Failure failure, Throwable cause) {
            super(failure.name(), cause);
            this.failure = failure;
        }

        public Failure failure() {
            return failure;
        }
    }

    /** The failure is an {@link UploadException}, seen as the cause of the {@code CompletionException}. */
    CompletableFuture<Uploaded> upload(Request request);
}
