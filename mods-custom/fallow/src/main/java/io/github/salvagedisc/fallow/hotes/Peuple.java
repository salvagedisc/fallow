package io.github.salvagedisc.fallow.hotes;

import java.util.Arrays;
import java.util.Optional;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.biome.Biome;

/**
 * Les six peuples des Hôtes, chacun veillant sur un dinosaure d'Archie's Dinosaurs et portant son armure. Chacun a son
 * émissaire attitré, son masque, sa parure, sa saison et son milieu : une clairière reçoit les peuples dont le milieu
 * l'entoure (tag de biomes {@code fallow:hotes/<peuple>}).
 */
public enum Peuple {
    ECORCE("ecorce", "lushsaur", "masque_de_l_ecorce", Saison.PRINTEMPS),
    BRAISE("braise", "obsidiceratops", "masque_de_la_braise", Saison.HIVER),
    GIVRE("givre", "chorusoatl", "masque_du_givre", Saison.AUTOMNE),
    FONDS("fonds", "coralosaur", "masque_des_fonds", Saison.ETE),
    SABLES("sables", "therimoss", "masque_des_sables", Saison.ETE),
    SILENCE("silence", "sculksaur", "masque_du_silence", Saison.AUCUNE);

    private final String id;
    private final String bete;
    private final String masque;
    private final Saison saison;
    private final TagKey<Biome> milieu;

    Peuple(String id, String bete, String masque, Saison saison) {
        this.id = id;
        this.bete = bete;
        this.masque = masque;
        this.saison = saison;
        this.milieu = TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath("fallow", "hotes/" + id));
    }

    /** L'identifiant de l'objet masque du peuple ({@code fallow:<masque>}). */
    public String masque() {
        return masque;
    }

    public String id() {
        return id;
    }

    /** Le nom du dinosaure gardien, tel qu'il apparaît dans les identifiants d'armure d'Archie ({@code ec_ad:<bête>_helmet}). */
    public String bete() {
        return bete;
    }

    /** La saison où le peuple vient le plus souvent ({@link Saison#AUCUNE} : les Taiseux viennent en toute saison). */
    public Saison saison() {
        return saison;
    }

    /** Vrai si le biome fait partie du milieu du peuple. */
    public boolean aime(Holder<Biome> biome) {
        return biome.is(milieu);
    }

    public static Optional<Peuple> parse(String id) {
        return Arrays.stream(values()).filter(p -> p.id.equals(id)).findFirst();
    }
}
