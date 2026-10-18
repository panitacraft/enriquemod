package com.panita.enriquecraft.client.mixin;

import com.panita.enriquecraft.client.share.ScreenshotSharing;
import net.minecraft.client.Screenshot;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.util.function.Consumer;

/** Adds the share button to the game's line about a saved screenshot. The game saves and words the line as it always did. */
@Mixin(Screenshot.class)
public abstract class ScreenshotMixin {

    @ModifyVariable(method = "grab(Ljava/io/File;Ljava/lang/String;Lcom/mojang/blaze3d/pipeline/RenderTarget;ILjava/util/function/Consumer;)V",
            at = @At("HEAD"), argsOnly = true)
    private static Consumer<Component> enriquecraft$addShareButton(Consumer<Component> callback) {
        return ScreenshotSharing.withShareButton(callback);
    }
}
