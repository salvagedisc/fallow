package io.github.salvagedisc.fallow.eveil;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.saveddata.SavedData;

/**
 * Les actes de chaque objet, par entrée des hauts faits : ce que la pioche a creusé, ce que l'épée a vaincu. Gardés
 * par le monde (pas sur l'objet, qui ne change ainsi qu'à l'éveil), sous l'identifiant de l'objet ; un objet perdu
 * cassé, brûlé ou disparu au sol emporte ses actes ({@link #oublier}).
 */
final class Actes extends SavedData {
    private static final String NAME = "fallow_eveil";
    private static final String FOIS = "#";
    /** Les notes de l'objet hors des actes (son attachement, s'il a été nommé…) : {@link #NOTE} devant la clé, hors des points. */
    private static final String NOTE = "~";

    private final Map<UUID, Map<String, Double>> objets = new HashMap<>();

    static Actes get(MinecraftServer server) {
        return server.overworld().getDataStorage().computeIfAbsent(
                new SavedData.Factory<>(Actes::new, Actes::load, null), NAME);
    }

    void ajouter(UUID id, String cle, double points) {
        if (points <= 0) {
            return;
        }
        objets.computeIfAbsent(id, k -> new HashMap<>()).merge(cle, points, Double::sum);
        setDirty();
    }

    /**
     * Combien de fois l'acte a eu lieu (une créature vaincue compte une, quel que soit son poids) : pour le récit de la table,
     * gardé à part des points ({@link #FOIS} devant la clé), et jamais rabattu.
     */
    void compter(UUID id, String cle, double fois) {
        if (fois > 0) {
            objets.computeIfAbsent(id, k -> new HashMap<>()).merge(FOIS + cle, fois, Double::sum);
            setDirty();
        }
    }

    double fois(UUID id, String cle) {
        return points(id, FOIS + cle);
    }

    double points(UUID id, String cle) {
        Map<String, Double> map = objets.get(id);
        return map == null ? 0.0 : map.getOrDefault(cle, 0.0);
    }

    double total(UUID id) {
        Map<String, Double> map = objets.get(id);
        return map == null ? 0.0 : map.entrySet().stream().filter(e -> !e.getKey().startsWith(FOIS) && !e.getKey().startsWith(NOTE))
                .mapToDouble(Map.Entry::getValue).sum();
    }

    /** Une note de l'objet ({@link Lien}), 0 si elle n'a jamais été posée. */
    double note(UUID id, String cle) {
        return points(id, NOTE + cle);
    }

    void noter(UUID id, String cle, double valeur) {
        objets.computeIfAbsent(id, k -> new HashMap<>()).put(NOTE + cle, valeur);
        setDirty();
    }

    /** L'objet a disparu (cassé, brûlé, disparu au sol) : ses actes avec lui. */
    void oublier(UUID id) {
        if (objets.remove(id) != null) {
            setDirty();
        }
    }

    /** Une copie de l'objet (mode créatif) reçoit un nouvel identifiant et emporte ce que l'original avait vécu. */
    void copier(UUID depuis, UUID vers) {
        Map<String, Double> map = objets.get(depuis);
        if (map != null) {
            objets.put(vers, new HashMap<>(map));
            setDirty();
        }
    }

    /** Ramène le total de l'objet à {@code cible} (jamais plus haut), chaque acte gardant sa part. */
    void rabattre(UUID id, double cible) {
        Map<String, Double> map = objets.get(id);
        if (map == null) {
            return;
        }
        double total = total(id);
        if (total <= cible) {
            return;
        }
        double ratio = total <= 0 ? 0 : Math.max(0, cible) / total;
        map.replaceAll((k, v) -> k.startsWith(FOIS) || k.startsWith(NOTE) ? v : v * ratio);
        setDirty();
    }

    private static Actes load(CompoundTag tag, HolderLookup.Provider registries) {
        Actes actes = new Actes();
        CompoundTag all = tag.getCompound("objets");
        for (String key : all.getAllKeys()) {
            try {
                UUID id = UUID.fromString(key);
                CompoundTag entries = all.getCompound(key);
                Map<String, Double> map = new HashMap<>();
                for (String cle : entries.getAllKeys()) {
                    map.put(cle, entries.getDouble(cle));
                }
                actes.objets.put(id, map);
            } catch (IllegalArgumentException ignored) {
                // une clé abîmée : on l'oublie
            }
        }
        return actes;
    }

    @Override
    public CompoundTag save(CompoundTag tag, HolderLookup.Provider registries) {
        CompoundTag all = new CompoundTag();
        objets.forEach((id, map) -> {
            CompoundTag entries = new CompoundTag();
            map.forEach(entries::putDouble);
            all.put(id.toString(), entries);
        });
        tag.put("objets", all);
        return tag;
    }
}
