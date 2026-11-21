package com.panita.enriquecraft.core.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/**
 * Replaces what an open screen shows. Screens are small, so the whole tree is sent again.
 */
public record UpdateUiS2C(int sessionId, UiElement root) implements CustomPacketPayload {

    public static final Type<UpdateUiS2C> TYPE = NetworkProtocol.type("update_ui");

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateUiS2C> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.VAR_INT, UpdateUiS2C::sessionId,
            UiElementCodec.STREAM_CODEC, UpdateUiS2C::root,
            UpdateUiS2C::new);

    @Override
    public Type<UpdateUiS2C> type() {
        return TYPE;
    }
}
