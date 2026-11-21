package com.panita.enriquecraft.client.mixin;

import com.panita.enriquecraft.client.chat.ChatImageRenderer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Draws the lines that hold a slice of an image while the chat is closed and its lines fade, instead of text. */
@Mixin(targets = "net.minecraft.client.gui.components.ChatComponent$DrawingBackgroundGraphicsAccess")
public abstract class DrawingBackgroundGraphicsAccessMixin {

    @Shadow
    @Final
    private GuiGraphicsExtractor graphics;

    @Inject(method = "handleMessage", at = @At("HEAD"), cancellable = true)
    private void enriquecraft$drawImage(int textTop, float opacity, FormattedCharSequence message, CallbackInfoReturnable<Boolean> callback) {
        if (ChatImageRenderer.draw(graphics, textTop, opacity, message)) {
            callback.setReturnValue(false);
        }
    }
}
