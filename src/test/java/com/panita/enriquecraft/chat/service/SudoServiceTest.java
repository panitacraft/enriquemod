package com.panita.enriquecraft.chat.service;

import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.server.permissions.PermissionLevel;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SudoServiceTest {

    @Test
    void levelOfAPermissionSetIsTheHighestLevelItReaches() {
        for (PermissionLevel level : PermissionLevel.values()) {
            assertEquals(level, SudoService.levelOf(LevelBasedPermissionSet.forLevel(level)));
        }
    }
}
