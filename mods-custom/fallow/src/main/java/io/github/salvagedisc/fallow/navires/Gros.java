package io.github.salvagedisc.fallow.navires;

import io.github.salvagedisc.fallow.vent.WindField;

/**
 * Le gros temps pour les navires, sans rien de Sable : les rafales d'orage et la poussée du vent dans une voile. Tout
 * se calcule depuis l'heure du monde et la météo, comme {@link WindField} : le serveur et chaque client trouvent les
 * mêmes rafales sans rien s'envoyer.
 */
public final class Gros {
    /** Vitesse d'une rafale au plus fort, sous l'orage au niveau de la mer, en blocs par seconde. */
    public static final double GUST_SPEED = 7.0;
    /** Part de la vitesse d'une rafale qui s'ajoute en altitude, au plus (à 210 blocs et au-dessus). */
    public static final double GUST_ALTITUDE = 1.0;
    /** Sous ce seuil du bruit, pas de rafale : elles viennent par à-coups. */
    private static final double GUST_THRESHOLD = 0.55;

    private Gros() {
    }

    /**
     * Rafale en un instant pour un navire, {@code [x, z]} en blocs par seconde, en plus du vent de fond : rien par beau
     * temps, quelques coups sous la pluie, des à-coups violents sous l'orage, plus forts en altitude. Chaque navire a
     * ses rafales ({@code seed}), à peu près de côté par rapport au vent, et elles tournent d'un coup à l'autre.
     *
     * @param windAngle direction du vent de fond (radians, celle de {@link WindField#angle})
     */
    public static double[] gust(long gameTime, long seed, float rain, float thunder, double y, double windAngle) {
        double weather = 0.3 * rain + 0.7 * thunder;
        if (weather <= 0.0) {
            return new double[] {0.0, 0.0};
        }
        double seconds = gameTime / 20.0;
        double offset = (seed & 0xFFFF) / 97.0;
        double noise = WindField.smoothNoise(seconds / 1.6 + offset);
        double pulse = Math.max(0.0, noise - GUST_THRESHOLD) / (1.0 - GUST_THRESHOLD);
        if (pulse <= 0.0) {
            return new double[] {0.0, 0.0};
        }
        double altitude = 1.0 + GUST_ALTITUDE * Math.min(1.0, Math.max(0.0, (y - 90.0) / 120.0));
        double speed = GUST_SPEED * weather * altitude * pulse * pulse;
        double turn = (WindField.smoothNoise(seconds / 4.0 + offset + 311.0) - 0.5) * 2.4;
        double angle = windAngle + turn;
        return new double[] {Math.cos(angle) * speed, Math.sin(angle) * speed};
    }

    /**
     * Ce que le vent donne à une voile selon l'allure, de 0 à 1,1 : rien face au vent (moins de 35° du lit du vent),
     * de plus en plus au près, le plus vent de travers et au largue, un peu moins vent arrière.
     *
     * @param headingX cap du bateau, x (pas forcément unitaire)
     * @param headingZ cap, z
     * @param windX direction vers laquelle souffle le vent, x
     * @param windZ direction du vent, z
     */
    public static double sail(double headingX, double headingZ, double windX, double windZ) {
        double h = Math.sqrt(headingX * headingX + headingZ * headingZ);
        double w = Math.sqrt(windX * windX + windZ * windZ);
        if (h < 1.0e-9 || w < 1.0e-9) {
            return 0.0;
        }
        double cos = (headingX * windX + headingZ * windZ) / (h * w);
        // Angle entre le cap et la direction d'où vient le vent : 0° face au vent, 180° vent arrière.
        double fromWind = Math.toDegrees(Math.acos(-Math.max(-1.0, Math.min(1.0, cos))));
        if (fromWind < 35.0) {
            return 0.0;
        }
        if (fromWind < 90.0) {
            return 0.3 + 0.7 * (fromWind - 35.0) / 55.0;
        }
        if (fromWind < 135.0) {
            return 1.0 + 0.1 * (fromWind - 90.0) / 45.0;
        }
        return 1.1 - 0.2 * (fromWind - 135.0) / 45.0;
    }

    /**
     * Poussée d'une voile dans le cap, en blocs par tick, pour un vent de {@code windSpeed} blocs par seconde : la
     * poussée du voilier de Fish 'N' Ships ({@code base}, 0,03) multipliée par l'allure et par la force du vent, de
     * moitié par petite brise à deux fois et demie sous l'orage.
     */
    public static double sailPush(double base, double allure, double windSpeed, double multiplier) {
        double wind = Math.max(0.5, Math.min(2.5, windSpeed / 3.0));
        return base * allure * wind * multiplier;
    }
}
