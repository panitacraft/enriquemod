package com.panita.enriquecraft.discord.message;

import com.panita.enriquecraft.core.message.Message;
import com.panita.enriquecraft.core.message.Messenger;
import com.panita.enriquecraft.discord.service.ImageDecoder;
import com.panita.enriquecraft.discord.service.MediaUploader;
import net.minecraft.server.level.ServerPlayer;

/** What a player is told while a screenshot they chose to share goes out. Presentation only. */
public final class ShareView {

    private final Messenger messenger;

    public ShareView(Messenger messenger) {
        this.messenger = messenger;
    }

    public void sending(ServerPlayer player) {
        send(player, DiscordMessages.Share.SENDING);
    }

    public void sentToDiscord(ServerPlayer player) {
        send(player, DiscordMessages.Share.SENT_DISCORD);
    }

    public void sentToChat(ServerPlayer player) {
        send(player, DiscordMessages.Share.SENT_CHAT);
    }

    public void unavailable(ServerPlayer player) {
        send(player, DiscordMessages.Share.UNAVAILABLE);
    }

    public void busy(ServerPlayer player) {
        send(player, DiscordMessages.Share.BUSY);
    }

    public void tooFast(ServerPlayer player) {
        send(player, DiscordMessages.Share.TOO_FAST);
    }

    public void tooLarge(ServerPlayer player) {
        send(player, DiscordMessages.Share.TOO_LARGE);
    }

    public void invalid(ServerPlayer player) {
        send(player, DiscordMessages.Share.INVALID);
    }

    /** Why the screenshot could not be shared, from the failure of the decoding or of the bot. */
    public void failed(ServerPlayer player, Throwable error) {
        String template = switch (error) {
            case ImageDecoder.DecodeException decode -> decode.failure() == ImageDecoder.Failure.TOO_LARGE
                    ? DiscordMessages.Share.TOO_LARGE : DiscordMessages.Share.INVALID;
            case MediaUploader.UploadException upload -> switch (upload.failure()) {
                case TOO_LARGE -> DiscordMessages.Share.TOO_LARGE;
                case RATE -> DiscordMessages.Share.TOO_FAST;
                case TYPE, BAD_REQUEST -> DiscordMessages.Share.INVALID;
                case AUTH, CHANNEL, UNAVAILABLE -> DiscordMessages.Share.UNAVAILABLE;
            };
            default -> DiscordMessages.Share.FAILED;
        };
        send(player, template);
    }

    private void send(ServerPlayer player, String template) {
        messenger.send(player, Message.plain(template).prefixed());
    }
}
