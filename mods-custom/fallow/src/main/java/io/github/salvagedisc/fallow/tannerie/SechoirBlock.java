package io.github.salvagedisc.fallow.tannerie;

import com.mojang.serialization.MapCodec;
import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.commun.Foyers;
import java.util.Locale;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
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
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le séchoir : un cadre de bois où le cuir mouillé est tendu. Il sèche par à-coups (toutes les 20 secondes) selon
 * l'endroit, voir {@link #rate}. Main vide, on décroche le cuir sec, ou on lit où en est le séchage.
 */
public class SechoirBlock extends HorizontalDirectionalBlock implements Ouvrage {
    public static final MapCodec<SechoirBlock> CODEC = simpleCodec(SechoirBlock::new);
    public static final EnumProperty<Etat> ETAT = EnumProperty.create("etat", Etat.class);
    public static final IntegerProperty SECHAGE = IntegerProperty.create("sechage", 0, 15);
    static final int INTERVAL = 400;
    // Le cadre droit sur ses patins (z 4 à 12 vu du nord).
    private static final VoxelShape SHAPE_NS = Block.box(0.0, 0.0, 2.0, 16.0, 16.0, 14.0);
    private static final VoxelShape SHAPE_EW = Block.box(2.0, 0.0, 0.0, 14.0, 16.0, 16.0);

    public enum Etat implements StringRepresentable {
        VIDE, HUMIDE, SEC;

        @Override
        public String getSerializedName() {
            return name().toLowerCase(Locale.ROOT);
        }
    }

