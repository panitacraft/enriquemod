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
