package com.panita.enriquecraft.client.chat;

import com.panita.enriquecraft.discord.service.ChatImageLayout;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.util.ARGB;
import net.minecraft.util.FormattedCharSequence;

/** Draws the slice of an image that a line of chat holds. The mixins on the chat's two drawing classes hand their lines here. */
public final class ChatImageRenderer {

    private ChatImageRenderer() {
    }

    /** Draws the line when it is a slice of an image; returns whether it was one, so the caller skips the text. */
    public static boolean draw(GuiGraphicsExtractor graphics, int textTop, float opacity, FormattedCharSequence line) {
        if (!(line instanceof ChatImageLine slice)) {
            return false;
        }
        ChatImage image = slice.image();
        image.advance(System.currentTimeMillis());
        int sliceHeight = ChatImageLayout.LINE_HEIGHT * ChatImages.SCALE;
        graphics.blit(RenderPipelines.GUI_TEXTURED, image.id(), 0, textTop, 0.0F, slice.row() * sliceHeight,
                image.width(), ChatImageLayout.LINE_HEIGHT, image.textureWidth(), sliceHeight,
                image.textureWidth(), image.textureHeight(), ARGB.white(opacity));
        return true;
    }
}
