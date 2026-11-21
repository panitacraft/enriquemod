package com.panita.enriquecraft.client.mixin;

import com.panita.enriquecraft.client.chat.ChatImages;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.client.multiplayer.chat.GuiMessage;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Reserves room under a line of chat that links to an image the client has ready, as lines that hold slices of it. The chat
 * lays its lines out again on its own when it is resized, so this runs for every line each time, and the image follows the
 * line when the chat is scrolled or fades.
 */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {

    private static final int MAX_LINES = 100;

    @Shadow
    @Final
    private List<GuiMessage.Line> trimmedMessages;

    @Inject(method = "addMessageToDisplayQueue", at = @At("TAIL"))
    private void enriquecraft$addImageLines(GuiMessage message, CallbackInfo callback) {
        List<FormattedCharSequence> slices = ChatImages.linesFor(message.content());
        for (int i = 0; i < slices.size(); i++) {
            // The list is newest first, so the slices go in from the top of the image down
            trimmedMessages.addFirst(new GuiMessage.Line(message, slices.get(i), i == slices.size() - 1));
        }
        while (trimmedMessages.size() > MAX_LINES) {
            trimmedMessages.removeLast();
        }
    }
}
