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
}
