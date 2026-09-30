package com.panita.enriquecraft.message;

import net.minecraft.network.chat.Component;

/**
 * A value inserted into a message template at a {@code {name}} position.
 */
public sealed interface MessageArgument {

    /**
     * A finished component. Its content is shown exactly as given and is never parsed.
     */
    record Text(Component component) implements MessageArgument {
    }

    /**
     * Raw text that is parsed for text tags and legacy color codes when the message is formatted.
     * Use it only for input from someone trusted to format text, for example an administrator.
     */
    record Markup(String raw) implements MessageArgument {
    }
}
