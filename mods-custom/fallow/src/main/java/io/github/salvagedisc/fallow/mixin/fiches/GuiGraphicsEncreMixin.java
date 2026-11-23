package io.github.salvagedisc.fallow.mixin.fiches;

import io.github.salvagedisc.fallow.fiches.Fiches;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Les textes d'une fenêtre en fiche : le nom « Inventaire » tu, le gris des titres à l'encre des livres ; ses boutons et
 * champs de texte vanilla en cartes de papier, leur texte à l'encre sans ombre ({@link Fiches}).
 */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsEncreMixin {
    @Inject(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)I", at = @At("HEAD"),
            cancellable = true)
    private void fallow$taire(Font font, Component text, int x, int y, int color, boolean shadow, CallbackInfoReturnable<Integer> cir) {
        if (Fiches.tait(text)) {
            cir.setReturnValue(x);
        }
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int fallow$encreSuite(int color) {
        return Fiches.couleur(color);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int fallow$encreTexte(int color) {
        return Fiches.couleur(color);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean fallow$ombreSuite(boolean shadow) {
        return Fiches.ombre(shadow);
    }

    @ModifyVariable(method = "drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;FFIZ)I",
            at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private boolean fallow$ombreTexte(boolean shadow) {
        return Fiches.ombre(shadow);
    }

    @ModifyVariable(method = "blitSprite(Lnet/minecraft/resources/ResourceLocation;IIII)V", at = @At("HEAD"), argsOnly = true,
            ordinal = 0)
    private ResourceLocation fallow$carte(ResourceLocation sprite) {
        return Fiches.sprite(sprite);
    }

    /** Le curseur d'un champ de texte (gris clair) à l'encre. */
    @ModifyVariable(method = "fill(Lnet/minecraft/client/renderer/RenderType;IIIII)V", at = @At("HEAD"), argsOnly = true,
            ordinal = 4)
    private int fallow$curseur(int color) {
        return Fiches.curseur(color);
    }
}
