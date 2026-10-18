package com.panita.enriquecraft.discord.service;

import com.panita.enriquecraft.discord.protocol.EventKind;
import com.panita.enriquecraft.discord.protocol.OutFrame;
import com.panita.enriquecraft.discord.protocol.PlayerInfo;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GameReporterTest {

    private static final PlayerInfo JAVI = new PlayerInfo(UUID.fromString("dd12be42-52a9-4a91-a8a1-11c01849e498"), "Javithor360", Optional.empty(), true, Optional.empty());

    private final List<OutFrame> sent = new ArrayList<>();
    private Set<String> bridged = Set.of("global");
    private final GameReporter reporter = new GameReporter(new FrameSink() {
        @Override
        public void send(OutFrame frame) {
            sent.add(frame);
        }

        @Override
        public boolean bridges(String channel) {
            return bridged.contains(channel);
        }
    });

    private static OutFrame.Event event(EventKind kind, Optional<PlayerInfo> player, Optional<String> text, Optional<String> key) {
        return new OutFrame.Event(kind, player, text, key);
    }

    @Test
    void passesAChatLineOfABridgedChannel() {
        reporter.chat("global", JAVI, "hola", false);

        assertEquals(List.of(new OutFrame.Chat("global", JAVI, "hola", false)), sent);
    }

    @Test
    void passesAnEmoteAsAnEmote() {
        reporter.chat("global", JAVI, "saluda", true);

        assertEquals(List.of(new OutFrame.Chat("global", JAVI, "saluda", true)), sent);
    }

    @Test
    void leavesOutAChannelTheBotDoesNotBridge() {
        reporter.chat("staff", JAVI, "secreto", false);

        assertTrue(sent.isEmpty());
    }

    @Test
    void namesTheChannelInLowercase() {
        reporter.chat("Global", JAVI, "hola", false);

        assertEquals(List.of(new OutFrame.Chat("global", JAVI, "hola", false)), sent);
    }

    @Test
    void sendsNoLineThatIsEmptyOnceItIsCleaned() {
        reporter.chat("global", JAVI, "  \u0007 ", false);

        assertTrue(sent.isEmpty());
    }

    @Test
    void cutsAChatLineToWhatTheProtocolAllows() {
        reporter.chat("global", JAVI, "x".repeat(400), false);

        assertEquals(256, ((OutFrame.Chat) sent.get(0)).text().length());
    }

    @Test
    void reportsAJoinWhenThePlayerIsReadyNotWhenTheyConnect() {
        reporter.arrived(JAVI.uuid(), false);
        assertTrue(sent.isEmpty());

        reporter.ready(JAVI);

        assertEquals(List.of(event(EventKind.JOIN, Optional.of(JAVI), Optional.empty(), Optional.empty())), sent);
    }

    @Test
    void reportsAFirstJoinOnlyOnceAndAsTheFirstTime() {
        reporter.arrived(JAVI.uuid(), true);
        reporter.ready(JAVI);
        reporter.left(JAVI);
        reporter.arrived(JAVI.uuid(), false);
        reporter.ready(JAVI);

        assertEquals(List.of(EventKind.FIRST_JOIN, EventKind.LEAVE, EventKind.JOIN),
                sent.stream().map(frame -> ((OutFrame.Event) frame).kind()).toList());
    }

    @Test
    void reportsALeaveOnlyForAPlayerWhoWasReady() {
        reporter.arrived(JAVI.uuid(), true);
        reporter.left(JAVI);

        assertTrue(sent.isEmpty());

        reporter.ready(JAVI);
        reporter.left(JAVI);
        reporter.left(JAVI);

        assertEquals(List.of(EventKind.JOIN, EventKind.LEAVE), sent.stream().map(frame -> ((OutFrame.Event) frame).kind()).toList());
    }

    @Test
    void aPlayerWhoLeftBeforeBeingReadyIsNotRememberedAsNew() {
        reporter.arrived(JAVI.uuid(), true);
        reporter.left(JAVI);

        reporter.ready(JAVI);

        assertEquals(EventKind.JOIN, ((OutFrame.Event) sent.get(0)).kind());
    }

    @Test
    void reportsDeathsAndAdvancementsWithTheirTextAndKey() {
        reporter.died(JAVI, "Javithor360 was slain by Zombie", Optional.of("death.attack.mob"));
        reporter.advanced(JAVI, "Stone Age", Optional.of("chat.type.advancement.task"));

        assertEquals(List.of(
                event(EventKind.DEATH, Optional.of(JAVI), Optional.of("Javithor360 was slain by Zombie"), Optional.of("death.attack.mob")),
                event(EventKind.ADVANCEMENT, Optional.of(JAVI), Optional.of("Stone Age"), Optional.of("chat.type.advancement.task"))), sent);
    }

    @Test
    void reportsABroadcastWithoutItsMarkup() {
        reporter.broadcast("<red>&lReinicio</red> en 5 minutos");

        assertEquals(List.of(event(EventKind.BROADCAST, Optional.empty(), Optional.of("Reinicio en 5 minutos"), Optional.empty())), sent);
    }

    @Test
    void doesNotReportABroadcastThatIsNothingButMarkup() {
        reporter.broadcast("<red></red>");

        assertTrue(sent.isEmpty());
    }

    @Test
    void reportsTheStartAndTheStopOfTheServer() {
        reporter.started();
        reporter.stopping();

        assertEquals(List.of(EventKind.STARTED, EventKind.STOPPING), sent.stream().map(frame -> ((OutFrame.Event) frame).kind()).toList());
    }
}
