package com.panita.enriquecraft.auth.message;

/**
 * Every player-visible text of the auth module, in Spanish, grouped by feature. The module owns its
 * texts; see {@code core.message.Messages} for the core module's.
 */
public final class AuthMessages {

    private AuthMessages() {
    }

    public static final class Pin {
        public static final String SCREEN_TITLE = "<bold><color #E040FB>P</color><color #E33EEE>A</color><color #E63CE1>N</color><color #E93AD4>I</color><color #EC38C7>T</color><color #F036BA>A</color><color #F335AC>C</color><color #F6339F>R</color><color #F93192>A</color><color #FC2F85>F</color><color #FF2D78>T</color></bold>";

        public static final String ENTER_TITLE = "Ingresa tu PIN";
        public static final String CHANGE_TITLE = "Ingresa tu PIN actual";
        public static final String CHANGE_HINT = "<gray>Luego elegirás uno nuevo";
        public static final String ENTER_HINT = "<gray>Hola, <white>{name}";
        public static final String CREATE_TITLE = "Crea tu PIN";
        public static final String CREATE_HINT = "<gray>Elige <white>{length}</white> dígitos que recuerdes";
        public static final String CONFIRM_TITLE = "Confirma tu PIN";
        public static final String CONFIRM_HINT = "<gray>Escríbelo de nuevo";
        public static final String WRONG_TITLE = "<red>PIN incorrecto";
        public static final String WRONG_HINT = "<gray>Te quedan <white>{attempts}</white> intento(s)";
        public static final String LOCKED_HINT = "<gray>Ya no te quedan intentos";
        public static final String MISMATCH_TITLE = "<red>No coinciden";
        public static final String MISMATCH_HINT = "<gray>Elige tu PIN otra vez";
        public static final String SUCCESS_TITLE = "<green>Bienvenido";
        public static final String SUCCESS_HINT = "<gray>Sesión recordada por <white>{hours}</white> hora(s)";
        public static final String CREATED_HINT = "<gray>PIN creado, sesión recordada por <white>{hours}</white> hora(s)";

        public static final String DIGIT = "<white>{digit}";
        public static final String DIGIT_LORE = "<gray>Clic para escribir este número";
        public static final String BACKSPACE = "<yellow>Borrar";
        public static final String BACKSPACE_LORE = "<gray>Borra el último número";
        public static final String CLEAR = "<red>Limpiar";
        public static final String CLEAR_LORE = "<gray>Borra todo lo escrito";
        public static final String TYPED = "<green>Número escrito";
        public static final String PENDING = "<gray>Falta un número";
        public static final String ATTEMPT_LEFT = "<green>Intento disponible";
        public static final String ATTEMPT_USED = "<red>Intento usado";
        public static final String IDENTITY = "<white>{title}";

        private Pin() {
        }
    }

    public static final class Whitelist {
        public static final String MODE_PREMIUM = "premium";
        public static final String MODE_CRACKED = "con PIN";

        public static final String ADDED = "<bold>{name}</bold> añadido a la lista blanca como cuenta <green>{mode}</green>.";
        public static final String REPLACED = "<bold>{name}</bold> ahora es una cuenta <green>{mode}</green>. Datos del jugador copiados: <white>{copied}</white> archivo(s).";
        public static final String REPLACED_KEPT = " Ya existían <white>{kept}</white> del nuevo modo y no se tocaron.";
        public static final String UNCHANGED = "<bold>{name}</bold> ya está en la lista blanca como cuenta <green>{mode}</green>.";
        public static final String NOT_PREMIUM = "No existe ninguna cuenta de Microsoft llamada <bold>{name}</bold>. Usa <white>cracked</white> si no tiene cuenta.";
        public static final String LOOKUP_FAILED = "No se pudo consultar a Mojang sobre <bold>{name}</bold>. Indica el modo: <white>cracked</white> o <white>premium</white>.";
        public static final String MUST_BE_OFFLINE = "<bold>{name}</bold> está conectado. Desconéctalo primero para cambiar su modo, así sus datos se copian completos.";
        public static final String INVALID_NAME = "<bold>{name}</bold> no es un nombre de jugador válido.";
        public static final String REMOVED = "<bold>{name}</bold> eliminado de la lista blanca.";
        public static final String NOT_LISTED = "<bold>{name}</bold> no está en la lista blanca.";

