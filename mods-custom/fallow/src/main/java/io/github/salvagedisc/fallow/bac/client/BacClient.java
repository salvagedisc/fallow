package io.github.salvagedisc.fallow.bac.client;

import io.github.salvagedisc.fallow.bac.BacModule;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;

/** Le côté client du module bac : la corde dessinée entre les pieux. */
public final class BacClient {
    private BacClient() {
    }

    public static void start(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class,
                event -> event.registerBlockEntityRenderer(BacModule.PIEU_BE.get(), CordeRenderer::new));
    }
}
