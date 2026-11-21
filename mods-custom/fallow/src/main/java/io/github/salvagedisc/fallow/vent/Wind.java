package io.github.salvagedisc.fallow.vent;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector4f;

/**
 * Le vent de Fallow, côté client : une direction qui tourne lentement, une force qui suit la météo et
 * l'altitude, et des fronts de rafale qui traversent le paysage. La direction et la force ne dépendent que de
 * l'heure du monde et de la météo : deux joueurs d'un même serveur voient le même vent. Les rafales sont tirées
 * au hasard autour de chaque caméra.
 *
 * <p>Le pack de shaders en reçoit l'état (plantes, pluie), la fumée des feux de camp le suit.
 */
public final class Wind {
    /** Rafales suivies en même temps. */
    public static final int MAX_GUSTS = 6;
    /** Vitesse du vent à force 1, en blocs par seconde. */
    public static final double SPEED_AT_ONE = WindField.SPEED_AT_ONE;

    private static final Gust[] GUSTS = new Gust[MAX_GUSTS];
    private static final RandomSource RANDOM = RandomSource.create();

    private static float dirX = 0.8F;
    private static float dirZ = 0.6F;
    private static float strength = 0.3F;
    private static float turbulence = 0.4F;
    private static double seconds;
    private static boolean ready;

    private Wind() {
    }

    private record Gust(double originX, double originZ, float dirX, float dirZ, float speed, float width, float peak,
            double start, double life) {
        /** Position du front, le long de sa direction, au temps t. */
        double frontX(double t) {
            return originX + dirX * speed * (t - start);
        }

        double frontZ(double t) {
            return originZ + dirZ * speed * (t - start);
        }

        /** Force sur la crête : monte puis retombe au fil de la traversée. */
        float crest(double t) {
            double age = (t - start) / life;
            return age <= 0.0 || age >= 1.0 ? 0.0F : peak * (float) Math.sin(Math.PI * age);
        }

        /** Force ressentie en (x, z) : une bande perpendiculaire à la direction, large de {@code width}. */
        float at(double x, double z, double t) {
            double offset = ((x - frontX(t)) * dirX + (z - frontZ(t)) * dirZ) / width;
            return crest(t) * (float) Math.exp(-1.65 * offset * offset);
        }
    }

    /** Une fois par tick client. */
    public static void tick(ClientLevel level, Vec3 camera) {
        seconds = level.getGameTime() / 20.0;
        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);

        // Direction : une lente dérive, plus vive sous l'orage ; même valeur pour tous les clients du monde (et le serveur).
        double angle = WindField.angle(level.getGameTime(), thunder);
        float targetX = (float) Math.cos(angle);
        float targetZ = (float) Math.sin(angle);
        float ease = ready ? 0.02F : 1.0F;
        dirX = Mth.lerp(ease, dirX, targetX);
        dirZ = Mth.lerp(ease, dirZ, targetZ);
        float len = Mth.sqrt(dirX * dirX + dirZ * dirZ);
        if (len > 1.0e-4F) {
            dirX /= len;
            dirZ /= len;
        }

        // Force : une brise, plus forte sous la pluie et l'orage, et en altitude.
        float targetStrength = WindField.strength(level.getGameTime(), rain, thunder, camera.y);
        strength = Mth.lerp(ready ? 0.01F : 1.0F, strength, targetStrength);
        turbulence = Mth.lerp(ready ? 0.01F : 1.0F, turbulence, WindField.turbulence(rain, thunder));
        ready = true;

