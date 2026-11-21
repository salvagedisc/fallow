package io.github.salvagedisc.fallow.navires;

import com.teamtea.eclipticseasons.api.util.EclipticUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.biome.Biome;

/** S'il neige selon Ecliptic Seasons, comme pour le module climat. Chargée seulement si Ecliptic Seasons est là. */
final class NeigeEcliptic {
    private NeigeEcliptic() {
    }

    static float snowing(ServerLevel level, BlockPos pos) {
        Biome biome = level.getBiome(pos).value();
        if (EclipticUtil.getRainOrSnow(level, biome, pos) != Biome.Precipitation.SNOW) {
            return 0.0F;
        }
        return level.isThundering() ? 1.0F : 0.4F;
    }
}
