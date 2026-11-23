package io.github.salvagedisc.fallow.fiches;

import io.github.salvagedisc.fallow.Fallow;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterClientReloadListenersEvent;

/**
 * Module fiches : les fenêtres de poste des mods (fours et tables d'Overgeared, Cold Sweat, la marmite, Quark,
 * Supplementaries, Exposure, Envelope...) en fiche de papier des livres posée sur la caisse en bois de l'inventaire. La
 * fiche est la texture du mod, redessinée par {@code scripts/fiches/fiches.py} dans les ressources du pack ; la caisse est
 * dessinée ici, avant le fond de la fenêtre, avec les bandes de la fenêtre du coffre vanilla, donc dans le bois du pack de
 * ressources actif. Client seulement ({@link Fiches}).
 *
 * <p>Sans ce module, les textures redessinées laissent l'inventaire sans fond : le module n'a pas d'interrupteur, il va
 * avec elles.
 */
public final class FichesModule {
    private FichesModule() {
    }

    public static void start(IEventBus modBus) {
        modBus.addListener(RegisterClientReloadListenersEvent.class,
                event -> event.registerReloadListener((ResourceManagerReloadListener) Fiches::reload));
        Fallow.LOGGER.info("fiches : les fenêtres des postes des mods sur la caisse de l'inventaire");
    }
}
