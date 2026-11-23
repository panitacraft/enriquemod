package com.panita.enriquecraft.client.mixin;

import com.panita.enriquecraft.client.media.MediaTooltip;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;
import java.util.Optional;

/**
 * The game draws the tooltip of a hovered text through one method, whatever screen the text is on. When the text is the label of an
 * image, this puts the picture under its text, as the game does with the picture of a bundle; any other text is left to the game.
 */
@Mixin(GuiGraphicsExtractor.class)
public abstract class GuiGraphicsExtractorHoverMixin {

    @Inject(method = "componentHoverEffect", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$showTheImage(Font font, Style style, int mouseX, int mouseY, CallbackInfo callback) {
        Optional<MediaTooltip.Hover> hover = MediaTooltip.hoverOf(style);
        if (hover.isEmpty()) {
            return;
        }
        GuiGraphicsExtractor graphics = (GuiGraphicsExtractor) (Object) this;
        // The text breaks into lines as the game's own tooltips do
        List<FormattedCharSequence> lines = font.split(hover.get().text(), Math.max(graphics.guiWidth() / 2, 200));
        graphics.setTooltipForNextFrame(font, lines, Optional.of(new MediaTooltip.Data(hover.get().url())),
                DefaultTooltipPositioner.INSTANCE, mouseX, mouseY, false, null);
        callback.cancel();
    }
}
