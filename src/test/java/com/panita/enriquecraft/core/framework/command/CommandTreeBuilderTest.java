package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.tree.CommandNode;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTreeBuilderTest {

    @CommandSpec(name = "clickable", description = "d", clickable = true)
    private static final class Clickable implements ModCommand {
        @Override
        public void configure(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> builder) {
        }
    }

    @Test
    void aClickableCommandIsAlwaysInTheTreeForEverySource() {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        CommandSpec spec = Clickable.class.getAnnotation(CommandSpec.class);

        new CommandTreeBuilder().registerOn(dispatcher, new CommandEntry(new Clickable(), spec, List.of("clickable"), List.of()));

        CommandNode<CommandSourceStack> node = dispatcher.getRoot().getChild("clickable");
        // The game marks a node as needing permissions when its requirement fails for a source with none, which makes a click on it ask first
        assertTrue(node.getRequirement().test(null));
    }
}
