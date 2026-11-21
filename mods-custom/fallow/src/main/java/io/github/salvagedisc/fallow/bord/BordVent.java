package io.github.salvagedisc.fallow.bord;

import io.github.salvagedisc.fallow.Fallow;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Le vent contraire du bord, envoyé chaque tick au joueur qui est dans la bande : la direction des terres
 * ({@code inX}, {@code inZ}), la force du vent ({@link BordModule#push}) et la distance au-delà du bord. Le joueur à
 * pied, sa monture ou son bateau avancent selon son client : c'est là que le vent les repousse
 * ({@link BordModule#gain}).
 */
public record BordVent(float inX, float inZ, float strength, float past) implements CustomPacketPayload {
    public static final Type<BordVent> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "bord"));
    public static final StreamCodec<ByteBuf, BordVent> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, BordVent::inX,
            ByteBufCodecs.FLOAT, BordVent::inZ,
            ByteBufCodecs.FLOAT, BordVent::strength,
            ByteBufCodecs.FLOAT, BordVent::past,
            BordVent::new);

    @Override
    public Type<BordVent> type() {
        return TYPE;
    }
}
