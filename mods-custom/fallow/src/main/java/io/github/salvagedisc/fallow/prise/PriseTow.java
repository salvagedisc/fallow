package io.github.salvagedisc.fallow.prise;

import io.github.salvagedisc.fallow.Fallow;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;

/**
 * Ce que le serveur dit au joueur qui mène une barque (une entité, que son client fait avancer) : le poisson tire vers
 * ({@code dx}, {@code dz}) et le bateau doit aller à {@code speed} blocs par seconde, pendant les quelques ticks qui
 * viennent.
 */
public record PriseTow(float dx, float dz, float speed) implements CustomPacketPayload {
    public static final Type<PriseTow> TYPE = new Type<>(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "prise"));
    public static final StreamCodec<ByteBuf, PriseTow> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, PriseTow::dx,
            ByteBufCodecs.FLOAT, PriseTow::dz,
            ByteBufCodecs.FLOAT, PriseTow::speed,
            PriseTow::new);

    @Override
    public Type<PriseTow> type() {
        return TYPE;
    }
}
