package com.panita.enriquecraft.client;

import com.panita.enriquecraft.client.art.InlineArtTextures;
import com.panita.enriquecraft.client.media.MediaImageSettings;
import com.panita.enriquecraft.client.media.MediaImages;
import com.panita.enriquecraft.client.media.MediaTooltip;
import com.panita.enriquecraft.client.share.ScreenshotSharing;
import com.panita.enriquecraft.client.ui.ServerUiNetworking;
import com.panita.enriquecraft.discord.network.MediaOpenS2C;
import com.panita.enriquecraft.discord.network.MediaPreviewS2C;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.core.network.HelloC2S;
import com.panita.enriquecraft.core.network.InlineArtS2C;
import com.panita.enriquecraft.core.network.NetworkProtocol;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.ClientTooltipComponentCallback;

import java.util.EnumSet;
import java.util.Set;

/**
 * The client companion's entrypoint. It only adds enhancements: on a server without the mod it
 * does nothing, and nothing it does changes game rules.
 */
public final class EnriquecraftClient implements ClientModInitializer {

    /** The enhancements this client implements; grows as each one is added. */
    private static final Set<ClientFeature> SUPPORTED = EnumSet.of(ClientFeature.CUSTOM_UI, ClientFeature.MEDIA_PREVIEW, ClientFeature.SCREENSHOT_SHARE,
            ClientFeature.INLINE_ART);

    @Override
    public void onInitializeClient() {
        NetworkProtocol.registerPayloads();
        ServerUiNetworking.register();
        CommandKeys.register();
        ScreenshotSharing.register();
        ClientPlayNetworking.registerGlobalReceiver(MediaPreviewS2C.TYPE, (payload, context) -> MediaImages.allow(payload.url()));
        ClientPlayNetworking.registerGlobalReceiver(MediaOpenS2C.TYPE, (payload, context) -> context.client().execute(() -> MediaImages.openViewer(payload.url())));
        ClientTooltipComponentCallback.EVENT.register(MediaTooltip::componentFor);
        ClientPlayNetworking.registerGlobalReceiver(InlineArtS2C.TYPE, (payload, context) -> InlineArtTextures.expect(payload.id(), payload.url()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> client.execute(() -> {
            MediaImages.clear();
            InlineArtTextures.clear();
        }));
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> announce());
    }

    private static void announce() {
        // A server without the mod never registered the hello channel, and sending to it would throw.
        if (ClientPlayNetworking.canSend(HelloC2S.TYPE)) {
            Set<ClientFeature> features = EnumSet.copyOf(SUPPORTED);
            // A player who turned images off is a vanilla client for them: the label opens the image on a map and nothing is fetched
            if (!MediaImageSettings.enabled()) {
                features.remove(ClientFeature.MEDIA_PREVIEW);
            }
            ClientPlayNetworking.send(HelloC2S.announcing(features));
        }
    }
}
