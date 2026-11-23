package io.github.salvagedisc.fallow.mixin.fiches;

import io.github.salvagedisc.fallow.fiches.Fiches;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Les fenêtres en fiche : la caisse de l'inventaire sous le fond du mod, les titres à l'encre, le temps de leur dessin
 * (pour leurs boutons vanilla) ({@link Fiches}).
 */
@Mixin(AbstractContainerScreen.class)
public abstract class ContainerScreenFicheMixin {
    @Shadow
    protected int leftPos;
    @Shadow
    protected int topPos;
    @Shadow
    protected int imageWidth;
    @Shadow
    protected int imageHeight;
    @Shadow
    @Final
    protected Component playerInventoryTitle;

    @Inject(method = "renderBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderBg(Lnet/minecraft/client/gui/GuiGraphics;FII)V"))
    private void fallow$caisse(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.Caisse c = Fiches.de((AbstractContainerScreen<?>) (Object) this);
        if (c != null) {
            Fiches.caisse(g, c, leftPos, topPos);
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void fallow$titresDebut(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.Caisse c = Fiches.de((AbstractContainerScreen<?>) (Object) this);
        if (c != null && (c.fiche() || !c.titre())) {
            Fiches.titres(true, c.fiche() ? playerInventoryTitle : null,
                    c.titre() ? null : ((AbstractContainerScreen<?>) (Object) this).getTitle());
        }
    }

    @Inject(method = "render", at = @At(value = "INVOKE", shift = At.Shift.AFTER,
            target = "Lnet/minecraft/client/gui/screens/inventory/AbstractContainerScreen;renderLabels(Lnet/minecraft/client/gui/GuiGraphics;II)V"))
    private void fallow$titresFin(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.titres(false, null, null);
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void fallow$feuilleDebut(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.Caisse c = Fiches.de((AbstractContainerScreen<?>) (Object) this);
        Fiches.feuille(c != null && c.fiche(), leftPos, topPos, imageWidth, imageHeight);
    }

    @Inject(method = "render", at = @At("RETURN"))
    private void fallow$feuilleFin(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.feuille(false, 0, 0, 0, 0);
    }
}
