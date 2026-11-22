package io.github.salvagedisc.fallow.feuillage;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;

/**
 * Module feuillage : tomber dans un feuillage amortit la chute de moitié, pas plus (décision 0052). Passable Foliage
 * laisse traverser les feuilles mais y efface presque toute chute ; le pack lui fait faire zéro dégât
 * ({@code fallDamageMultiplier: 1.0}) et ce module rend, au premier contact avec les feuilles, la moitié des dégâts
 * qu'aurait faits la chute jusque-là (mixin {@code feuillage.ChuteDansLesFeuillesMixin}). Sous le feuillage, la chute
 * repart d'un bloc jusqu'au sol, comme le veut Passable Foliage.
 */
public final class FeuillageModule {
    /** Part des dégâts normaux qu'une chute garde en entrant dans un feuillage. */
    static final float AMORTI = 0.5F;
    /** Passable Foliage ramène la chute à 1 bloc au plus dans ses feuilles : au-delà, c'est le premier contact. */
    private static final float DEJA_DEDANS = 1.0F;

    private FeuillageModule() {
    }

    public static void start(IEventBus modBus) {
        Fallow.LOGGER.info("feuillage : une chute dans les feuilles garde la moitié de ses dégâts");
    }

    /** Appelé à chaque contact d'une entité avec des feuilles traversables, avant que Passable Foliage efface sa chute. */
    public static void entre(Level level, Entity entity) {
        if (level.isClientSide() || !(entity instanceof LivingEntity living) || entity.fallDistance <= DEJA_DEDANS
                || !FallowConfig.feuillageEnabled()) {
            return;
        }
        // La chute de l'entité elle-même, à moitié : les créatures qui ne craignent pas les chutes (poule, monture qui
        // compte à sa façon, tag fall_damage_immune) restent comme le jeu les veut ; Chute amortie et Protection jouent.
        living.causeFallDamage(entity.fallDistance, AMORTI, entity.damageSources().fall());
    }
}
