package com.panita.enriquecraft.core.gui;

import net.minecraft.world.inventory.ContainerInput;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuClickTest {

    private static MenuClick click(ContainerInput input, int button) {
        return new MenuClick(null, 0, button, input);
    }

    @Test
    void leftAndRightClicksAreRecognized() {
        assertTrue(click(ContainerInput.PICKUP, 0).isLeft());
        assertFalse(click(ContainerInput.PICKUP, 0).isRight());
        assertTrue(click(ContainerInput.PICKUP, 1).isRight());
        assertFalse(click(ContainerInput.PICKUP, 1).isLeft());
    }

    @Test
    void shiftClickIsShiftAndStillLeftOrRight() {
        MenuClick shiftLeft = click(ContainerInput.QUICK_MOVE, 0);

        assertTrue(shiftLeft.isShift());
        assertTrue(shiftLeft.isLeft());
        assertFalse(click(ContainerInput.PICKUP, 0).isShift());
    }

    @Test
    void middleClickIsNeitherLeftNorRight() {
        assertFalse(click(ContainerInput.PICKUP, 2).isLeft());
        assertFalse(click(ContainerInput.PICKUP, 2).isRight());
    }

    @Test
    void onlyPickupAndQuickMoveArePlain() {
        assertTrue(click(ContainerInput.PICKUP, 0).isPlain());
        assertTrue(click(ContainerInput.QUICK_MOVE, 0).isPlain());
        for (ContainerInput input : new ContainerInput[]{ContainerInput.SWAP, ContainerInput.THROW, ContainerInput.CLONE,
                ContainerInput.QUICK_CRAFT, ContainerInput.PICKUP_ALL}) {
            assertFalse(click(input, 0).isPlain(), input.toString());
            assertFalse(click(input, 0).isLeft(), input.toString());
        }
    }
}
