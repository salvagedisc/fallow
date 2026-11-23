package io.github.salvagedisc.fallow.mixin.fiches;

import io.github.salvagedisc.fallow.fiches.Fiches;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.EditBox;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Le dessin d'un bouton ou d'un champ de texte vanilla : dans une fenêtre en fiche, une carte de papier ({@link Fiches}). */
@Mixin({AbstractButton.class, EditBox.class})
public abstract class WidgetFicheMixin {
    @Inject(method = "renderWidget", at = @At("HEAD"))
    private void fallow$carteDebut(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        AbstractWidget w = (AbstractWidget) (Object) this;
        Fiches.widget(true, w.getX(), w.getY(), w.getWidth(), w.getHeight());
    }

    @Inject(method = "renderWidget", at = @At("RETURN"))
    private void fallow$carteFin(GuiGraphics g, int mouseX, int mouseY, float partialTick, CallbackInfo ci) {
        Fiches.widget(false, 0, 0, 0, 0);
    }
}
