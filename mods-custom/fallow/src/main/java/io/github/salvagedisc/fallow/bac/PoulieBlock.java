package io.github.salvagedisc.fallow.bac;

import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import com.mojang.serialization.MapCodec;

/**
 * La poulie de bac, posée sur un radeau sous une corde de bac. Un clic sur la poulie d'un radeau encore posé sur
 * l'eau le prend à la corde (le radeau devient un navire de Sable qui ne fait que glisser le long d'elle) ; ensuite,
 * un clic le fait partir vers la rive d'en face ou l'arrête, Maj + clic le fait repartir dans l'autre sens.
 */
public class PoulieBlock extends HorizontalDirectionalBlock {
    public static final MapCodec<PoulieBlock> CODEC = simpleCodec(PoulieBlock::new);
    private static final VoxelShape SHAPE = Block.box(2.0, 0.0, 2.0, 14.0, 10.0, 14.0);

    public PoulieBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!FallowConfig.bacEnabled() || !(level instanceof ServerLevel server)) {
            return InteractionResult.PASS;
        }
        Bac.use(server, pos, player, player.isShiftKeyDown());
        return InteractionResult.CONSUME;
    }
}
