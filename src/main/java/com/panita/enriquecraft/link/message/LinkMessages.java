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
}