    public SechoirBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH).setValue(ETAT, Etat.VIDE).setValue(SECHAGE, 0));
    }

    @Override
    protected MapCodec<? extends HorizontalDirectionalBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, ETAT, SECHAGE);
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
        return travailler(stack, state, level, pos, player, rendu -> player.setItemInHand(hand, rendu), sorti -> {
        });
    }

    /** Vrai si le séchoir, dans cet état, prend {@code stack} : un cuir mouillé sur un cadre vide. */
    public static boolean accepte(BlockState state, ItemStack stack) {
        return state.getValue(ETAT) == Etat.VIDE && stack.is(TannerieModule.CUIR_MOUILLE.get());
    }

    @Override
    public ItemInteractionResult travailler(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            java.util.function.Consumer<ItemStack> rendre, java.util.function.Consumer<ItemStack> produit) {
        if (!stack.is(TannerieModule.CUIR_MOUILLE.get())) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (state.getValue(ETAT) != Etat.VIDE) {
            if (!level.isClientSide()) {
                player.displayClientMessage(Component.translatable("tannerie.fallow.sechoir.occupe"), true);
            }
            return ItemInteractionResult.CONSUME;
        }
        if (!level.isClientSide()) {
            if (!FallowConfig.tannerieEnabled()) {
                return ItemInteractionResult.FAIL;
            }
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(ETAT, Etat.HUMIDE).setValue(SECHAGE, 0), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 0.7F, 0.8F);
            level.scheduleTick(pos, this, INTERVAL);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return mainVide(state, level, pos, player);
    }

    @Override
    public InteractionResult mainVide(BlockState state, Level level, BlockPos pos, Player player) {
        Etat etat = state.getValue(ETAT);
        if (etat == Etat.VIDE) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (etat == Etat.HUMIDE) {
            int percent = state.getValue(SECHAGE) * 100 / needed();
            player.displayClientMessage(Component.translatable("tannerie.fallow.sechoir.seche", percent,
                    Component.translatable("tannerie.fallow.sechoir.cause." + cause(level, pos))), true);
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(ETAT, Etat.VIDE).setValue(SECHAGE, 0), Block.UPDATE_ALL);
        ItemStack leather = new ItemStack(Items.LEATHER);
        if (!player.addItem(leather)) {
            player.drop(leather, false);
        }
        level.playSound(null, pos, SoundEvents.ARMOR_EQUIP_LEATHER.value(), SoundSource.BLOCKS, 1.0F, 1.2F);
        return InteractionResult.CONSUME;
    }

    public static int needed() {
        return Math.max(1, Math.min(15, FallowConfig.tannerieDryingPoints()));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Déplacé par un piston ou une machine de Create, le séchoir perd son tick prévu : il faut le reprendre.
        if (!oldState.is(this) && !level.isClientSide() && state.getValue(ETAT) == Etat.HUMIDE) {
            level.scheduleTick(pos, this, INTERVAL);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (state.getValue(ETAT) != Etat.HUMIDE) {
            return;
        }
        if (FallowConfig.tannerieEnabled()) {
            int points = Math.max(0, Math.min(15, state.getValue(SECHAGE) + rate(level, pos)));
            if (points >= needed()) {
                level.setBlock(pos, state.setValue(ETAT, Etat.SEC).setValue(SECHAGE, 0), Block.UPDATE_ALL);
                return;
            }
            if (points != state.getValue(SECHAGE)) {
                level.setBlock(pos, state.setValue(SECHAGE, points), Block.UPDATE_CLIENTS);
            }
        }
        level.scheduleTick(pos, this, INTERVAL);
    }

    /**
     * Points de séchage gagnés en 20 secondes : la pluie mouille (−1) ; sinon 2 au soleil, 1 à l'ombre ou la nuit,
     * 2 de plus près d'un feu allumé (4 blocs), 1 de moins quand il neige là (biome froid, ou l'hiver d'Ecliptic
     * Seasons, qui change la neige du biome).
     */
    static int rate(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        boolean sky = level.canSeeSky(above);
        if (sky && level.isRainingAt(above)) {
            return -1;
        }
        int rate = sky && level.isDay() ? 2 : 1;
        if (nearFire(level, pos)) {
            rate += 2;
        }
        if (cold(level, pos)) {
            rate -= 1;
        }
        return rate;
    }

    public static String cause(Level level, BlockPos pos) {
        BlockPos above = pos.above();
        boolean sky = level.canSeeSky(above);
        if (sky && level.isRainingAt(above)) {
            return "pluie";
        }
        if (nearFire(level, pos)) {
            return "feu";
        }
        if (cold(level, pos)) {
            return "froid";
        }
        return sky && level.isDay() ? "soleil" : "ombre";
    }

    private static boolean cold(Level level, BlockPos pos) {
        return level.getBiome(pos).value().coldEnoughToSnow(pos)
                || level.getBiome(pos).value().getPrecipitationAt(pos) == net.minecraft.world.level.biome.Biome.Precipitation.SNOW;
    }

    private static boolean nearFire(Level level, BlockPos pos) {
        return Foyers.near(level, pos, 4, 2);
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(ETAT) == Etat.HUMIDE && random.nextInt(8) == 0) {
            // Sous le bas du cuir, qui est à 5 pixels de l'avant du cadre.
            Vec3 bas = surCuir(state, pos, 0.25 + random.nextDouble() * 0.5, 0.25);
            level.addParticle(net.minecraft.core.particles.ParticleTypes.DRIPPING_WATER, bas.x, bas.y, bas.z, 0.0, 0.0, 0.0);
        }
        // Près d'un feu ou au soleil, le cuir mouillé fume un peu : on le voit sécher en passant (la recherche du feu, qui coûte,
        // n'est faite qu'une fois sur quatre).
        if (state.getValue(ETAT) == Etat.HUMIDE && random.nextInt(4) == 0) {
            String cause = cause(level, pos);
            int fois = "feu".equals(cause) ? 2 : "soleil".equals(cause) && random.nextBoolean() ? 1 : 0;
            for (int i = 0; i < fois; i++) {
                Vec3 face = surCuir(state, pos, 0.25 + random.nextDouble() * 0.5, 0.3 + random.nextDouble() * 0.55);
                level.addParticle(net.minecraft.core.particles.ParticleTypes.WHITE_SMOKE, face.x, face.y, face.z, 0.0, 0.015, 0.0);
            }
        }
    }

    /**
     * Un point devant le cuir tendu : {@code u} de gauche à droite (0 à 1) vu de devant, {@code h} la hauteur dans le bloc
     * (0 à 1). Vu du nord, le cuir est au milieu du bloc, à z = 8 pixels.
     */
    static Vec3 surCuir(BlockState state, BlockPos pos, double u, double h) {
        double profondeur = 7.6 / 16.0;
        double x;
        double z;
        switch (state.getValue(FACING)) {
            case SOUTH -> { x = 1.0 - u; z = 1.0 - profondeur; }
            case EAST -> { x = 1.0 - profondeur; z = u; }
            case WEST -> { x = profondeur; z = 1.0 - u; }
            default -> { x = u; z = profondeur; }
        }
        return new Vec3(pos.getX() + x, pos.getY() + h, pos.getZ() + z);
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(ETAT) != Etat.VIDE) {
            Block.popResource(level, pos, new ItemStack(state.getValue(ETAT) == Etat.SEC ? Items.LEATHER
                    : TannerieModule.CUIR_MOUILLE.get()));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
