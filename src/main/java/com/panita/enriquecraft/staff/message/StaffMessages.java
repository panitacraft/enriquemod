package com.panita.enriquecraft.staff.message;

/**
 * Every player-visible text of the staff module, in Spanish, grouped by feature. The module owns
 * its texts; see {@code core.message.Messages} for the core module's.
 */
public final class StaffMessages {

    private StaffMessages() {
    }

    public static final class Staff {
        public static final String DESCRIPTION = "Herramientas para el equipo de administración";

        private Staff() {
        }
    }

    public static final class Coordinates {
        public static final String DESCRIPTION = "Gestiona las coordenadas guardadas; sin argumentos abre el menú";
        public static final String SAVE_DESCRIPTION = "Guarda tu posición actual con un nombre";
        public static final String REMOVE_DESCRIPTION = "Elimina una coordenada guardada";
        public static final String TP_DESCRIPTION = "Te teletransporta a una coordenada guardada";

        public static final String SAVED = "Coordenada <bold>{name}</bold> guardada en {dimension} (<white>{x}, {y}, {z}</white>).";
        public static final String DUPLICATE = "Ya existe una coordenada llamada <bold>{name}</bold>.";
        public static final String INVALID_NAME = "El nombre debe tener de 1 a 32 caracteres: letras, números, _ y -.";
        public static final String REMOVED = "Coordenada <bold>{name}</bold> eliminada.";
        public static final String NOT_FOUND = "No existe ninguna coordenada llamada <bold>{name}</bold>.";
        public static final String TELEPORTED = "Te has teletransportado a <bold>{name}</bold>.";
        public static final String DIMENSION_UNAVAILABLE = "La dimensión de <bold>{name}</bold> ({dimension}) no está disponible.";

        public static final String MENU_TITLE = "Coordenadas guardadas";
        public static final String ENTRY_NAME = "<gold>{name}";
        public static final String ENTRY_DIMENSION = "<gray>Dimensión: <white>{dimension}";
        public static final String ENTRY_POSITION = "<gray>Posición: <white>{x}, {y}, {z}";
        public static final String ENTRY_SAVED_BY = "<gray>Guardada por: <white>{player}";
        public static final String ENTRY_DATE = "<gray>Fecha: <white>{date}";
        public static final String ENTRY_CLICK_HINT = "<green>Clic izquierdo para teletransportarte";

        private Coordinates() {
        }
    }
}
