package io.github.salvagedisc.fallow.bac;

import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * Le pieu de bac, planté sur chaque rive. Une corde en main sur un pieu, puis sur celui d'en face : la corde est
 * tendue entre les deux (une corde pour huit blocs, au moins une). Maj + clic main vide sur un pieu défait sa corde et
 * rend les cordes ; casser un pieu aussi.
 */
public class PieuBlock extends Block implements EntityBlock {
    /** Portée d'une corde de bac, en blocs. */
    static final int MAX_LENGTH = 96;
    private static final String PENDING = "fallow_bac_pieu";
    private static final String PENDING_WORLD = "fallow_bac_pieu_monde";
    private static final String PENDING_TIME = "fallow_bac_pieu_temps";
    /** Le premier nœud attend le second cinq minutes ; au-delà (ou après une déconnexion), on recommence. */
    private static final long PENDING_TICKS = 6000L;
    private static final VoxelShape SHAPE = Block.box(5.0, 0.0, 5.0, 11.0, 16.0, 11.0);

    public PieuBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PieuBlockEntity(pos, state);
    }

    /** Cordes demandées pour une corde de bac de cette longueur. */
    static int cost(double length) {
        return Math.max(1, (int) Math.ceil(length / 8.0));
    }

    @Override
    protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!stack.is(BacModule.CORDES)) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        if (level.isClientSide()) {
            return ItemInteractionResult.SUCCESS;
        }
        if (!FallowConfig.bacEnabled()) {
            return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        }
        CompoundTag data = player.getPersistentData();
        String world = level.dimension().location().toString();
        long now = level.getGameTime();
        long since = now - data.getLong(PENDING_TIME);
        BlockPos first = data.contains(PENDING) && world.equals(data.getString(PENDING_WORLD)) && since >= 0 && since < PENDING_TICKS
                ? BlockPos.of(data.getLong(PENDING)) : null;
        if (first != null && !first.equals(pos) && first.distSqr(pos) > (double) MAX_LENGTH * MAX_LENGTH) {
            // Trop loin du premier nœud : le dire sans aller lire (ni charger) le tronçon où il est resté.
            data.remove(PENDING);
            data.remove(PENDING_WORLD);
            data.remove(PENDING_TIME);
            player.displayClientMessage(Component.translatable("message.fallow.bac.trop_loin", MAX_LENGTH), true);
            return ItemInteractionResult.SUCCESS;
        }
        if (first == null || !level.isLoaded(first) || first.equals(pos) || !(level.getBlockEntity(first) instanceof PieuBlockEntity start)) {
            data.putLong(PENDING, pos.asLong());
            data.putString(PENDING_WORLD, world);
            data.putLong(PENDING_TIME, now);
            player.displayClientMessage(Component.translatable("message.fallow.bac.attache"), true);
            return ItemInteractionResult.SUCCESS;
        }
        data.remove(PENDING);
        data.remove(PENDING_WORLD);
        data.remove(PENDING_TIME);
        if (!(level.getBlockEntity(pos) instanceof PieuBlockEntity end)) {
            return ItemInteractionResult.SUCCESS;
        }
        double length = Math.sqrt(first.distSqr(pos));
        if (length > MAX_LENGTH) {
            player.displayClientMessage(Component.translatable("message.fallow.bac.trop_loin", MAX_LENGTH), true);
            return ItemInteractionResult.SUCCESS;
        }
        int cost = cost(length);
        if (!player.getAbilities().instabuild) {
            int held = count(player);
            if (held < cost) {
                player.displayClientMessage(Component.translatable("message.fallow.bac.cordes", cost), true);
                return ItemInteractionResult.SUCCESS;
            }
            take(player, cost);
        }
        release(level, start);
        release(level, end);
        start.link(pos, true, player.getAbilities().instabuild ? 0 : cost);
        end.link(first, false, 0);
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 0.8F);
        player.displayClientMessage(Component.translatable("message.fallow.bac.tendue"), true);
        return ItemInteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown() || !(level.getBlockEntity(pos) instanceof PieuBlockEntity post) || post.other() == null) {
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            release(level, post);
            level.playSound(null, pos, SoundEvents.LEASH_KNOT_BREAK, SoundSource.BLOCKS, 1.0F, 1.0F);
            player.displayClientMessage(Component.translatable("message.fallow.bac.defaite"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof PieuBlockEntity post) {
            if (moved) {
                // Déplacé (un navire de Sable ou une machine l'emporte, avec ses données) : la copie garde les cordes et
                // les rendra si on la casse. Ici on défait seulement le pieu d'en face, sans rien rendre.
                detach(level, post);
            } else {
                release(level, post);
            }
        }
        super.onRemove(state, level, pos, newState, moved);
    }

    /** Défait la corde de ce pieu des deux côtés, et rend les cordes au pied du pieu qui la portait. */
    private static void release(Level level, PieuBlockEntity post) {
        BlockPos other = post.other();
        if (other == null || level.isClientSide()) {
            return;
        }
        PieuBlockEntity owner = post.owner() ? post
                : level.getBlockEntity(other) instanceof PieuBlockEntity o && o.owner() && post.getBlockPos().equals(o.other()) ? o : null;
        if (owner != null && owner.ropes() > 0) {
            Block.popResource(level, owner.getBlockPos(), new ItemStack(BacModule.cordeRendue(), owner.ropes()));
        }
        if (level.getBlockEntity(other) instanceof PieuBlockEntity far && post.getBlockPos().equals(far.other())) {
            far.unlink();
        }
        post.unlink();
    }

    /** Défait le pieu d'en face s'il tient encore à celui-ci, sans rendre de cordes. */
    private static void detach(Level level, PieuBlockEntity post) {
        BlockPos other = post.other();
        if (other == null || level.isClientSide()) {
            return;
        }
        if (level.getBlockEntity(other) instanceof PieuBlockEntity far && post.getBlockPos().equals(far.other())) {
            far.unlink();
        }
    }

    /** Les piles où chercher des cordes : l'inventaire et la main gauche. */
    private static Iterable<ItemStack> stacks(Player player) {
        java.util.List<ItemStack> all = new java.util.ArrayList<>(player.getInventory().items);
        all.addAll(player.getInventory().offhand);
        return all;
    }

    private static int count(Player player) {
        int count = 0;
        for (ItemStack stack : stacks(player)) {
            if (stack.is(BacModule.CORDES)) {
                count += stack.getCount();
            }
        }
        return count;
    }

    private static void take(Player player, int amount) {
        for (ItemStack stack : stacks(player)) {
            if (amount <= 0) {
                return;
            }
            if (stack.is(BacModule.CORDES)) {
                int taken = Math.min(amount, stack.getCount());
                stack.shrink(taken);
                amount -= taken;
            }
        }
    }
}
