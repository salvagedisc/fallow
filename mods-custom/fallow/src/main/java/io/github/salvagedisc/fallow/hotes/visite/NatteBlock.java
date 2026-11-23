package io.github.salvagedisc.fallow.hotes.visite;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * La natte d'offrande que l'émissaire pose à la lisière. Elle ne se fabrique pas et ne se ramasse pas : elle porte les
 * dons jusqu'à l'aube, puis le contre-don, et s'efface une fois vidée. Roulée ({@link #ROULEE}), c'est la natte d'une
 * visite oubliée : un clic la retire.
 */
public class NatteBlock extends BaseEntityBlock {
    public static final MapCodec<NatteBlock> CODEC = simpleCodec(NatteBlock::new);
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final BooleanProperty ROULEE = BooleanProperty.create("roulee");
    private static final VoxelShape PLATE = Block.box(1.0, 0.0, 1.0, 15.0, 1.0, 15.0);
    private static final VoxelShape ROULEAU_NS = Block.box(2.0, 0.0, 6.0, 14.0, 4.0, 10.0);
    private static final VoxelShape ROULEAU_EO = Block.box(6.0, 0.0, 2.0, 10.0, 4.0, 14.0);

    public NatteBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ROULEE, false));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ROULEE);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        if (!state.getValue(ROULEE)) {
            return PLATE;
        }
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? ROULEAU_EO : ROULEAU_NS;
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return !level.isEmptyBlock(pos.below());
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        // Pendant la visite, la natte et ses dons ne se reprennent pas.
        if (level.getBlockEntity(pos) instanceof NatteBlockEntity natte && natte.attend()) {
            return 0.0F;
        }
        return super.getDestroyProgress(state, player, level, pos);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof NatteBlockEntity natte)) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (natte.attend()) {
            player.displayClientMessage(Component.translatable("message.fallow.hotes.natte.attend"), true);
            return InteractionResult.CONSUME;
        }
        for (ItemStack pile : natte.vide()) {
            if (!player.getInventory().add(pile)) {
                player.drop(pile, false);
            }
        }
        efface((ServerLevel) level, pos);
        return InteractionResult.CONSUME;
    }

    /** La natte s'efface dans un peu de brume. */
    static void efface(ServerLevel level, BlockPos pos) {
        level.removeBlock(pos, false);
        level.sendParticles(ParticleTypes.CLOUD, pos.getX() + 0.5, pos.getY() + 0.2, pos.getZ() + 0.5, 12, 0.35, 0.05, 0.35, 0.01);
        level.playSound(null, pos, SoundEvents.WOOL_BREAK, SoundSource.BLOCKS, 0.6F, 1.2F);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new NatteBlockEntity(pos, state);
    }
}
