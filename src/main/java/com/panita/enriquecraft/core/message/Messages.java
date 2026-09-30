package com.panita.enriquecraft.core.message;

/**
 * Every player-visible text of the mod, in Spanish, grouped by feature. Vanilla clients cannot
 * resolve custom translation keys, so texts are sent as literal templates.
 * <p>
 * Templates accept text tags, {@code %placeholders%} and {@code {arguments}}; see {@link Message}.
 */
public final class Messages {

    private Messages() {
    }

    public static final class Gui {
        public static final String PREVIOUS = "<green>◀ Página anterior";
        public static final String NEXT = "<green>Página siguiente ▶";
        public static final String CLOSE = "<red>Cerrar";
        public static final String BACK = "<yellow>Volver";
        public static final String PAGE_INDICATOR = "<gray>Página <white>{page}</white> de <white>{pages}</white>";
        public static final String EMPTY = "<gray>No hay nada que mostrar";

        private Gui() {
        }
    }

    public static final class Prefix {
        public static final String DEFAULT = "<color #F2B134>[Enriquecraft]</color>";

        private Prefix() {
        }
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

    public static final class Reload {
        public static final String DESCRIPTION = "Recarga la configuración del mod";
        public static final String SUCCESS = "Configuración recargada.";
        public static final String WITH_ISSUES = "Configuración recargada con <bold>{count}</bold> aviso(s). Revisa la consola para ver el detalle.";
        public static final String INVALID_VALUE = "<gray>-</gray> <yellow>{path}</yellow>: valor no válido, se usa el valor por defecto.";
        public static final String UNKNOWN_KEY = "<gray>-</gray> <yellow>{path}</yellow>: clave desconocida, no tiene efecto.";
        public static final String SYNTAX_ERROR = "El archivo de configuración tiene errores de sintaxis y no se pudo leer. Se mantienen los valores anteriores. Revisa la consola para ver el detalle.";

        private Reload() {
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
        public static final String DESCRIPTION = "Envía un mensaje a todos los jugadores, con o sin prefijo";

        private Broadcast() {
        }
    }
}
