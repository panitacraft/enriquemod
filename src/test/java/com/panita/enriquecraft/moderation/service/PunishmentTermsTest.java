package com.panita.enriquecraft.moderation.service;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PunishmentTermsTest {

    private static PunishmentTerms terms(Duration duration, String reason) {
        return new PunishmentTerms(Optional.ofNullable(duration), Optional.ofNullable(reason));
    }

    @Test
    void nothingWrittenIsAPermanentPunishmentWithoutReason() {
        assertEquals(PunishmentTerms.NONE, PunishmentTerms.parse(""));
        assertEquals(PunishmentTerms.NONE, PunishmentTerms.parse("   "));
    }

    @Test
    void aSpanFirstIsTheTimeAndTheRestIsTheReason() {
        assertEquals(terms(Duration.ofDays(7), "griefing en el spawn"), PunishmentTerms.parse("1w griefing en el spawn"));
        assertEquals(terms(Duration.ofSeconds(7 * 86_400 + 86_400 + 60 + 1), null), PunishmentTerms.parse("1w1d1m1s"));
        assertEquals(terms(Duration.ofHours(2), "x"), PunishmentTerms.parse("2H   x"));
    }

    @Test
    void permanentFirstIsNoTimeAndTheRestIsTheReason() {
        assertEquals(terms(null, "hacks"), PunishmentTerms.parse("permanent hacks"));
        assertEquals(terms(null, null), PunishmentTerms.parse("Permanent"));
        assertEquals(terms(null, "1d no es un tiempo aquí"), PunishmentTerms.parse("permanente 1d no es un tiempo aquí"));
    }

    @Test
    void withoutATimeEverythingIsTheReason() {
        assertEquals(terms(null, "usar hacks, ñandú!"), PunishmentTerms.parse("usar hacks, ñandú!"));
        assertEquals(terms(null, "spam"), PunishmentTerms.parse("  spam "));
    }

    @Test
    void aWordThatIsNotAWholeSpanIsPartOfTheReason() {
        assertEquals(terms(null, "10 minutos de spam"), PunishmentTerms.parse("10 minutos de spam"));
        assertEquals(terms(null, "0s nada"), PunishmentTerms.parse("0s nada"));
    }
}
