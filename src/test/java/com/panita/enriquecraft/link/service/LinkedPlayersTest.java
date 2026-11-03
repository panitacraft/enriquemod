package com.panita.enriquecraft.link.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkedPlayersTest {

    private final LinkedPlayers players = new LinkedPlayers(() -> new LinkedSnapshot(List.of(
            new LinkedPlayer(12, "Javithor360", AuthMode.PREMIUM, "1"),
            new LinkedPlayer(30, "Tezzlar", AuthMode.CRACKED, "2")), Optional.empty()));

    @Test
    void findsAPlayerByNameWhateverTheCapitalization() {
        assertEquals(12, players.byName("javithor360").orElseThrow().userId());
        assertEquals(30, players.byName("TEZZLAR").orElseThrow().userId());
    }

    @Test
    void answersNothingForAPlayerWhoIsNotLinked() {
        assertEquals(Optional.empty(), players.byName("Nobody"));
    }
}
