package io.github.salvagedisc.fallow.navires;

import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.companion.math.BoundingBox3ic;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.ModIds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La neige sur les ponts : quand il neige là où passe un navire de Sable à ciel ouvert, de vraies couches de neige se
 * posent sur ce qu'il a de plus haut (pont, toit, ballon), plus vite sous la tempête. Sable compte leur masse : le
 * navire s'alourdit, un dirigeable perd de l'altitude. La neige fond à l'abri (sous un toit, dans un hangar) ou quand
 * il ne neige plus et qu'il fait assez doux. Les couches s'empilent jusqu'à {@code navires.snowMaxLayers}.
 *
 * <p>Avec Ecliptic Seasons, c'est lui qui dit s'il neige (selon la saison), comme pour le module climat ; sans lui, la
 * précipitation du biome.
 */
final class Neige {
    /** Un passage tous les tant de ticks. */
    private static final int INTERVAL = 40;
    /** Colonnes du navire visitées à chaque passage, par simple neige ; trois fois plus sous la tempête. */
    private static final int COLUMNS = 2;
    /** Colonnes visitées pour la fonte à chaque passage. */
    private static final int MELT_COLUMNS = 3;

    private Neige() {
    }

    static void tick(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD || level.getGameTime() % INTERVAL != 0 || !FallowConfig.naviresSnow()) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        int maxLayers = FallowConfig.naviresSnowMaxLayers();
        for (ServerSubLevel ship : container.getAllSubLevels()) {
            if (ship.isRemoved()) {
                continue;
            }
            try {
                weather(level, ship, maxLayers);
            } catch (RuntimeException e) {
                Fallow.LOGGER.debug("navires : neige impossible sur {}", ship, e);
            }
        }
    }

    private static void weather(ServerLevel level, ServerSubLevel ship, int maxLayers) {
        BoundingBox3dc box = ship.boundingBox();
        if (Math.max(box.maxX() - box.minX(), Math.max(box.maxY() - box.minY(), box.maxZ() - box.minZ())) < ShipForces.MIN_SIZE) {
            return;
        }
        BlockPos above = BlockPos.containing((box.minX() + box.maxX()) * 0.5, box.maxY() + 1.0, (box.minZ() + box.maxZ()) * 0.5);
        if (!level.isLoaded(above)) {
            return;
        }
        boolean exposed = level.canSeeSky(above);
        float snow = exposed ? snowing(level, above) : 0.0F;
        LevelPlot plot = ship.getPlot();
        BoundingBox3ic blocks = plot == null ? null : plot.getBoundingBox();
        if (blocks == null) {
            return;
        }
        RandomSource random = level.getRandom();
        if (snow > 0.0F) {
            int columns = snow >= 1.0F ? COLUMNS * 3 : COLUMNS;
            for (int i = 0; i < columns; i++) {
                settle(level, plot, blocks, random, maxLayers);
            }
        } else if (!exposed || warm(level, above)) {
            for (int i = 0; i < (exposed ? MELT_COLUMNS : MELT_COLUMNS * 2); i++) {
                melt(level, plot, blocks, random);
            }
        }
    }

    /** 0 s'il ne neige pas là, 0,4 pour une simple neige, 1 sous la tempête de neige. */
    private static float snowing(ServerLevel level, BlockPos pos) {
        if (!level.isRaining()) {
            return 0.0F;
        }
        if (ModIds.isLoaded(ModIds.ECLIPTIC_SEASONS)) {
            return NeigeEcliptic.snowing(level, pos);
        }
        Biome biome = level.getBiome(pos).value();
        if (biome.getPrecipitationAt(pos) != Biome.Precipitation.SNOW) {
            return 0.0F;
        }
        return level.isThundering() ? 1.0F : 0.4F;
    }

    /** Assez doux pour que la neige fonde à découvert. */
    private static boolean warm(ServerLevel level, BlockPos pos) {
        return level.getBiome(pos).value().warmEnoughToRain(pos);
    }

    /**
     * Le plus haut bloc de la colonne du navire, ou {@code null} si la colonne est vide. Les blocs d'un navire sont
     * rangés dans sa parcelle, loin du monde : on ne touche jamais à une colonne hors de la parcelle.
     */
    private static BlockPos top(ServerLevel level, LevelPlot plot, BoundingBox3ic blocks, int x, int z) {
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(x, blocks.maxY(), z);
        if (!plot.contains(x + 0.5, z + 0.5) || !level.isLoaded(pos)) {
            return null;
        }
        for (int y = blocks.maxY(); y >= blocks.minY(); y--) {
            pos.setY(y);
            if (!level.getBlockState(pos).isAir()) {
                return pos.immutable();
            }
        }
        return null;
    }

    private static void settle(ServerLevel level, LevelPlot plot, BoundingBox3ic blocks, RandomSource random, int maxLayers) {
        int x = blocks.minX() + random.nextInt(blocks.maxX() - blocks.minX() + 1);
        int z = blocks.minZ() + random.nextInt(blocks.maxZ() - blocks.minZ() + 1);
        BlockPos top = top(level, plot, blocks, x, z);
        if (top == null) {
            return;
        }
        BlockState state = level.getBlockState(top);
        if (state.is(Blocks.SNOW)) {
            int layers = state.getValue(SnowLayerBlock.LAYERS);
            if (layers < maxLayers) {
                level.setBlock(top, state.setValue(SnowLayerBlock.LAYERS, layers + 1), Block.UPDATE_ALL);
            }
            return;
        }
        BlockPos on = top.above();
        BlockState snow = Blocks.SNOW.defaultBlockState();
        if (level.getBlockState(on).isAir() && snow.canSurvive(level, on)) {
            level.setBlock(on, snow, Block.UPDATE_ALL);
        }
    }

    private static void melt(ServerLevel level, LevelPlot plot, BoundingBox3ic blocks, RandomSource random) {
        int x = blocks.minX() + random.nextInt(blocks.maxX() - blocks.minX() + 1);
        int z = blocks.minZ() + random.nextInt(blocks.maxZ() - blocks.minZ() + 1);
        BlockPos top = top(level, plot, blocks, x, z);
        if (top == null) {
            return;
        }
        BlockState state = level.getBlockState(top);
        if (!state.is(Blocks.SNOW)) {
            return;
        }
        int layers = state.getValue(SnowLayerBlock.LAYERS);
        level.setBlock(top, layers > 1 ? state.setValue(SnowLayerBlock.LAYERS, layers - 1) : Blocks.AIR.defaultBlockState(),
                Block.UPDATE_ALL);
    }
}
