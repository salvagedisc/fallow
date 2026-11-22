package io.github.salvagedisc.fallow.mixin.eveil;

import io.github.salvagedisc.fallow.eveil.client.Isolement;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.LevelRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** À la table d'éveil, quand le monde est presque parti, ni soleil, ni lune, ni étoiles : le ciel est caché comme sous la cécité. */
@Mixin(LevelRenderer.class)
abstract class CielDeTableMixin {
    @Inject(method = "doesMobEffectBlockSky", at = @At("RETURN"), cancellable = true)
    private void fallow$cielDeTable(Camera camera, CallbackInfoReturnable<Boolean> callback) {
        if (Isolement.cielCache()) {
            callback.setReturnValue(true);
        }
    }
}
