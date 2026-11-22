package io.github.salvagedisc.fallow.tannerie;

import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * La cuve à tan : un seau d'eau, de l'écorce (une par peau), puis jusqu'à quatre peaux raclées. Elles trempent
 * {@link FallowConfig#tannerieTanningMinutes()} minutes, toutes ensemble ; une peau ajoutée en route relance le
 * trempage. Main vide, on sort le cuir mouillé, qui doit encore sécher.
 */
public class CuveBlock extends Block implements Ouvrage {
    public static final BooleanProperty EAU = BooleanProperty.create("eau");
    public static final IntegerProperty TANIN = IntegerProperty.create("tanin", 0, 8);
    public static final IntegerProperty PEAUX = IntegerProperty.create("peaux", 0, 4);
    /** Étapes de trempage, de 0 à 5 ; à 5, les peaux sont tannées. */
    public static final IntegerProperty TANNAGE = IntegerProperty.create("tannage", 0, 5);
    public static final int ETAPES = 5;
    private static final VoxelShape SHAPE = Shapes.join(Shapes.block(), Block.box(2.0, 2.0, 2.0, 14.0, 16.0, 14.0), BooleanOp.ONLY_FIRST);

    public CuveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(EAU, false).setValue(TANIN, 0).setValue(PEAUX, 0).setValue(TANNAGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EAU, TANIN, PEAUX, TANNAGE);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    static boolean ready(BlockState state) {
        return state.getValue(PEAUX) > 0 && state.getValue(TANNAGE) >= ETAPES;
    }

    private static int stepTicks() {
        return Math.max(20, (int) Math.round(FallowConfig.tannerieTanningMinutes() * 1200.0 / ETAPES));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        return travailler(stack, state, level, pos, player, rendu -> player.setItemInHand(hand, rendu), sorti -> {
        });
    }

    /**
     * Vrai si la cuve, dans cet état, prend {@code stack} (les mêmes règles que {@link #travailler}) : le poste le montre avant
     * le clic, et tremble aussitôt sur un refus.
     */
    public static boolean accepte(BlockState state, ItemStack stack) {
        boolean water = state.getValue(EAU);
        int hides = state.getValue(PEAUX);
        if (stack.is(Items.WATER_BUCKET)) {
            return !water;
        }
        if (stack.is(Items.BUCKET)) {
            return water && hides == 0;
        }
        if (!water || ready(state)) {
            return false;
        }
        if (stack.is(TannerieModule.TANINS)) {
            return state.getValue(TANIN) < 8;
        }
        return stack.is(TannerieModule.PEAU_RACLEE.get()) && hides < 4 && state.getValue(TANIN) > hides;
    }

    @Override
    public ItemInteractionResult travailler(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            java.util.function.Consumer<ItemStack> rendre, java.util.function.Consumer<ItemStack> produit) {
        boolean water = state.getValue(EAU);
        if (stack.is(Items.WATER_BUCKET)) {
            if (water) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                rendre.accept(ItemUtils.createFilledResult(stack, player, new ItemStack(Items.BUCKET)));
                level.setBlock(pos, state.setValue(EAU, true), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        if (stack.is(Items.BUCKET)) {
            if (!water || state.getValue(PEAUX) > 0) {
                return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
            }
            if (!level.isClientSide()) {
                rendre.accept(ItemUtils.createFilledResult(stack, player, new ItemStack(Items.WATER_BUCKET)));
                // Seule l'eau s'en va : l'écorce reste au fond pour la prochaine eau.
                level.setBlock(pos, state.setValue(EAU, false), Block.UPDATE_ALL);
                level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.7F, 1.0F);
            }
            return ItemInteractionResult.sidedSuccess(level.isClientSide());
        }
        boolean bark = stack.is(TannerieModule.TANINS);
        boolean hide = stack.is(TannerieModule.PEAU_RACLEE.get());
        if (!bark && !hide) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!water) {
            return refuse(player, "tannerie.fallow.cuve.sans_eau");
        }
        if (ready(state)) {
            return refuse(player, "tannerie.fallow.cuve.a_vider");
        }
        if (bark) {
            if (state.getValue(TANIN) >= 8) {
                return refuse(player, "tannerie.fallow.cuve.tan_plein");
            }
            stack.consume(1, player);
            level.setBlock(pos, state.setValue(TANIN, state.getValue(TANIN) + 1), Block.UPDATE_ALL);
            level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.4F, 1.4F);
            return ItemInteractionResult.SUCCESS;
        }
        int hides = state.getValue(PEAUX);
        if (hides >= 4) {
            return refuse(player, "tannerie.fallow.cuve.pleine");
        }
        if (state.getValue(TANIN) <= hides) {
            return refuse(player, "tannerie.fallow.cuve.sans_tan");
        }
        if (!FallowConfig.tannerieEnabled()) {
            return ItemInteractionResult.FAIL;
        }
        stack.consume(1, player);
        level.setBlock(pos, state.setValue(PEAUX, hides + 1).setValue(TANNAGE, 0), Block.UPDATE_ALL);
        level.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.6F, 0.9F);
        // Le tannage repart de zéro : le pas déjà prévu (le jeu n'en garde qu'un par bloc) tomberait trop tôt.
        if (level instanceof ServerLevel server) {
            server.getBlockTicks().clearArea(new BoundingBox(pos));
        }
        level.scheduleTick(pos, this, stepTicks());
        return ItemInteractionResult.SUCCESS;
    }

    private static ItemInteractionResult refuse(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
        return ItemInteractionResult.CONSUME;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        return mainVide(state, level, pos, player);
    }

    @Override
    public InteractionResult mainVide(BlockState state, Level level, BlockPos pos, Player player) {
        int hides = state.getValue(PEAUX);
        if (hides == 0) {
            return InteractionResult.PASS;
        }
        if (level.isClientSide()) {
            return InteractionResult.SUCCESS;
        }
        if (!ready(state)) {
            int percent = state.getValue(TANNAGE) * 100 / ETAPES;
            player.displayClientMessage(Component.translatable("tannerie.fallow.cuve.trempe", hides, percent), true);
            return InteractionResult.CONSUME;
        }
        level.setBlock(pos, state.setValue(PEAUX, 0).setValue(TANNAGE, 0).setValue(TANIN, state.getValue(TANIN) - hides),
                Block.UPDATE_ALL);
        ItemStack leather = new ItemStack(TannerieModule.CUIR_MOUILLE.get(), hides);
        if (!player.addItem(leather)) {
            player.drop(leather, false);
        }
        level.playSound(null, pos, SoundEvents.BUCKET_FILL, SoundSource.BLOCKS, 0.6F, 1.2F);
        return InteractionResult.CONSUME;
    }

    /** Vrai tant que des peaux trempent : la cuve attend alors son prochain pas de tannage. */
    private static boolean soaking(BlockState state) {
        return state.getValue(PEAUX) > 0 && !ready(state) && state.getValue(EAU);
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        // Déplacée par un piston ou une machine de Create, la cuve perd son tick prévu : il faut le reprendre.
        if (!oldState.is(this) && !level.isClientSide() && soaking(state)) {
            level.scheduleTick(pos, this, stepTicks());
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!soaking(state)) {
            return;
        }
        if (!FallowConfig.tannerieEnabled()) {
            // Module coupé : le tannage attend sans s'arrêter, pour reprendre quand on le rallume.
            level.scheduleTick(pos, this, stepTicks());
            return;
        }
        int step = state.getValue(TANNAGE) + 1;
        level.setBlock(pos, state.setValue(TANNAGE, step), Block.UPDATE_CLIENTS);
        if (step < ETAPES) {
            level.scheduleTick(pos, this, stepTicks());
        } else {
            level.playSound(null, pos, SoundEvents.BUBBLE_COLUMN_UPWARDS_AMBIENT, SoundSource.BLOCKS, 0.6F, 0.8F);
        }
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (state.getValue(PEAUX) > 0 && !ready(state) && random.nextInt(5) == 0) {
            level.addParticle(ParticleTypes.BUBBLE_POP, pos.getX() + 0.25 + random.nextDouble() * 0.5, pos.getY() + 0.82,
                    pos.getZ() + 0.25 + random.nextDouble() * 0.5, 0.0, 0.01, 0.0);
        }
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean movedByPiston) {
        if (!movedByPiston && !state.is(newState.getBlock()) && state.getValue(PEAUX) > 0) {
            Block.popResource(level, pos, new ItemStack(ready(state) ? TannerieModule.CUIR_MOUILLE.get()
                    : TannerieModule.PEAU_RACLEE.get(), state.getValue(PEAUX)));
        }
        super.onRemove(state, level, pos, newState, movedByPiston);
    }
}
