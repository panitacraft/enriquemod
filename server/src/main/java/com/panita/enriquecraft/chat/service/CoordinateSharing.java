package com.panita.enriquecraft.chat.service;

import com.panita.enriquecraft.auth.service.HeldPlayers;
import com.panita.enriquecraft.chat.config.ChatConfig;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.message.CoordinateShareView;
import com.panita.enriquecraft.moderation.service.MuteService;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

/**
 * Telling players where someone is, with everyone or with one player. A muted player shares nothing, since this is a way of
 * speaking, and a player who has not entered their PIN yet receives nothing, as with chat.
 */
public final class CoordinateSharing {

    private final ChatConfig config;
    private final HeldPlayers held;
    private final MuteService mutes;
    private final CoordinateShareView view;
    private final ShareSounds sounds;

    public CoordinateSharing(ChatConfig config, HeldPlayers held, MuteService mutes, CoordinateShareView view, ShareSounds sounds) {
        this.config = config;
        this.held = held;
        this.mutes = mutes;
        this.view = view;
        this.sounds = sounds;
    }

    /**
     * Tells every player where the sender is.
     *
     * @return how many players were told, as a command reports
     */
    public int withEveryone(ServerPlayer sender) {
        if (!mayShare(sender)) {
            return 0;
        }
        List<ServerPlayer> readers = sender.level().getServer().getPlayerList().getPlayers().stream()
                .filter(player -> !held.isHeld(player))
                .toList();
        view.everyone(sender, readers);
        ChatEvents.COORDINATES.invoker().onCoordinatesShared(sender, sender.blockPosition(), sender.level().dimension());
        readers.forEach(this::playPublicSound);
        return readers.size();
    }

    /**
     * Tells one player where the sender is, and the sender that it was done. A sender who picks themselves is only told their own
     * position.
     *
     * @return 1 when the position was shown, 0 when it was not, as a command reports
     */
    public int withPlayer(ServerPlayer sender, ServerPlayer receiver) {
        if (!mayShare(sender)) {
            return 0;
        }
        if (receiver == sender) {
            view.self(sender);
            playPrivateSound(sender);
            return 1;
        }
        if (held.isHeld(receiver)) {
            view.notOnline(sender, receiver);
            return 0;
        }
        view.toPlayer(sender, receiver);
        view.sent(sender, receiver);
        playPrivateSound(receiver);
        playPrivateSound(sender);
        return 1;
    }

    private boolean mayShare(ServerPlayer sender) {
        if (!config.shareCoordinatesEnabled.get()) {
            view.disabled(sender);
            return false;
        }
        return !held.isHeld(sender) && !mutes.blocks(sender);
    }

    private void playPublicSound(ServerPlayer player) {
        if (config.shareCoordinatesPlaySound.get()) {
            sounds.play(player, config.shareCoordinatesSoundPublic.get());
        }
    }

    private void playPrivateSound(ServerPlayer player) {
        if (config.shareCoordinatesPlaySound.get()) {
            sounds.play(player, config.shareCoordinatesSoundPrivate.get());
        }
    }
}
