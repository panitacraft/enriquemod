package com.panita.enriquecraft.link.message;

/**
 * Every player-visible text of the link module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class LinkMessages {

    private LinkMessages() {
    }

    public static final class Kick {
        public static final String DEACTIVATED = "<red>Tu cuenta de Panitacraft fue desactivada.</red>\n<gray>Si saliste del servidor de Discord, vuelve a unirte y usa <white>/register</white> para volver a jugar.";

        private Kick() {
        }
    }

    public static final class Sync {
        public static final String STARTED = "Sincronizando la lista blanca con Panitacraft...";
        public static final String DONE_CHANGED = "Lista blanca sincronizada con Panitacraft: <white>{added}</white> añadido(s), <white>{replaced}</white> con el modo cambiado, <white>{renamed}</white> renombrado(s), <white>{removed}</white> eliminado(s).";
        public static final String DONE_UNCHANGED = "La lista blanca ya coincide con Panitacraft (<white>{unchanged}</white> jugador(es) al día).";
        public static final String WAITING = "Esperan a desconectarse para aplicar su cambio: <white>{names}</white>.";
        public static final String PROBLEM_NOT_PREMIUM = "<bold>{name}</bold>: ninguna cuenta de Microsoft tiene ese nombre. El miembro debe corregir su tipo de cuenta con /register.";
        public static final String PROBLEM_LOOKUP_FAILED = "<bold>{name}</bold>: no se pudo consultar a Mojang. Se intentará de nuevo en la próxima sincronización.";
        public static final String PROBLEM_INVALID_NAME = "<bold>{name}</bold>: no es un nombre de jugador válido.";
        public static final String PROBLEM_DUPLICATE_NAME = "<bold>{name}</bold>: otro jugador de la lista tiene el mismo nombre y se ignoró.";
        public static final String FAILED = "No se pudo consultar la API de Panitacraft; la lista blanca no cambió. Revisa la consola.";
        public static final String NO_KEY = "No hay llave de la API de Panitacraft. Guárdala en <white>config/enriquecraft-link.key</white> y usa <white>/enriquecraft reload</white>.";
        public static final String REFUSED = "La lista de Panitacraft quitaría a <white>{leaving}</white> de <white>{linked}</white> jugadores vinculados, más del <white>{percent}%</white> permitido, y no se aplicó. Revisa la API o sube <white>link.maxRemovalPercent</white>.";
        public static final String BUSY = "Ya hay una sincronización en curso.";

        private Sync() {
        }
    }

    public static final class Status {
        public static final String KEY_FOUND = "Llave de la API: <green>encontrada</green>";
        public static final String KEY_MISSING = "Llave de la API: <red>no encontrada</red>";
        public static final String URL = "Dirección de la API: <white>{url}";
        public static final String LINKED = "Jugadores vinculados: <white>{count}";
        public static final String LAST_NONE = "Última sincronización: <gray>ninguna desde que arrancó el servidor";
        public static final String LAST_APPLIED = "Última sincronización: <green>aplicada</green> hace <white>{ago}</white>";
        public static final String LAST_REFUSED = "Última sincronización: <red>rechazada</red> hace <white>{ago}</white>";
        public static final String LAST_FAILED = "Última sincronización: <red>falló</red> hace <white>{ago}</white>";
        public static final String LAST_NO_KEY = "Última sincronización: <red>sin llave</red> hace <white>{ago}</white>";
        public static final String WAITING = "En espera de desconexión: <white>{names}";

        private Status() {
        }
    }

    public static final class Commands {
        public static final String LINK_DESCRIPTION = "Mantiene la lista blanca al día con las cuentas de Panitacraft";
        public static final String SYNC_DESCRIPTION = "Consulta la API de Panitacraft y actualiza la lista blanca ahora";
        public static final String STATUS_DESCRIPTION = "Muestra el estado de la sincronización con Panitacraft";

        private Commands() {
        }
    }
}
