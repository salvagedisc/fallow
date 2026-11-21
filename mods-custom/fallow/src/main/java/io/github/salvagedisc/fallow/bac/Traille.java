package io.github.salvagedisc.fallow.bac;

/**
 * Le calcul du bac à traille, sans rien de Sable : la corde tendue d'un pieu à l'autre tient le radeau, qui ne peut
 * que glisser le long d'elle, et le courant, pris de biais, le fait traverser. Tout est dans le plan (x, z), en
 * blocs et en secondes ; les résultats sont des accélérations, à multiplier par la masse du radeau.
 */
public final class Traille {
    /** Raideur du rappel vers la corde, par seconde carrée. */
    public static final double STIFFNESS = 6.0;
    /** Amortissement du rappel vers la corde, par seconde. */
    public static final double DAMPING = 4.0;
    /** Rappel au plus, en blocs par seconde carrée. */
    public static final double MAX_LATERAL = 15.0;
    /** Au-delà de cet écart à la corde, en blocs, la corde lâche. */
    public static final double SLIP = 8.0;
    /**
     * Vitesse visée sans courant, en blocs par seconde : le passeur tire sur la corde. L'eau freine le radeau, qui va
     * en eau calme à environ 1 bloc par seconde (mesuré sur un radeau de 3 × 3 planches).
     */
    public static final double HAND_SPEED = 1.4;
    /** Vitesse de traversée gagnée par bloc par seconde de courant le long du fleuve. */
    public static final double CURRENT_GAIN = 1.5;
    /** Vitesse d'un courant de rivière (écoulement unitaire), en blocs par seconde. */
    public static final double CURRENT_SPEED = 1.5;
    /** Le radeau s'arrête à cette distance d'un pieu, en blocs. */
    public static final double BERTH = 2.5;
    /** Rapidité avec laquelle le radeau prend sa vitesse le long de la corde, par seconde. */
    public static final double ALONG_RATE = 4.0;
    /** Accélération le long de la corde au plus, en blocs par seconde carrée. */
    public static final double MAX_ALONG = 6.0;

    private Traille() {
    }

    /** Où se trouve le radeau par rapport à la corde. */
    public record Place(double t, double length, double ux, double uz, double offsetX, double offsetZ) {
        /** Distance au pied de la perpendiculaire depuis le premier pieu, en blocs (0 à {@code length}). */
        public double along() {
            return t * length;
        }

        /** Écart à la corde, en blocs. */
        public double offset() {
            return Math.sqrt(offsetX * offsetX + offsetZ * offsetZ);
        }
    }

    /**
     * Le radeau en (x, z) vu depuis la corde de (ax, az) à (bx, bz) : la part du chemin faite ({@code t}, bornée à la
     * corde), la direction unitaire de la corde, et l'écart au point le plus proche de la corde.
     */
    public static Place place(double ax, double az, double bx, double bz, double x, double z) {
        double dx = bx - ax;
        double dz = bz - az;
        double length = Math.sqrt(dx * dx + dz * dz);
        if (length < 1.0e-6) {
            return new Place(0.0, 0.0, 0.0, 0.0, x - ax, z - az);
        }
        double ux = dx / length;
        double uz = dz / length;
        double t = Math.max(0.0, Math.min(1.0, ((x - ax) * ux + (z - az) * uz) / length));
        return new Place(t, length, ux, uz, x - (ax + dx * t), z - (az + dz * t));
    }

    /**
     * Rappel vers la corde, {@code [x, z]} : un ressort amorti sur l'écart, qui ne retient que le mouvement en travers
     * de la corde.
     */
    public static double[] hold(Place place, double vx, double vz) {
        double along = vx * place.ux() + vz * place.uz();
        double crossX = vx - along * place.ux();
        double crossZ = vz - along * place.uz();
        double ax = -STIFFNESS * place.offsetX() - DAMPING * crossX;
        double az = -STIFFNESS * place.offsetZ() - DAMPING * crossZ;
        double size = Math.sqrt(ax * ax + az * az);
        if (size > MAX_LATERAL) {
            ax *= MAX_LATERAL / size;
            az *= MAX_LATERAL / size;
        }
        return new double[] {ax, az};
    }

    /**
     * Vitesse de traversée visée, en blocs par seconde le long de la corde (positive vers le second pieu) : rien à
     * l'arrêt ou une fois à quai, sinon la traction du passeur plus ce que donne le courant pris de biais.
     *
     * @param direction 1 vers le second pieu, -1 vers le premier, 0 à l'arrêt
     * @param currentX courant (blocs par seconde), x
     * @param currentZ courant, z
     */
    public static double target(Place place, int direction, double currentX, double currentZ) {
        if (direction == 0 || place.length() <= 2.0 * BERTH) {
            return 0.0;
        }
        if (direction > 0 && place.along() >= place.length() - BERTH || direction < 0 && place.along() <= BERTH) {
            return 0.0;
        }
        // Le courant de travers à la corde (le long du fleuve) est celui que la coque inclinée change en traversée.
        double across = Math.abs(currentX * place.uz() - currentZ * place.ux());
        return direction * (HAND_SPEED + CURRENT_GAIN * across);
    }

    /** Accélération le long de la corde pour aller de la vitesse {@code speed} à {@code target}. */
    public static double along(double speed, double target) {
        double a = ALONG_RATE * (target - speed);
        return Math.max(-MAX_ALONG, Math.min(MAX_ALONG, a));
    }
}
