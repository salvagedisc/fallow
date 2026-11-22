package io.github.salvagedisc.fallow.mixin.eveil;

import io.github.salvagedisc.fallow.eveil.EveilModule;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Un objet éveillé brûlé, fondu ou détruit par une explosion au sol emporte ses actes avec lui. */
@Mixin(ItemEntity.class)
abstract class ObjetBruleMixin {
    @Inject(method = "hurt(Lnet/minecraft/world/damagesource/DamageSource;F)Z", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/item/ItemStack;onDestroyed(Lnet/minecraft/world/entity/item/ItemEntity;"
                    + "Lnet/minecraft/world/damagesource/DamageSource;)V"))
    private void fallow$detruit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> info) {
        ItemEntity self = (ItemEntity) (Object) this;
        if (!self.level().isClientSide()) {
            EveilModule.detruit(self.getItem());
        }
    }
}
