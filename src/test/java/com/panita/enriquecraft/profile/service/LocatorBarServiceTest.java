package com.panita.enriquecraft.profile.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeMap;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.attributes.DefaultAttributes;
import net.minecraft.world.waypoints.Waypoint;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocatorBarServiceTest {

    @BeforeAll
    static void bootstrap() {
        MinecraftTestSupport.bootstrap();
    }

    private static AttributeInstance range() {
        AttributeInstance range = new AttributeMap(DefaultAttributes.getSupplier(EntityTypes.PLAYER)).getInstance(Attributes.WAYPOINT_TRANSMIT_RANGE);
        assertNotNull(range);
        return range;
    }

    @Test
    void hidingZeroesTheRangeAndSharingRestoresIt() {
        AttributeInstance range = range();
        double normal = range.getValue();
        assertTrue(normal > 0.0);

        LocatorBarService.applyTo(range, false);
        assertEquals(0.0, range.getValue());

        LocatorBarService.applyTo(range, true);
        assertEquals(normal, range.getValue());
    }

    @Test
    void applyingTwiceKeepsOneModifier() {
        AttributeInstance range = range();
        double normal = range.getValue();

        LocatorBarService.applyTo(range, false);
        LocatorBarService.applyTo(range, false);
        assertEquals(0.0, range.getValue());

        LocatorBarService.applyTo(range, true);
        assertFalse(range.hasModifier(LocatorBarService.HIDDEN_ID));
        assertEquals(normal, range.getValue());
    }

    @Test
    void sharingAnAlreadySharedPlayerChangesNothing() {
        AttributeInstance range = range();
        double normal = range.getValue();

        LocatorBarService.applyTo(range, true);
        assertEquals(normal, range.getValue());
    }

    @Test
    void theModifierIsTransientAndKeepsTheHidingOfOtherSources() {
        AttributeInstance range = range();
        range.addTransientModifier(Waypoint.WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER);
        double normal = 0.0;

        LocatorBarService.applyTo(range, false);
        assertEquals(0.0, range.getValue());

        // Showing the player removes only this mod's modifier; a mob head still hides them.
        LocatorBarService.applyTo(range, true);
        assertEquals(normal, range.getValue());
        assertTrue(range.hasModifier(Waypoint.WAYPOINT_TRANSMIT_RANGE_HIDE_MODIFIER.id()));

        AttributeModifier ours = LocatorBarService.HIDDEN;
        assertEquals(-1.0, ours.amount());
        assertEquals(AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL, ours.operation());
        assertEquals("enriquecraft:hidden_from_locator", ours.id().toString());
        assertTrue(range.getModifiers().stream().noneMatch(modifier -> modifier.id().equals(ours.id())));
    }
}
