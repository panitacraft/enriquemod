package com.panita.enriquecraft.moderation.service;

import com.panita.enriquecraft.moderation.config.ModerationConfig;
import net.minecraft.resources.Identifier;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

/**
 * Decides whether a client may play from the channels it announced. A client cannot be asked for its mods, so a mod is recognized
 * by the channel it listens on; what a client announces is untrusted and only ever used to refuse it.
 */
public final class ClientModPolicy {

    private final ModerationConfig config;

    public ClientModPolicy(ModerationConfig config) {
        this.config = config;
    }

    /** @return the name shown for the first announced channel that is blocked, when the check is on and there is one */
    public Optional<String> blockedMod(Collection<Identifier> announced) {
        if (!config.clientModsEnabled.get()) {
            return Optional.empty();
        }
        Map<String, String> blocked = config.blockedChannels.get();
        return announced.stream().map(Identifier::toString).filter(blocked::containsKey).findFirst().map(blocked::get);
    }
}
