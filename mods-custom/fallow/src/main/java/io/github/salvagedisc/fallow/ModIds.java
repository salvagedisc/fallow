package io.github.salvagedisc.fallow;

import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.LoadingModList;

/** Identifiants des mods que Fallow relie, et leur détection. */
public final class ModIds {
    public static final String ATMOSPHERICS = "atmospherics";
    public static final String ENHANCED_CELESTIALS = "enhancedcelestials2core";
    public static final String DISTANT_HORIZONS = "distanthorizons";
    public static final String CHUNKY = "chunky";
    public static final String CREATE = "create";
    public static final String PUNCHY = "punchy";
    public static final String SENSIBLE_STACKABLES = "sensible_stackables";
    public static final String SODIUM = "sodium";
    public static final String TRUE_ADAPTIVE_MUSIC = "trueadaptivemusic";
    public static final String STREAMS_REFLOWING = "streamsreflowing";
    public static final String SPELUNKERY = "spelunkery";
    public static final String LITHOSTITCHED = "lithostitched";
    public static final String SABLE = "sable";
    public static final String IRIS = "iris";
    public static final String ARCHIES_DINOSAURS = "ec_ad";
    public static final String EXPOSURE = "exposure";
    public static final String COLD_SWEAT = "cold_sweat";
    public static final String ECLIPTIC_SEASONS = "eclipticseasons";
    public static final String THIRST = "thirst";
    public static final String DIEGESIS = "diegesis";
    public static final String OVERGEARED = "overgeared";
    public static final String HANDS_ON_VANILLA = "handson_vanilla";
    public static final String FISH_N_SHIPS = "fish_n_ships";

    private ModIds() {
    }

    /**
     * Vrai si le mod est présent. Marche aussi avant la construction des mods (plugin de mixins),
     * où {@link ModList} n'existe pas encore : on lit alors la liste de chargement de FML.
     */
    public static boolean isLoaded(String modId) {
        ModList modList = ModList.get();
        if (modList != null) {
            return modList.isLoaded(modId);
        }
        LoadingModList loading = LoadingModList.get();
        return loading != null && loading.getModFileById(modId) != null;
    }
}
