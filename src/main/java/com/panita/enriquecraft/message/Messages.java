package com.panita.enriquecraft.message;

/**
 * Every player-visible text of the mod, in Spanish, grouped by feature. Vanilla clients cannot
 * resolve custom translation keys, so texts are sent as literal templates.
 * <p>
 * Templates accept text tags, {@code %placeholders%} and {@code {arguments}}; see {@link Message}.
 */
public final class Messages {

    private Messages() {
    }

    public static final class Enriquecraft {
        public static final String DESCRIPTION = "Comandos generales de Enriquecraft";

        private Enriquecraft() {
        }
    }

    public static final class Help {
        public static final String DESCRIPTION = "Muestra los comandos disponibles";
        public static final String HEADER = "<bold>Comandos disponibles</bold>";
        public static final String ENTRY = "<color #F2B134>/{command}</color> <gray>-</gray> {description}";
        public static final String USAGE_HEADER = "<bold>/{command}</bold> <gray>-</gray> {description}";
        public static final String USAGE_LINE = "<gray>-</gray> <color #F2B134>/{usage}</color>";
        public static final String UNKNOWN_COMMAND = "No existe un comando disponible llamado <bold>{command}</bold>.";

        private Help() {
        }
    }

    public static final class Info {
        public static final String DESCRIPTION = "Muestra información del mod y del servidor";
        public static final String BODY = """
                <bold>{name}</bold> <gray>v{version}</gray>
                <gray>Minecraft:</gray> {minecraft}
                <gray>Fabric Loader:</gray> {loader}
                <gray>Jugadores:</gray> {players}/{maxPlayers}""";

        private Info() {
        }
    }

    public static final class Ping {
        public static final String DESCRIPTION = "Muestra tu latencia con el servidor";
        public static final String RESULT = "Tu latencia es de <bold>{ping} ms</bold>.";
        public static final String PLAYERS_ONLY = "Este comando solo puede usarlo un jugador.";

        private Ping() {
        }
    }

    public static final class Broadcast {
        public static final String DESCRIPTION = "Envía un anuncio a todos los jugadores";
        public static final String FORMAT = "<bold>Anuncio</bold> <gray>»</gray> {message}";

        private Broadcast() {
        }
    }
}
