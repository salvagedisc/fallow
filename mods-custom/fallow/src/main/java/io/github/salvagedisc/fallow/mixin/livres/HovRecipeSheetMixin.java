package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.HovRecipeBooks;

/**
 * Les pages des livres de recettes de Hands-On Vanilla : leurs flèches ne sont plus dessinées, et le coin corné l'est sur le papier
 * de chaque page qui a une page à tourner de son côté ({@link HovRecipeBooks#dogEar}).
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.RecipeBook$Sheet", remap = false)
abstract class HovRecipeSheetMixin {
    @Shadow(remap = false)
    @Final
    private PoseStack poseStack;
    @Shadow(remap = false)
    @Final
    private MultiBufferSource buffers;

    @Inject(method = "arrow", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$sansFleche(float x, float y, boolean next, boolean over, CallbackInfo callback) {
        if (HovRecipeBooks.dogEarsReady(this)) {
            callback.cancel();
        }
    }

    @Inject(method = "paper", at = @At("TAIL"), remap = false, require = 0)
    private void fallow$coinCorne(boolean left, CallbackInfo callback) {
        HovRecipeBooks.dogEar(this, poseStack, buffers, left);
    }
}
