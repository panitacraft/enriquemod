package com.panita.enriquecraft.discord.listeners;

import com.panita.enriquecraft.auth.event.AuthEvents;
import com.panita.enriquecraft.chat.event.ChatEvents;
import com.panita.enriquecraft.chat.event.SystemEvent;
import com.panita.enriquecraft.core.event.CoreEvents;
import com.panita.enriquecraft.core.framework.listener.ModListener;
import com.panita.enriquecraft.discord.service.GameReporter;
import com.panita.enriquecraft.discord.service.PlayerInfos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

import java.util.Optional;

/**
 * Follows the modules that know what happens in the game (chat, auth, core) through their own events, never the game's, and
 * tells the bot through the reporter.
 */
public final class GameEventsListener implements ModListener {

    private final GameReporter reporter;
    private final PlayerInfos infos;

    public GameEventsListener(GameReporter reporter, PlayerInfos infos) {
        this.reporter = reporter;
        this.infos = infos;
    }

    @Override
    public void register() {
        ChatEvents.MESSAGE.register(message ->
                reporter.chat(message.channel().id(), infos.of(message.sender()), message.text(), message.emote()));
        ChatEvents.SYSTEM.register(this::system);
        AuthEvents.PLAYER_READY.register(player -> reporter.ready(infos.of(player)));
        CoreEvents.BROADCAST.register(reporter::broadcast);
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
