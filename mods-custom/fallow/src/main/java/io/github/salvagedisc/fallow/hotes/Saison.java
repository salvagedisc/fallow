package io.github.salvagedisc.fallow.hotes;

import io.github.salvagedisc.fallow.ModIds;
import net.minecraft.server.level.ServerLevel;

/** La saison d'Ecliptic Seasons, vue par les Hôtes ; {@link #AUCUNE} sans Ecliptic ou hors des dimensions à saisons. */
public enum Saison {
    PRINTEMPS("printemps"), ETE("ete"), AUTOMNE("automne"), HIVER("hiver"), AUCUNE("aucune");

    private final String id;

    Saison(String id) {
        this.id = id;
    }

    /** Le nom de la saison dans les fichiers de demandes ({@code data/<espace>/hotes/<peuple>.json}). */
    public String id() {
        return id;
    }

    public static Saison de(ServerLevel level) {
        if (!ModIds.isLoaded(ModIds.ECLIPTIC_SEASONS)) {
            return AUCUNE;
        }
        return SaisonEcliptic.saison(level);
    }
}
