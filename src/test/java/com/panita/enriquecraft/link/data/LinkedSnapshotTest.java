package com.panita.enriquecraft.link.data;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.link.client.LinkedPlayer;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class LinkedSnapshotTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    private static LinkedSnapshot roundTrip(LinkedSnapshot snapshot) {
        Tag tag = LinkedSnapshot.CODEC.encodeStart(NbtOps.INSTANCE, snapshot).getOrThrow();
        return LinkedSnapshot.CODEC.parse(NbtOps.INSTANCE, tag).getOrThrow();
    }

    @Test
    void aSnapshotComesBackAsItWasStored() {
        LinkedSnapshot snapshot = new LinkedSnapshot(List.of(
                new LinkedPlayer(12, "Javithor360", AuthMode.PREMIUM, "123456789012345678"),
                new LinkedPlayer(30, "Tezzlar", AuthMode.CRACKED, "223456789012345678")), Optional.of("\"v1\""));

        assertEquals(snapshot, roundTrip(snapshot));
    }

    @Test
    void theEmptySnapshotRoundTripsWithoutATag() {
        assertEquals(LinkedSnapshot.EMPTY, roundTrip(LinkedSnapshot.EMPTY));
    }
}
