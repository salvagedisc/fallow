package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.hotes.Peuple;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

/**
 * Une visite d'un peuple à une clairière. Le soir du signe ({@link Phase#SIGNE}), un masque pend au bord de la clairière ;
 * le soir suivant ({@link Phase#VENUE}), l'émissaire attend à la lisière avec la natte, jusqu'à l'aube.
 */
public final class Visite {
    public enum Phase { SIGNE, VENUE }

    final Peuple peuple;
    final BlockPos clairiere;
    /** Le jour où l'émissaire vient (le soir du jour {@code jour}) et repart (à l'aube du même jour). */
    long jour;
    Phase phase = Phase.SIGNE;
    @Nullable BlockPos signe;
    @Nullable BlockPos natte;
    @Nullable UUID hote;
    final List<Demande> demandes = new ArrayList<>();
    /** Les souhaits déjà remplis, un bit par souhait. */
    int donnes;
    boolean refus;

    Visite(Peuple peuple, BlockPos clairiere, long jour) {
        this.peuple = peuple;
        this.clairiere = clairiere.immutable();
        this.jour = jour;
    }

    public Peuple peuple() {
        return peuple;
    }

    public BlockPos clairiere() {
        return clairiere;
    }

    public Phase phase() {
        return phase;
    }

    public List<Demande> demandes() {
        return demandes;
    }

    public boolean donne(int index) {
        return (donnes & (1 << index)) != 0;
    }

    public int nombreDeDons() {
        return Integer.bitCount(donnes);
    }

    public boolean refus() {
        return refus;
    }

    @Nullable
    public UUID hote() {
        return hote;
    }

    @Nullable
    public BlockPos natte() {
        return natte;
    }

    CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("peuple", peuple.id());
        tag.putLong("clairiere", clairiere.asLong());
        tag.putLong("jour", jour);
        tag.putString("phase", phase.name());
        if (signe != null) {
            tag.putLong("signe", signe.asLong());
        }
        if (natte != null) {
            tag.putLong("natte", natte.asLong());
        }
        if (hote != null) {
            tag.putUUID("hote", hote);
        }
        ListTag liste = new ListTag();
        for (Demande demande : demandes) {
            liste.add(demande.save());
        }
        tag.put("demandes", liste);
        tag.putInt("donnes", donnes);
        tag.putBoolean("refus", refus);
        return tag;
    }

    @Nullable
    static Visite load(CompoundTag tag) {
        Peuple peuple = Peuple.parse(tag.getString("peuple")).orElse(null);
        if (peuple == null) {
            return null;
        }
        Visite visite = new Visite(peuple, BlockPos.of(tag.getLong("clairiere")), tag.getLong("jour"));
        try {
            visite.phase = Phase.valueOf(tag.getString("phase"));
        } catch (IllegalArgumentException e) {
            visite.phase = Phase.SIGNE;
        }
        visite.signe = tag.contains("signe") ? BlockPos.of(tag.getLong("signe")) : null;
        visite.natte = tag.contains("natte") ? BlockPos.of(tag.getLong("natte")) : null;
        visite.hote = tag.hasUUID("hote") ? tag.getUUID("hote") : null;
        // Un souhait dont l'objet n'existe plus (mod retiré) est oublié ; les souhaits remplis suivent leur nouvelle place.
        int donnes = tag.getInt("donnes");
        net.minecraft.nbt.ListTag liste = tag.getList("demandes", Tag.TAG_COMPOUND);
        for (int i = 0; i < liste.size(); i++) {
            Demande demande = Demande.load(liste.getCompound(i));
            if (demande != null) {
                if ((donnes & (1 << i)) != 0) {
                    visite.donnes |= 1 << visite.demandes.size();
                }
                visite.demandes.add(demande);
            }
        }
        visite.refus = tag.getBoolean("refus");
        return visite;
    }
}
