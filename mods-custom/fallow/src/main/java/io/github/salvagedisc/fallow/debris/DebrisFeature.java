package io.github.salvagedisc.fallow.debris;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;

/**
 * Les débris au sol : quelques places essayées autour du point de départ ; pour chacune, on descend du haut du terrain
 * à travers l'air et le feuillage jusqu'au sol, en notant la feuille la plus basse au-dessus de la place, puis la
 * première règle dont le sol et le couvert conviennent dit quoi poser. Sous un épicéa, une pomme de pin ou du bois
 * d'épicéa ; sur du granite à découvert, un caillou de granite.
 */
public class DebrisFeature extends Feature<DebrisConfig> {
    /** Hauteur des plus grands arbres du pack, au-dessus de la carte des hauteurs, d'où l'on part pour descendre. */
    private static final int DESCENTE = 48;

    public DebrisFeature(Codec<DebrisConfig> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<DebrisConfig> ctx) {
        DebrisConfig cfg = ctx.config();
        WorldGenLevel level = ctx.level();
        RandomSource random = ctx.random();
        BlockPos origin = ctx.origin();
        int poses = 0;
        int essais = cfg.essais().sample(random);
        int e = cfg.etendue();
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        for (int n = 0; n < essais; n++) {
            int x = origin.getX() + (e == 0 ? 0 : random.nextInt(2 * e + 1) - e);
            int z = origin.getZ() + (e == 0 ? 0 : random.nextInt(2 * e + 1) - e);
            if (poser(level, random, cfg, x, z, cur)) {
                poses++;
            }
        }
        return poses > 0;
    }

    private static boolean poser(WorldGenLevel level, RandomSource random, DebrisConfig cfg, int x, int z,
            BlockPos.MutableBlockPos cur) {
        // La carte des hauteurs de la génération ne voit pas toujours les arbres qui viennent d'être posés : on part
        // plus haut et on descend à travers l'air et le feuillage.
        int haut = Math.min(level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z) + DESCENTE, level.getMaxBuildHeight() - 1);
        cur.set(x, haut, z);
        BlockState feuille = null;
        BlockState state = level.getBlockState(cur);
        for (int i = 0; i < 2 * DESCENTE && cur.getY() > level.getMinBuildHeight(); i++) {
            cur.move(Direction.DOWN);
            state = level.getBlockState(cur);
            if (state.isAir()) {
                continue;
            }
            if (state.is(BlockTags.LEAVES)) {
                feuille = state;
                continue;
            }
            break;
        }
        BlockState sol = state;
        if (sol.isAir() || sol.is(BlockTags.LEAVES) || !sol.getFluidState().isEmpty()
                || !sol.isFaceSturdy(level, cur, Direction.UP)) {
            return false;
        }
        BlockPos place = cur.above();
        if (!level.getBlockState(place).isAir()) {
            return false;
        }
        for (DebrisConfig.Regle regle : cfg.regles()) {
            if (regle.sol().isPresent() && !sol.is(regle.sol().get())) {
                continue;
            }
            if (regle.feuilles().isPresent()) {
                if (feuille == null || !feuille.is(regle.feuilles().get())) {
                    continue;
                }
            } else if (regle.sousLeCiel() && feuille != null) {
                continue;
            }
            if (random.nextFloat() >= regle.chance()) {
                return false;
            }
            BlockState pose = regle.pose().getState(random, place);
            if (!pose.canSurvive(level, place)) {
                return false;
            }
            level.setBlock(place, pose, Block.UPDATE_CLIENTS);
            return true;
        }
        return false;
    }
}
