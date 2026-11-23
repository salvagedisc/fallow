package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.hotes.HotesModule;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * La natte d'offrande : ce qui y est posé. Tant que l'émissaire attend ({@link #attend()}), ce sont les dons, qu'on ne
 * reprend pas ; à l'aube, c'est le contre-don, que le joueur ramasse d'un clic.
 */
public class NatteBlockEntity extends BlockEntity {
    /** Au plus neuf piles : trois dons de trois piles, ou le contre-don. */
    /** Les places que la natte montre (NatteRenderer en dessine autant). */
    static final int PLACES = 9;

    private final List<ItemStack> poses = new ArrayList<>();
    private boolean attend;

    public NatteBlockEntity(BlockPos pos, BlockState state) {
        super(HotesModule.NATTE_BE.get(), pos, state);
    }

    public List<ItemStack> poses() {
        return poses;
    }

    public boolean attend() {
        return attend;
    }

    void attendre(boolean attend) {
        this.attend = attend;
        changed();
    }

    /**
     * Pose les piles sur la natte, en regroupant celles du même objet. Rien n'est jamais refusé ni perdu : au-delà des
     * {@link #PLACES} places dessinées, les piles restent sur la natte sans se voir.
     */
    void pose(List<ItemStack> piles) {
        for (ItemStack pile : piles) {
            ItemStack reste = pile.copy();
            for (ItemStack deja : poses) {
                if (ItemStack.isSameItemSameComponents(deja, reste)) {
                    deja.grow(reste.getCount());
                    reste = ItemStack.EMPTY;
                    break;
                }
            }
            if (!reste.isEmpty()) {
                poses.add(reste);
            }
        }
        changed();
    }

    /** Vide la natte et rend ce qu'elle portait. */
    List<ItemStack> vide() {
        List<ItemStack> out = new ArrayList<>(poses);
        poses.clear();
        changed();
        return out;
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ListTag liste = new ListTag();
        for (ItemStack pile : poses) {
            if (!pile.isEmpty()) {
                liste.add(pile.save(registries));
            }
        }
        tag.put("poses", liste);
        tag.putBoolean("attend", attend);
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        poses.clear();
        for (Tag entree : tag.getList("poses", Tag.TAG_COMPOUND)) {
            ItemStack.parse(registries, entree).ifPresent(poses::add);
        }
        attend = tag.getBoolean("attend");
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveWithoutMetadata(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