        // Rafales : plus fréquentes et plus fortes par mauvais temps.
        for (int i = 0; i < MAX_GUSTS; i++) {
            if (GUSTS[i] != null && seconds - GUSTS[i].start() > GUSTS[i].life()) {
                GUSTS[i] = null;
            }
        }
        float chance = 0.006F + 0.02F * rain + 0.03F * thunder + 0.01F * strength;
        if (RANDOM.nextFloat() < chance) {
            spawnGust(camera);
        }
    }

    private static void spawnGust(Vec3 camera) {
        int slot = -1;
        for (int i = 0; i < MAX_GUSTS; i++) {
            if (GUSTS[i] == null) {
                slot = i;
                break;
            }
        }
        if (slot < 0) {
            return;
        }
        // Un front naît en amont de la caméra et la traverse, un peu dévié de la direction du vent.
        float turn = (RANDOM.nextFloat() - 0.5F) * 0.6F;
        float gx = dirX * Mth.cos(turn) - dirZ * Mth.sin(turn);
        float gz = dirX * Mth.sin(turn) + dirZ * Mth.cos(turn);
        double back = 40.0 + RANDOM.nextDouble() * 50.0;
        double side = (RANDOM.nextDouble() - 0.5) * 60.0;
        double ox = camera.x - gx * back - gz * side;
        double oz = camera.z - gz * back + gx * side;
        float speed = (float) (SPEED_AT_ONE * (0.9 + strength) * (0.8 + RANDOM.nextFloat() * 0.6));
        float width = 8.0F + RANDOM.nextFloat() * 12.0F;
        float peak = (0.35F + RANDOM.nextFloat() * 0.5F) * (0.6F + strength);
        double life = back * 2.0 / speed;
        GUSTS[slot] = new Gust(ox, oz, gx, gz, speed, width, peak, seconds, life);
    }

    /** Temps du vent, en secondes, interpolé dans le tick pour une image. */
    private static double now() {
        Minecraft minecraft = Minecraft.getInstance();
        return seconds + minecraft.getTimer().getGameTimeDeltaPartialTick(false) / 20.0;
    }

    /** Force des rafales en (x, z). */
    public static float gustAt(double x, double z) {
        double t = now();
        float sum = 0.0F;
        for (Gust gust : GUSTS) {
            if (gust != null) {
                sum += gust.at(x, z, t);
            }
        }
        return sum;
    }

    /** Vitesse du vent en (x, z), en blocs par seconde (y toujours nul). */
    public static Vec3 velocity(double x, double z) {
        double speed = SPEED_AT_ONE * (strength + gustAt(x, z));
        return new Vec3(dirX * speed, 0.0, dirZ * speed);
    }

    public static float strength() {
        return strength;
    }

    /**
     * Pour le pack de shaders. {@code wind} : direction (x, z), force, turbulence. {@code gusts} : les fronts qui
     * pèsent le plus près de la caméra, (direction / largeur en x et z, distance de la caméra au front / largeur,
     * force sur la crête) ; le pack calcule la rafale en un point p (depuis la caméra) par
     * {@code w * exp(-1.65 * (dot(p.xz, xy) + z)²)}.
     */
    public static void fill(Vector4f wind, Vector4f[] gusts, Vec3 camera) {
        wind.set(dirX, dirZ, strength, turbulence);
        double t = now();
        float[] felt = new float[gusts.length];
        for (Gust gust : GUSTS) {
            if (gust == null) {
                continue;
            }
            float crest = gust.crest(t);
            if (crest <= 1.0e-4F) {
                continue;
            }
            float offset = (float) (((camera.x - gust.frontX(t)) * gust.dirX() + (camera.z - gust.frontZ(t)) * gust.dirZ()) / gust.width());
            // Un front à plus de six largeurs de la caméra ne se voit pas.
            float weight = crest * (float) Math.exp(-0.12 * offset * offset);
            int slot = -1;
            for (int j = 0; j < gusts.length; j++) {
                if (weight > felt[j] && (slot < 0 || felt[j] < felt[slot])) {
                    slot = j;
                }
            }
            if (slot >= 0) {
                felt[slot] = weight;
                gusts[slot].set(gust.dirX() / gust.width(), gust.dirZ() / gust.width(), offset, crest);
            }
        }
    }
}
