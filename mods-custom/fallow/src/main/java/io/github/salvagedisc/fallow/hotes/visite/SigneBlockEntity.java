package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.hotes.HotesModule;
import io.github.salvagedisc.fallow.hotes.Peuple;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Le signe de la veille : de quel peuple est le masque qui pend au piquet. */
public class SigneBlockEntity extends BlockEntity {
    private Peuple peuple = Peuple.ECORCE;

    public SigneBlockEntity(BlockPos pos, BlockState state) {
        super(HotesModule.SIGNE_BE.get(), pos, state);
    }

    public Peuple peuple() {
        return peuple;
    }

    void peuple(Peuple peuple) {
        this.peuple = peuple;
        setChanged();
        if (level != null) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putString("peuple", peuple.id());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        peuple = Peuple.parse(tag.getString("peuple")).orElse(Peuple.ECORCE);
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
