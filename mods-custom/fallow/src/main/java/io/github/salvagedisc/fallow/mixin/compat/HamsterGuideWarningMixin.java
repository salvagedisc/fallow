package io.github.salvagedisc.fallow.mixin.compat;

import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adorable Hamster Pets, quelques minutes après l'arrivée d'un joueur sans son livre « Hamster Tips » (le pack ne le donne
 * pas : il est en anglais), écrit dans le tchat un avertissement en plusieurs lignes, en majuscules, qui tutoie le joueur et
 * l'envoie vers un serveur Discord. Rien ne le coupe dans sa configuration : il ne part jamais. Le livre reste en EMI et se
 * fabrique toujours. Appliqué seulement avec Adorable Hamster Pets.
 */
@Mixin(targets = "net.dawson.adorablehamsterpets.AdorableHamsterPetsClient", remap = false)
abstract class HamsterGuideWarningMixin {
    @Inject(method = "handleGuidebookWarning", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fallow$pasDAvertissement(Minecraft minecraft, CallbackInfo info) {
        info.cancel();
    }
}
