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

    public static final class Session {
        public static final String REMEMBERED = "<gray>Tu sesión sigue activa: no necesitas ingresar tu PIN.";

        private Session() {
        }
    }

    /** Shown on the disconnect screen, so a vanilla client can read them. */
    public static final class Refusal {
        public static final String NOT_WHITELISTED = "<red>No estás en la lista blanca de este servidor.";
        public static final String BLOCKED = "<red>Demasiados intentos fallidos.</red>\n<gray>Vuelve a intentarlo en <white>{minutes}</white> minuto(s).";
        public static final String UNAVAILABLE = "<red>No se pudo verificar tu cuenta con Mojang.</red>\n<gray>Inténtalo de nuevo en un momento.";
        public static final String TOO_MANY_ATTEMPTS = "<red>PIN incorrecto demasiadas veces.</red>\n<gray>Vuelve a entrar para intentarlo de nuevo.";
        public static final String TIMEOUT = "<red>Tardaste demasiado en ingresar tu PIN.</red>\n<gray>Vuelve a entrar para intentarlo de nuevo.";
        public static final String LOGGED_OUT = "<yellow>Cerraste tu sesión.</yellow>\n<gray>Tendrás que ingresar tu PIN al volver a entrar.";

        private Refusal() {
        }
    }
}
