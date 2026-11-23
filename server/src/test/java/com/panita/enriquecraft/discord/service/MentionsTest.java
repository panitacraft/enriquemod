package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class MentionsTest {

    private final Mentions mentions = new Mentions(new LinkedPlayers(() -> new LinkedSnapshot(List.of(
            new LinkedPlayer(1, "Javithor360", Optional.of(AuthMode.PREMIUM), "111111111111111111"),
            new LinkedPlayer(2, "Tezzlar", Optional.empty(), "222222222222222222"),
            new LinkedPlayer(3, "Third", Optional.empty(), "333333333333333333"),
            new LinkedPlayer(4, "Fourth", Optional.empty(), "444444444444444444"),
            new LinkedPlayer(5, "Fifth", Optional.empty(), "555555555555555555"),
            new LinkedPlayer(6, "Sixth", Optional.empty(), "666666666666666666")), Optional.empty())));

    @Test
    void findsALinkedMemberByTheirMinecraftNameWhateverTheCapitalization() {
        assertEquals(List.of(new OutFrame.Mention("javithor360", "111111111111111111")), mentions.in("hola @javithor360, vienes?"));
    }

    @Test
    void keepsTheSpellingOfTheTextAndTheOrderOfTheMentions() {
        assertEquals(List.of(new OutFrame.Mention("TEZZLAR", "222222222222222222"), new OutFrame.Mention("Javithor360", "111111111111111111")),
                mentions.in("@TEZZLAR y @Javithor360"));
    }

    @Test
    void mentionsEachMemberOnce() {
        assertEquals(1, mentions.in("@Tezzlar @tezzlar @TEZZLAR").size());
    }

    @Test
    void ignoresNamesThatAreNotOnTheListAndWordsDiscordWouldTreatAsGroups() {
        assertEquals(List.of(), mentions.in("@everyone @here @Nobody @admin"));
    }

    @Test
    void ignoresAnAtSignInsideAWordOrDoubled() {
        assertEquals(List.of(), mentions.in("write to me@Tezzlar or @@Tezzlar"));
    }

    @Test
    void ignoresAMentionOfANameThatContinuesIntoLongerOne() {
        assertEquals(List.of(), mentions.in("@Tezzlarito"));
    }

    @Test
    void stopsAtTheMostAMessageMayMention() {
        assertEquals(5, mentions.in("@Javithor360 @Tezzlar @Third @Fourth @Fifth @Sixth").size());
    }
}
