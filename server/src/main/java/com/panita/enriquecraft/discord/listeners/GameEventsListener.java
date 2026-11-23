package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.event.SystemEvent;
import com.panita.enriquecraft.core.event.CoreEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.DiscordPermissions;
import com.panita.enriquecraft.discord.config.DiscordConfig;
import com.panita.enriquecraft.discord.service.GameReporter;
import com.panita.enriquecraft.discord.service.PlayerInfos;
import com.panita.enriquecraft.staff.data.Dimensions;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;

import java.util.Optional;

/**
 * Follows the modules that know what happens in the game (chat, auth, core) through their own events, never the game's, and
 * tells the bot through the reporter.
 */
public final class GameEventsListener implements ModListener {

    private final GameReporter reporter;
    private final PlayerInfos infos;
    private final DiscordConfig config;

    public GameEventsListener(GameReporter reporter, PlayerInfos infos, DiscordConfig config) {
        this.reporter = reporter;
        this.infos = infos;
        this.config = config;
    }

    @Override
    public void register() {
        ChatEvents.MESSAGE.register(message -> reporter.chat(message.channel().id(), infos.of(message.sender()), message.text(),
                message.emote(), DiscordPermissions.canMention(message.sender())));
        ChatEvents.SYSTEM.register(this::system);
        ChatEvents.AFK.register((player, afk) -> {
            if (config.reportAfk.get()) {
                reporter.afk(infos.of(player), afk);
            }
        });
        ChatEvents.COORDINATES.register((player, position, dimension) -> reporter.coordinates(infos.of(player), positionText(position, dimension)));
        AuthEvents.PLAYER_READY.register(player -> reporter.ready(infos.of(player)));
        CoreEvents.BROADCAST.register(reporter::broadcast);
    }

    /** The position as it reads in the game's line about shared coordinates, with the dimension named as staff menus name it. */
    static String positionText(BlockPos position, ResourceKey<Level> dimension) {
        return "X: " + position.getX() + " Y: " + position.getY() + " Z: " + position.getZ() + " (" + Dimensions.displayName(dimension) + ")";
    }

    private void system(SystemEvent event) {
        switch (event) {
            case SystemEvent.Joined joined -> reporter.arrived(joined.player().getUUID(), joined.first());
            case SystemEvent.Left left -> reporter.left(infos.of(left.player()));
            case SystemEvent.Died died -> reporter.died(infos.of(died.player()), died.message().getString(), keyOf(died.message()));
            case SystemEvent.Advanced advanced ->
                    reporter.advanced(infos.of(advanced.player()), advancementTitle(advanced.message()), keyOf(advanced.message()));
        }
    }

    /**
     * The title of the advancement alone. The game's sentence names the player, which the bot already shows, and carries the
     * title in square brackets as its second argument; a message of another shape is sent whole.
     */
    static String advancementTitle(Component message) {
        if (message.getContents() instanceof TranslatableContents translatable && translatable.getArgs().length >= 2
                && translatable.getArgs()[1] instanceof Component title) {
            String text = title.getString();
            return text.length() > 2 && text.startsWith("[") && text.endsWith("]") ? text.substring(1, text.length() - 1) : text;
        }
        return message.getString();
    }

    private static Optional<String> keyOf(Component message) {
        return message.getContents() instanceof TranslatableContents translatable ? Optional.of(translatable.getKey()) : Optional.empty();
    }
}
