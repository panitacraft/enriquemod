package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.ImageFetcher;
import net.minecraft.server.level.ServerPlayer;

/**
 * What a player is told while an image is loaded onto a map for them. Presentation only.
 */
public final class ImageView {

    private final Messenger messenger;

    public ImageView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void loading(ServerPlayer player) {
        send(player, DiscordMessages.Image.LOADING);
    }

    public void shown(ServerPlayer player) {
        send(player, DiscordMessages.Image.SHOWN);
    }

    public void expired(ServerPlayer player) {
        send(player, DiscordMessages.Image.EXPIRED);
    }

    public void busy(ServerPlayer player) {
        send(player, DiscordMessages.Image.BUSY);
    }

    public void cooldown(ServerPlayer player) {
        send(player, DiscordMessages.Image.COOLDOWN);
    }

    public void unavailable(ServerPlayer player) {
        send(player, DiscordMessages.Image.UNAVAILABLE);
    }

    /** Why the image could not be shown, from the failure of the download or of the decoding. */
    public void failed(ServerPlayer player, Throwable error) {
        String template = switch (error) {
            case ImageFetcher.FetchException fetch -> switch (fetch.failure()) {
                case TOO_LARGE -> DiscordMessages.Image.TOO_LARGE;
                case NOT_AN_IMAGE, UNAVAILABLE -> DiscordMessages.Image.UNAVAILABLE;
            };
            case ImageDecoder.DecodeException decode -> switch (decode.failure()) {
                case UNSUPPORTED -> DiscordMessages.Image.UNSUPPORTED;
                case TOO_LARGE -> DiscordMessages.Image.TOO_LARGE;
                case CORRUPT -> DiscordMessages.Image.FAILED;
            };
            default -> DiscordMessages.Image.FAILED;
        };
        send(player, template);
    }

    private void send(ServerPlayer player, String template) {
        messenger.send(player, Message.plain(template).prefixed());
    }
}
