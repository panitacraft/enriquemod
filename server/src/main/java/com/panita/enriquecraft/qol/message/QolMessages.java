package com.panita.enriquecraft.qol.message;

/**
 * Every player-visible text of the quality-of-life module, in Spanish, grouped by feature. The module owns its texts; see
 * {@code core.message.Messages} for the core module's.
 */
public final class QolMessages {

    private QolMessages() {
    }

    public static final class ShareCoordinates {
        public static final String DESCRIPTION = "Comparte tus coordenadas con todos o con un jugador";

        /** Sent to everyone. {player}, {x}, {y}, {z} and {dimension} describe the player who shares. */
        public static final String PUBLIC = "&b{player} &7se encuentra en &bX: &7{x} &bY: &7{y} &bZ: &7{z} (&b{dimension}&7)";
        /** Sent to the chosen player. The arguments describe the player who shares. */
        public static final String PRIVATE = "&b{player} &7te ha compartido su ubicación, &bX: &7{x} &bY: &7{y} &bZ: &7{z} (&b{dimension}&7)";
        /** Sent to a player who picks themselves. The arguments describe that player. */
        public static final String SELF = "&7Tu ubicación es &bX: &7{x} &bY: &7{y} &bZ: &7{z} (&b{dimension}&7)";
        /** Sent to the player who shared with another one. {player} is the player who received it. */
        public static final String SENT_CONFIRMATION = "&7Has compartido tu ubicación con &b{player}&7.";
        public static final String NOT_ONLINE = "&cEl jugador {player} no está en línea.";
        public static final String DISABLED = "Compartir coordenadas está desactivado.";

        private ShareCoordinates() {
        }
    }
}
