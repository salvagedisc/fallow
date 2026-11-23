package io.github.salvagedisc.fallow.hotes.client;

import io.github.salvagedisc.fallow.hotes.Hote;
import io.github.salvagedisc.fallow.hotes.HotesModule;
import io.github.salvagedisc.fallow.hotes.Peuple;
import io.github.salvagedisc.fallow.hotes.visite.Echanges;
import net.minecraft.client.Minecraft;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.EntityRenderersEvent;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

/** Côté client des Hôtes : le rendu au modèle du joueur et la fenêtre d'échange. */
public final class HotesClient {
    private HotesClient() {
    }

    public static void start(IEventBus modBus) {
        modBus.addListener(EntityRenderersEvent.RegisterRenderers.class, event -> {
            event.registerEntityRenderer(HotesModule.HOTE.get(), HoteRenderer::new);
            event.registerBlockEntityRenderer(HotesModule.NATTE_BE.get(), NatteRenderer::new);
            event.registerBlockEntityRenderer(HotesModule.SIGNE_BE.get(), SigneRenderer::new);
        });
        modBus.addListener(RegisterClientExtensionsEvent.class, event -> {
            for (Peuple peuple : Peuple.values()) {
                event.registerItem(new MasqueClient(peuple), HotesModule.MASQUES.get(peuple).get());
            }
        });
        modBus.addListener(EntityRenderersEvent.RegisterLayerDefinitions.class, event -> {
            for (Peuple peuple : Peuple.values()) {
                event.registerLayerDefinition(MasqueModel.couche(peuple), () -> MasqueModel.definition(peuple));
                event.registerLayerDefinition(ParureLayer.couche(peuple), () -> ParureLayer.definition(peuple));
            }
        });
    }

    /** Le serveur ouvre la fenêtre d'échange, ou met à jour celle qui est ouverte avec ce même émissaire. */
    public static void echange(Echanges.Echange echange) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || !(mc.level.getEntity(echange.entite()) instanceof Hote hote)) {
            return;
        }
        if (mc.screen instanceof DialogueScreen ecran && ecran.hote() == hote) {
            ecran.maj(echange);
        } else if (echange.ouvrir()) {
            mc.setScreen(new DialogueScreen(hote, echange));
        }
    }
}
