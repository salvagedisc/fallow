package io.github.salvagedisc.fallow.mixin.eveil;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RepairItemRecipe;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Deux objets assemblés à l'établi pour se réparer en font un neuf : ses éveils et ses hauts faits seraient perdus. Un objet
 * qui a commencé sa vie (le composant {@code fallow:eveil}) ne s'y répare donc pas ; l'enclume le répare en gardant tout.
 */
@Mixin(RepairItemRecipe.class)
abstract class ReparationEtabliMixin {
    @Inject(method = "matches(Lnet/minecraft/world/item/crafting/CraftingInput;Lnet/minecraft/world/level/Level;)Z",
            at = @At("HEAD"), cancellable = true)
    private void fallow$pasUnObjetQuiAVecu(CraftingInput input, Level level, CallbackInfoReturnable<Boolean> callback) {
        if (!FallowConfig.eveilEnabled()) {
            return;
        }
        for (int i = 0; i < input.size(); i++) {
            if (input.getItem(i).has(EveilModule.ETAT.get())) {
                callback.setReturnValue(false);
                return;
            }
        }
    }
}
