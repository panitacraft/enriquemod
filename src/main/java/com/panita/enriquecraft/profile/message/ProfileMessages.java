package com.panita.enriquecraft.profile.message;

/**
 * Every player-visible text of the profile module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class ProfileMessages {

    private ProfileMessages() {
    }

    public static final class Commands {
        public static final String PROFILE_DESCRIPTION = "Muestra tu perfil y tus emblemas";
        public static final String REFRESH_DESCRIPTION = "Actualiza tus emblemas desde Panitacraft";
        public static final String REFRESH_ALL_DESCRIPTION = "Actualiza los emblemas de todos los jugadores conectados";

        private Commands() {
        }
    }

    public static final class Menu {
        public static final String TITLE = "Tu perfil";
        public static final String LINE_PLAYER = "<gray>Jugador: <white>{name}";
        public static final String LINE_DISCORD = "<gray>Discord: <white>{discord}";
        public static final String LINE_PLAY_TIME = "<gray>Tiempo de juego: <white>{time}";
        public static final String LINE_STATUS = "<gray>Estado de cuenta: <white>{status}";
        public static final String PREMIUM = "Premium";
        public static final String NO_PREMIUM = "No premium";
        public static final String LINE_DEATHS = "<gray>Muertes: <white>{count}";
        public static final String EMBLEMS_HEADER = "<light_purple>Emblemas";
        public static final String NO_DATA = "<gray>Tu cuenta no está vinculada a Panitacraft, o no se pudieron leer tus datos.";
        public static final String NO_EMBLEMS = "<gray>Aún no tienes emblemas de esta edición.";
        public static final String EMBLEMS = "<yellow>Emblemas";
        public static final String EMBLEMS_LORE = "<gray>Elige qué emblemas se ven junto a tu nombre.";
        public static final String EMBLEMS_OPEN = "ver tus emblemas";
        public static final String REFRESH = "<aqua>Actualizar";
        public static final String REFRESH_LORE = "<gray>Vuelve a leer tus emblemas desde Panitacraft.";

        private Menu() {
        }
    }

    public static final class Prank {
        /** The title of the level every player has until Panitacraft says otherwise. */
        public static final String DEFAULT_TITLE = "Pranks: Nivel Cero";

        private Prank() {
        }
    }

    public static final class Emblems {
        public static final String TITLE = "Tus emblemas";
        public static final String PRANK_DEFAULT = "Es tu nivel de Pranks por defecto y se muestra antes de tu nombre. Elige uno de tus otros niveles para mostrarlo en su lugar.";
        public static final String PRANK_SHOWN = "<green>Es tu nivel de Pranks y se muestra antes de tu nombre.";
        public static final String PRANK_HIDDEN = "<gray>Es uno de tus niveles de Pranks.";
        public static final String PRANK_RESET = "volver al nivel por defecto";
        public static final String SHOWN = "<green>Se muestra junto a tu nombre.";
        public static final String HIDDEN = "<gray>No se muestra.";
        public static final String HIDE = "ocultarlo";
        public static final String SHOW = "mostrarlo";
        public static final String FULL = "Solo puedes mostrar <white>{max}</white> emblema(s) junto a tu nombre. Oculta uno primero.";
        public static final String RESET = "<red>Restablecer";
        public static final String RESET_LORE = "<gray>Vuelve a mostrar tus primeros emblemas, sin el orden que elegiste.";
        public static final String RESET_TITLE = "Restablecer emblemas";
        public static final String RESET_HEADLINE = "<yellow>¿Restablecer tus emblemas?";
        public static final String RESET_WARNING = "<gray>Perderás la selección y el orden que elegiste.";

        private Emblems() {
        }
    }

    public static final class Refresh {
        public static final String UPDATED = "Tus emblemas se actualizaron.";
        public static final String NOT_LINKED = "Tu cuenta no está vinculada a Panitacraft, así que no hay emblemas que mostrar.";
        public static final String FAILED = "No se pudo consultar Panitacraft. Inténtalo de nuevo en un rato.";
        public static final String BUSY = "Ya se está actualizando tu perfil.";
        public static final String COOLDOWN = "Espera <white>{seconds}</white> segundo(s) para volver a actualizar tus emblemas.";
        public static final String ALL_DONE = "Emblemas actualizados: <white>{updated}</white> jugador(es), <white>{notLinked}</white> sin cuenta vinculada y <white>{failed}</white> con error.";
        public static final String ALL_BUSY = "Ya hay una actualización de todos los jugadores en curso.";

        private Refresh() {
        }
    }
}
