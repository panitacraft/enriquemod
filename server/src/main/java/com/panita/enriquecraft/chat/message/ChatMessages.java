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
        public static final String GLOBAL = "%enriquecraft:emblems_system%%enriquecraft:rank%{sender}%luckperms:suffix%%enriquecraft:emblems_lore%<dark_gray> » <white>{message}";
        public static final String STAFF = "<dark_red>[Staff]</dark_red> %enriquecraft:rank%{sender}<dark_gray> » <aqua>{message}";
        public static final String EXTERNAL = "{sender}<dark_gray> » <white>{message}";
        public static final String EXTERNAL_STAFF = "<dark_red>[Staff]</dark_red> {sender}<dark_gray> » <aqua>{message}";
        public static final String NAME_HOVER = "<yellow>Jugador {nickname}</yellow>\n\n<#6BF3FA>· <#B574FC>Vida: <red>%player:health%\n<#6BF3FA>· <#B574FC>Experiencia: <green>{level} niveles\n<#6BF3FA>· <#B574FC>Latencia: <yellow>%player:ping%\n\n<i><gray>Visitar perfil en la web";
        public static final String EMOTE = "<light_purple>* {sender} {message}";
        public static final String PRIVATE_INCOMING = "<dark_gray>[<gold>{sender}</gold> <gray>→</gray> <gold>tú</gold>]</dark_gray> <white>{message}";
        public static final String PRIVATE_OUTGOING = "<dark_gray>[<gold>tú</gold> <gray>→</gray> <gold>{target}</gold>]</dark_gray> <white>{message}";
        public static final String TAB_NAME = "%enriquecraft:emblems_system%%enriquecraft:rank%%enriquecraft:name_text%%luckperms:suffix%%enriquecraft:emblems_lore%";
        public static final String NAME_TAG_PREFIX = "%enriquecraft:emblems_system%%enriquecraft:rank%";
        public static final String NAME_TAG_SUFFIX = "%luckperms:suffix%%enriquecraft:emblems_lore%";
        public static final String LINK_TOOLTIP = "<gray>Clic para abrir el enlace";

        private Format() {
        }
    }

    /** What pointing at the head of a player says: whether their account is a Microsoft one, and that a click opens the profile. */
    public static final class Head {
        public static final String PREMIUM = "<green>Cuenta premium";
        public static final String NO_PREMIUM = "<gray>Cuenta no premium";
        public static final String PROFILE = "<gray>Clic para ver el perfil";

        private Head() {
        }
    }

    /** The rank a bot shows (a player no client is behind, such as the ones Carpet spawns): LuckPerms has no user for it, so it has no group. */
    public static final class Bot {
        public static final String RANK = "&7&l[BOT]";
        public static final String TITLE = "<gray><bold>Bot";
        public static final String DESCRIPTION = "Es un jugador controlado por el servidor, no por una persona.";

        private Bot() {
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

    /** The text of the server list: two lines, and server placeholders such as the number of players online. */
    public static final class Motd {
        public static final String TEXT = "<bold><gradient:#920000:#BA5B5B:#920000>Enriquecraft</gradient></bold> <dark_gray>»</dark_gray> <gray>Servidor privado\n"
                + "<gray>Jugadores en línea: <white>%server:online%</white>/<white>%server:max_players%</white>";

        private Motd() {
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

    public static final class Sudo {
        public static final String DESCRIPTION = "Hace que un jugador ejecute un comando o diga un mensaje";
        public static final String DONE = "Hecho como <bold>{player}</bold>: <white>{text}</white>";
        public static final String TOO_POWERFUL = "No puedes usar sudo sobre un jugador con más permisos que tú.";
        public static final String NOT_AVAILABLE = "Ese jugador no está disponible.";

        private Sudo() {
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
        public static final String BAD_CHARACTERS = "El apodo solo puede llevar letras, números y _ , sin espacios.";
        public static final String TAKEN = "Ese nombre ya es de otro jugador o ya lo usa como apodo.";
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

    public static final class ShareCoordinates {
        public static final String DESCRIPTION = "Comparte tus coordenadas con todos o con un jugador";

        /** Sent to everyone. {player}, {x}, {y}, {z} and {dimension} describe the player who shares. */
        public static final String PUBLIC = "<aqua>{player}</aqua> <gray>se encuentra en <aqua>X:</aqua> {x} <aqua>Y:</aqua> {y} <aqua>Z:</aqua> {z} (<aqua>{dimension}</aqua>)</gray>";
        /** Sent to the chosen player. The arguments describe the player who shares. */
        public static final String PRIVATE = "<aqua>{player}</aqua> <gray>te ha compartido su ubicación, <aqua>X:</aqua> {x} <aqua>Y:</aqua> {y} <aqua>Z:</aqua> {z} (<aqua>{dimension}</aqua>)</gray>";
        /** Sent to a player who picks themselves. The arguments describe that player. */
        public static final String SELF = "<gray>Tu ubicación es <aqua>X:</aqua> {x} <aqua>Y:</aqua> {y} <aqua>Z:</aqua> {z} (<aqua>{dimension}</aqua>)</gray>";
        /** Sent to the player who shared with another one. {player} is the player who received it. */
        public static final String SENT_CONFIRMATION = "<gray>Has compartido tu ubicación con <aqua>{player}</aqua>.</gray>";
        public static final String NOT_ONLINE = "<red>El jugador {player} no está en línea.</red>";
        public static final String DISABLED = "Compartir coordenadas está desactivado.";

        private ShareCoordinates() {
        }
    }
}
