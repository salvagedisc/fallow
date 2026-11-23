package io.github.salvagedisc.fallow.hotes.visite;

import net.minecraft.nbt.CompoundTag;

/**
 * Le lien d'un peuple avec une clairière. Les points montent avec les dons et descendent avec l'oubli ; le joueur ne
 * les voit jamais, il lit le palier aux signes et aux contre-dons.
 */
public final class Lien {
    /** Pas encore de refus poli : le premier est toujours accepté. */
    private static final long JAMAIS = Long.MIN_VALUE / 2;

    int points;
    /** Visites de suite restées sans don ni refus poli. */
    int oublis;
    /** Le jour (heure du monde / 24000) à partir duquel le peuple peut revenir. */
    long prochaine;
    long dernierRefus = JAMAIS;

    public Palier palier() {
        return Palier.de(points);
    }

    public int points() {
        return points;
    }

    public int oublis() {
        return oublis;
    }

    public long prochaine() {
        return prochaine;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("points", points);
        tag.putInt("oublis", oublis);
        tag.putLong("prochaine", prochaine);
        tag.putLong("dernierRefus", dernierRefus);
        return tag;
    }

    static Lien load(CompoundTag tag) {
        Lien lien = new Lien();
        lien.points = tag.getInt("points");
        lien.oublis = tag.getInt("oublis");
        lien.prochaine = tag.getLong("prochaine");
        lien.dernierRefus = tag.contains("dernierRefus") ? tag.getLong("dernierRefus") : JAMAIS;
        return lien;
    }
}
