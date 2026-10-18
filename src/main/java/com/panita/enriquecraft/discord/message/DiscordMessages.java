package com.panita.enriquecraft.discord.message;

/**
 * Every player-visible text of the discord module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class DiscordMessages {

    private DiscordMessages() {
    }

    public static final class Status {
        public static final String OFF_DISABLED = "Discord: <gray>desactivado</gray> (<white>discord.enabled</white>)";
        public static final String OFF_NO_URL = "Discord: <red>sin dirección</red>. Indica la del puente en <white>discord.url</white> y usa <white>/enriquecraft reload</white>.";
        public static final String OFF_NO_KEY = "Discord: <red>sin llave</red>. Guárdala en <white>config/enriquecraft-discord.key</white> y usa <white>/discord reconnect</white>.";
        public static final String OFF_IDLE = "Discord: <gray>inactivo</gray>. Usa <white>/discord reconnect</white> para conectar.";
        public static final String CONNECTING = "Discord: <yellow>conectando...</yellow>";
        public static final String WAITING = "Discord: <red>sin conexión</red>, se reintenta solo.";
        public static final String INCOMPATIBLE = "Discord: <red>protocolo incompatible</red> con el bot. Actualiza el mod o el bot y usa <white>/discord reconnect</white>.";
        public static final String CONNECTED = "Discord: <green>conectado</green> desde <white>{since}</white>";
        public static final String CHANNELS = "Canales unidos: <white>{channels}";
        public static final String NO_CHANNELS = "El bot no tiene canales unidos.";
        public static final String ADDRESS = "Dirección: <white>{url}";
        public static final String FRAMES = "Tramas: <white>{sent}</white> enviadas, <white>{received}</white> recibidas, <white>{dropped}</white> descartadas";
        public static final String LAST_ERROR = "Último error: <gray>{error}";

        private Status() {
        }
    }

    public static final class Reconnect {
        public static final String STARTED = "Conectando con el bot de Panitacraft...";

        private Reconnect() {
        }
    }

    public static final class Delivery {
        public static final String HOVER = "<gray>Mensaje desde Discord";
        /** The line before a reply, which says whose message it answers. */
        public static final String REPLY_TO = "<gray><i>(responde a {author})";

        private Delivery() {
        }
    }

    public static final class Media {
        public static final String LABEL = "<aqua>[{name}]";
        public static final String LABEL_SPOILER = "<aqua>[spoiler]";
        public static final String HOVER = "<gray>Haz clic para verla en un mapa\n<dark_gray>{details}";
        public static final String HOVER_SPOILER = "<gray>Marcada como spoiler\nHaz clic para verla en un mapa";
        public static final String LINK = " <green>(Link)";
        public static final String LINK_HOVER = "<gray>Abrir la imagen original";

        private Media() {
        }
    }

    public static final class Image {
        public static final String COMMAND_DESCRIPTION = "Muestra en un mapa una imagen que se envió desde Discord";
        public static final String SHOWING = "<gray>La imagen se mostrará en un mapa.";
        public static final String NOT_ALLOWED = "<red>No tienes permiso para ver imágenes.";
        public static final String EXPIRED = "<red>Esa imagen ya no está disponible.";
        public static final String BUSY = "<red>Ya estás cargando una imagen, espera un momento.";
        public static final String COOLDOWN = "<red>Espera unos segundos antes de ver otra imagen.";
        public static final String UNAVAILABLE = "<red>No se pudo descargar la imagen.";
        public static final String TOO_LARGE = "<red>La imagen es demasiado grande para mostrarla.";
        public static final String UNSUPPORTED = "<red>Ese formato de imagen no se puede mostrar en un mapa.";
        public static final String FAILED = "<red>No se pudo preparar la imagen.";

        private Image() {
        }
    }

    public static final class Players {
        /** The line the server shows for a player in the list the bot draws: their rank before the name, and what follows it. */
        public static final String ENTRY = "%luckperms:prefix%%player:displayname%%luckperms:suffix%";

        private Players() {
        }
    }

    public static final class Share {
        public static final String SENDING = "<gray>Enviando la captura...";
        public static final String UNAVAILABLE = "<red>Ahora no se pueden compartir capturas.";
        public static final String BUSY = "<red>Ya estás enviando una captura, espera un momento.";
        public static final String TOO_FAST = "<red>Estás compartiendo capturas muy rápido, espera un poco.";
        public static final String TOO_LARGE = "<red>La captura es demasiado grande.";
        public static final String INVALID = "<red>No se pudo leer la captura.";
        public static final String FAILED = "<red>No se pudo compartir la captura.";
        public static final String CHAT_NAME = "captura.jpg";

        private Share() {
        }
    }

    public static final class Commands {
        public static final String DISCORD_DESCRIPTION = "Administra la conexión del servidor con el bot de Discord";
        public static final String STATUS_DESCRIPTION = "Muestra si el servidor está conectado al bot y qué canales usa";
        public static final String RECONNECT_DESCRIPTION = "Vuelve a conectar con el bot ahora";

        private Commands() {
        }
    }
}
