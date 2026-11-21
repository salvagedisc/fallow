package io.github.salvagedisc.fallow.eveil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Ce que porte un éclat de grande bête : l'enchantement, son niveau, la bête (clé de traduction de la créature), la
 * couleur de l'éclat et son récit (clé de traduction, facultatif).
 */
public record Eclat(ResourceLocation enchantement, int niveau, String bete, int couleur, String recit) {
    static final int COULEUR = 0xB98CFF;

    public static final Codec<Eclat> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            ResourceLocation.CODEC.fieldOf("enchantement").forGetter(Eclat::enchantement),
            Codec.intRange(1, 255).fieldOf("niveau").forGetter(Eclat::niveau),
            Codec.STRING.fieldOf("bete").forGetter(Eclat::bete),
            Codec.INT.optionalFieldOf("couleur", COULEUR).forGetter(Eclat::couleur),
            Codec.STRING.optionalFieldOf("recit", "").forGetter(Eclat::recit)
    ).apply(instance, Eclat::new));

    public static final StreamCodec<ByteBuf, Eclat> STREAM_CODEC = StreamCodec.composite(
            ResourceLocation.STREAM_CODEC, Eclat::enchantement,
            ByteBufCodecs.VAR_INT, Eclat::niveau,
            ByteBufCodecs.STRING_UTF8, Eclat::bete,
            ByteBufCodecs.INT, Eclat::couleur,
            ByteBufCodecs.STRING_UTF8, Eclat::recit,
            Eclat::new);
}
