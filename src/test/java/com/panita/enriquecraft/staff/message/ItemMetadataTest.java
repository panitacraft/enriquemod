package com.panita.enriquecraft.staff.message;

import com.panita.enriquecraft.MinecraftTestSupport;
import com.panita.enriquecraft.core.gui.MenuFactory;
import com.panita.enriquecraft.core.item.CustomItemTag;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.item.component.DyedItemColor;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.component.TooltipDisplay;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemMetadataTest {

    @TempDir
    Path directory;

    private MenuFactory factory;

    @BeforeEach
    void createFactory() {
        factory = MinecraftTestSupport.menuFactory(directory);
    }

    private List<String> lines(ItemStack stack) {
        return ItemMetadata.lines(stack, factory).stream().map(Component::getString).toList();
    }

    @Test
    void aPlainSingleItemHasNothingToShow() {
        assertEquals(List.of("Sin datos adicionales"), lines(new ItemStack(Items.SADDLE)));
    }

    @Test
    void aStackShowsItsCountAgainstTheMaximum() {
        assertTrue(lines(new ItemStack(Items.APPLE, 5)).stream().anyMatch(line -> line.endsWith("Cantidad: 5 / 64")));
    }

    @Test
    void aDamagedItemShowsWhatDurabilityIsLeft() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(100);

        String expected = "Durabilidad: " + (sword.getMaxDamage() - 100) + " / " + sword.getMaxDamage();
        assertTrue(lines(sword).stream().anyMatch(line -> line.endsWith(expected)));
    }

    /** The texture of the picture a line starts with, or an empty string when it starts with text. */
    private String iconOf(ItemStack stack, String text) {
        Component line = ItemMetadata.lines(stack, factory).stream()
                .filter(candidate -> candidate.getString().contains(text)).findFirst().orElseThrow();
        // A line with a picture is an empty root holding the picture first, then the text.
        Component first = line.getSiblings().isEmpty() ? line : line.getSiblings().getFirst();
        if (first.getContents() instanceof net.minecraft.network.chat.contents.ObjectContents objects
                && objects.contents() instanceof net.minecraft.network.chat.contents.objects.AtlasSprite sprite) {
            return sprite.sprite().getPath();
        }
        return "";
    }

    @Test
    void countStartsWithABundle() {
        assertEquals("item/bundle", iconOf(new ItemStack(Items.APPLE, 2), "Cantidad"));
    }

    @Test
    void durabilityStartsWithADiamondPickaxe() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.setDamageValue(10);

        assertEquals("item/diamond_pickaxe", iconOf(sword, "Durabilidad"));
    }

    @Test
    void attributesStartWithAnEnchantedGoldenApple() {
        assertEquals("item/enchanted_golden_apple", iconOf(new ItemStack(Items.DIAMOND_SWORD), "Atributos"));
    }

    @Test
    void aLineWithoutAKindStartsWithText() {
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        assertEquals("", iconOf(stick, "Otros datos"));
    }

    @Test
    void theNameAndTheLoreAreNeverListed() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.CUSTOM_NAME, Component.literal("Filo"));
        sword.set(DataComponents.LORE, new ItemLore(List.of(Component.literal("Lore"))));

        String all = String.join("|", lines(sword));

        assertFalse(all.contains("Filo"));
        assertFalse(all.contains("Lore"));
        assertFalse(all.contains("custom_name"));
        assertFalse(all.contains("lore"));
    }

    @Test
    void modelDataIsListedByKind() {
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(DataComponents.CUSTOM_MODEL_DATA, new CustomModelData(List.of(1.5F), List.of(true), List.of("a"), List.of()));

        assertTrue(lines(stick).contains("Datos de modelo: floats [1.5], flags [true], strings [a]"));
    }

    @Test
    void aDyeColorIsShownAsHex() {
        ItemStack boots = new ItemStack(Items.LEATHER_BOOTS);
        boots.set(DataComponents.DYED_COLOR, new DyedItemColor(0x336699));

        assertTrue(lines(boots).contains("Color: #336699"));
    }

    @Test
    void anUnbreakableItemSaysSo() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.UNBREAKABLE, net.minecraft.util.Unit.INSTANCE);

        assertTrue(lines(sword).contains("Irrompible: Sí"));
    }

    @Test
    void aHiddenTooltipAndItsHiddenPartsAreShown() {
        ItemStack sword = new ItemStack(Items.DIAMOND_SWORD);
        sword.set(DataComponents.TOOLTIP_DISPLAY, new TooltipDisplay(true,
                new LinkedHashSet<>(List.of(DataComponents.ENCHANTMENTS))));

        List<String> lines = lines(sword);

        assertTrue(lines.contains("Tooltip oculto: Sí"));
        assertTrue(lines.contains("Datos ocultos en el tooltip: minecraft:enchantments"));
    }

    @Test
    void anythingWithoutALineOfItsOwnIsListedByName() {
        ItemStack stick = new ItemStack(Items.STICK);
        stick.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);

        assertTrue(lines(stick).contains("Otros datos: minecraft:enchantment_glint_override"));
    }

    @Test
    void theModsOwnMarkerIsNotListed() {
        ItemStack stick = new ItemStack(Items.STICK);
        CustomItemTag.apply(stick, "palo");

        assertFalse(String.join("|", lines(stick)).contains("custom_data"));
    }
}
