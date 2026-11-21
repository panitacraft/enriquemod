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
        public static final String FIELD_NONE = "<dark_gray>Sin definir";
        public static final String FIELD_PRONOUNS = "<white>Pronombres:";
        public static final String FIELD_BIRTHDAY = "<white>Cumpleaños:";
        public static final String FIELD_BIO = "<white>Descripción:";
        public static final String FIELD_PLAY_TIME = "<white>Tiempo de juego:";
        public static final String FIELD_DEATHS = "<white>Muertes:";
        public static final String FIELD_BASE = "<white>Ubicación de la base:";
        public static final String FIELD_PRANK = "<white>Nivel de Pranks:";
        public static final String BASE_VALUE = "<gray>{position} <gray>({dimension})";
        public static final String LINE_ACCOUNT = "<gray>Cuenta Panita: <white>{id} <gray>({mode})";
        public static final String MODE_PREMIUM = "premium";
        public static final String MODE_PIN = "con PIN";
        public static final String NOT_FOUND = "No hay ningún jugador llamado <white>{name}</white> en este servidor.";
        public static final String EMBLEM_DETAIL = "ver el emblema";
        public static final String EDIT = "<yellow>Editar perfil";
        public static final String EDIT_LORE = "<gray>Cambia tu descripción, tus pronombres, tu base y lo que se muestra de ti.";
        public static final String VIEW = "<yellow>Volver al perfil";
        public static final String NOT_ALLOWED = "<red>No tienes permiso para ver perfiles.";

        private Menu() {
        }
    }

    public static final class Edit {
        public static final String BASE_MARK = "<green>Marcar ubicación";
        public static final String BASE_MARK_LORE = "<gray>Guarda el bloque donde estás y su dimensión.";
        public static final String BASE_CLEAR = "<red>Quitar ubicación";
        public static final String LOCATOR = "Compartir mi ubicación";
        public static final String BIO = "Descripción";
        public static final String SAVE = "Guardar cambios";
        public static final String PRANK_WAIT = "<dark_gray>Podrás cambiarlo en {time}";
        public static final String PRONOUNS_HINT = "Pronombres";
        public static final String PRONOUNS_SAVED = "Pronombres guardados.";
        public static final String PRONOUNS_TOO_LONG = "Tus pronombres tienen más de <white>{max}</white> caracteres. Acórtalos e inténtalo de nuevo.";
        public static final String EMBLEM_CHANGE = "cambiar este emblema";
        public static final String BASE_CLEAR_ICON = "<red>✕";
        public static final String PRANK_CHANGE = "cambiar el nivel";
        public static final String PRANK_CHOOSE = "elegir este nivel";
        public static final String PRANK_CURRENT = "<green>Es tu nivel actual.";
        public static final String PRANKS_TITLE = "Tu nivel de Pranks";
        public static final String BASE_SAVED = "Base guardada en <white>{position}</white>.";
        public static final String BASE_CLEARED = "Base eliminada.";
        public static final String BIO_SAVED = "Descripción guardada.";
        public static final String BIO_TOO_LONG = "Tu descripción tiene más de <white>{max}</white> caracteres. Acórtala e inténtalo de nuevo.";
        public static final String LOCATOR_ON = "Ahora los demás te ven en su barra de localización.";
        public static final String LOCATOR_OFF = "Ahora los demás no te ven en su barra de localización.";
        public static final String NOT_LINKED = "Tu cuenta no está vinculada a Panitacraft, así que no se pueden guardar estos datos.";
        public static final String DIALOG_DONE = "Listo";
        public static final String DIALOG_CLOSE = "Cerrar";
        public static final String DIALOG_EDIT = "Editar perfil";

        private Edit() {
        }
    }

    /** The texts of the dialogs players without the client companion see. */
    public static final class Dialogs {
        public static final String HEADING_ABOUT = "<gold><bold>Sobre mí";
        public static final String HEADING_GAME = "<gold><bold>En el juego";
        public static final String HUB_HINT = "<gray>Elige qué quieres cambiar. Pasa el cursor por un botón para ver lo que tiene ahora.";
        public static final String HUB_TEXTS = "Pronombres y descripción";
        public static final String HUB_BASE = "Ubicación";
        public static final String HUB_EMBLEMS = "Emblemas";
        public static final String HUB_PRANK = "Nivel de Pranks";
        public static final String LOCATOR_SHARED = "<green>Te ven en la barra de localización";
        public static final String LOCATOR_HIDDEN = "<red>No te ven en la barra de localización";
        public static final String TEXTS_NOTE = "<gray>Lo que escribas lo verá cualquiera que abra tu perfil.";
        public static final String TEXTS_PRONOUNS = "Pronombres (hasta {max} caracteres)";
        public static final String TEXTS_BIO = "Descripción (hasta {max} caracteres)";
        public static final String BASE_NOTE = "<gray>Marca el bloque donde estás como tu base. Dejar de compartir tu ubicación solo te oculta de la barra de localización: tu base se sigue viendo en tu perfil.";
        public static final String PRANK_CURRENT = "Tu nivel actual: {level}";
        public static final String PRANK_PICK = "<gray>Elige tu nuevo nivel.";
        public static final String EMBLEMS_INTRO = "<gray>Elige el lugar que quieres cambiar.";
        public static final String SLOT = "Lugar {n}: {emblem}";
        public static final String SLOT_HINT = "<gray>Haz clic para cambiar este lugar.";
        public static final String PICK_TITLE = "Emblema del lugar {n}";
        public static final String PICK_INTRO = "<gray>Elige el emblema para el lugar {n}. Si ya está en otro lugar, los dos lugares se intercambian; si es el de este lugar, lo quitas.";
        public static final String EMBLEMS_NONE = "<gray>Aún no tienes emblemas de esta edición.";
        public static final String SAVE = "Guardar";
        public static final String BACK = "Volver";
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
        public static final String HERE = "<green>Está en este lugar.";
        public static final String ELSEWHERE = "<gray>Ya lo muestras en otro lugar.";
        public static final String PUT = "ponerlo en este lugar";
        public static final String REMOVE = "quitarlo de este lugar";
        public static final String SWAP = "intercambiar lugares";
        public static final String EMPTY_TITLE = "Espacio vacío";
        public static final String EMPTY_DESCRIPTION = "Aún no elegiste un emblema para este espacio. Elígelo en tu perfil.";

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