        private Whitelist() {
        }
    }

    public static final class Commands {
        public static final String AUTH_DESCRIPTION = "Administra el acceso de los jugadores: PIN, sesiones y modo de cuenta";
        public static final String RESET_DESCRIPTION = "Borra el PIN y la sesión de un jugador; elegirá un PIN nuevo al entrar";
        public static final String FORCE_LOGIN_DESCRIPTION = "Deja entrar sin PIN, por esta vez, a un jugador que espera";
        public static final String MODE_DESCRIPTION = "Cambia a un jugador de la lista blanca entre cuenta premium y con PIN";
        public static final String LOGOUT_DESCRIPTION = "Cierra tu sesión: tendrás que ingresar tu PIN al volver a entrar";

        public static final String PIN_DESCRIPTION = "Administra tu PIN de acceso";
        public static final String CHANGE_DESCRIPTION = "Cambia tu PIN: escribes el actual y luego eliges uno nuevo";
        public static final String CHANGE_REFUSED = "Demasiados intentos con el PIN actual: tu PIN no se cambió.";
        public static final String PREMIUM_HAS_NO_PIN = "Las cuentas premium no usan PIN: entras con tu cuenta de Microsoft.";
        public static final String CHANGE_BLOCKED = "Demasiados intentos fallidos desde tu conexión. Espera unos minutos antes de cambiar tu PIN.";
        public static final String NOT_A_SERVER = "Esto solo funciona en un servidor, no en un mundo de un jugador.";
        public static final String NO_PIN_YET = "Todavía no tienes un PIN guardado.";
        public static final String RESET_DONE = "PIN de <bold>{name}</bold> borrado. Elegirá uno nuevo la próxima vez que entre.";
        public static final String NO_ACCOUNT = "<bold>{name}</bold> no tiene un PIN guardado.";
        public static final String FORCED = "<bold>{name}</bold> entró sin PIN.";
        public static final String NOT_WAITING = "<bold>{name}</bold> no está esperando su PIN.";
        public static final String LOGGED_OUT_PREMIUM = "Las cuentas premium no usan PIN: no hay sesión que cerrar.";

        private Commands() {
        }
    }

    public static final class Session {
        public static final String REMEMBERED = "<gray>Tu sesión sigue activa: no necesitas ingresar tu PIN.";

        private Session() {
        }
    }

    /** Shown on the disconnect screen, so a vanilla client can read them. */
    public static final class Refusal {
        public static final String NOT_WHITELISTED = "<red>No estás en la lista blanca de este servidor.";
        public static final String BLOCKED = "<red>Demasiados intentos fallidos.</red>\n<gray>Vuelve a intentarlo en <white>{minutes}</white> minuto(s).";
        public static final String ALREADY_ONLINE = "<red>Ya hay alguien conectado con ese nombre.</red>\n<gray>Si eres tú, espera unos segundos y vuelve a intentarlo.";
        public static final String UNAVAILABLE = "<red>No se pudo verificar tu cuenta con Mojang.</red>\n<gray>Inténtalo de nuevo en un momento.";
        public static final String TOO_MANY_ATTEMPTS = "<red>PIN incorrecto demasiadas veces.</red>\n<gray>Vuelve a entrar para intentarlo de nuevo.";
        public static final String TIMEOUT = "<red>Tardaste demasiado en ingresar tu PIN.</red>\n<gray>Vuelve a entrar para intentarlo de nuevo.";
        public static final String LOGGED_OUT = "<yellow>Cerraste tu sesión.</yellow>\n<gray>Tendrás que ingresar tu PIN al volver a entrar.";

        private Refusal() {
        }
    }
}
