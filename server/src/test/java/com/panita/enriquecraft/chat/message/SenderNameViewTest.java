package com.panita.enriquecraft.chat.message;

import org.junit.jupiter.api.Test;

import java.net.URI;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SenderNameViewTest {

    @Test
    void profileLinkPointsToTheWebProfileOfTheRealName() {
        assertEquals(URI.create("https://www.panitacraft.com/profile/Javithor360"), SenderNameView.profileUri("Javithor360"));
    }

    @Test
    void profileLinkEncodesNamesThatAreNotPlain() {
        assertEquals("https://www.panitacraft.com/profile/a%20b%2Fc", SenderNameView.profileUri("a b/c").toString());
    }
}
