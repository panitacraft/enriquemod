package com.panita.enriquecraft.link.service;

import net.minecraft.server.players.NameAndId;

import java.util.Collection;
import java.util.List;

/**
 * The server a sync works on, seen as the things a sync needs, so the sync itself can be tested without a
 * running server.
 */
public interface LinkTarget {

    /** Runs a task on the server thread, the only one that may read or change the whitelist and the players. */
    void onServerThread(Runnable task);

    /** The entries of the whitelist. Server thread only. */
    Collection<NameAndId> whitelist();

    /** Finds the entries a plan needs; may wait for Mojang, so never on the server thread. */
    List<LinkApplier.Resolved> resolve(LinkPlan plan);

    /** Changes the whitelist. Server thread only. */
    LinkReport apply(LinkPlan plan, List<LinkApplier.Resolved> resolved);
}
