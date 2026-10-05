package com.panita.enriquecraft.core.network;

import org.junit.jupiter.api.Test;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ClientCapabilitiesTest {

    private static final UUID PLAYER = UUID.randomUUID();

    @Test
    void aPlayerWhoNeverAnnouncedIsVanilla() {
        assertFalse(new ClientCapabilities().supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void anAnnouncedFeatureIsSupported() {
        ClientCapabilities capabilities = new ClientCapabilities();

        capabilities.accept(PLAYER, HelloC2S.announcing(EnumSet.of(ClientFeature.CUSTOM_UI)));

        assertTrue(capabilities.supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void aFeatureThatWasNotAnnouncedIsNotSupported() {
        ClientCapabilities capabilities = new ClientCapabilities();

        capabilities.accept(PLAYER, HelloC2S.announcing(Set.of()));

        assertFalse(capabilities.supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void aDifferentProtocolVersionMakesThePlayerVanilla() {
        ClientCapabilities capabilities = new ClientCapabilities();

        capabilities.accept(PLAYER, new HelloC2S(NetworkProtocol.VERSION + 1, List.of(ClientFeature.CUSTOM_UI.id())));

        assertFalse(capabilities.supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void unknownFeatureIdsAreIgnored() {
        Set<ClientFeature> accepted = ClientCapabilities.negotiate(NetworkProtocol.VERSION,
                List.of("from_the_future", ClientFeature.CUSTOM_UI.id()));

        assertEquals(EnumSet.of(ClientFeature.CUSTOM_UI), accepted);
    }

    @Test
    void aNewHelloReplacesThePreviousOne() {
        ClientCapabilities capabilities = new ClientCapabilities();
        capabilities.accept(PLAYER, HelloC2S.announcing(EnumSet.of(ClientFeature.CUSTOM_UI)));

        capabilities.accept(PLAYER, HelloC2S.announcing(Set.of()));

        assertFalse(capabilities.supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void forgettingAPlayerMakesThemVanillaAgain() {
        ClientCapabilities capabilities = new ClientCapabilities();
        capabilities.accept(PLAYER, HelloC2S.announcing(EnumSet.of(ClientFeature.CUSTOM_UI)));

        capabilities.forget(PLAYER);

        assertFalse(capabilities.supports(PLAYER, ClientFeature.CUSTOM_UI));
    }

    @Test
    void announcementsOfOnePlayerDoNotAffectAnother() {
        ClientCapabilities capabilities = new ClientCapabilities();

        capabilities.accept(PLAYER, HelloC2S.announcing(EnumSet.of(ClientFeature.CUSTOM_UI)));

        assertFalse(capabilities.supports(UUID.randomUUID(), ClientFeature.CUSTOM_UI));
    }
}
