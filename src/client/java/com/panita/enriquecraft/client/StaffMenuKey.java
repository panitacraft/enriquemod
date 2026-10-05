package com.panita.enriquecraft.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.panita.enriquecraft.core.network.HelloC2S;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;

/**
 * The key that opens the staff menu, "U" unless the player rebinds it in the controls. It only runs the
 * {@code /staff} command, so the server decides, as it always does, whether the player may use it. It does
 * nothing on a server without the mod, where that command does not exist.
 */
final class StaffMenuKey {

    private static final KeyMapping.Category CATEGORY =
            KeyMapping.Category.register(Identifier.fromNamespaceAndPath("enriquecraft", "main"));
    private static final KeyMapping OPEN_STAFF_MENU = KeyMappingHelper.registerKeyMapping(
            new KeyMapping("key.enriquecraft.staff_menu", InputConstants.KEY_U, CATEGORY));

    private StaffMenuKey() {
    }

    static void register() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (OPEN_STAFF_MENU.consumeClick()) {
                if (client.player != null && ClientPlayNetworking.canSend(HelloC2S.TYPE)) {
                    client.player.connection.sendCommand("staff");
                }
            }
        });
    }
}
