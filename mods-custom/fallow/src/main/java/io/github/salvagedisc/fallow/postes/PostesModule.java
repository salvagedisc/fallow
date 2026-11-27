package io.github.salvagedisc.fallow.postes;

import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * Le module postes : les blocs de Fallow travaillés à la main sur place, sur le modèle de Hands-On Delight et Hands-On Vanilla
 * (bibliothèque Diegesis). Les postes sont enregistrés des deux côtés ; leur allure (scène, livre) seulement sur le client.
 */
public final class PostesModule {
    private PostesModule() {
    }

    public static void start(IEventBus modBus) {
        PostesTannerie.register();
        if (FMLEnvironment.dist.isClient()) {
            io.github.salvagedisc.fallow.postes.client.PostesClient.init(modBus);
        }
    }
}
