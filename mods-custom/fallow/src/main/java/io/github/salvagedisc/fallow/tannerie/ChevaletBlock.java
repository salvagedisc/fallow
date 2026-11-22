package io.github.salvagedisc.fallow.tannerie;

import com.mojang.serialization.MapCodec;
import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.HorizontalDirectionalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le chevalet à écharner : une poutre inclinée sur deux pieds. On y pose une peau brute, puis on la racle au couteau
 * (quelques coups, {@link FallowConfig#tannerieStrokes()}) ; elle tombe raclée, parfois avec un morceau de gras.
 * Main vide, on reprend la peau.
 */
public class ChevaletBlock extends HorizontalDirectionalBlock implements Ouvrage {
    public static final MapCodec<ChevaletBlock> CODEC = simpleCodec(ChevaletBlock::new);
    public static final BooleanProperty PEAU = BooleanProperty.create("peau");
    /** Coups déjà donnés sur la peau posée (le réglage s'arrête à 15). */
    public static final IntegerProperty RACLAGE = IntegerProperty.create("raclage", 0, 15);
    /** Le banc : plateau de 10 sur 16, en long dans le sens du bloc. */
    private static final VoxelShape SHAPE_NS = Block.box(3.0, 0.0, 0.0, 13.0, 12.0, 16.0);
    private static final VoxelShape SHAPE_EW = Block.box(0.0, 0.0, 3.0, 16.0, 12.0, 13.0);

    public ChevaletBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(PEAU, false).setValue(RACLAGE, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PEAU, RACLAGE);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getHorizontalDirection().getOpposite());
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return state.getValue(FACING).getAxis() == Direction.Axis.Z ? SHAPE_NS : SHAPE_EW;
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return travailler(stack, state, level, pos, player, rendu -> player.setItemInHand(hand, rendu),
                sorti -> Block.popResourceFromFace(level, pos, Direction.UP, sorti));
    }

    /** Vrai si le chevalet, dans cet état, prend {@code stack} : une peau brute s'il est nu, un couteau si une peau est tendue. */
    public static boolean accepte(BlockState state, ItemStack stack) {
        return state.getValue(PEAU) ? stack.is(TannerieModule.COUTEAUX) : stack.is(TannerieModule.PEAU_BRUTE.get());
    }

    @Override
    public ItemInteractionResult travailler(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            java.util.function.Consumer<ItemStack> rendre, java.util.function.Consumer<ItemStack> produit) {
        if (!state.getValue(PEAU)) {
            if (!stack.is(TannerieModule.PEAU_BRUTE.get())) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                stack.consume(1, player);
                level.setBlock(pos, state.setValue(PEAU, true).setValue(RACLAGE, 0), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.8F, 0.9F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (!stack.is(TannerieModule.COUTEAUX)) {
            if (stack.is(TannerieModule.PEAU_BRUTE.get())) {
                // Une peau est déjà tendue : le dire, sans la reprendre aussitôt (useWithoutItem).
                if (!level.isClientSide()) {
                    player.displayClientMessage(Component.translatable("tannerie.fallow.chevalet.occupe"), true);
                }
                return ItemInteractionResult.CONSUME;
            }
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            // Dans le monde, le raclement s'entend tout de suite chez celui qui racle ; le serveur le fait entendre aux autres.
            level.playSound(player, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.55F, 0.8F + level.getRandom().nextFloat() * 0.3F);
            return ItemInteractionResult.SUCCESS;
        }
        if (!FallowConfig.tannerieEnabled()) {
            return ItemInteractionResult.FAIL;
        }
        ServerLevel server = (ServerLevel) level;
        stack.hurtAndBreak(1, server, player, item -> server.playSound(null, pos, SoundEvents.ITEM_BREAK, SoundSource.PLAYERS, 0.8F, 1.0F));
        server.playSound(player, pos, SoundEvents.BRUSH_GENERIC, SoundSource.BLOCKS, 0.55F, 0.8F + level.getRandom().nextFloat() * 0.3F);
        server.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, new ItemStack(TannerieModule.PEAU_BRUTE.get())),
                pos.getX() + 0.5, pos.getY() + 0.85, pos.getZ() + 0.5, 6, 0.2, 0.05, 0.2, 0.05);
        int done = state.getValue(RACLAGE) + 1;
        int needed = Math.min(15, FallowConfig.tannerieStrokes());
        if (done < needed) {
            level.setBlock(pos, state.setValue(RACLAGE, done), Block.UPDATE_ALL);
            return ItemInteractionResult.SUCCESS;
        }
        level.setBlock(pos, state.setValue(PEAU, false).setValue(RACLAGE, 0), Block.UPDATE_ALL);
        produit.accept(new ItemStack(TannerieModule.PEAU_RACLEE.get()));
        if (level.getRandom().nextDouble() < FallowConfig.tannerieFatChance()) {
            BuiltInRegistries.ITEM.getOptional(TannerieModule.GRAS).filter(item -> item != Items.AIR)
                    .ifPresent(item -> produit.accept(new ItemStack(item)));
        }
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        // Main vide seulement : un bloc en main se pose contre le chevalet au lieu de reprendre la peau.
        if (!player.getMainHandItem().isEmpty()) {
            return InteractionResult.PASS;
        }
        return mainVide(state, level, pos, player);
    }

    @Override
    public InteractionResult mainVide(BlockState state, Level level, BlockPos pos, Player player) {
        if (!state.getValue(PEAU)) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            level.setBlock(pos, state.setValue(PEAU, false).setValue(RACLAGE, 0), Block.UPDATE_ALL);
            ItemStack hide = new ItemStack(TannerieModule.PEAU_BRUTE.get());
            if (!player.addItem(hide)) {
                player.drop(hide, false);
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(PEAU)) {
            Block.popResource(level, pos, new ItemStack(TannerieModule.PEAU_BRUTE.get()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
