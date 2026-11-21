package io.github.salvagedisc.fallow.vent;

import net.minecraft.util.Mth;

/**
 * Le vent de fond, commun au client et au serveur : sa direction et sa force ne dépendent que de l'heure du monde,
 * de la météo et de l'altitude, si bien que le serveur (l'approche des bêtes) et chaque client (fumée, shaders)
 * calculent le même vent sans rien s'envoyer. {@link Wind} y ajoute, côté client, le lissage et les rafales.
 */
public final class WindField {
    /** Vitesse du vent à force 1, en blocs par seconde. */
    public static final double SPEED_AT_ONE = 6.0;

    private WindField() {
    }

    /**
     * Vent de fond en un lieu, sans les rafales : vitesse en blocs par seconde, {@code [x, z]}. C'est le vent que le
     * serveur fait souffler sur les navires de Sable (module navires) ; les rafales, tirées au hasard autour de chaque
     * caméra, restent au client.
     */
    public static double[] velocity(long gameTime, float rain, float thunder, double y) {
        double angle = angle(gameTime, thunder);
        double speed = SPEED_AT_ONE * strength(gameTime, rain, thunder, y);
        return new double[] {Math.cos(angle) * speed, Math.sin(angle) * speed};
    }

    /** Angle du vent (radians) : direction vers laquelle il souffle, dans le plan (x, z). */
    public static double angle(long gameTime, float thunder) {
        double seconds = gameTime / 20.0;
        double drift = seconds / 900.0;
        // Direction d'un vecteur fait de deux bruits centrés : toutes les directions sont aussi probables
        // (un seul bruit étiré sur 1,6 tour revenait deux fois sur la même moitié du cercle).
        double base = Math.atan2(smoothNoise(drift + 57.31) - 0.5, smoothNoise(drift) - 0.5);
        return base + 0.6 * smoothNoise(seconds / 140.0 + 31.7) * (0.3 + thunder);
    }

    /** Force du vent (environ 0,2 par temps calme, plus de 1 sous l'orage en altitude). */
    public static float strength(long gameTime, float rain, float thunder, double y) {
        double seconds = gameTime / 20.0;
        float calm = (float) smoothNoise(seconds / 300.0 + 7.3);
        float altitude = Mth.clamp((float) (y - 90.0) / 140.0F, 0.0F, 0.35F);
        return 0.18F + 0.24F * calm + 0.32F * rain + 0.35F * thunder + altitude;
    }

    public static float turbulence(float rain, float thunder) {
        return 0.25F + 0.35F * rain + 0.4F * thunder;
    }

    /** Valeur lisse entre 0 et 1, qui varie d'une unité de t à l'autre. */
    public static double smoothNoise(double t) {
        long i = (long) Math.floor(t);
        double f = t - i;
        f = f * f * (3.0 - 2.0 * f);
        return Mth.lerp(f, hash(i), hash(i + 1));
    }

    private static double hash(long i) {
        long v = i * 0x9E3779B97F4A7C15L;
        v ^= v >>> 31;
        v *= 0xBF58476D1CE4E5B9L;
        v ^= v >>> 29;
        return (v >>> 11) * 0x1.0p-53;
    }
}
