package com.panita.enriquecraft.chat.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.auth.mixin.CommandNodeAccessor;
import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.commands.EmoteCommands;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes {@code /me} say what a player does the way the chat module shows it. The argument and the permission stay the
 * game's; only what the command runs changes.
 */
@Mixin(EmoteCommands.class)
public abstract class EmoteCommandsMixin {

    @Inject(method = "register", at = @At("RETURN"))
    private static void enriquecraft$takeOverTheExecutor(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        ChatHooks hooks = ChatHooks.get();
        if (hooks == null) {
            return;
        }
        CommandNode<CommandSourceStack> action = dispatcher.getRoot().getChild("me").getChild("action");
        ((CommandNodeAccessor) action).<CommandSourceStack>enriquecraft$setCommand(
                context -> hooks.emote(context.getSource(), MessageArgument.getMessage(context, "action").getString()));
    }
}
