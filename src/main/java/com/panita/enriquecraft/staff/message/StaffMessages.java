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
        public static final String ENTRY_NAME = "<gold>→ {name}";
        public static final String ENTRY_ID = "<gray>ID: <white>{id}";
        public static final String ENTRY_DIMENSION = "<gray>Dimensión: <white>{dimension}";
        public static final String ENTRY_POSITION = "<gray>Posición: <white>{x}, {y}, {z}";
        public static final String ENTRY_SAVED_BY = "<gray>Guardada por: <white>{player}";
        public static final String ENTRY_DATE = "<gray>Fecha: <white>{date}";

        public static final String DETAIL_TITLE = "Detalles de la coordenada";
        public static final String DETAIL_NAME = "<gold>{name}";
        public static final String DETAIL_NAME_HINT = "<yellow>Nombre visible";
        public static final String DETAIL_TELEPORT = "<green>Ir a esta coordenada";
        public static final String DETAIL_TELEPORT_LORE = "<gray>Te teletransporta hasta aquí";
        public static final String DETAIL_DELETE = "<red>Eliminar";
        public static final String DETAIL_DELETE_LORE = "<gray>Elimina esta coordenada";

        public static final String DELETE_TITLE = "Eliminar coordenada";
        public static final String DELETE_HEADLINE = "<red>¿Eliminar <gold>{name}</gold>?";
        public static final String DELETE_WARNING = "<gray>Esta acción no se puede deshacer.";

        public static final String ICON_MENU_TITLE = "Elegir icono";
        public static final String ICON_CURRENT = "<yellow>Icono actual";

        public static final String FILTER_DIMENSION = "<gray>Dimensión: <white>{dimension}";
        public static final String FILTER_TODAY = "<gray>Fecha: <white>hoy";
        public static final String FILTER_WEEK = "<gray>Fecha: <white>últimos 7 días";
        public static final String FILTER_MONTH = "<gray>Fecha: <white>últimos 30 días";
        public static final String FILTER_USER = "<gray>Usuario: <white>{player}";

        private Coordinates() {
        }
    }

    public static final class Items {
        public static final String DESCRIPTION = "Gestiona los objetos personalizados guardados; sin argumentos abre el menú";
        public static final String SAVE_DESCRIPTION = "Guarda el objeto de tu mano como objeto personalizado";
        public static final String REMOVE_DESCRIPTION = "Elimina un objeto personalizado guardado";
        public static final String INFO_DESCRIPTION = "Muestra los datos de un objeto personalizado";

        public static final String EMPTY_HAND = "Sostén en la mano el objeto que quieres guardar.";
        public static final String SAVED = "Objeto <bold>{name}</bold> guardado ({item} x{count}).";
        public static final String DUPLICATE = "Ya existe un objeto llamado <bold>{name}</bold>.";
        public static final String INVALID_NAME = "El nombre debe tener de 1 a 32 caracteres: letras minúsculas, números y _.";
        public static final String REMOVED = "Objeto <bold>{name}</bold> eliminado.";
        public static final String NOT_FOUND = "No existe ningún objeto llamado <bold>{name}</bold>.";
        public static final String GIVEN = "Has recibido <bold>{name}</bold>.";

        public static final String INFO_HEADER = "<bold>{name}</bold>";
        public static final String INFO_ID = "<gray>ID: <white>{id}";
        public static final String INFO_ITEM = "<gray>Objeto: <white>{item} x{count}";
        public static final String INFO_SAVED_BY = "<gray>Guardado por: <white>{player}";
        public static final String INFO_DATE = "<gray>Fecha: <white>{date}";

        public static final String MENU_TITLE = "Objetos personalizados";
        public static final String ENTRY_ID = "<gray>ID: <white>{id}";
        public static final String ENTRY_ID_LINE = "<dark_gray>{id}";
        public static final String ENTRY_SAVED_BY = "<gray>Guardado por: <white>{player}";
        public static final String ENTRY_DATE = "<gray>Fecha: <white>{date}";

        public static final String DETAIL_TITLE = "Detalles del objeto";
        public static final String DETAIL_GET = "<green>Obtener copia";
        public static final String DETAIL_GET_LORE = "<gray>Te da una copia de este objeto";
        public static final String DETAIL_DELETE = "<red>Eliminar";
        public static final String DETAIL_DELETE_LORE = "<gray>Elimina este objeto guardado";

        public static final String DELETE_TITLE = "Eliminar objeto";
        public static final String DELETE_HEADLINE = "<red>¿Eliminar <gold>{name}</gold>?";
        public static final String DELETE_WARNING = "<gray>Esta acción no se puede deshacer.";

        public static final String META_LINE = "<gray>{label}: <white>{value}";
        public static final String META_SECTION = "<gray>{label}:";
        public static final String META_VALUE = "<white>  {value}";
        public static final String META_NONE = "<gray>Sin datos adicionales";
        public static final String META_COUNT = "Cantidad";
        public static final String META_DURABILITY = "Durabilidad";
        public static final String META_ENCHANTMENTS = "Encantamientos";
        public static final String META_STORED_ENCHANTMENTS = "Encantamientos guardados";
        public static final String META_ATTRIBUTES = "Atributos";
        public static final String META_MODEL_DATA = "Datos de modelo";
        public static final String META_ITEM_MODEL = "Modelo";
        public static final String META_DYE = "Color";
        public static final String META_UNBREAKABLE = "Irrompible";
        public static final String META_YES = "Sí";
        public static final String META_REPAIR_COST = "Coste de reparación";
        public static final String META_RARITY = "Rareza";
        public static final String META_TOOLTIP_HIDDEN = "Tooltip oculto";
        public static final String META_HIDDEN_COMPONENTS = "Datos ocultos en el tooltip";
        public static final String META_OTHER = "Otros datos";

        private Items() {
        }
    }

    public static final class Deaths {
        public static final String DESCRIPTION = "Abre los inventarios de muerte de un jugador para inspeccionarlos y recuperarlos";
        public static final String SINGLE_PLAYER = "Indica un único jugador.";

        public static final String LIST_TITLE = "Muertes de {player}";
        public static final String ENTRY_NAME = "<red>☠ Muerte del {date}";
        public static final String ENTRY_CAUSE = "<gray>Causa: <white>{cause}";
        public static final String ENTRY_DIMENSION = "<gray>Dimensión: <white>{dimension}";
        public static final String ENTRY_POSITION = "<gray>Posición: <white>{x}, {y}, {z}";
        public static final String ENTRY_ITEMS = "<gray>Objetos: <white>{count}";
        public static final String ENTRY_XP = "<gray>Nivel de experiencia: <white>{level}";
        public static final String ENTRY_RESTORED = "<green>✔ Inventario devuelto el {date}";

        public static final String INSPECT_TITLE = "Inventario de {player}";
        public static final String BACK = "<yellow>Volver a la lista";
        public static final String TELEPORT_NAME = "<aqua>Ir al lugar de la muerte";
        public static final String TELEPORT_LORE = "<gray>Te teletransporta a las coordenadas exactas.";
        public static final String CHESTS_NAME = "<gold>Obtener en cofres";
        public static final String CHESTS_LORE = "<gray>Empaqueta todo el inventario en cofres y te los entrega.";
        public static final String RESTORE_NAME = "<green>Devolver al jugador";
        public static final String RESTORE_LORE = "<gray>Añade los objetos al inventario del jugador.";
        public static final String RESTORE_WARNING = "<red>El jugador debe estar conectado.";
        public static final String DELETE_NAME = "<dark_red>Eliminar registro";
        public static final String DELETE_LORE = "<gray>Borra este inventario de muerte para siempre.";
        public static final String DELETE_TITLE = "Eliminar muerte";
        public static final String DELETE_HEADLINE = "<red>¿Eliminar la <gold>muerte del {date}</gold>?";
        public static final String DELETE_WARNING = "<gray>Esta acción no se puede deshacer.";
        public static final String RESTORE_AGAIN = "<gold>Ya se devolvió: hacerlo otra vez duplica los objetos.";
        public static final String INFO_NAME = "<white>Datos de la muerte";

        public static final String CHEST_NAME = "<gold>Inventario de {player}";
        public static final String CHEST_NAME_PART = "<gold>Inventario de {player} ({number}/{total})";

        public static final String TELEPORTED = "Te has teletransportado al lugar de la muerte de <bold>{player}</bold>.";
        public static final String DIMENSION_UNAVAILABLE = "La dimensión {dimension} no está disponible.";
        public static final String ITEM_GIVEN = "Has recibido una copia de {item}.";
        public static final String CHESTS_GIVEN = "Has recibido <bold>{count}</bold> cofre(s) con el inventario de <bold>{player}</bold>.";
        public static final String RESTORED = "Se devolvieron <bold>{count}</bold> objeto(s) a <bold>{player}</bold>.";
        public static final String RESTORED_TO_PLAYER = "Un miembro del equipo te ha devuelto tus objetos perdidos.";
        public static final String TARGET_OFFLINE = "<bold>{player}</bold> debe estar conectado para recibir sus objetos.";
        public static final String DELETED = "Registro de muerte eliminado.";

        private Deaths() {
        }
    }
}
