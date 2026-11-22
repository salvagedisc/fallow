package io.github.salvagedisc.fallow.climat.client;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.climat.Storm;
import io.github.salvagedisc.fallow.vent.Mote;
import io.github.salvagedisc.fallow.vent.Wind;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Ce qu'on voit d'une tempête, côté client. Dans le sable, une poussière fauve file avec le vent du pack ; dans une
 * tempête de neige, des flocons soufflés s'ajoutent à la neige du jeu. Dans les deux, la brume se ferme et prend la
 * couleur de la tempête ({@link StormView}, lue par la brume d'ATMOSPHERICS). La force monte et retombe en quelques
 * secondes ; à l'abri, on voit encore la tempête dehors, mais plus faiblement.
 */
public final class StormClient {
    /** Constante de temps du lissage de la force, en secondes. */
    private static final float EASE_SECONDS = 4.0F;
    /** Rayon de la boîte où naissent les grains et les flocons, autour de la caméra. */
    private static final int RADIUS = 14;
    private static final RandomSource RANDOM = RandomSource.create();

    private StormClient() {
    }

    public static void start() {
        NeoForge.EVENT_BUS.addListener(StormClient::tick);
    }

    private static void tick(ClientTickEvent.Post event) {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            if (level == null) {
                StormView.sand = 0.0F;
                StormView.snow = 0.0F;
            }
            return;
        }
        Vec3 camera = minecraft.gameRenderer.getMainCamera().getPosition();
        BlockPos head = BlockPos.containing(camera);
        float targetSand = 0.0F;
        float targetSnow = 0.0F;
        if (FallowConfig.climatEnabled() && FallowConfig.climatStormVisuals()) {
            Storm storm = Storm.at(level, head);
            float shelter = Storm.exposed(level, head) ? 1.0F : 0.35F;
            // La pluie et l'orage montent et descendent en douceur dans le jeu : la tempête les suit.
            float weather = level.getRainLevel(1.0F) * Mth.lerp(level.getThunderLevel(1.0F), 0.6F, 1.0F);
            if (storm.isSand()) {
                targetSand = weather * shelter;
            } else if (storm.isStorm()) {
                targetSnow = level.getRainLevel(1.0F) * level.getThunderLevel(1.0F) * shelter;
            }
        }
        float k = 1.0F - (float) Math.exp(-1.0F / (20.0F * EASE_SECONDS));
        StormView.sand += (targetSand - StormView.sand) * k;
        StormView.snow += (targetSnow - StormView.snow) * k;

        if (StormView.sand > 0.02F) {
            spawn(level, camera, StormView.sand, true);
        }
        if (StormView.snow > 0.02F) {
            spawn(level, camera, StormView.snow, false);
        }
    }

    /** Fait naître grains ou flocons dans la boîte autour de la caméra, seulement là où le ciel est ouvert. */
    private static void spawn(ClientLevel level, Vec3 camera, float strength, boolean sand) {
        int count = Math.round((sand ? 22.0F : 16.0F) * strength);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double x = camera.x + (RANDOM.nextDouble() * 2.0 - 1.0) * RADIUS;
            double y = camera.y + (RANDOM.nextDouble() * 2.0 - 0.8) * RADIUS * 0.5;
            double z = camera.z + (RANDOM.nextDouble() * 2.0 - 1.0) * RADIUS;
            pos.set(x, y, z);
            if (!level.canSeeSky(pos) || !level.getBlockState(pos).isAir()) {
                continue;
            }
            // Le vent du pack, en blocs par tick, avec une part de remous ; la tempête souffle plus fort que lui.
            double push = 0.9 + strength;
            Vec3 wind = Wind.velocity(x, z).scale(push / 20.0);
            double vx = wind.x + RANDOM.nextGaussian() * 0.03;
            double vz = wind.z + RANDOM.nextGaussian() * 0.03;
            if (sand) {
                float shade = (0.85F + RANDOM.nextFloat() * 0.25F) * level.getSkyDarken(1.0F);
                // Un grain qui suit le vent (les poussières du jeu perdent presque toute leur vitesse de départ).
                Mote.release(level, x, y, z, new float[] {0.82F * shade, 0.67F * shade, 0.46F * shade},
                        0.04F + RANDOM.nextFloat() * 0.05F, push, 20 + RANDOM.nextInt(25));
            } else {
                level.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, vx, -0.05 - RANDOM.nextDouble() * 0.05, vz);
            }
        }
    }
}
