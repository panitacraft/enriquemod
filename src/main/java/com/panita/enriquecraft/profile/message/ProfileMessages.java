package com.panita.enriquecraft.profile.message;

/**
 * Every player-visible text of the profile module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class ProfileMessages {

    private ProfileMessages() {
    }

    public static final class Commands {
        public static final String PROFILE_DESCRIPTION = "Muestra tu perfil o el de otro jugador";
        public static final String EDIT_DESCRIPTION = "Edita tu perfil";
        public static final String REFRESH_DESCRIPTION = "Actualiza tus emblemas desde Panitacraft";
        public static final String REFRESH_ALL_DESCRIPTION = "Actualiza los emblemas de todos los jugadores conectados";
        public static final String PRANK_DESCRIPTION = "Muestra tu nivel de Pranks y los que puedes elegir";
        public static final String PRANK_SET_DESCRIPTION = "Cambia tu nivel de Pranks";

        private Commands() {
        }
    }

    public static final class Menu {
        public static final String TITLE = "Tu perfil";
        public static final String TITLE_OTHER = "Perfil de {name}";
        public static final String TITLE_EDIT = "Editando tu perfil";
        public static final String LINE_DISCORD = "<gray>Discord: <white>{discord}";
        public static final String LINE_PLAY_TIME = "<gray>Tiempo de juego: <white>{time}";
        public static final String LINE_BIRTHDAY = "<gray>Cumpleaños: <white>{date}";
        public static final String LINE_BASE = "<gray>Base: <white>{position} <gray>({dimension})";
        public static final String LINE_NO_BASE = "<gray>Base: <dark_gray>sin marcar";
        public static final String LINE_LOCATOR = "<gray>Barra de localización: {state}";
        public static final String LOCATOR_SHARED = "<green>visible";
        public static final String LOCATOR_HIDDEN = "<red>oculto";
        public static final String LINE_ACCOUNT = "<gray>Cuenta Panita: <white>{id} <gray>({mode})";
        public static final String MODE_PREMIUM = "premium";
        public static final String MODE_PIN = "con PIN";
        public static final String BIO_HEADER = "<light_purple>Bio";
        public static final String NO_BIO = "<gray>Sin bio";
        public static final String EMBLEMS_HEADER = "<light_purple>Emblemas";
        public static final String NO_DATA = "<gray>Esta cuenta no está vinculada a Panitacraft, o no se pudieron leer sus datos.";
        public static final String NO_EMBLEMS = "<gray>Aún no hay emblemas de esta edición.";
        public static final String NOT_FOUND = "No hay ningún jugador llamado <white>{name}</white> en este servidor.";
        public static final String EMBLEM_DETAIL = "ver el emblema";
        public static final String REFRESH = "<aqua>Actualizar";
        public static final String REFRESH_LORE = "<gray>Vuelve a leer los datos desde Panitacraft.";
        public static final String EDIT = "<yellow>Editar perfil";
        public static final String EDIT_LORE = "<gray>Cambia tu bio, tu base y lo que se muestra de ti.";
        public static final String VIEW = "<yellow>Volver al perfil";
        public static final String NOT_ALLOWED = "<red>No tienes permiso para ver perfiles.";

        private Menu() {
        }
    }

    public static final class Edit {
        public static final String PRANK = "Nivel de Pranks";
        public static final String BASE = "Base";
        public static final String BASE_MARK = "<green>Marcar base";
        public static final String BASE_MARK_LORE = "<gray>Guarda el bloque donde estás y su dimensión.";
        public static final String BASE_CLEAR = "<red>Eliminar base";
        public static final String BIRTHDAY_NOTE = "<gray>Tu cumpleaños se cambia en la web de Panitacraft.";
        public static final String BIRTHDAY_NONE = "sin definir";
        public static final String LOCATOR = "Mostrarme en la barra de localización";
        public static final String LOCATOR_LORE = "<gray>Si lo apagas, los demás no te ven en su barra de localización.";
        public static final String BIO = "Bio";
        public static final String BIO_SAVE = "Guardar bio";
        public static final String EMBLEMS = "<yellow>Elegir emblemas";
        public static final String EMBLEMS_LORE = "<gray>Elige cuáles se ven junto a tu nombre y en qué orden.";
        public static final String BASE_SAVED = "Base guardada en <white>{position}</white>.";
        public static final String BASE_CLEARED = "Base eliminada.";
        public static final String BIO_SAVED = "Bio guardada.";
        public static final String BIO_TOO_LONG = "Tu bio tiene más de <white>{max}</white> caracteres. Acórtala e inténtalo de nuevo.";
        public static final String LOCATOR_ON = "Ahora los demás te ven en su barra de localización.";
        public static final String LOCATOR_OFF = "Ahora los demás no te ven en su barra de localización.";
        public static final String NOT_LINKED = "Tu cuenta no está vinculada a Panitacraft, así que no se pueden guardar estos datos.";
        public static final String DIALOG_DONE = "Listo";
        public static final String DIALOG_CANCEL = "Cancelar";
        public static final String DIALOG_CLOSE = "Cerrar";
        public static final String DIALOG_EDIT = "Editar perfil";
        public static final String DIALOG_REFRESH = "Actualizar";
        public static final String DIALOG_EMBLEMS = "Emblemas";

        private Edit() {
        }
    }

    /** The day and month of a birthday, one template for each month, with the day as {day}. */
    public static final class Months {
        public static final String JANUARY = "{day} de enero";
        public static final String FEBRUARY = "{day} de febrero";
        public static final String MARCH = "{day} de marzo";
        public static final String APRIL = "{day} de abril";
        public static final String MAY = "{day} de mayo";
        public static final String JUNE = "{day} de junio";
        public static final String JULY = "{day} de julio";
        public static final String AUGUST = "{day} de agosto";
        public static final String SEPTEMBER = "{day} de septiembre";
        public static final String OCTOBER = "{day} de octubre";
        public static final String NOVEMBER = "{day} de noviembre";
        public static final String DECEMBER = "{day} de diciembre";

        private Months() {
        }
    }

    public static final class Prank {
        public static final String ZERO = "Pranks: Nivel Cero";
        public static final String ONE = "Pranks: Nivel Uno";
        public static final String ONE_PLUS = "Pranks+: Nivel Uno";
        public static final String TWO = "Pranks: Nivel Dos";
        public static final String TWO_PLUS = "Pranks+: Nivel Dos";
        public static final String THREE = "Pranks: Nivel Tres";
        public static final String THREE_PLUS = "Pranks+: Nivel Tres";
        public static final String CHANGED = "<yellow>{player}</yellow> cambió su nivel de Pranks a <white>{level}</white>.";
        public static final String COOLDOWN = "Podrás cambiar tu nivel de Pranks de nuevo en <white>{time}</white>.";
        public static final String NOT_LINKED = "Tu cuenta no está vinculada a Panitacraft, así que no se puede guardar tu nivel de Pranks.";
        public static final String SAME = "Ya estás en el nivel <white>{level}</white>.";
        public static final String LOCKED = "El nivel inicial no se puede elegir: solo es el punto de partida.";
        public static final String UNKNOWN = "<white>{level}</white> no es un nivel de Pranks. Usa <white>/prank</white> para ver los que hay.";
        public static final String LIST_CURRENT = "Tu nivel de Pranks es <white>{level}</white>.";
        public static final String LIST_HEADER = "<gray>Niveles que puedes elegir (haz clic para escribir el comando):";
        public static final String LIST_ENTRY = "<gray>- <white>{title}</white> <dark_gray>({id})";
        public static final String LIST_HOVER = "Escribe <white>/prank set</white> con este nivel";

        private Prank() {
        }
    }

    public static final class Emblems {
        public static final String TITLE = "Tus emblemas";
        public static final String EMPTY_TITLE = "Espacio vacío";
        public static final String EMPTY_DESCRIPTION = "Aún no elegiste un emblema para este espacio. Elígelo en tu perfil.";
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
