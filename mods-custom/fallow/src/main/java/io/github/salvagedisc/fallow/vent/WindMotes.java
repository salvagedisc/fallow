package io.github.salvagedisc.fallow.vent;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.ModIds;
import io.github.salvagedisc.fallow.climat.client.StormView;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * Le vent qu'on lit sans pack de shaders. Sous Brumaire, les herbes et les feuilles ondulent au vent du pack ; sans
 * lui, rien ne bouge que la fumée des feux et les feuilles de Particular, alors que le vent porte l'odeur du joueur
 * vers les bêtes (module approche). À l'air libre sur la Surface, de fins brins pâles (poussière, pollen) naissent
 * près du sol autour de la caméra et filent avec le vent : plus nombreux et plus rapides quand il forcit, presque
 * aucun par temps calme, rares et sombres la nuit. Rien dans une tempête : le module climat a sa poussière.
 */
public final class WindMotes {
    /** Rayon de la boîte où naissent les brins, autour de la caméra. */
    private static final int RADIUS = 12;
    private static final RandomSource RANDOM = RandomSource.create();
    private static final boolean IRIS = ModIds.isLoaded(ModIds.IRIS);

    private WindMotes() {
    }

    static void tick(ClientLevel level, Vec3 camera) {
        if (!FallowConfig.ventMotes() || level.dimension() != Level.OVERWORLD || shaderInUse()
                || StormView.sand() > 0.1F || StormView.snow() > 0.1F) {
            return;
        }
        float force = Wind.strength();
        // D'un brin tous les huit ticks par temps calme (force 0,18) à trois par tick dans un vent fort.
        float rate = Math.min(3.0F, Math.max(0.0F, force - 0.15F) * 4.0F);
        if (level.isThundering()) {
            rate *= 0.5F;
        }
        // La clarté du ciel (1 de jour, 0,2 la nuit, moins sous l'orage) : la nuit, peu de brins, plus sombres.
        float light = level.getSkyDarken(1.0F);
        rate *= Mth.clamp((light - 0.2F) / 0.6F, 0.15F, 1.0F);
        int count = (int) rate + (RANDOM.nextFloat() < rate - (int) rate ? 1 : 0);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double x = camera.x + (RANDOM.nextDouble() * 2.0 - 1.0) * RADIUS;
            double z = camera.z + (RANDOM.nextDouble() * 2.0 - 1.0) * RADIUS;
            int ground = level.getHeight(Heightmap.Types.MOTION_BLOCKING, Mth.floor(x), Mth.floor(z));
            if (Math.abs(ground - camera.y) > 10.0) {
                continue;
            }
            double y = ground + 0.2 + RANDOM.nextDouble() * 2.5;
            pos.set(x, y, z);
            if (!level.getBlockState(pos).isAir() || !level.canSeeSky(pos)) {
                continue;
            }
            float shade = (0.82F + RANDOM.nextFloat() * 0.18F) * light;
            Mote.release(level, x, y, z, new float[] {0.93F * shade, 0.91F * shade, 0.84F * shade},
                    0.06F + RANDOM.nextFloat() * 0.04F, 1.0, 40 + RANDOM.nextInt(40));
        }
    }

    private static boolean shaderInUse() {
        return IRIS && IrisHolder.inUse();
    }

    /** Iris n'est chargé qu'ici : sans lui, la classe n'est jamais lue. */
    private static final class IrisHolder {
        static boolean inUse() {
            return net.irisshaders.iris.api.v0.IrisApi.getInstance().isShaderPackInUse();
        }
    }
}
