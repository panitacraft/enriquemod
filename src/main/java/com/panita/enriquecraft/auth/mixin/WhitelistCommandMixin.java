package com.panita.enriquecraft.auth.mixin;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.context.ParsedCommandNode;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.auth.AuthHooks;
import com.panita.enriquecraft.auth.data.AuthMode;
import com.panita.enriquecraft.auth.event.AuthEvents;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.GameProfileArgument;
import net.minecraft.server.commands.WhitelistCommand;
import net.minecraft.server.players.NameAndId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;
import java.util.Optional;

/**
 * Makes {@code /whitelist add} and {@code remove} aware of how players log in. A whitelist entry has to
 * carry the right UUID, the real one of a Microsoft account or the offline one of a name nobody proves, and
 * the game's own commands cannot tell them apart on a server that is not in online mode, nor keep the
 * capitalization of a name they do not know. {@code add} detects the mode, or takes it as an extra word
 * ({@code cracked} or {@code premium}); {@code remove} finds players by name. The arguments, suggestions
 * and permissions of the commands stay the game's; only what they run changes. {@code reload} is announced as an
 * event, for the modules that keep the whitelist up to date.
 */
@Mixin(WhitelistCommand.class)
public abstract class WhitelistCommandMixin {

    private static final String TARGETS = "targets";

    @Inject(method = "reload", at = @At("RETURN"))
    private static void enriquecraft$announceTheReload(CommandSourceStack source, CallbackInfoReturnable<Integer> callback) {
        AuthEvents.WHITELIST_RELOADED.invoker().onReloaded(source.getServer());
    }

    @Inject(method = "register", at = @At("RETURN"))
    private static void enriquecraft$takeOverTheExecutors(CommandDispatcher<CommandSourceStack> dispatcher, CallbackInfo callback) {
        AuthHooks hooks = AuthHooks.get();
        if (hooks == null) {
            return;
        }
        CommandNode<CommandSourceStack> whitelist = dispatcher.getRoot().getChild("whitelist");
        CommandNode<CommandSourceStack> addTargets = whitelist.getChild("add").getChild(TARGETS);
        ((CommandNodeAccessor) addTargets).<CommandSourceStack>enriquecraft$setCommand(
                context -> hooks.whitelistAdd(context.getSource(), typedNames(context), Optional.empty()));
        for (AuthMode mode : AuthMode.values()) {
            addTargets.addChild(Commands.literal(mode.getSerializedName())
                    .executes(context -> hooks.whitelistAdd(context.getSource(), typedNames(context), Optional.of(mode)))
                    .build());
        }
        ((CommandNodeAccessor) whitelist.getChild("remove").getChild(TARGETS)).<CommandSourceStack>enriquecraft$setCommand(
                context -> hooks.whitelistRemove(context.getSource(), typedNames(context)));
    }

    /**
     * The names exactly as they were typed. A selector such as {@code @a} stands for the players it picks, so
     * their names are used instead.
     */
    private static List<String> typedNames(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
        for (ParsedCommandNode<CommandSourceStack> node : context.getNodes()) {
            if (node.getNode().getName().equals(TARGETS)) {
                String typed = node.getRange().get(context.getInput());
                if (!typed.startsWith("@")) {
                    return List.of(typed);
                }
            }
        }
        return GameProfileArgument.getGameProfiles(context, TARGETS).stream().map(NameAndId::name).toList();
    }
}
