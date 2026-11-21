package io.github.salvagedisc.fallow.debris;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderSet;
import net.minecraft.core.RegistryCodecs;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.levelgen.feature.configurations.FeatureConfiguration;
import net.minecraft.world.level.levelgen.feature.stateproviders.BlockStateProvider;

/**
 * Réglages des débris au sol.
 *
 * @param essais  nombre de places essayées autour du point de départ
 * @param etendue écart horizontal maximal d'une place au point de départ
 * @param regles  ce qu'on pose selon la place, la première règle qui convient l'emporte
 */
public record DebrisConfig(IntProvider essais, int etendue, List<Regle> regles) implements FeatureConfiguration {

    public static final Codec<DebrisConfig> CODEC = RecordCodecBuilder.create(i -> i.group(
            IntProvider.codec(1, 64).fieldOf("essais").forGetter(DebrisConfig::essais),
            Codec.intRange(0, 8).optionalFieldOf("etendue", 6).forGetter(DebrisConfig::etendue),
            Regle.CODEC.listOf().fieldOf("regles").forGetter(DebrisConfig::regles)
    ).apply(i, DebrisConfig::new));

    /**
     * Une règle : si le sol et le couvert conviennent, on pose {@code pose} avec la chance donnée. Une règle qui convient
     * mais dont la chance ne tombe pas arrête la recherche : la place reste vide.
     *
     * @param sol      blocs sur lesquels la règle pose (absent : tout sol plein)
     * @param feuilles feuillage qui doit couvrir la place, le plus bas au-dessus d'elle (absent : la règle ne regarde
     *                 pas le couvert ; {@code sous_le_ciel} dit alors s'il faut qu'aucune feuille ne la couvre)
     * @param sousLeCiel vrai : la place ne doit être couverte d'aucune feuille (pour les cailloux des terrains ouverts)
     * @param pose     ce qu'on pose
     * @param chance   chance de poser quand la règle convient
     */
    public record Regle(Optional<HolderSet<Block>> sol, Optional<HolderSet<Block>> feuilles, boolean sousLeCiel,
            BlockStateProvider pose, float chance) {
        public static final Codec<Regle> CODEC = RecordCodecBuilder.create(i -> i.group(
                RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("sol").forGetter(Regle::sol),
                RegistryCodecs.homogeneousList(Registries.BLOCK).optionalFieldOf("feuilles").forGetter(Regle::feuilles),
                Codec.BOOL.optionalFieldOf("sous_le_ciel", false).forGetter(Regle::sousLeCiel),
                BlockStateProvider.CODEC.fieldOf("pose").forGetter(Regle::pose),
                Codec.floatRange(0, 1).optionalFieldOf("chance", 1f).forGetter(Regle::chance)
        ).apply(i, Regle::new));
    }
}
