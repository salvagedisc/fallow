package io.github.salvagedisc.fallow.debris;

import com.mojang.serialization.MapCodec;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Une pomme de pin, tombée d'un conifère. Elle brûle un peu plus longtemps qu'un bâton. */
public class PommeDePinBlock extends DebrisBlock {
    public static final MapCodec<PommeDePinBlock> CODEC = simpleCodec(PommeDePinBlock::new);
    private static final VoxelShape FORME = Block.box(4.5, 0, 4.5, 11.5, 4.5, 11.5);

    public PommeDePinBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected VoxelShape forme(BlockState state) {
        return FORME;
    }
}
