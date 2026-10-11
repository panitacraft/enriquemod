package com.panita.enriquecraft.chat.mixin;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.auth.mixin.CommandNodeAccessor;
import com.panita.enriquecraft.chat.ChatHooks;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.MessageArgument;
import net.minecraft.server.commands.MsgCommand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Makes {@code /msg}, and so {@code /tell} and {@code /w}, which redirect to it, send private messages the way the chat module
 * shows them. The arguments, the suggestions and the permission stay the game's; only what the command runs changes.
 */
@Mixin(MsgCommand.class)
public abstract class MsgCommandMixin {

    @Inject(method = "register", at = @At("RETURN"))
    private static void enriquecraft$takeOverTheExecutor(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        ChatHooks hooks = ChatHooks.get();
        if (hooks == null) {
            return;
        }
        CommandNode<CommandSourceStack> message = dispatcher.getRoot().getChild("msg").getChild("targets").getChild("message");
        ((CommandNodeAccessor) message).<CommandSourceStack>enriquecraft$setCommand(context -> hooks.privateMessage(context.getSource(),
                EntityArgument.getPlayers(context, "targets"), MessageArgument.getMessage(context, "message").getString()));
    }
}
