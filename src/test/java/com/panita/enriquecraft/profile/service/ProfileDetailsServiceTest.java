package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import com.panita.enriquecraft.link.data.LinkedSnapshot;
import com.panita.enriquecraft.link.service.LinkedPlayers;
import com.panita.enriquecraft.profile.data.ProfileDetails;
import com.panita.enriquecraft.profile.data.ProfileDetailsBook;
import net.minecraft.world.level.Level;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProfileDetailsServiceTest {

    private final AtomicReference<ProfileDetailsBook> book = new AtomicReference<>(ProfileDetailsBook.EMPTY);
    private ProfileDetailsService service;

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    @BeforeEach
    void bind() {
        LinkedSnapshot linked = new LinkedSnapshot(List.of(new LinkedPlayer(7, "Alex", Optional.empty(), "111")), Optional.empty());
        service = new ProfileDetailsService(new LinkedPlayers(() -> linked), book::get, book::set);
    }

    @Test
    void aLinkedPlayerSavesTheirTextTheirBaseAndTheirChoice() {
        ProfileDetails.Base base = new ProfileDetails.Base(Level.OVERWORLD, 159, 62, -231);

        assertEquals(ProfileDetailsService.Outcome.SAVED, service.setBio("alex", "hola\nmundo"));
        assertEquals(ProfileDetailsService.Outcome.SAVED, service.setBase("Alex", Optional.of(base)));
        assertEquals(ProfileDetailsService.Outcome.SAVED, service.setShareLocation("ALEX", false));

        assertEquals(new ProfileDetails(Optional.of(base), "hola\nmundo", false), service.detailsOf("Alex"));
    }

    @Test
    void theTextIsCleanedBeforeItIsSaved() {
        service.setBio("Alex", "§hola\u0007");

        assertEquals("hola", service.detailsOf("Alex").bio());
    }

    @Test
    void aTextThatIsTooLongChangesNothing() {
        service.setBio("Alex", "uno");

        assertEquals(ProfileDetailsService.Outcome.TOO_LONG, service.setBio("Alex", "a".repeat(ProfileDetails.BIO_MAX + 1)));

        assertEquals("uno", service.detailsOf("Alex").bio());
    }

    @Test
    void aPlayerWithNoPanitaAccountCannotChangeAnythingAndHasTheDefaults() {
        assertEquals(ProfileDetailsService.Outcome.NOT_LINKED, service.setBio("Stranger", "hola"));
        assertEquals(ProfileDetailsService.Outcome.NOT_LINKED, service.setShareLocation("Stranger", false));

        assertEquals(ProfileDetails.DEFAULT, service.detailsOf("Stranger"));
        assertTrue(book.get().entries().isEmpty());
    }

    @Test
    void removingTheBaseKeepsTheRest() {
        service.setBio("Alex", "hola");
        service.setBase("Alex", Optional.of(new ProfileDetails.Base(Level.NETHER, 1, 2, 3)));

        service.setBase("Alex", Optional.empty());

        assertEquals(ProfileDetails.DEFAULT.withBio("hola"), service.detailsOf("Alex"));
    }
}
