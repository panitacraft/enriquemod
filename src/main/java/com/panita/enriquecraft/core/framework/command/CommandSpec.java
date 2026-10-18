package com.panita.enriquecraft.core.framework.command;

import net.minecraft.server.permissions.PermissionLevel;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares a {@link ModCommand}. Classes with this annotation inside a module's {@code commands}
 * package are discovered and registered automatically.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CommandSpec {

    /** The literal used to run the command. */
    String name();

    /** Short Spanish description shown in help output; use a constant from {@code Messages}. */
    String description();

    /**
     * The command this one is nested under. The default, {@link ModCommand}, means a top-level command.
     */
    Class<? extends ModCommand> parent() default ModCommand.class;

    /** Extra literals that run the same command. */
    String[] aliases() default {};

    /** Vanilla permission level required when no permission manager grants or denies the node. */
    PermissionLevel access() default PermissionLevel.ALL;

    /**
     * Whether text in the chat runs this command when it is clicked. The game asks the player to confirm a click on a command
     * that its command tree marks as needing permissions, and a permission manager marks every command that way, even one open to
     * everyone. A clickable command is therefore always in the tree, and its executor must check
     * {@link CommandPermissions#canUse} itself.
     */
    boolean clickable() default false;
}
