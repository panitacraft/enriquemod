package com.panita.enriquecraft.auth.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.auth.config.AuthConfig;
import com.panita.enriquecraft.auth.service.AccountService;
import com.panita.enriquecraft.auth.service.PinEntry;
import com.panita.enriquecraft.auth.service.PinFlow;
import com.panita.enriquecraft.auth.service.PinHasher;
import com.panita.enriquecraft.core.framework.config.ConfigManager;
import com.panita.enriquecraft.core.framework.data.WorldData;
import net.minecraft.nbt.NbtOps;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PinViewTest {

    private static final UUID PLAYER = UUID.fromString("00000000-0000-0000-0000-000000000001");

    @TempDir
    Path directory;

    private PinView view;
    private AccountService accounts;

    @BeforeEach
    void create() {
        MinecraftTestSupport.bootstrap();
        AuthConfig config = new ConfigManager(directory).bind("auth", AuthConfig.class);
        view = new PinView(MinecraftTestSupport.menuFactory(directory), config);
        WorldData worldData = new WorldData();
        worldData.attach(directory, NbtOps.INSTANCE);
        accounts = new AccountService(worldData, new PinHasher(new byte[32]), Instant::now);
    }

    private PinFlow flow(boolean hasPin) {
        if (hasPin) {
            accounts.setPin(PLAYER, "Tezzlar", "1234");
        }
        return new PinFlow(PLAYER, "Tezzlar", new PinEntry(4, 3, hasPin), accounts, new PinFlow.Listener() {
            @Override
            public void accepted() {
            }

            @Override
            public void wrongPin() {
            }

            @Override
            public void lockedOut() {
            }
        });
    }

    private static void type(PinFlow flow, String digits) {
        digits.chars().forEach(digit -> flow.digit(digit - '0'));
    }

    @Test
    void enteringAPinAsksForItAndNamesTheAccount() {
        PinView.Texts texts = view.of(flow(true));

        assertEquals("Ingresa tu PIN", texts.title().getString());
        assertEquals("Hola, Tezzlar", texts.hint().getString());
    }

    @Test
    void changingAPinAsksForTheCurrentOneFirst() {
        accounts.setPin(PLAYER, "Tezzlar", "1234");
        PinFlow flow = new PinFlow(PLAYER, "Tezzlar", PinEntry.changing(4, 4, 3), accounts, new PinFlow.Listener() {
            @Override
            public void accepted() {
            }

            @Override
            public void wrongPin() {
            }

            @Override
            public void lockedOut() {
            }
        });

        assertEquals("Ingresa tu PIN actual", view.of(flow).title().getString());
        assertEquals("Luego elegirás uno nuevo", view.of(flow).hint().getString());
    }

    @Test
    void choosingAPinAsksForTheLengthAndThenForConfirmation() {
        PinFlow flow = flow(false);

        assertEquals("Crea tu PIN", view.of(flow).title().getString());
        assertTrue(view.of(flow).hint().getString().contains("4 dígitos"));
        type(flow, "4821");
        for (int tick = 0; tick < 6; tick++) {
            flow.tick();
        }
        assertEquals("Confirma tu PIN", view.of(flow).title().getString());
    }

    @Test
    void aWrongPinSaysHowManyAttemptsAreLeft() {
        PinFlow flow = flow(true);

        type(flow, "0000");

        assertEquals("PIN incorrecto", view.of(flow).title().getString());
        assertEquals("Te quedan 2 intento(s)", view.of(flow).hint().getString());
    }

    @Test
    void aMismatchIsNotAWrongPin() {
        PinFlow flow = flow(false);
        type(flow, "4821");
        for (int tick = 0; tick < 6; tick++) {
            flow.tick();
        }

        type(flow, "4822");

        assertEquals("No coinciden", view.of(flow).title().getString());
    }

    @Test
    void successSaysHowLongTheSessionLasts() {
        PinFlow flow = flow(true);

        type(flow, "1234");

        assertEquals("Bienvenido", view.of(flow).title().getString());
        assertEquals("Sesión recordada por 24 hora(s)", view.of(flow).hint().getString());
    }
}
