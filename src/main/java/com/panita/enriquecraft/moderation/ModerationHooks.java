package com.panita.enriquecraft.moderation;

import com.panita.enriquecraft.moderation.message.KickView;
import com.panita.enriquecraft.moderation.service.BanService;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.server.players.NameAndId;
import net.minecraft.server.players.UserBanList;
import org.jspecify.annotations.Nullable;

import java.util.Optional;

/**
 * The way the moderation module's mixins reach its services. A mixin is instantiated by Mixin, not by the service registry,
 * so it cannot receive services through a constructor; this is the one static door, installed once when the module loads.
 * Without it every mixin leaves vanilla behavior untouched.
 */
public final class ModerationHooks {

    /** The reason the game gives a kick when the command had none. */
    private static final String DEFAULT_KICK_REASON = "multiplayer.disconnect.kicked";

    private static volatile @Nullable ModerationHooks instance;

    private final BanService bans;
    private final KickView kicks;

    private ModerationHooks(BanService bans, KickView kicks) {
        this.bans = bans;
        this.kicks = kicks;
    }

    static void install(BanService bans, KickView kicks) {
        instance = new ModerationHooks(bans, kicks);
    }

    public static @Nullable ModerationHooks get() {
        return instance;
    }

    /** The screen of a player who is kicked, for the reason the command had. */
    public Component kickScreen(Component reason) {
        boolean given = !(reason.getContents() instanceof TranslatableContents translatable && translatable.getKey().equals(DEFAULT_KICK_REASON));
        return kicks.screen(given ? Optional.of(reason) : Optional.empty());
    }

    /** The screen of a player who tries to log in, when they are banned. */
    public Optional<Component> banScreen(UserBanList list, NameAndId profile) {
        return bans.screenFor(list, profile);
    }
}
