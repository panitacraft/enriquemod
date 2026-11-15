package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.profile.data.ProfileDetails;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BioTextTest {

    @Test
    void plainTextWithLineBreaksIsKept() {
        assertEquals(Optional.of("holiwi uwu\n\nel o el"), BioText.clean("holiwi uwu\n\nel o el"));
    }

    @Test
    void windowsLineBreaksBecomeOnes() {
        assertEquals(Optional.of("a\nb\nc"), BioText.clean("a\r\nb\rc"));
    }

    @Test
    void theSectionSignControlAndInvisibleCharactersAreRemoved() {
        // A right-to-left override and a zero width space change how text reads without showing
        assertEquals(Optional.of("crojo negrita"), BioText.clean("§crojo\u0007 ‮negrita​"));
    }

    @Test
    void longRunsOfBlankLinesAreShortened() {
        assertEquals(Optional.of("a\n\nb"), BioText.clean("a\n\n\n\n\nb"));
    }

    @Test
    void theEndsAreTrimmedAndNothingIsEmpty() {
        assertEquals(Optional.of("hola"), BioText.clean("  \n hola \n "));
        assertEquals(Optional.of(""), BioText.clean(" \n "));
    }

    @Test
    void aTextOverTheLimitIsRefusedRatherThanCut() {
        assertEquals(Optional.of("a".repeat(ProfileDetails.BIO_MAX)), BioText.clean("a".repeat(ProfileDetails.BIO_MAX)));
        assertEquals(Optional.empty(), BioText.clean("a".repeat(ProfileDetails.BIO_MAX + 1)));
    }
}
