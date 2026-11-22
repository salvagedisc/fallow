package io.github.salvagedisc.fallow.mixin.eveil;

import io.github.salvagedisc.fallow.eveil.client.Isolement;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * À la table d'éveil, la vue s'ouvre quand on s'assoit puis se resserre pendant le rite ({@link Isolement#champ}). Priorité plus haute
 * que celle de Diegesis, qui pose le champ du poste au même point : on part du sien. Suit le réglage des effets de champ de vision.
 */
@Mixin(value = GameRenderer.class, priority = 1100)
abstract class VueDeTableMixin {
    @Inject(method = "getFov", at = @At("RETURN"), cancellable = true)
    private void fallow$vueDeTable(Camera camera, float partialTick, boolean useFovSetting, CallbackInfoReturnable<Double> callback) {
        if (useFovSetting) {
            double champ = Isolement.champ();
            if (champ != 1.0) {
                callback.setReturnValue(callback.getReturnValue() * champ);
            }
        }
    }
}
