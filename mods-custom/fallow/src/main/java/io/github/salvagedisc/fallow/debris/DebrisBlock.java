package io.github.salvagedisc.fallow.debris;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Un débris posé au sol : un caillou, du bois mort, une pomme de pin. On passe au travers, il se casse d'un coup et
 * se ramasse main vide (le geste « reprendre » de docs/plan/interactions.md). Il lui faut un sol plein dessous.
 * Chaque débris est un peu décalé dans son bloc, comme les fleurs, pour que le sol ne paraisse pas quadrillé.
 */
public abstract class DebrisBlock extends Block {
    protected DebrisBlock(Properties properties) {
        super(properties);
    }

    /** La forme du débris dans cet état, avant le décalage. */
    protected abstract VoxelShape forme(BlockState state);

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        Vec3 decalage = state.getOffset(level, pos);
        return forme(state).move(decalage.x, decalage.y, decalage.z);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        BlockPos dessous = pos.below();
        return level.getBlockState(dessous).isFaceSturdy(level, dessous, Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, Direction direction, BlockState neighborState, LevelAccessor level,
            BlockPos pos, BlockPos neighborPos) {
        if (direction == Direction.DOWN && !canSurvive(state, level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, level, pos, neighborPos);
    }

    /** Main vide, on ramasse le débris. Avec quelque chose en main, le clic garde son usage (poser un bloc…). */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            ItemStack pris = new ItemStack(this);
            if (!player.getInventory().add(pris)) {
                player.drop(pris, false);
            }
            level.removeBlock(pos, false);
            level.gameEvent(player, GameEvent.BLOCK_DESTROY, pos);
            level.playSound(null, pos, SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 0.2F,
                    (level.getRandom().nextFloat() - level.getRandom().nextFloat()) * 1.4F + 2.0F);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    /** Un nombre de 0 à {@code bornes - 1} tiré de la position, le même sur le client et le serveur. */
    protected static int tirage(BlockPos pos, int bornes) {
        long h = pos.asLong() * 0x9E3779B97F4A7C15L;
        return (int) Math.floorMod(h >>> 33, (long) bornes);
    }
}
