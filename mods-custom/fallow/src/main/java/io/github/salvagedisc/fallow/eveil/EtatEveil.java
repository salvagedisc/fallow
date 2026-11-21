package io.github.salvagedisc.fallow.eveil;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;

/**
 * Ce que l'objet a gardé de sa vie : son identifiant (la clé de ses actes dans {@link Actes}), le nombre d'éveils
 * qu'il peut porter (fixé par sa qualité de forge au premier acte), les enchantements qu'il a appris et quand, et
 * deux états : prêt à s'éveiller, ou éteint (à bout de durabilité, il vient d'oublier son dernier éveil), et le palier
 * de son chemin vers le prochain éveil (0 à 3, par quarts), que l'infobulle dit en mots.
 * Le composant ne change qu'à ces moments-là, jamais à chaque acte : l'objet en main ne « rebondit » pas.
 */
public record EtatEveil(UUID id, int places, List<Trace> traces, boolean pret, boolean eteint, int palier) {
    public static final Codec<EtatEveil> CODEC = RecordCodecBuilder.create(instance -> instance.group(
            UUIDUtil.CODEC.fieldOf("id").forGetter(EtatEveil::id),
            Codec.intRange(0, 16).optionalFieldOf("places", 1).forGetter(EtatEveil::places),
            Trace.CODEC.listOf().optionalFieldOf("traces", List.of()).forGetter(EtatEveil::traces),
            Codec.BOOL.optionalFieldOf("pret", false).forGetter(EtatEveil::pret),
            Codec.BOOL.optionalFieldOf("eteint", false).forGetter(EtatEveil::eteint),
            Codec.intRange(0, 3).optionalFieldOf("palier", 0).forGetter(EtatEveil::palier)
    ).apply(instance, EtatEveil::new));

    public static final StreamCodec<ByteBuf, EtatEveil> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, EtatEveil::id,
            ByteBufCodecs.VAR_INT, EtatEveil::places,
            Trace.STREAM_CODEC.apply(ByteBufCodecs.list()), EtatEveil::traces,
            ByteBufCodecs.BOOL, EtatEveil::pret,
            ByteBufCodecs.BOOL, EtatEveil::eteint,
            ByteBufCodecs.VAR_INT, EtatEveil::palier,
            EtatEveil::new);

    public EtatEveil {
        traces = List.copyOf(traces);
    }

    static EtatEveil neuf(int places) {
        return new EtatEveil(UUID.randomUUID(), places, List.of(), false, false, 0);
    }

    public int occupees() {
        return traces.size();
    }

    public boolean plein() {
        return traces.size() >= places;
    }

    EtatEveil avecPret(boolean value) {
        return new EtatEveil(id, places, traces, value, eteint, palier);
    }

    EtatEveil avecEteint(boolean value) {
        return new EtatEveil(id, places, traces, pret, value, palier);
    }

    EtatEveil avecTraces(List<Trace> value) {
        return new EtatEveil(id, places, value, pret, eteint, palier);
    }

    EtatEveil avecPalier(int value) {
        return new EtatEveil(id, places, traces, pret, eteint, value);
    }

    EtatEveil avecId(UUID value) {
        return new EtatEveil(value, places, traces, pret, eteint, palier);
    }

    /** Le niveau que les éveils ont donné à cet enchantement (0 s'il n'en vient pas). */
    public int niveau(ResourceLocation enchantement) {
        int max = 0;
        for (Trace trace : traces) {
            if (trace.enchantement().equals(enchantement)) {
                max = Math.max(max, trace.niveau());
            }
        }
        return max;
    }

    EtatEveil plus(Trace trace) {
        List<Trace> list = new ArrayList<>(traces);
        list.add(trace);
        return new EtatEveil(id, places, list, false, eteint, 0);
    }

    /**
     * Un éveil : l'enchantement appris, son niveau, celui qu'il remplaçait (0 s'il était nouveau), le jour du monde,
     * le joueur, et la bête dont venait l'éclat (clé de traduction de la créature), s'il y en avait un.
     */
    public record Trace(ResourceLocation enchantement, int niveau, int avant, long jour, String par, Optional<String> eclat) {
        public static final Codec<Trace> CODEC = RecordCodecBuilder.create(instance -> instance.group(
                ResourceLocation.CODEC.fieldOf("enchantement").forGetter(Trace::enchantement),
                Codec.intRange(1, 255).fieldOf("niveau").forGetter(Trace::niveau),
                Codec.intRange(0, 255).optionalFieldOf("avant", 0).forGetter(Trace::avant),
                Codec.LONG.optionalFieldOf("jour", 0L).forGetter(Trace::jour),
                Codec.STRING.optionalFieldOf("par", "").forGetter(Trace::par),
                Codec.STRING.optionalFieldOf("eclat").forGetter(Trace::eclat)
        ).apply(instance, Trace::new));

        public static final StreamCodec<ByteBuf, Trace> STREAM_CODEC = StreamCodec.composite(
                ResourceLocation.STREAM_CODEC, Trace::enchantement,
                ByteBufCodecs.VAR_INT, Trace::niveau,
                ByteBufCodecs.VAR_INT, Trace::avant,
                ByteBufCodecs.VAR_LONG, Trace::jour,
                ByteBufCodecs.STRING_UTF8, Trace::par,
                ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), Trace::eclat,
                Trace::new);
    }
}
