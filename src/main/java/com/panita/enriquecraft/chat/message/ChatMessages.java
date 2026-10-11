package com.panita.enriquecraft.chat.message;

/**
 * Every player-visible text of the chat module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class ChatMessages {

    private ChatMessages() {
    }

    /**
     * How a line of chat looks. The first two are the lines of the channels a new config file starts with; the lines of
     * channels are set in the channel itself.
     */
    public static final class Format {
        public static final String GLOBAL = "%luckperms:prefix%{sender}%luckperms:suffix%<dark_gray> » <white>{message}";
        public static final String STAFF = "<dark_red>[Staff]</dark_red> %luckperms:prefix%{sender}<dark_gray> » <aqua>{message}";
        public static final String NAME_HOVER = "<yellow>{nickname}</yellow>\n<gray>Nombre real: <white>{name}</white>\n\n<gray>Clic para escribirle un mensaje privado";
        public static final String LINK_TOOLTIP = "<gray>Clic para abrir el enlace";

        private Format() {
        }
    }

    public static final class Channel {
        public static final String CHAT_DESCRIPTION = "Cambia el canal en el que escribes";
        public static final String ALL_DESCRIPTION = "Envía un mensaje al chat global sin cambiar de canal";
        public static final String STAFF_DESCRIPTION = "Envía un mensaje al chat del staff sin cambiar de canal";
        public static final String SWITCHED = "Ahora escribes en el canal <bold>{channel}</bold>.";
        public static final String CURRENT = "Estás escribiendo en el canal <bold>{channel}</bold>.";
        public static final String UNKNOWN = "No existe el canal <bold>{channel}</bold>.";
        public static final String DENIED = "No tienes permiso para usar el canal <bold>{channel}</bold>.";

        private Channel() {
        }
    }
}
