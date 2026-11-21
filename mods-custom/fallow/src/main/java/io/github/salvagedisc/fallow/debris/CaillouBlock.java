package io.github.salvagedisc.fallow.debris;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Un caillou de la roche du sol, en quatre tailles (0 : un éclat, 3 : une pierre qu'on enjambe). */
public class CaillouBlock extends DebrisBlock {
    public static final MapCodec<CaillouBlock> CODEC = simpleCodec(CaillouBlock::new);
    public static final IntegerProperty TAILLE = IntegerProperty.create("taille", 0, 3);
    private static final VoxelShape[] FORMES = {
            Block.box(4, 0, 4, 12, 1.5, 12),
            Block.box(3, 0, 3, 13, 2, 13),
            Block.box(1, 0, 1, 15, 2.5, 15),
            Block.box(1, 0, 1, 15, 4, 15)
    };

    public CaillouBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(TAILLE, 1));
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(TAILLE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(TAILLE, tirage(context.getClickedPos(), 3));
    }

    @Override
    protected VoxelShape forme(BlockState state) {
        return FORMES[state.getValue(TAILLE)];
    }
}
