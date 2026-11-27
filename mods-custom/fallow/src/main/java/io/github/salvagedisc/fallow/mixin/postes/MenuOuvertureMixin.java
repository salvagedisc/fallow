package io.github.salvagedisc.fallow.mixin.postes;

import net.neoforged.neoforge.network.handlers.ClientPayloadHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.postes.client.MenusAvecDonnees;

/** L'ouverture d'un menu de poste ouvert avec des données passe par le paquet vanilla, que Diegesis écoute ({@link MenusAvecDonnees}). */
@Mixin(value = ClientPayloadHandler.class, remap = false)
abstract class MenuOuvertureMixin {
    @Inject(method = "handle(Lnet/neoforged/neoforge/network/payload/AdvancedOpenScreenPayload;Lnet/neoforged/neoforge/network/handling/IPayloadContext;)V",
            at = @At("HEAD"), cancellable = true)
    private static void fallow$parLePaquetVanilla(AdvancedOpenScreenPayload payload, IPayloadContext context, CallbackInfo callback) {
        if (MenusAvecDonnees.rejouer(payload, context.listener().getConnectionType())) {
            callback.cancel();
        }
    }
}
