package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.moderation.data.MuteRecord;
import com.panita.enriquecraft.moderation.message.MuteView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Muting and unmuting players, and the question the chat asks before it lets a player say something. A muted player cannot write
 * in the chat or send private messages and emotes; the chat module decides which channels are still open to them.
 */
public final class MuteService {

    private final MuteBook book;
    private final MuteView view;
    private final Supplier<Instant> clock;

    public MuteService(MuteBook book, MuteView view, Supplier<Instant> clock) {
        this.book = book;
        this.view = view;
        this.clock = clock;
    }

    /** Whether the player is muted; when they are, they are told so. */
    public boolean blocks(ServerPlayer player) {
        Optional<MuteRecord> mute = book.activeFor(player.getUUID());
        mute.ifPresent(record -> view.notice(player, record, clock.get()));
        return mute.isPresent();
    }

    /** @return how many players were muted, as a command reports it */
    public int mute(CommandSourceStack source, Collection<NameAndId> targets, PunishmentTerms terms) {
        int muted = 0;
        for (NameAndId target : targets) {
            Optional<MuteRecord> mute = book.mute(target.id(), target.name(), terms.duration(), source.getTextName(), terms.reason());
            if (mute.isEmpty()) {
                view.alreadyMuted(source);
                continue;
            }
            muted++;
            view.muted(source, mute.get(), terms);
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(target.id());
            if (online != null) {
                view.notice(online, mute.get(), clock.get());
            }
        }
        return muted;
    }

    /** @return how many players were unmuted, as a command reports it */
    public int unmute(CommandSourceStack source, String name) {
        List<MuteRecord> lifted = book.unmute(name);
        if (lifted.isEmpty()) {
            view.notMuted(source);
            return 0;
        }
        for (MuteRecord record : lifted) {
            view.unmuted(source, record.name());
            ServerPlayer online = source.getServer().getPlayerList().getPlayer(record.id());
            if (online != null) {
                view.unmutedNotice(online);
            }
        }
        return lifted.size();
    }

    /** The names of the players who are muted now, for the command to suggest. */
    public List<String> mutedNames() {
        return book.mutedNames();
    }
}
