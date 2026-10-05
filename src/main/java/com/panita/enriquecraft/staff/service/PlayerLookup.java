package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.staff.data.PlayerRef;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

/**
 * Finds a player by name, whether or not they are online. A seam so menus that ask staff for a name can be
 * tested without a server.
 */
@FunctionalInterface
public interface PlayerLookup {

    /** @param asker the staff member asking, whose server does the lookup */
    Optional<PlayerRef> find(ServerPlayer asker, String name);

    /** The server's own record of players, which asks the account service for names it has not seen. */
    static PlayerLookup ofServer() {
        return (asker, name) -> asker.level().getServer().services().nameToIdCache().get(name)
                .map(found -> new PlayerRef(found.name(), found.id()));
    }
}
