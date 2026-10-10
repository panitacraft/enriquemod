package com.panita.enriquecraft.auth.message;

/**
 * Every player-visible text of the auth module, in Spanish, grouped by feature. The module owns its
 * texts; see {@code core.message.Messages} for the core module's.
 */
public final class AuthMessages {

    private AuthMessages() {
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
