package com.panita.enriquecraft.core.message.channel;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundClearTitlesPacket;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.server.level.ServerPlayer;

/**
 * Delivers titles and subtitles using vanilla packets.
 */
public final class TitleChannel {

    public void show(ServerPlayer player, Component title, Component subtitle, TitleTimes times) {
        // Timings and subtitle must arrive before the title text, which is what triggers the display.
        player.connection.send(new ClientboundSetTitlesAnimationPacket(
                times.fadeInTicks(), times.stayTicks(), times.fadeOutTicks()));
        player.connection.send(new ClientboundSetSubtitleTextPacket(subtitle));
        player.connection.send(new ClientboundSetTitleTextPacket(title));
    }

    public void clear(ServerPlayer player) {
        player.connection.send(new ClientboundClearTitlesPacket(true));
    }
}
