package com.panita.enriquecraft.client.share;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.client.ModKeys;
import com.panita.enriquecraft.discord.network.ShareBeginC2S;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;

/**
 * The key that opens the share screen for the latest screenshot. It does not depend on the game's line about a saved
 * screenshot, which another mod may replace with its own (Essential shows a toast instead), so sharing always has a way in.
 */
final class ShareLastKey {

    private static final KeyMapping SHARE_LAST = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.enriquecraft.share_last_screenshot", InputConstants.KEY_G, ModKeys.CATEGORY));

    private ShareLastKey() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (SHARE_LAST.consumeClick()) {
                if (client.player == null || client.gui.screen() != null) {
                    continue;
                }
                if (!ClientPlayNetworking.canSend(ShareBeginC2S.TYPE)) {
                    client.gui.hud.getChat().addClientSystemMessage(Component.translatable("enriquecraft.share.unsupported"));
                } else {
                    ScreenshotSharing.latest(client.gameDirectory).ifPresentOrElse(
                            file -> client.setScreenAndShow(new ShareScreen(file)),
                            () -> client.gui.hud.getChat().addClientSystemMessage(Component.translatable("enriquecraft.share.none")));
                }
            }
        });
    }
}
