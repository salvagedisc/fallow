package io.github.salvagedisc.fallow.mixin.postes;

import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.postes.client.TailleDePierre;

/**
 * La taille de pierre d'Overgeared s'ouvre sans fenêtre ({@link TailleDePierre}) quand le réglage {@code postes.taille} est vrai :
 * l'écran est choisi à l'ouverture, d'après le type de menu. Appliqué seulement avec Overgeared.
 */
@Mixin(MenuScreens.class)
abstract class TailleDePierreMixin {
    private static final ResourceLocation TAILLE = ResourceLocation.fromNamespaceAndPath("overgeared", "rock_knapping_menu");

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Inject(method = "getConstructor", at = @At("HEAD"), cancellable = true)
    private static <T extends AbstractContainerMenu> void fallow$taille(MenuType<T> type,
            CallbackInfoReturnable<MenuScreens.ScreenConstructor<T, ?>> callback) {
        if (FallowConfig.postesTaille() && TAILLE.equals(BuiltInRegistries.MENU.getKey(type))) {
            callback.setReturnValue((MenuScreens.ScreenConstructor) (MenuScreens.ScreenConstructor<net.stirdrem.overgeared.screen.RockKnappingMenu, TailleDePierre>) TailleDePierre::new);
        }
    }
}
