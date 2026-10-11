package com.panita.enriquecraft.auth.config;

import com.panita.enriquecraft.core.framework.config.ConfigSectionBuilder;
import com.panita.enriquecraft.core.framework.config.ConfigValue;
import com.panita.enriquecraft.core.framework.config.ModConfig;

/**
 * Settings of the auth module, stored in the {@code auth} section of the config file.
 */
public final class AuthConfig implements ModConfig {

    public final ConfigValue<Integer> pinLength;
    public final ConfigValue<Integer> maxAttempts;
    public final ConfigValue<Integer> loginTimeoutSeconds;
    public final ConfigValue<Boolean> spectatorWhileWaiting;
    public final ConfigValue<Integer> introSeconds;
    public final ConfigValue<Integer> sessionHours;
    public final ConfigValue<Integer> kicksBeforeBlock;
    public final ConfigValue<Integer> windowSeconds;
    public final ConfigValue<Integer> blockSeconds;
    public final ConfigValue<Integer> lookupCacheMinutes;
    public final ConfigValue<Integer> lookupMaxRequestsPerTenMinutes;

    public AuthConfig(ConfigSectionBuilder builder) {
        pinLength = builder.intRange("pin.length", 4, 4, 6,
                "How many digits the PIN of a new account has. Accounts keep the length they were created with. Digits go from 1 to 9: there is no zero.");
        maxAttempts = builder.intRange("pin.maxAttempts", 3, 1, 10,
                "How many wrong PINs a player may type before being kicked.");
        loginTimeoutSeconds = builder.intRange("pin.timeoutSeconds", 60, 10, 600,
                "How long a player may take to enter their PIN before being kicked.");
        spectatorWhileWaiting = builder.bool("pin.spectatorWhileWaiting", true,
                "Whether a player who has not entered their PIN yet is put in spectator mode, which also hides them from the others. Their game mode is restored when they get in or leave.");
        introSeconds = builder.intRange("pin.introSeconds", 3, 0, 30,
                "How long the explanation of the PIN screen is shown to players without the client companion before the screen opens. Zero opens it at once.");
        sessionHours = builder.intRange("session.hours", 24, 1, 720,
                "How long a correct PIN is remembered for the same address, counted from the moment it was typed.");
        kicksBeforeBlock = builder.intRange("lockout.kicksBeforeBlock", 3, 1, 20,
                "How many kicks for wrong PINs, within the window, block an address. A kick is as many wrong PINs as pin.maxAttempts allows. Every wrong PIN counts when it is typed, so leaving before the kick does not avoid it.");
        windowSeconds = builder.intRange("lockout.windowSeconds", 600, 1, 86400,
                "How long a wrong PIN keeps counting toward a block, in seconds.");
        blockSeconds = builder.intRange("lockout.blockSeconds", 900, 1, 2592000,
                "How long a blocked address is refused, in seconds. The default is 15 minutes.");
        lookupCacheMinutes = builder.intRange("premiumLookup.cacheMinutes", 30, 1, 1440,
                "How long the answer of Mojang about a name is remembered. Only used for names that are not in the whitelist or the op list.");
        lookupMaxRequestsPerTenMinutes = builder.intRange("premiumLookup.maxRequestsPerTenMinutes", 500, 1, 600,
                "How many questions about names the server asks Mojang every ten minutes; Mojang allows 600. Beyond it, players are asked to try again.");
    }
}
