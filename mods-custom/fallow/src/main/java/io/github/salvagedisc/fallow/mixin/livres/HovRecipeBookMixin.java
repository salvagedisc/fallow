package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.HovRecipeBooks;

/**
 * Les livres de recettes de Hands-On Vanilla (établi, tailleur de pierre, métier à tisser…) tournent leurs feuilles courbées, ce
 * qui y est écrit avec elles, comme le livre des postes : leurs pages ouvertes sont dessinées par {@link HovRecipeBooks} pendant
 * qu'une feuille passe ; sinon, rien ne change.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.RecipeBook", remap = false)
abstract class HovRecipeBookMixin {
    @Shadow(remap = false)
    private float flip;
    @Shadow(remap = false)
    private int flipWay;

    @Inject(method = "spread", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$feuilleCourbee(PoseStack poseStack, MultiBufferSource buffers, float tilt, float height, CallbackInfo callback) {
        if (flip < 1.0F && HovRecipeBooks.spread(this, poseStack, buffers, tilt, height, flip, flipWay > 0)) {
            callback.cancel();
        }
    }
}
