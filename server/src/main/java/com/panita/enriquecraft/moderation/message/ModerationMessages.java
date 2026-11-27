package com.panita.enriquecraft.moderation.message;

/**
 * The texts of the moderation module, in Spanish. The disconnect screens begin with the mod prefix on its own line, so the
 * reason sits below it.
 */
public final class ModerationMessages {

    private ModerationMessages() {
    }

    public static final class Common {
        /** Written where a reason belongs when staff gave none. */
        public static final String NO_REASON = "Sin motivo especificado";

        private Common() {
        }
    }

    public static final class Kick {
        /** The whole disconnect screen: nothing but the prefix and the reason. */
        public static final String SCREEN = "{{PREFIX}}\n<white>{reason}";

        private Kick() {
        }
    }

    public static final class ClientMods {
        public static final String SCREEN = "{{PREFIX}}\n<red>No se permite jugar con <white>{mod}</white> en este servidor.</red>\n<gray>Quítalo de tu cliente para poder entrar.";

        private ClientMods() {
        }
    }

    public static final class Ban {
        public static final String DESCRIPTION = "Banea a un jugador, para siempre o por un tiempo";
        public static final String SCREEN_PERMANENT = "{{PREFIX}}\n<red>Estás baneado permanentemente de este servidor.</red>\n<gray>Motivo: <white>{reason}";
        public static final String SCREEN_TEMPORARY = "{{PREFIX}}\n<red>Estás baneado de este servidor por <white>{time}</white>.</red>\n<gray>Motivo: <white>{reason}";
        public static final String BANNED_PERMANENT = "<bold>{player}</bold> fue baneado permanentemente. Motivo: <white>{reason}</white>";
        public static final String BANNED_TEMPORARY = "<bold>{player}</bold> fue baneado por <white>{time}</white>. Motivo: <white>{reason}</white>";
        public static final String ALREADY_BANNED = "Ese jugador ya está baneado.";

        private Ban() {
        }
    }

    public static final class VoiceChat {
        public static final String DESCRIPTION = "Administra el chat de voz";
        public static final String MUTE_DESCRIPTION = "Silencia el chat de voz de un jugador o de todos";
        public static final String UNMUTE_DESCRIPTION = "Devuelve el chat de voz a un jugador o a todos";
        public static final String MUTED_ALL = "El chat de voz fue silenciado para todos, menos para <bold>{staff}</bold> miembro(s) del staff.";
        public static final String UNMUTED_ALL = "El chat de voz volvió a estar abierto para todos.";
        public static final String MUTED_PLAYER = "<bold>{player}</bold> fue silenciado en el chat de voz.";
        public static final String UNMUTED_PLAYER = "<bold>{player}</bold> ya puede hablar en el chat de voz.";
        public static final String INVALID_PLAYER = "Ese no es un nombre de jugador válido: usa de 1 a 16 letras, números o _.";
        public static final String LUCKPERMS_MISSING = "LuckPerms no está instalado, así que no se puede cambiar el permiso del chat de voz.";

        private VoiceChat() {
        }
    }

    public static final class Mute {
        public static final String DESCRIPTION = "Silencia a un jugador, para siempre o por un tiempo";
        public static final String UNMUTE_DESCRIPTION = "Le devuelve la palabra a un jugador silenciado";
        public static final String NOTICE_PERMANENT = "<red>Estás silenciado y no puedes enviar mensajes.</red> <gray>Motivo: <white>{reason}";
        public static final String NOTICE_TEMPORARY = "<red>Estás silenciado por <white>{time}</white> más y no puedes enviar mensajes.</red> <gray>Motivo: <white>{reason}";
        public static final String MUTED_PERMANENT = "<bold>{player}</bold> fue silenciado permanentemente. Motivo: <white>{reason}</white>";
        public static final String MUTED_TEMPORARY = "<bold>{player}</bold> fue silenciado por <white>{time}</white>. Motivo: <white>{reason}</white>";
        public static final String ALREADY_MUTED = "Ese jugador ya está silenciado.";
        public static final String UNMUTED = "<bold>{player}</bold> ya no está silenciado.";
        public static final String UNMUTED_NOTICE = "Ya puedes volver a escribir en el chat.";
        public static final String NOT_MUTED = "Ese jugador no está silenciado.";

        private Mute() {
        }
    }
}
