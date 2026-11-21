package io.github.salvagedisc.fallow.prise;

import io.github.salvagedisc.fallow.vent.WindField;

/**
 * Le calcul de la grosse prise qui tire le bateau, sans rien de Starcatcher ni de Sable. Pendant le combat, le poisson
 * file droit devant lui en changeant peu à peu de cap, par à-coups ; ligne tendue, le poisson et le bateau vont du même
 * pas, et ce pas est la vitesse de nage du poisson réduite par le poids du bateau qu'il traîne. En blocs, kilogrammes
 * et secondes.
 */
public final class Prise {
    /** En dessous de ce poids, en kilogrammes, le poisson ne tire rien. */
    public static final double MIN_KG = 8.0;
    /** Vitesse de nage d'un gros poisson qui file, ligne libre, en blocs par seconde. */
    public static final double FISH_SPEED = 2.5;
    /** Poids d'une unité de masse de Sable (à peu près un bloc), en kilogrammes. */
    public static final double KG_PER_MASS = 40.0;
    /** Poids d'une barque (entité) par bloc carré d'emprise au sol, en kilogrammes, passager compris. */
    public static final double KG_PER_AREA = 150.0;
    /** Rapidité avec laquelle le bateau prend le pas du poisson, par seconde. */
    public static final double RATE = 5.0;
    /** Accélération au plus, en blocs par seconde carrée. */
    public static final double MAX_ACCEL = 4.0;
    /** Le poisson change de cap au plus de cet angle de part et d'autre de sa fuite, en radians. */
    public static final double WANDER = 0.9;
    /** Ligne au plus, en blocs : au-delà, le poisson ne prend plus de champ et le bateau le rattrape. */
    public static final double REACH = 40.0;
    /** Mou que le poisson peut prendre au-delà de la distance du bouchon au début du combat, en blocs. */
    public static final double SLACK = 6.0;

    private Prise() {
    }

    /**
     * Part de sa vitesse de nage que le poisson garde en traînant le bateau : son poids contre celui du bateau, 0 pour un
     * petit poisson.
     */
    public static double share(double fishKg, double boatKg) {
        if (fishKg < MIN_KG) {
            return 0.0;
        }
        return fishKg / (fishKg + Math.max(0.0, boatKg));
    }

    /**
     * Force du poisson à l'instant, de 0,5 (il souffle) à 1,2 (il rue), d'un bruit lent propre à la prise.
     *
     * @param ticks ticks depuis le début du combat
     */
    public static double surge(long ticks, double seed) {
        return 0.5 + 0.7 * WindField.smoothNoise(ticks / 30.0 + seed * 97.0);
    }

    /** Cap du poisson, en radians (0 vers +x, π/2 vers +z) : sa fuite de départ, qui dérive lentement. */
    public static double heading(double start, long ticks, double seed) {
        return start + WANDER * (2.0 * WindField.smoothNoise(ticks / 80.0 + seed * 131.0) - 1.0);
    }

    /** Vitesse visée du bateau, en blocs par seconde : la nage du poisson, sa force du moment et la part qu'il garde. */
    public static double target(double fishKg, double boatKg, double surge) {
        return FISH_SPEED * surge * share(fishKg, boatKg);
    }

    /**
     * Accélération vers la vitesse visée le long du cap, en blocs par seconde carrée : le poisson tire, il ne retient
     * jamais.
     */
    public static double pull(double speedAlong, double target) {
        return Math.max(0.0, Math.min(MAX_ACCEL, RATE * (target - speedAlong)));
    }
}
