package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.DogEar;

/**
 * Le livre du pupitre de Hands-On Vanilla se tourne par le coin corné de ses pages, comme nos livres : là où le pupitre dessine sa
 * flèche (une page suit ou précède), le coin de la page est corné, au pied du côté extérieur, et se soulève sous le curseur. La
 * zone qui tourne la page reste celle du pupitre : le tiers extérieur du pied de la page, qui contient le coin.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.OpenBook$Canvas", remap = false)
abstract class HovLecternCanvasMixin {
    /** La couleur de la flèche sous le curseur, chez le pupitre. */
    @Unique
    private static final int FALLOW$LIT = 0xFF8A5A2B;
    /** L'encre des livres. */
    @Unique
    private static final int FALLOW$INK = 0x3B2A1A;
    /** Le coin, en unités de la page du pupitre (155 x 242). */
    @Unique
    private static final float FALLOW$SIZE = 26.0F;

    @Shadow(remap = false)
    @Final
    private PoseStack poseStack;
    @Shadow(remap = false)
    @Final
    private MultiBufferSource buffers;
    @Shadow(remap = false)
    @Final
    private int light;
    @Shadow(remap = false)
    @Final
    private int wide;
    @Shadow(remap = false)
    @Final
    private int high;

    /**
     * What Hands-On Vanilla writes in black on its books' pages (the lectern's text) is written in the books' brown ink, as
     * every page of the pack is: black never sits on this paper.
     */
    @ModifyVariable(method = "text(Lnet/minecraft/util/FormattedCharSequence;FFIF)V", at = @At("HEAD"), argsOnly = true, index = 4,
            remap = false, require = 0)
    private int fallow$encre(int colour) {
        return (colour & 0xFFFFFF) == 0 ? (colour & 0xFF000000) | FALLOW$INK : colour;
    }

    @Inject(method = "arrow", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$coinCorne(float x, float y, boolean next, int colour, CallbackInfo callback) {
        DogEar.draw(poseStack, buffers, light, next ? wide : 0.0F, high, FALLOW$SIZE, !next, colour == FALLOW$LIT, DogEar.Paper.CREAM,
                DogEar.LIFT);
        callback.cancel();
    }
}
