package io.github.salvagedisc.fallow.mixin.eveil;

import com.mojang.blaze3d.shaders.FogShape;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.salvagedisc.fallow.eveil.client.Isolement;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.FogRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * À la table d'éveil, la brume avale le monde ({@link Isolement}). Appliqué en fin de {@code setupFog} et de {@code setupColor},
 * après ATMOSPHERICS (priorité plus haute que la sienne) mais avant la relève de {@code horizon.FogRendererMixin} (1500) : les LOD
 * de Distant Horizons voient une brume courte et ne se dessinent pas.
 */
@Mixin(value = FogRenderer.class, priority = 1400)
abstract class BrumeDeTableMixin {
    @Shadow
    private static float fogRed;
    @Shadow
    private static float fogGreen;
    @Shadow
    private static float fogBlue;

    @Inject(method = "setupFog", at = @At("TAIL"))
    private static void fallow$brumeDeTable(Camera camera, FogRenderer.FogMode fogMode, float farPlaneDistance, boolean shouldCreateFog,
            float partialTick, CallbackInfo callback) {
        if (!Isolement.actif()) {
            return;
        }
        float fin = RenderSystem.getShaderFogEnd();
        if (fogMode == FogRenderer.FogMode.FOG_SKY) {
            RenderSystem.setShaderFogStart(Math.min(RenderSystem.getShaderFogStart(), 0.0F));
            RenderSystem.setShaderFogEnd(Math.max(0.5F, Isolement.finDuCiel(fin)));
        } else {
            RenderSystem.setShaderFogStart(Isolement.debut(RenderSystem.getShaderFogStart()));
            RenderSystem.setShaderFogEnd(Isolement.fin(fin));
            RenderSystem.setShaderFogShape(FogShape.SPHERE);
        }
    }

    @Inject(method = "setupColor", at = @At("TAIL"))
    private static void fallow$couleurDeTable(Camera camera, float partialTick, ClientLevel level, int renderDistanceChunks,
            float bossColorModifier, CallbackInfo callback) {
        if (!Isolement.actif()) {
            return;
        }
        fogRed = Isolement.rouge(fogRed);
        fogGreen = Isolement.vert(fogGreen);
        fogBlue = Isolement.bleu(fogBlue);
        RenderSystem.clearColor(fogRed, fogGreen, fogBlue, 0.0F);
    }
}
