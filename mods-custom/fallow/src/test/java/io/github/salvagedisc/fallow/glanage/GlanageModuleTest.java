package io.github.salvagedisc.fallow.glanage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CherryLeavesBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.junit.jupiter.api.Test;

class GlanageModuleTest {
    @Test
    void forgetsOnlyShakesOlderThanADay() {
        Long2LongOpenHashMap shakes = new Long2LongOpenHashMap();
        long now = 100_000L;
        shakes.put(1L, now - GlanageModule.REPOS_FEUILLES);
        shakes.put(2L, now - GlanageModule.REPOS_FEUILLES + 1);
        shakes.put(3L, now);
        GlanageModule.oublier(shakes, now);
        assertFalse(shakes.containsKey(1L));
        assertTrue(shakes.containsKey(2L));
        assertTrue(shakes.containsKey(3L));
    }

    @Test
    void clearsWhenStillFullOfRecentShakes() {
        Long2LongOpenHashMap shakes = new Long2LongOpenHashMap();
        long now = 50_000L;
        for (long pos = 0; pos < GlanageModule.MEMOIRE; pos++) {
            shakes.put(pos, now - 10);
        }
        GlanageModule.oublier(shakes, now);
        assertEquals(0, shakes.size());
    }

    @Test
    void shakesPlainLeavesButNotLeavesWithTheirOwnClick() {
        assertFalse(GlanageModule.aSonGeste(LeavesBlock.class));
        assertFalse(GlanageModule.aSonGeste(CherryLeavesBlock.class));
        assertTrue(GlanageModule.aSonGeste(FruitLeaves.class));
        assertTrue(GlanageModule.aSonGeste(RipeFruitLeaves.class));
    }

    /** Leaves picked by hand, like Regions Unexplored's apple leaves. */
    abstract static class FruitLeaves extends LeavesBlock {
        FruitLeaves(Properties properties) {
            super(properties);
        }

        @Override
        protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player,
                BlockHitResult hit) {
            return InteractionResult.SUCCESS;
        }
    }

    abstract static class RipeFruitLeaves extends FruitLeaves {
        RipeFruitLeaves(Properties properties) {
            super(properties);
        }
    }
}
