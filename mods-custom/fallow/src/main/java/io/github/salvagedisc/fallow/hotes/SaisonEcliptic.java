package io.github.salvagedisc.fallow.hotes;

import com.teamtea.eclipticseasons.api.constant.solar.SolarTerm;
import com.teamtea.eclipticseasons.common.core.solar.SolarDataManager;
import com.teamtea.eclipticseasons.common.core.SolarHolders;
import net.minecraft.server.level.ServerLevel;

/** Lit la saison dans Ecliptic Seasons. Chargée seulement si Ecliptic Seasons est là. */
final class SaisonEcliptic {
    private SaisonEcliptic() {
    }

    static Saison saison(ServerLevel level) {
        SolarDataManager solar = SolarHolders.getSaveData(level);
        if (solar == null || !solar.isValidDimension()) {
            return Saison.AUCUNE;
        }
        SolarTerm term = solar.getSolarTerm();
        if (term == SolarTerm.NONE) {
            return Saison.AUCUNE;
        }
        return switch (term.getSeason()) {
            case SPRING -> Saison.PRINTEMPS;
            case SUMMER -> Saison.ETE;
            case AUTUMN -> Saison.AUTOMNE;
            case WINTER -> Saison.HIVER;
            default -> Saison.AUCUNE;
        };
    }
}
