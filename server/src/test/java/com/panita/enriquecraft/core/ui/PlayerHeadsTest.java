package com.panita.enriquecraft.core.ui;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PlayerHeadsTest {

    private static final UUID ANA = UUID.fromString("11111111-2222-3333-4444-555555555555");

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void theItemIsAPlayerHeadOfThatPlayer() {
        ItemStack head = PlayerHeads.item(ANA);

        assertEquals(Items.PLAYER_HEAD, head.getItem());
        assertNotNull(head.get(DataComponents.PROFILE));
    }

    @Test
    void theInlineHeadComesBeforeTheNameAndKeepsItsStyle() {
        Component line = PlayerHeads.inline(ANA, Component.literal("Ana"));

        assertTrue(line.getString().endsWith(" Ana"));
    }
}
