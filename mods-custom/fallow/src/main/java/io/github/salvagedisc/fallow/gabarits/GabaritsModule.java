package io.github.salvagedisc.fallow.gabarits;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.gabarits.client.GabaritsPack;
import net.neoforged.bus.api.IEventBus;

/**
 * Module gabarits : quand un pack de textures redessine le fer du jeu (épée, pioche, lingot, armure...), les objets en
 * métal des mods qui copiaient le fer du jeu reprennent ce nouveau dessin, chacun dans sa propre couleur. Rien n'est
 * enregistré dans le pack : les images sont faites au chargement des ressources, à partir des packs actifs, et servies
 * par un pack intégré caché, posé tout en haut. La liste des textures concernées est dans
 * {@code assets/fallow/gabarits/regles.json}. Client seulement.
 */
public final class GabaritsModule {
    private GabaritsModule() {
    }

    public static void start(IEventBus modBus) {
        modBus.addListener(GabaritsPack::register);
        Fallow.LOGGER.info("gabarits : les métaux des mods suivront le pack de textures actif (selon config/fallow-client.toml)");
    }
}
