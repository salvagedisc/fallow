package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.OpenSheets;

/**
 * Le livre du pupitre de Hands-On Vanilla tourne ses feuilles courbées, ce qui y est écrit avec elles, comme le livre d'éveil de la
 * table : la feuille qui passe ({@code sheet} à plat, à la hauteur des feuilles qu'on tourne) est dessinée par {@link OpenSheets}
 * avec les images de ses deux faces, peintes par le livre lui-même. Les feuilles d'un feuilletage restent à plat.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.OpenBook", remap = false)
abstract class HovOpenBookMixin {
    /** La hauteur de la feuille qu'on tourne au-dessus des autres, comme le livre la pose. */
    @Unique
    private static final float FALLOW$SHEET_LIFT = 0.06F;

    @Shadow(remap = false)
    private int spread;
    @Shadow(remap = false)
    private int fromSpread;
    @Shadow(remap = false)
    private float flip;
    @Shadow(remap = false)
    private int flipWay;
    @Shadow(remap = false)
    private float time;
    @Shadow(remap = false)
    private float riffleAt;
    @Shadow(remap = false)
    private float riffleSpan;

    @Unique
    private float fallow$right;
    @Unique
    private float fallow$left;
    @Unique
    private boolean fallow$peint;

    @Shadow(remap = false)
    private void sheet(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float angle, float lift, boolean curved,
            int page, boolean resting, float ink, float alpha) {
        throw new AssertionError();
    }

    @Inject(method = "draw", at = @At("HEAD"), remap = false, require = 0)
    private void fallow$angles(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float right, float left, float ink,
            float own, CallbackInfo callback) {
        fallow$right = right;
        fallow$left = left;
    }

    @Inject(method = "sheet", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$feuilleCourbee(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float angle, float lift,
            boolean curved, int page, boolean resting, float ink, float alpha, CallbackInfo callback) {
        if (fallow$peint || curved || flip >= 1.0F || Math.abs(lift - FALLOW$SHEET_LIFT) > 1.0E-4F || time - riffleAt < riffleSpan) {
            return;
        }
        boolean forward = flipWay > 0;
        int left0 = 2 * spread;
        int from0 = 2 * fromSpread;
        int front = forward ? from0 + 1 : left0 + 1;
        int back = forward ? left0 : from0;
        float over = flip * flip * (3.0F - 2.0F * flip);
        float from = forward ? fallow$right : fallow$left;
        float to = forward ? fallow$left : fallow$right;
        boolean bent = OpenSheets.turn(poseStack, buffers, light, open, lift, from, to, over, this, front, back,
                (pose, drawn, shown, onFront) -> {
                    fallow$peint = true;
                    try {
                        sheet(pose, drawn, LightTexture.FULL_BRIGHT, 0.0F, onFront ? OpenSheets.FLAT : -OpenSheets.FLAT, 0.0F, false, shown,
                                false, ink, 1.0F);
                    } finally {
                        fallow$peint = false;
                    }
                }, alpha);
        if (bent) {
            callback.cancel();
        }
    }
}
