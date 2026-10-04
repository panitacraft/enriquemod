package com.panita.enriquecraft.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentSerialization;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Asks the client companion to show a screen. It replaces the vanilla chest for players who
 * announced {@link ClientFeature#CUSTOM_UI}.
 *
 * @param sessionId names this showing of the screen in every later payload about it
 */
public record OpenUiS2C(int sessionId, Component title, UiElement root) implements CustomPacketPayload {

    public static final Type<OpenUiS2C> TYPE = NetworkProtocol.type("open_ui");

    public static final StreamCodec<RegistryFriendlyByteBuf, OpenUiS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, OpenUiS2C::sessionId,
            ComponentSerialization.STREAM_CODEC, OpenUiS2C::title,
            UiElementCodec.STREAM_CODEC, OpenUiS2C::root,
            OpenUiS2C::new);

    @Override
    public Type<OpenUiS2C> type() {
        return TYPE;
    }
}
