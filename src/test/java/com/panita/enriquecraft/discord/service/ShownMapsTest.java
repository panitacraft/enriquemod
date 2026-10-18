package com.panita.enriquecraft.discord.service;

import net.minecraft.network.protocol.common.ServerboundClientInformationPacket;
import net.minecraft.network.protocol.common.ServerboundKeepAlivePacket;
import net.minecraft.network.protocol.game.ServerboundChatCommandPacket;
import net.minecraft.network.protocol.game.ServerboundChatPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerAbilitiesPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPunchPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ShownMapsTest {

    @Test
    void movingAndChattingDoNotEndTheMap() {
        assertTrue(ShownMaps.isResting(ServerboundMovePlayerPacket.Pos.class));
        assertTrue(ShownMaps.isResting(ServerboundMovePlayerPacket.Rot.class));
        assertTrue(ShownMaps.isResting(ServerboundMovePlayerPacket.PosRot.class));
        assertTrue(ShownMaps.isResting(ServerboundMovePlayerPacket.StatusOnly.class));
        assertTrue(ShownMaps.isResting(ServerboundPlayerInputPacket.class));
        assertTrue(ShownMaps.isResting(ServerboundPlayerCommandPacket.class), "sprinting");
        assertTrue(ShownMaps.isResting(ServerboundPlayerAbilitiesPacket.class), "flying");
        assertTrue(ShownMaps.isResting(ServerboundKeepAlivePacket.class));
        assertTrue(ShownMaps.isResting(ServerboundChatPacket.class));
        assertTrue(ShownMaps.isResting(ServerboundChatCommandPacket.class));
        assertTrue(ShownMaps.isResting(ServerboundClientInformationPacket.class));
    }

    @Test
    void anythingElseThePlayerDoesEndsIt() {
        assertFalse(ShownMaps.isResting(ServerboundSetCarriedItemPacket.class), "scrolling the hotbar");
        assertFalse(ShownMaps.isResting(ServerboundContainerClickPacket.class), "clicking in an inventory");
        assertFalse(ShownMaps.isResting(ServerboundContainerClosePacket.class), "closing a container");
        assertFalse(ShownMaps.isResting(ServerboundPlayerActionPacket.class), "dropping, swapping, digging");
        assertFalse(ShownMaps.isResting(ServerboundUseItemPacket.class), "using the item");
        assertFalse(ShownMaps.isResting(ServerboundUseItemOnPacket.class), "using a block");
        assertFalse(ShownMaps.isResting(ServerboundInteractPacket.class), "interacting with an entity");
        assertFalse(ShownMaps.isResting(ServerboundPunchPacket.class), "swinging");
        assertFalse(ShownMaps.isResting(ServerboundAttackPacket.class), "attacking");
    }

    @Test
    void anAnimationSendsOnlyTheRectangleThatChanged() {
        byte[] before = new byte[MapPixels.SIZE * MapPixels.SIZE];
        byte[] after = before.clone();
        after[10 * MapPixels.SIZE + 20] = 5;
        after[12 * MapPixels.SIZE + 23] = 7;

        var patch = ShownMaps.changes(before, after).orElseThrow();

        assertEquals(20, patch.startX());
        assertEquals(10, patch.startY());
        assertEquals(4, patch.width());
        assertEquals(3, patch.height());
        assertEquals(12, patch.mapColors().length);
        assertEquals(5, patch.mapColors()[0]);
        assertEquals(7, patch.mapColors()[2 * 4 + 3]);
    }

    @Test
    void twoEqualFramesSendNothing() {
        byte[] frame = new byte[MapPixels.SIZE * MapPixels.SIZE];

        assertTrue(ShownMaps.changes(frame, frame.clone()).isEmpty());
    }

    @Test
    void aFullyDifferentFrameIsTheWholeMap() {
        byte[] before = new byte[MapPixels.SIZE * MapPixels.SIZE];
        byte[] after = new byte[MapPixels.SIZE * MapPixels.SIZE];
        java.util.Arrays.fill(after, (byte) 9);

        var patch = ShownMaps.changes(before, after).orElseThrow();

        assertEquals(0, patch.startX());
        assertEquals(MapPixels.SIZE, patch.width());
        assertEquals(MapPixels.SIZE, patch.height());
    }
}
