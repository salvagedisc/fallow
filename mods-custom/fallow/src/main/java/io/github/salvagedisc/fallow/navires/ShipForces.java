package io.github.salvagedisc.fallow.navires;

import io.github.salvagedisc.fallow.bord.BordModule;

/**
 * Le calcul des deux poussées du module navires, sans rien de Sable : le vent sur la voilure et le vent contraire
 * du bord. Tout est dans le plan (x, z), en blocs et en secondes ; les masses sont celles de Sable (environ 1 par
 * bloc plein).
 */
public final class ShipForces {
    /** Coefficient du vent : force par bloc carré de voilure et par bloc par seconde de vent relatif. */
    public static final double WIND_COEFFICIENT = 0.12;
    /** Voilure prise en compte au plus, en blocs carrés : un très grand navire ne devient pas une feuille au vent. */
    public static final double MAX_AREA = 600.0;
    /** Un navire plus petit que cela dans ses trois dimensions n'est pas poussé (corps de Sable Ragdoll, petits objets). */
    public static final double MIN_SIZE = 3.0;
    /** Largeur, en blocs, de la bande du bord où le vent contraire monte sur un navire (plus large que pour un joueur). */
    public static final double EDGE_BAND = 48.0;
    /** Accélération vers les terres au bord même, en blocs par seconde carrée. */
    public static final double EDGE_ACCELERATION = 3.0;
    /** Accélération vers les terres au plus. */
    public static final double MAX_EDGE_ACCELERATION = 12.0;
    /** Au-delà du bord, part de la vitesse vers le large retirée chaque seconde. */
    public static final double EDGE_BRAKE = 0.8;
    /** Les rafales frappent au-dessus du centre de masse, à cette part de la hauteur du navire : elles le font gîter. */
    public static final double GUST_LEVER = 0.4;

    private ShipForces() {
    }

    /**
     * Voilure offerte au vent, en blocs carrés : la face du volume du navire que le vent voit, hauteur hors de l'eau
     * seulement, sans dépasser {@link #MAX_AREA}.
     *
     * @param sizeX largeur du navire en x
     * @param sizeZ largeur en z
     * @param height hauteur hors de l'eau
     * @param windX direction du vent, x (pas forcément unitaire)
     * @param windZ direction du vent, z
     */
    public static double area(double sizeX, double sizeZ, double height, double windX, double windZ) {
        double length = Math.sqrt(windX * windX + windZ * windZ);
        if (length < 1.0e-9 || height <= 0.0) {
            return 0.0;
        }
        double face = height * (Math.abs(windZ) / length * sizeX + Math.abs(windX) / length * sizeZ);
        return Math.min(MAX_AREA, face);
    }

    /**
     * Force du vent sur le navire, {@code [x, z]} : proportionnelle à la voilure et au vent relatif (le vent moins la
     * vitesse du navire). Un navire qui file avec le vent n'est plus poussé ; un navire face au vent est freiné.
     */
    public static double[] wind(double area, double windX, double windZ, double velocityX, double velocityZ, double multiplier) {
        double k = WIND_COEFFICIENT * multiplier * area;
        return new double[] {k * (windX - velocityX), k * (windZ - velocityZ)};
    }

    /**
     * Accélération du vent contraire du bord, vers les terres, en blocs par seconde carrée : nulle avant la bande,
     * elle monte jusqu'à {@link #EDGE_ACCELERATION} au bord, puis plus fort au-delà ; s'y ajoute un frein sur ce qui
     * va encore vers le large au-delà du bord.
     *
     * @param past distance au-delà du bord (négative en deçà)
     * @param outwardSpeed vitesse vers le large, en blocs par seconde (négative si le navire revient)
     */
    public static double edge(double past, double outwardSpeed) {
        double push = BordModule.push(past, EDGE_BAND);
        if (push <= 0.0) {
            return 0.0;
        }
        double brake = past > 0.0 && outwardSpeed > 0.0 ? EDGE_BRAKE * outwardSpeed : 0.0;
        return Math.min(MAX_EDGE_ACCELERATION, EDGE_ACCELERATION * push + brake);
    }
}
