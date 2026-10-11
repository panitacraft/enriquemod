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
        public static final String EMOTE = "<light_purple>* {sender} {message}";
        public static final String PRIVATE_INCOMING = "<dark_gray>[<gold>{sender}</gold> <gray>→</gray> <gold>tú</gold>]</dark_gray> <white>{message}";
        public static final String PRIVATE_OUTGOING = "<dark_gray>[<gold>tú</gold> <gray>→</gray> <gold>{target}</gold>]</dark_gray> <white>{message}";
        public static final String LINK_TOOLTIP = "<gray>Clic para abrir el enlace";

        private Format() {
        }
    }

    /** The lines the game's messages about players turn into. {@code {player}} is the name the player shows. */
    public static final class Events {
        public static final String JOIN = "<dark_gray>[<green>+</green>]</dark_gray> <gray>{player} entró al servidor";
        public static final String JOIN_FIRST = "<dark_gray>[<gold>★</gold>]</dark_gray> <yellow>¡{player} se une al servidor por primera vez!";
        public static final String JOIN_RENAMED = "<dark_gray>[<green>+</green>]</dark_gray> <gray>{player} entró al servidor <dark_gray>(antes {oldName})";
        public static final String LEAVE = "<dark_gray>[<red>-</red>]</dark_gray> <gray>{player} salió del servidor";
        public static final String DEATH = "<dark_gray>[<red>☠</red>]</dark_gray> <gray>{message}";
        public static final String ADVANCEMENT = "<dark_gray>[<green>✔</green>]</dark_gray> <gray>{message}";

        private Events() {
        }
    }

    public static final class Welcome {
        public static final String TEXT = "<bold>¡Bienvenido, {player}!</bold>\n"
                + "<gray>Usa <yellow>/chat</yellow> para cambiar de canal y <yellow>/enriquecraft help</yellow> para ver los comandos.";

        private Welcome() {
        }
    }

    public static final class Private {
        public static final String REPLY_DESCRIPTION = "Responde al último jugador con el que hablaste en privado";
        public static final String NOT_AVAILABLE = "Ese jugador no está disponible.";
        public static final String NOBODY_TO_REPLY_TO = "No tienes a nadie a quien responder.";

        private Private() {
        }
    }

    public static final class Nickname {
        public static final String NICK_DESCRIPTION = "Cambia tu apodo en el chat";
        public static final String REAL_NAME_DESCRIPTION = "Muestra quién tiene un apodo";
        public static final String CHANGED = "Tu apodo ahora es {nickname}.";
        public static final String CHANGED_OTHER = "El apodo de <bold>{player}</bold> ahora es {nickname}.";
        public static final String CLEARED = "Tu apodo fue quitado.";
        public static final String CLEARED_OTHER = "Se quitó el apodo de <bold>{player}</bold>.";
        public static final String NOT_SET = "No tienes un apodo.";
        public static final String NOT_SET_OTHER = "<bold>{player}</bold> no tiene un apodo.";
        public static final String TOO_SHORT = "El apodo debe tener al menos <bold>{min}</bold> caracteres.";
        public static final String TOO_LONG = "El apodo puede tener hasta <bold>{max}</bold> caracteres.";
        public static final String BAD_CHARACTERS = "El apodo solo puede llevar letras, números, espacios y los signos _ . -";
        public static final String TAKEN = "Ese nombre ya lo usa otro jugador.";
        public static final String COOLDOWN = "Debes esperar <bold>{time}</bold> para cambiar el apodo otra vez.";
        public static final String REAL_NAME = "<bold>{nickname}</bold> es <bold>{name}</bold>.";
        public static final String REAL_NAME_NONE = "Nadie tiene el apodo <bold>{nickname}</bold>.";

        private Nickname() {
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
