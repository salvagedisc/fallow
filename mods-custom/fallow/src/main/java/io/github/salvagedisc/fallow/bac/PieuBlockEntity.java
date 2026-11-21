package io.github.salvagedisc.fallow.bac;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Un pieu de bac : il sait à quel pieu d'en face sa corde va, s'il la porte (le pieu où elle a été attachée en
 * premier, qui l'inscrit et la dessine) et combien de cordes elle a coûté, rendues si on la défait.
 */
public class PieuBlockEntity extends BlockEntity {
    private BlockPos other;
    private boolean owner;
    private int ropes;

    public PieuBlockEntity(BlockPos pos, BlockState state) {
        super(BacModule.PIEU_BE.get(), pos, state);
    }

    public BlockPos other() {
        return other;
    }

    public boolean owner() {
        return owner;
    }

    int ropes() {
        return ropes;
    }

    void link(BlockPos to, boolean isOwner, int cost) {
        if (owner && level != null && !level.isClientSide()) {
            Cordes.remove(level, worldPosition);
        }
        other = to == null ? null : to.immutable();
        owner = to != null && isOwner;
        ropes = owner ? cost : 0;
        if (owner && level != null && !level.isClientSide()) {
            Cordes.add(level, worldPosition, other);
        }
        changed();
    }

    void unlink() {
        link(null, false, 0);
    }

    private void changed() {
        setChanged();
        if (level != null) {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_CLIENTS);
        }
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (owner && other != null && level != null && !level.isClientSide()) {
            Cordes.add(level, worldPosition, other);
        }
    }

    @Override
    public void setRemoved() {
        if (owner && level != null && !level.isClientSide()) {
            Cordes.remove(level, worldPosition);
        }
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        if (owner && level != null && !level.isClientSide()) {
            Cordes.remove(level, worldPosition);
        }
        super.onChunkUnloaded();
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (other != null) {
            tag.putLong("autre", other.asLong());
            tag.putBoolean("porteur", owner);
            tag.putInt("cordes", ropes);
        }
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        other = tag.contains("autre") ? BlockPos.of(tag.getLong("autre")) : null;
        owner = other != null && tag.getBoolean("porteur");
        ropes = tag.getInt("cordes");
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
