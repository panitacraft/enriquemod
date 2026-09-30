package com.panita.enriquecraft.staff.service;

import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CoordinateIconsTest {

    @BeforeAll
    static void startMinecraft() {
        MinecraftTestSupport.bootstrap();
    }

    @Test
    void thereAreTwentyFourDifferentRealItems() {
        List<Item> all = CoordinateIcons.all();

        assertEquals(24, all.size());
        assertEquals(24, new HashSet<>(all).size(), "no icon twice");
        assertFalse(all.contains(Items.AIR));
    }

    @Test
    void randomPicksFromTheListAndFollowsTheGivenRandom() {
        Item first = CoordinateIcons.random(new Random(42));
        Item again = CoordinateIcons.random(new Random(42));

        assertTrue(CoordinateIcons.all().contains(first));
        assertEquals(first, again);
    }

    @Test
    void differentSeedsReachDifferentIcons() {
        HashSet<Item> seen = new HashSet<>();
        for (int seed = 0; seed < 200; seed++) {
            seen.add(CoordinateIcons.random(new Random(seed)));
        }

        assertTrue(seen.size() > 12, "picked only " + seen.size() + " different icons in 200 tries");
    }
}
