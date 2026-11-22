package io.github.salvagedisc.fallow.mixin.compat;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.salvagedisc.fallow.eveil.Eveil;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/**
 * Les recettes de KubeJS qui usent un outil (les planches à la hache) posent son usure directement, sans passer par
 * {@code hurtAndBreak} : un objet éveillé entretenu y serait détruit avec ses éveils. Il s'arrête à son dernier point,
 * comme partout ailleurs. Appliqué seulement avec KubeJS.
 */
@Mixin(targets = "dev.latvian.mods.kubejs.recipe.ingredientaction.DamageAction", remap = false)
abstract class RecetteUsanteMixin {
    @ModifyArg(method = "transform", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;setDamageValue(I)V"), require = 0)
    private int fallow$retenir(int usure, @Local(argsOnly = true) ItemStack stack) {
        return Eveil.plafond(stack, usure);
    }
}
