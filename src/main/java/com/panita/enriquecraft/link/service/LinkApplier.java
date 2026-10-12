package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.auth.service.PlayerDataMigration;
import com.panita.enriquecraft.auth.service.WhitelistAdmin;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.message.LinkView;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.PlayerList;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Carries out a {@link LinkPlan} on the whitelist, in two steps because only one of them may run on the server
 * thread: {@link #resolve} finds the entry of every player, which may mean asking Mojang and waiting for the answer,
 * and {@link #apply} changes the whitelist, which only the server thread may do.
 */
public final class LinkApplier {

    /** An action with the entry it puts in the whitelist; removals need none. */
    public record Resolved(LinkPlan.Action action, Optional<WhitelistAdmin.Resolution> resolution) {
    }

    private final WhitelistAdmin admin;
    private final PlayerDataMigration migration;
    private final LinkView view;

    public LinkApplier(WhitelistAdmin admin, PlayerDataMigration migration, LinkView view) {
        this.admin = admin;
        this.migration = migration;
        this.view = view;
    }

    /** Finds the entries the plan needs. Blocks while Mojang answers, so never call it on the server thread. */
    public List<Resolved> resolve(LinkPlan plan) {
        return plan.actions().stream().map(action -> new Resolved(action, switch (action) {
            case LinkPlan.Action.Add add -> Optional.of(entryOf(add.player()));
            case LinkPlan.Action.Replace replace -> Optional.of(entryOf(replace.player()));
            case LinkPlan.Action.Rename rename -> Optional.of(entryOf(rename.player()));
            case LinkPlan.Action.Remove remove -> Optional.<WhitelistAdmin.Resolution>empty();
        })).toList();
    }

    private WhitelistAdmin.Resolution entryOf(LinkedPlayer player) {
        return admin.resolve(player.ign(), Optional.of(player.mode()));
    }

    /** Changes the whitelist. Must run on the server thread. */
    public LinkReport apply(MinecraftServer server, LinkPlan plan, List<Resolved> resolved) {
        LinkReport.Builder report = new LinkReport.Builder(plan.unchanged());
        plan.duplicates().forEach(player -> report.problem(player.ign(), LinkReport.Reason.DUPLICATE_NAME));
        for (Resolved item : resolved) {
            applyOne(server, item, report);
        }
        return report.build();
    }

    private void applyOne(MinecraftServer server, Resolved item, LinkReport.Builder report) {
        PlayerList players = server.getPlayerList();
        switch (item.action()) {
            case LinkPlan.Action.Remove remove -> remove(players, remove, report);
            case LinkPlan.Action.Add add -> withEntry(add.player(), item, report, entry -> {
                admin.apply(players, entry);
                report.added();
            });
            case LinkPlan.Action.Replace replace -> withEntry(replace.player(), item, report, entry -> {
                if (players.getPlayer(replace.existing().id()) != null) {
                    report.waiting(replace.player().ign());
                    return;
                }
                admin.apply(players, entry);
                migration.copy(server::getWorldPath, replace.existing().id(), entry.id());
                report.replaced();
            });
            case LinkPlan.Action.Rename rename -> withEntry(rename.player(), item, report, entry -> {
                rename(server, rename, entry, report);
            });
        }
    }

    private void withEntry(LinkedPlayer player, Resolved item, LinkReport.Builder report,
                           Consumer<NameAndId> action) {
        switch (item.resolution().orElseThrow()) {
            case WhitelistAdmin.Resolution.Entry found -> action.accept(found.entry());
            case WhitelistAdmin.Resolution.NotPremium missing -> report.problem(player.ign(), LinkReport.Reason.NOT_PREMIUM);
            case WhitelistAdmin.Resolution.LookupFailed failed -> report.problem(player.ign(), LinkReport.Reason.LOOKUP_FAILED);
            case WhitelistAdmin.Resolution.InvalidName invalid -> report.problem(player.ign(), LinkReport.Reason.INVALID_NAME);
        }
    }

    private void rename(MinecraftServer server, LinkPlan.Action.Rename rename, NameAndId entry, LinkReport.Builder report) {
        PlayerList players = server.getPlayerList();
        Optional<NameAndId> old = admin.find(players, rename.previousName());
        boolean newUuid = old.isPresent() && !old.get().id().equals(entry.id());
        // A new UUID means the data has to be copied, and the game saves a player's data when they leave.
        if (newUuid && players.getPlayer(old.get().id()) != null) {
            report.waitingRename(rename.player().ign(), rename.player().userId());
            return;
        }
        if (newUuid) {
            migration.copy(server::getWorldPath, old.get().id(), entry.id());
        }
        admin.remove(players, rename.previousName());
        admin.apply(players, entry);
        report.renamed();
    }

    private void remove(PlayerList players, LinkPlan.Action.Remove remove, LinkReport.Builder report) {
        admin.remove(players, remove.entry().name());
        report.removed();
        ServerPlayer online = players.getPlayer(remove.entry().id());
        // Whether the player may still be in depends on the server: with the whitelist off, nobody is let out.
        if (online != null && !players.isWhiteListed(online.nameAndId())) {
            online.connection.disconnect(view.deactivated());
        }
    }
}
