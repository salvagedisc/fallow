package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.salvagedisc.fallow.livres.client.HovRecipeBooks;

/**
 * Les livres de recettes de Hands-On Vanilla (établi, tailleur de pierre, métier à tisser…) tournent leurs feuilles courbées, ce
 * qui y est écrit avec elles, comme le livre des postes : leurs pages ouvertes sont dessinées par {@link HovRecipeBooks} pendant
 * qu'une feuille passe ; sinon, rien ne change. Ses pages se tournent par leur coin corné au pied du côté extérieur, là où une
 * page suit ou précède ({@link HovRecipeBooks#footSide}), au lieu des flèches sous la liste ; le coin est dessiné à la fin de
 * chaque page, quand on sait si ce qu'elle montre l'atteint ({@link HovRecipeBooks#pageDrawn}).
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.RecipeBook", remap = false)
abstract class HovRecipeBookMixin {
    /**
     * How wide a recipe's name may run before it wraps (Hands-On Vanilla's 61): centred on the right page, it keeps clear of the
     * ribbon and as far from the outer edge as from the fold.
     */
    private static final float NAME_WIDE = 54.0F;

    @Shadow(remap = false)
    private float flip;
    @Shadow(remap = false)
    private int flipWay;
    @Shadow(remap = false)
    private float cursorX;
    @Shadow(remap = false)
    private float cursorY;
    @Shadow(remap = false)
    private int page;

    @Shadow(remap = false)
    private int pages() {
        throw new AssertionError();
    }

    @Inject(method = "spread", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$feuilleCourbee(PoseStack poseStack, MultiBufferSource buffers, float tilt, float height, CallbackInfo callback) {
        if (flip < 1.0F && HovRecipeBooks.spread(this, poseStack, buffers, tilt, height, flip, flipWay > 0)) {
            callback.cancel();
        }
    }

    @Inject(method = "footSide", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$coinCorne(CallbackInfoReturnable<Integer> callback) {
        if (HovRecipeBooks.cornersOn()) {
            callback.setReturnValue(HovRecipeBooks.footSide(this, cursorX, cursorY, page, pages()));
        }
    }

    /** Le coin corné de la page qui vient d'être dessinée, une fois tout ce qu'elle montre posé (on sait alors s'il touche le coin). */
    @Inject(method = "side", at = @At(value = "INVOKE", target = "Lcom/mojang/blaze3d/vertex/PoseStack;popPose()V"), remap = false,
            require = 0)
    private void fallow$coinDeLaPage(PoseStack poseStack, MultiBufferSource buffers, boolean left, float turn, float height,
            @Coerce Object leaf, boolean resting, CallbackInfo callback) {
        HovRecipeBooks.pageDrawn(poseStack, buffers, resting);
    }

    @ModifyConstant(method = "nameLines", constant = @Constant(floatValue = 61.0F), remap = false, require = 0)
    private float fallow$largeurDuNom(float wide) {
        return NAME_WIDE;
    }
}
