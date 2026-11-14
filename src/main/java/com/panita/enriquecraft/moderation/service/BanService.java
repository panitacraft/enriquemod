package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.moderation.message.BanView;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;

import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Bans, written in the game's own ban list ({@code banned-players.json}), which already knows bans that end and forgets them
 * when they do, so {@code /pardon}, {@code /banlist} and every other tool that reads that file keep working. What this adds is
 * how long a ban lasts, and the screen the banned player sees.
 */
public final class BanService {

    private final BanView view;
    private final Supplier<Instant> clock;

    public BanService(BanView view, Supplier<Instant> clock) {
        this.view = view;
        this.clock = clock;
    }

    /** @return how many players were banned, as a command reports it */
    public int ban(CommandSourceStack source, Collection<NameAndId> targets, PunishmentTerms terms) {
        PlayerList players = source.getServer().getPlayerList();
        UserBanList bans = players.getBans();
        Instant now = clock.get();
        int banned = 0;
        for (NameAndId target : targets) {
            if (bans.isBanned(target)) {
                view.alreadyBanned(source);
                continue;
            }
            Optional<Instant> expires = terms.duration().map(now::plus);
            bans.add(new UserBanListEntry(target, Date.from(now), source.getTextName(), expires.map(Date::from).orElse(null),
                    terms.reason().orElse(null)));
            banned++;
            view.banned(source, target.name(), terms);
            ServerPlayer online = players.getPlayer(target.id());
            if (online != null) {
                online.connection.disconnect(view.screen(expires, terms.reason(), now));
            }
        }
        return banned;
    }

    /** The screen of a player who tries to log in while banned; empty when they are not banned. */
    public Optional<Component> screenFor(UserBanList bans, NameAndId profile) {
        UserBanListEntry entry = bans.get(profile);
        if (entry == null) {
            return Optional.empty();
        }
        return Optional.of(view.screen(Optional.ofNullable(entry.getExpires()).map(Date::toInstant),
                Optional.ofNullable(entry.getReason()).filter(reason -> !reason.isBlank()), clock.get()));
    }
}
