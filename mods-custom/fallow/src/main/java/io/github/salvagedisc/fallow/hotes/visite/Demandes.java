package io.github.salvagedisc.fallow.hotes.visite;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.hotes.Peuple;
import io.github.salvagedisc.fallow.hotes.Saison;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * Ce que chaque peuple demande et rend, lu dans le pack de données :
 * <ul>
 *   <li>{@code data/<ns>/hotes/<peuple>.json} : {@code "demandes"} par saison ({@code "toujours"}, {@code "printemps"},
 *   {@code "ete"}, {@code "automne"}, {@code "hiver"}), des souhaits {@link Demande} ; {@code "contre_dons"} par palier
 *   ({@code "inconnus"}…{@code "familiers"}), des tirages {@code {"item", "min", "max", "poids"}}. Un palier sans liste
 *   reprend celle du palier d'en dessous.</li>
 * </ul>
 * Un objet d'un mod absent est ignoré, sans erreur.
 */
public final class Demandes extends SimpleJsonResourceReloadListener {
    private record Tirage(Item item, int min, int max, int poids) {
    }

    private static final Map<Peuple, Map<String, List<Demande>>> DEMANDES = new EnumMap<>(Peuple.class);
    private static final Map<Peuple, Map<Palier, List<Tirage>>> CONTRE_DONS = new EnumMap<>(Peuple.class);

    public Demandes() {
        super(new com.google.gson.Gson(), "hotes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> fichiers, ResourceManager manager, ProfilerFiller profiler) {
        DEMANDES.clear();
        CONTRE_DONS.clear();
        int souhaits = 0;
        for (Map.Entry<ResourceLocation, JsonElement> fichier : fichiers.entrySet()) {
            Peuple peuple = Peuple.parse(fichier.getKey().getPath()).orElse(null);
            if (peuple == null || !fichier.getValue().isJsonObject()) {
                continue;
            }
            JsonObject json = fichier.getValue().getAsJsonObject();
            Map<String, List<Demande>> parSaison = new HashMap<>();
            JsonObject demandes = GsonHelper.getAsJsonObject(json, "demandes", new JsonObject());
            for (String saison : demandes.keySet()) {
                List<Demande> liste = new ArrayList<>();
                for (JsonElement e : GsonHelper.getAsJsonArray(demandes, saison, new JsonArray())) {
                    try {
                        Demande demande = Demande.lire(e.getAsJsonObject());
                        if (demande != null) {
                            liste.add(demande);
                        }
                    } catch (RuntimeException erreur) {
                        // Un souhait mal écrit (pack de données d'un tiers) est sauté, pas tout le fichier.
                        Fallow.LOGGER.warn("hotes : souhait ignoré dans {} : {}", fichier.getKey(), erreur.getMessage());
                    }
                }
                souhaits += liste.size();
                parSaison.put(saison, liste);
            }
            DEMANDES.put(peuple, parSaison);
            Map<Palier, List<Tirage>> parPalier = new EnumMap<>(Palier.class);
            JsonObject dons = GsonHelper.getAsJsonObject(json, "contre_dons", new JsonObject());
            for (Palier palier : Palier.values()) {
                if (!dons.has(palier.id())) {
                    continue;
                }
                List<Tirage> liste = new ArrayList<>();
                JsonArray tirages = GsonHelper.getAsJsonArray(dons, palier.id(), new JsonArray());
                for (JsonElement e : tirages) {
                    try {
                        JsonObject t = e.getAsJsonObject();
                        ResourceLocation id = ResourceLocation.parse(GsonHelper.getAsString(t, "item"));
                        if (BuiltInRegistries.ITEM.containsKey(id)) {
                            int min = GsonHelper.getAsInt(t, "min", 1);
                            liste.add(new Tirage(BuiltInRegistries.ITEM.get(id), min, Math.max(min, GsonHelper.getAsInt(t, "max", min)),
                                    Math.max(1, GsonHelper.getAsInt(t, "poids", 1))));
                        }
                    } catch (RuntimeException erreur) {
                        Fallow.LOGGER.warn("hotes : contre-don ignoré dans {} : {}", fichier.getKey(), erreur.getMessage());
                    }
                }
                parPalier.put(palier, liste);
            }
            CONTRE_DONS.put(peuple, parPalier);
        }
        Fallow.LOGGER.info("hotes : {} souhaits lus pour {} peuples", souhaits, DEMANDES.size());
    }

    /**
     * Trois souhaits pour une visite, tirés parmi ceux de la saison et ceux de toujours, sans doublon. Les peuples proches
     * demandent un peu plus (une fois et demie aux Proches, deux fois aux Familiers).
     */
    public static List<Demande> tire(Peuple peuple, Saison saison, Palier palier, RandomSource hasard) {
        Map<String, List<Demande>> parSaison = DEMANDES.getOrDefault(peuple, Map.of());
        List<Demande> choix = new ArrayList<>(parSaison.getOrDefault(saison.id(), List.of()));
        // La saison passe d'abord : deux souhaits d'elle quand il y en a, le reste de toujours.
        List<Demande> toujours = new ArrayList<>(parSaison.getOrDefault("toujours", List.of()));
        List<Demande> tires = new ArrayList<>();
        melange(choix, hasard);
        melange(toujours, hasard);
        for (Demande d : choix) {
            if (tires.size() < 2) {
                tires.add(d);
            }
        }
        for (Demande d : toujours) {
            if (tires.size() < 3 && tires.stream().noneMatch(t -> t.icone() == d.icone())) {
                tires.add(d);
            }
        }
        float facteur = switch (palier) {
            case PROCHES -> 1.5F;
            case FAMILIERS -> 2.0F;
            default -> 1.0F;
        };
        tires.replaceAll(d -> d.fois(facteur));
        return tires;
    }

    /**
     * Le contre-don de l'aube : un tirage par souhait rempli, dans la liste du palier (ou d'un palier plus bas), sans ce
     * qu'on vient de donner.
     */
    public static List<ItemStack> contreDon(Peuple peuple, Palier palier, int dons, java.util.Set<Item> exclus, RandomSource hasard) {
        Map<Palier, List<Tirage>> parPalier = CONTRE_DONS.getOrDefault(peuple, Map.of());
        List<Tirage> liste = List.of();
        for (Palier p = palier; ; p = p.precedent()) {
            liste = parPalier.getOrDefault(p, List.of()).stream().filter(t -> !exclus.contains(t.item())).toList();
            if (!liste.isEmpty() || p == Palier.INCONNUS) {
                break;
            }
        }
        List<ItemStack> out = new ArrayList<>();
        int total = liste.stream().mapToInt(Tirage::poids).sum();
        for (int i = 0; i < dons && total > 0; i++) {
            int r = hasard.nextInt(total);
            for (Tirage t : liste) {
                r -= t.poids();
                if (r < 0) {
                    out.add(new ItemStack(t.item(), Mth.randomBetweenInclusive(hasard, t.min(), t.max())));
                    break;
                }
            }
        }
        if (out.isEmpty() && dons > 0) {
            out.add(new ItemStack(Items.BONE_MEAL, 4));
        }
        return out;
    }

    private static <T> void melange(List<T> liste, RandomSource hasard) {
        for (int i = liste.size() - 1; i > 0; i--) {
            int j = hasard.nextInt(i + 1);
            T t = liste.get(i);
            liste.set(i, liste.get(j));
            liste.set(j, t);
        }
    }
}
