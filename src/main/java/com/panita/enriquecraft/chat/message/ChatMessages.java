package com.panita.enriquecraft.chat.message;

/**
 * Every player-visible text of the chat module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class ChatMessages {

    private ChatMessages() {
    }

    public static final class Format {
        public static final String LINK_TOOLTIP = "<gray>Clic para abrir el enlace";

        private Format() {
        }
    }
}
