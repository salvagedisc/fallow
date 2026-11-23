package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.hotes.Peuple;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
import org.jetbrains.annotations.Nullable;

/** Ce que les Hôtes retiennent d'un monde : le lien de chaque peuple avec chaque clairière, et les visites en cours. */
public final class Liens extends SavedData {
    private static final String NOM = "fallow_hotes";

    private final Long2ObjectOpenHashMap<EnumMap<Peuple, Lien>> liens = new Long2ObjectOpenHashMap<>();
    private final Long2ObjectOpenHashMap<Visite> visites = new Long2ObjectOpenHashMap<>();
    /** Le dernier jour où chaque clairière était là (feu ou lanterne allumés). */
    private final it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap vues = new it.unimi.dsi.fastutil.longs.Long2LongOpenHashMap();

    public static Liens de(ServerLevel level) {
        return level.getServer().overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Liens::new, Liens::load, null), NOM);
    }

    /** Le lien du peuple avec la clairière de centre {@code clairiere} ; {@code null} s'il ne l'a jamais vue. */
    @Nullable
    public Lien lien(BlockPos clairiere, Peuple peuple) {
        EnumMap<Peuple, Lien> parPeuple = liens.get(clairiere.asLong());
        return parPeuple == null ? null : parPeuple.get(peuple);
    }

    /** Le lien, créé au besoin : un peuple qui découvre la clairière pourra venir à partir du jour {@code premier}. */
    Lien lienOuNouveau(BlockPos clairiere, Peuple peuple, long premier) {
        return liens.computeIfAbsent(clairiere.asLong(), k -> new EnumMap<>(Peuple.class)).computeIfAbsent(peuple, p -> {
            Lien lien = new Lien();
            lien.prochaine = premier;
            setDirty();
            return lien;
        });
    }

    @Nullable
    public Visite visite(BlockPos clairiere) {
        return visites.get(clairiere.asLong());
    }

    Collection<Visite> visites() {
        return visites.values();
    }

    /** La visite dont l'émissaire est {@code hote}. */
    @Nullable
    public Visite visiteDe(UUID hote) {
        for (Visite visite : visites.values()) {
            if (hote.equals(visite.hote)) {
                return visite;
            }
        }
        return null;
    }

    void ajoute(Visite visite) {
        visites.put(visite.clairiere.asLong(), visite);
        setDirty();
    }

    void retire(Visite visite) {
        visites.remove(visite.clairiere.asLong());
        setDirty();
    }

    /** Les liens d'une clairière, pour la commande de banc. */
    public Map<Peuple, Lien> liens(BlockPos clairiere) {
        EnumMap<Peuple, Lien> parPeuple = liens.get(clairiere.asLong());
        return parPeuple == null ? Map.of() : parPeuple;
    }

    /**
     * Oublie les clairières rendues à la brume depuis plus de {@code delai} jours. Un feu qu'on rallume, une lanterne
     * qu'on déplace d'un soir ne font rien perdre : les peuples se souviennent d'une clairière tant qu'elle revient vite.
     */
    void garde(Collection<Long> centres, long jour, long delai) {
        for (long centre : centres) {
            if (vues.put(centre, jour) != jour) {
                setDirty();
            }
        }
        var it = liens.long2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            long centre = it.next().getLongKey();
            long vue = vues.getOrDefault(centre, jour);
            if (!vues.containsKey(centre)) {
                vues.put(centre, jour);
            }
            if (jour - vue > delai) {
                it.remove();
                vues.remove(centre);
                setDirty();
            }
        }
    }

    public void change() {
        setDirty();
    }

    private static Liens load(CompoundTag tag, HolderLookup.Provider registries) {
        Liens data = new Liens();
        for (Tag entree : tag.getList("liens", Tag.TAG_COMPOUND)) {
            CompoundTag c = (CompoundTag) entree;
            EnumMap<Peuple, Lien> parPeuple = new EnumMap<>(Peuple.class);
            for (Peuple peuple : Peuple.values()) {
                if (c.contains(peuple.id())) {
                    parPeuple.put(peuple, Lien.load(c.getCompound(peuple.id())));
                }
            }
            data.liens.put(c.getLong("clairiere"), parPeuple);
            if (c.contains("vue")) {
                data.vues.put(c.getLong("clairiere"), c.getLong("vue"));
            }
        }
        for (Tag entree : tag.getList("visites", Tag.TAG_COMPOUND)) {
            Visite visite = Visite.load((CompoundTag) entree);
            if (visite != null) {
                data.visites.put(visite.clairiere.asLong(), visite);
            }
        }
        return data;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        ListTag liste = new ListTag();
        for (Long2ObjectOpenHashMap.Entry<EnumMap<Peuple, Lien>> entree : liens.long2ObjectEntrySet()) {
            CompoundTag c = new CompoundTag();
            c.putLong("clairiere", entree.getLongKey());
            if (vues.containsKey(entree.getLongKey())) {
                c.putLong("vue", vues.get(entree.getLongKey()));
            }
            for (Map.Entry<Peuple, Lien> lien : entree.getValue().entrySet()) {
                c.put(lien.getKey().id(), lien.getValue().save());
            }
            liste.add(c);
        }
        tag.put("liens", liste);
        ListTag v = new ListTag();
        for (Visite visite : new ArrayList<>(visites.values())) {
            v.add(visite.save());
        }
        tag.put("visites", v);
        return tag;
    }
}
