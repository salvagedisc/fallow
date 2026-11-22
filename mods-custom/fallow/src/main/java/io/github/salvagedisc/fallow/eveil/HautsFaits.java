package io.github.salvagedisc.fallow.eveil;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.salvagedisc.fallow.Fallow;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.tags.TagKey;
import net.minecraft.util.GsonHelper;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Les hauts faits : quels actes comptent pour quels objets, et vers quels enchantements ils penchent. Lus dans
 * {@code data/<espace>/eveil/*.json}, un fichier par famille d'objets :
 *
 * <pre>{@code
 * {
 *   "objets": "#minecraft:pickaxes",
 *   "actes": [
 *     {"acte": "casser", "cible": "#c:ores", "points": 3, "enchantements": {"minecraft:fortune": 3, "minecraft:efficiency": 1}}
 *   ]
 * }
 * }</pre>
 *
 * {@code objets} et {@code cible} prennent un identifiant ou un tag ({@code #}). Les actes : {@code casser} (cible : bloc),
 * {@code vaincre} (cible : créature ; un point par 20 points de vie de la bête, plafonné à 10), {@code encaisser}
 * (cible : type de dégâts ; un point par point de dégât encaissé, sur chaque pièce d'armure portée), {@code parcourir}
 * (cible : {@code marche} ou {@code nage} ; un point par bloc), {@code sous_l_eau} (un point par seconde),
 * {@code bloquer} (un point par point de dégât arrêté au bouclier), {@code pecher} (un point par prise),
 * {@code reparer} (un point par point de durabilité regagné). Sans cible, l'acte compte toujours.
 *
 * <p>{@code si} (facultatif, une condition ou une liste, toutes requises, {@code !} pour la négation) précise le moment :
 * {@code nether}, {@code end}, {@code surface} (la dimension du joueur), {@code accroupi}, {@code pluie} (il pleut sur
 * le joueur), {@code dans_l_eau} (eau ou pluie), {@code cible_en_feu}, {@code armure} (la créature porte de l'armure),
 * {@code lance} (le coup vient d'un projectile), {@code loin} (la créature est à plus de 20 blocs), {@code ecrase} (le
 * joueur tombait en frappant), {@code sous:<bloc ou #tag>} (le bloc sous les pieds). {@code objets} prend aussi une liste.
 * {@code nom} (facultatif) est la clé de traduction qui dit l'acte au livre de la table, avec le nombre en {@code %s}
 * (« 1 240 minerais extraits »).
 * Une entrée garde sa place dans le fichier : c'est sa clé dans {@link Actes}.
 */
public final class HautsFaits extends SimpleJsonResourceReloadListener {
    public enum Type {
        CASSER, VAINCRE, ENCAISSER, PARCOURIR, SOUS_L_EAU, BLOQUER, PECHER, REPARER;

        static Type of(String name) {
            return valueOf(name.toUpperCase(Locale.ROOT));
        }
    }

    /** Le moment d'un acte : le joueur, et selon l'acte la créature, la source des dégâts, le bloc sous les pieds. */
    public record Contexte(ServerPlayer joueur, LivingEntity cible, DamageSource source, BlockState sous) {
        static Contexte de(ServerPlayer joueur) {
            return new Contexte(joueur, null, null, null);
        }
    }

    /**
     * Une entrée : son acte, sa cible (identifiant ou tag, vide pour toutes), ses conditions, ses points par unité et
     * ses penchants.
     */
    public record Entree(String cle, Type type, String cible, List<String> si, double points,
            Map<ResourceLocation, Double> enchantements, String nom) {
        /** Toutes les conditions de l'entrée tiennent à ce moment. */
        boolean si(Contexte c) {
            for (String condition : si) {
                boolean non = condition.startsWith("!");
                if (vraie(non ? condition.substring(1) : condition, c) == non) {
                    return false;
                }
            }
            return true;
        }

        private static boolean vraie(String condition, Contexte c) {
            ServerPlayer joueur = c.joueur();
            LivingEntity cible = c.cible();
            if (condition.startsWith("sous:")) {
                return c.sous() != null && bloc(condition.substring(5), c.sous());
            }
            return switch (condition) {
                case "nether" -> joueur.level().dimension() == Level.NETHER;
                case "end" -> joueur.level().dimension() == Level.END;
                case "surface" -> joueur.level().dimension() == Level.OVERWORLD;
                case "accroupi" -> joueur.isCrouching();
                case "pluie" -> joueur.level().isRainingAt(joueur.blockPosition())
                        || cible != null && cible.level().isRainingAt(cible.blockPosition());
                case "dans_l_eau" -> joueur.isInWaterRainOrBubble();
                case "cible_en_feu" -> cible != null && cible.isOnFire();
                case "armure" -> cible != null && cible.getArmorValue() > 0;
                case "lance" -> c.source() != null && c.source().getDirectEntity() instanceof Projectile;
                case "loin" -> cible != null && cible.distanceToSqr(joueur) > LOIN * LOIN;
                case "ecrase" -> c.source() != null && c.source().getDirectEntity() == joueur
                        && joueur.fallDistance > 1.5F && !joueur.isFallFlying();
                // Les terres des armures d'Archie : la chaleur, les hauteurs, les profondeurs, le sec, l'ombre, les bêtes.
                case "pres_de_lave" -> joueur.level().getBlockStates(joueur.getBoundingBox().inflate(3.0))
                        .anyMatch(state -> state.getFluidState().is(net.minecraft.tags.FluidTags.LAVA));
                case "haut" -> joueur.getY() >= HAUT;
                case "profond" -> joueur.isUnderWater() && joueur.getY() <= PROFOND;
                case "sec" -> !joueur.level().getBiome(joueur.blockPosition()).value().hasPrecipitation();
                case "obscur" -> joueur.level().getMaxLocalRawBrightness(joueur.blockPosition()) <= 4;
                case "pres_des_betes" -> !joueur.level().getEntitiesOfClass(net.minecraft.world.entity.animal.Animal.class,
                        joueur.getBoundingBox().inflate(8.0)).isEmpty();
                default -> false;
            };
        }

        private static boolean bloc(String cible, BlockState state) {
            if (cible.startsWith("#")) {
                return state.is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(cible.substring(1))));
            }
            return state.is(BuiltInRegistries.BLOCK.get(ResourceLocation.parse(cible)));
        }

        boolean bloc(BlockState state) {
            return cible.isEmpty() || bloc(cible, state);
        }

        boolean creature(EntityType<?> type) {
            if (cible.isEmpty()) {
                return true;
            }
            if (cible.startsWith("#")) {
                return type.is(TagKey.create(Registries.ENTITY_TYPE, ResourceLocation.parse(cible.substring(1))));
            }
            return BuiltInRegistries.ENTITY_TYPE.getKey(type).equals(ResourceLocation.parse(cible));
        }

        boolean degats(DamageSource source) {
            if (cible.isEmpty()) {
                return true;
            }
            if (cible.startsWith("#")) {
                return source.is(TagKey.create(Registries.DAMAGE_TYPE, ResourceLocation.parse(cible.substring(1))));
            }
            return source.typeHolder().is(ResourceLocation.parse(cible));
        }

        boolean mode(String value) {
            return cible.isEmpty() || cible.equals(value);
        }
    }

    record Famille(List<String> objets, List<Entree> entrees) {
        boolean pour(ItemStack stack) {
            for (String objet : objets) {
                if (objet.startsWith("#") ? stack.is(TagKey.create(Registries.ITEM, ResourceLocation.parse(objet.substring(1))))
                        : BuiltInRegistries.ITEM.getOptional(ResourceLocation.parse(objet)).map(stack::is).orElse(false)) {
                    return true;
                }
            }
            return false;
        }
    }

    /** Une chaîne ou une liste de chaînes. */
    private static List<String> liste(JsonObject json, String key) {
        if (!json.has(key)) {
            return List.of();
        }
        JsonElement element = json.get(key);
        if (element.isJsonArray()) {
            List<String> out = new ArrayList<>();
            element.getAsJsonArray().forEach(e -> out.add(e.getAsString()));
            return List.copyOf(out);
        }
        return List.of(element.getAsString());
    }

    /** Au-delà de cette distance, la créature a été vaincue de loin. */
    private static final double LOIN = 20.0;
    /** Les hauteurs (Chorusoatl) et les profondeurs (Coralosaur), en hauteur du monde. */
    private static final double HAUT = 120.0;
    private static final double PROFOND = 40.0;

    private static final Gson GSON = new Gson();
    private static volatile List<Famille> familles = List.of();

    HautsFaits() {
        super(GSON, "eveil");
    }

    /** Toutes les entrées qui valent pour cet objet. */
    static List<Entree> pour(ItemStack stack) {
        if (stack.isEmpty()) {
            return List.of();
        }
        List<Entree> out = new ArrayList<>();
        for (Famille famille : familles) {
            if (famille.pour(stack)) {
                out.addAll(famille.entrees());
            }
        }
        return out;
    }

    static int nombre() {
        return familles.size();
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> files, ResourceManager manager, ProfilerFiller profiler) {
        List<Famille> loaded = new ArrayList<>();
        files.forEach((id, json) -> {
            try {
                JsonObject root = GsonHelper.convertToJsonObject(json, "eveil");
                List<String> objets = liste(root, "objets");
                List<Entree> entrees = new ArrayList<>();
                int index = 0;
                for (JsonElement element : GsonHelper.getAsJsonArray(root, "actes")) {
                    JsonObject acte = GsonHelper.convertToJsonObject(element, "acte");
                    Map<ResourceLocation, Double> penchants = new LinkedHashMap<>();
                    GsonHelper.getAsJsonObject(acte, "enchantements").entrySet()
                            .forEach(e -> penchants.put(ResourceLocation.parse(e.getKey()), e.getValue().getAsDouble()));
                    entrees.add(new Entree(id + "#" + index++, Type.of(GsonHelper.getAsString(acte, "acte")),
                            GsonHelper.getAsString(acte, "cible", ""), liste(acte, "si"),
                            GsonHelper.getAsDouble(acte, "points", 1.0),
                            Map.copyOf(penchants), GsonHelper.getAsString(acte, "nom", "")));
                }
                loaded.add(new Famille(objets, List.copyOf(entrees)));
            } catch (RuntimeException e) {
                Fallow.LOGGER.error("eveil : fichier {} illisible", id, e);
            }
        });
        familles = List.copyOf(loaded);
        Fallow.LOGGER.info("eveil : {} familles d'objets", loaded.size());
    }
}
