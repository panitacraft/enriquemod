package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.network.ClientCapabilities;
import com.panita.enriquecraft.core.network.ClientFeature;
import com.panita.enriquecraft.core.network.CloseUiS2C;
import com.panita.enriquecraft.core.network.OpenUiS2C;
import com.panita.enriquecraft.core.network.UpdateUiS2C;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;
import java.util.OptionalInt;

/**
 * Shows {@link UiMenu}s to players in the form their client can display: the client companion's own
 * screen for players who announced it, a vanilla chest for everyone else.
 */
public final class UiService {

    private final MenuFactory factory;
    private final ClientCapabilities capabilities;
    private final UiSessions sessions;

    public UiService(MenuFactory factory, ClientCapabilities capabilities, UiSessions sessions) {
        this.factory = factory;
        this.capabilities = capabilities;
        this.sessions = sessions;
    }

    /** Creates the texts and items that screens are made of. */
    public MenuFactory factory() {
        return factory;
    }

    void open(ServerPlayer player, UiMenu menu) {
        if (offersCustomScreens(player)) {
            openCustomScreen(player, menu);
        } else {
            UiChestMenu chest = new UiChestMenu(menu);
            menu.showingAs(chest);
            chest.open(player);
        }
    }

    void refresh(UiMenu menu) {
        ServerPlayer viewer = menu.viewer();
        Optional<UiSessions.Showing> showing =
                viewer == null ? Optional.empty() : sessions.showing(viewer.getUUID(), menu);
        if (showing.isPresent()) {
            UiLayout layout = menu.layout(showing.get().nextElementId());
            sessions.update(viewer.getUUID(), layout);
            ServerPlayNetworking.send(viewer, new UpdateUiS2C(showing.get().sessionId(), layout.root()));
        } else if (menu.chest() != null) {
            menu.chest().refresh();
        }
    }

    /** Closes whatever screen the player has open. */
    public void close(ServerPlayer player) {
        OptionalInt session = sessions.end(player.getUUID());
        if (session.isPresent()) {
            ServerPlayNetworking.send(player, new CloseUiS2C(session.getAsInt()));
        } else {
            player.closeContainer();
        }
    }

    private boolean offersCustomScreens(ServerPlayer player) {
        // The announcement says the client wants them; the registered channel says it can receive them.
        return capabilities.supports(player.getUUID(), ClientFeature.CUSTOM_UI)
                && ServerPlayNetworking.canSend(player, OpenUiS2C.TYPE);
    }

    private void openCustomScreen(ServerPlayer player, UiMenu menu) {
        // A custom screen has no container; one left open would stay open on the server only.
        if (player.containerMenu != player.inventoryMenu) {
            player.closeContainer();
        }
        UiLayout layout = menu.layout(0);
        int session = sessions.begin(player.getUUID(), menu, layout);
        ServerPlayNetworking.send(player, new OpenUiS2C(session, menu.title(), layout.root()));
    }
}
