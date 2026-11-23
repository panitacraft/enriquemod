package com.panita.enriquecraft.core.framework.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.tree.CommandNode;
import com.panita.enriquecraft.MinecraftTestSupport;
import net.minecraft.commands.CommandSourceStack;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CommandTreeBuilderTest {

    @BeforeAll
    static void bootstrap() {
        // Commands needs the game's registries; without this the test only passed when another test had started them
        MinecraftTestSupport.bootstrap();
    }

    @CommandSpec(name = "clickable", description = "d", clickable = true)
    private static final class Clickable implements ModCommand {
        @Override
        public void configure(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> builder) {
        }
    }

    @CommandSpec(name = "taken", description = "d", aliases = "tomado", clickable = true, overrides = true)
    private static final class Overriding implements ModCommand {
        @Override
        public void configure(com.mojang.brigadier.builder.LiteralArgumentBuilder<CommandSourceStack> builder) {
            builder.executes(context -> 1);
        }
    }

    @Test
    void anOverridingCommandReplacesWhatAnotherModRegisteredUnderItsName() throws Exception {
        CommandDispatcher<CommandSourceStack> dispatcher = new CommandDispatcher<>();
        dispatcher.register(com.mojang.brigadier.builder.LiteralArgumentBuilder.<CommandSourceStack>literal("taken")
                .executes(context -> 2)
                .then(com.mojang.brigadier.builder.LiteralArgumentBuilder.<CommandSourceStack>literal("health").executes(context -> 3)));
        CommandSpec spec = Overriding.class.getAnnotation(CommandSpec.class);

        new CommandTreeBuilder().replaceOn(dispatcher, new CommandEntry(new Overriding(), spec, List.of("taken"), List.of()));

        assertEquals(1, dispatcher.execute("taken", null));
        assertEquals(1, dispatcher.execute("tomado", null));
        assertThrows(CommandSyntaxException.class, () -> dispatcher.execute("taken health", null));
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
