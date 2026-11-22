package io.github.salvagedisc.fallow.mixin.eveil;

import io.github.salvagedisc.fallow.eveil.Eveil;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'usure d'un objet éveillé, vue au moment même : entretenu, il s'arrête à son dernier point au lieu de casser (même un
 * outil qui perd plusieurs points en une seconde) ; sinon il oublie son dernier éveil au point exact. Brûlé ou détruit au
 * sol, il emporte ses actes. Priorité basse : passe avant les autres mods qui regardent la casse (Overgeared).
 */
@Mixin(value = ItemStack.class, priority = 500)
abstract class ObjetEveilleMixin {
    private static final String HURT = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V";

    @ModifyVariable(method = HURT, at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private int fallow$retenir(int usure) {
        return Eveil.retenir((ItemStack) (Object) this, usure);
    }

    @Inject(method = HURT, at = @At("RETURN"))
    private void fallow$apresUsure(int usure, ServerLevel level, LivingEntity porteur, Consumer<Item> casse, CallbackInfo info) {
        Eveil.apresUsure(porteur, (ItemStack) (Object) this);
    }

    @Inject(method = "onDestroyed(Lnet/minecraft/world/entity/item/ItemEntity;)V", at = @At("HEAD"))
    private void fallow$detruit(ItemEntity entity, CallbackInfo info) {
        if (!entity.level().isClientSide()) {
            EveilModule.detruit((ItemStack) (Object) this);
        }
    }
}
