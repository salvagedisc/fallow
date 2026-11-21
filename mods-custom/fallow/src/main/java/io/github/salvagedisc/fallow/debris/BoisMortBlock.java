package io.github.salvagedisc.fallow.debris;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Du bois mort tombé de l'arbre au-dessus, de son essence, en trois tailles (une brindille, deux, une branche). */
public class BoisMortBlock extends DebrisBlock {
    public static final MapCodec<BoisMortBlock> CODEC = simpleCodec(BoisMortBlock::new);
    public static final IntegerProperty TAILLE = IntegerProperty.create("taille", 0, 2);
    private static final VoxelShape[] FORMES = {
            Block.box(2, 0, 2, 14, 1, 14),
            Block.box(1, 0, 1, 15, 1, 15),
            Block.box(0, 0, 0, 16, 1, 16)
    };

    public BoisMortBlock(Properties properties) {
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
