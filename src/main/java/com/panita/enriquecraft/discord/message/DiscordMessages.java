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

        private Delivery() {
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
