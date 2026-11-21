package com.panita.enriquecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.HelloC2S;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;

/**
 * The keys that open the mod's menus: "U" for the staff menu and "P" for the player's profile, unless the player rebinds them in the
 * controls. A key only runs the command of its menu, so the server decides, as it always does, whether the player may use it. Both do
 * nothing on a server without the mod, where those commands do not exist.
 */
final class CommandKeys {

    private CommandKeys() {
    }

    static void register() {
        open("key.enriquecraft.staff_menu", InputConstants.KEY_U, "staff");
        open("key.enriquecraft.profile_menu", InputConstants.KEY_P, "profile");
    }

    private static void open(String translationKey, int defaultKey, String command) {
        KeyMapping key = KeyMappingHelper.registerKeyMapping(new KeyMapping(translationKey, defaultKey, ModKeys.CATEGORY));
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (key.consumeClick()) {
                if (client.player != null && ClientPlayNetworking.canSend(HelloC2S.TYPE)) {
                    client.player.connection.sendCommand(command);
                }
            }
        });
    }
}
