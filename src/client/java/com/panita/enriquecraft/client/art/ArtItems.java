package com.panita.enriquecraft.client.art;

import com.panita.enriquecraft.core.art.InlineArts;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import java.util.Optional;

/**
 * Draws an item the way the screens of the client companion draw every item, except one the server marked with an image
 * ({@link InlineArts#item}): that one is the image, at the size of an item and fitted into it with its proportions, as soon as it is
 * ready, and the item meanwhile.
 */
public final class ArtItems {

    private static final int SIZE = 16;

    private ArtItems() {
    }

    public static void draw(GuiGraphicsExtractor graphics, ItemStack stack, int x, int y) {
        Optional<InlineArtTextures.Icon> art = artOf(stack, screenSide(graphics));
        if (art.isEmpty()) {
            graphics.item(stack, x, y);
            return;
        }
        // The texture was made at the size the picture takes on the screen, so it is drawn one pixel to one pixel
        graphics.blit(RenderPipelines.GUI_TEXTURED, art.get().texture(), x, y, 0.0F, 0.0F, SIZE, SIZE, art.get().side(), art.get().side(), art.get().side(), art.get().side(),
                ARGB.white(1.0F));
    }


    /** The size the picture takes on the screen: the item's 16 pixels, times what the pose scales them by, times the GUI scale. */
    private static int screenSide(GuiGraphicsExtractor graphics) {
        double poseScale = Math.hypot(graphics.pose().m00(), graphics.pose().m01());
        return (int) Math.round(SIZE * poseScale * Minecraft.getInstance().getWindow().getGuiScale());
    }

    private static Optional<InlineArtTextures.Icon> artOf(ItemStack stack, int screenSide) {
        CustomData data = stack.get(DataComponents.CUSTOM_DATA);
        if (data == null) {
            return Optional.empty();
        }
        String id = data.copyTag().getStringOr(InlineArts.ITEM_KEY, "");
        return id.isEmpty() ? Optional.empty() : InlineArtTextures.icon(id, screenSide);
    }
}
