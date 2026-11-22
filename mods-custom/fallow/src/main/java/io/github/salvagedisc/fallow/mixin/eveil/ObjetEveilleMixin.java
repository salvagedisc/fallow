package io.github.salvagedisc.fallow.mixin.eveil;

import com.llamalad7.mixinextras.sugar.Local;
import io.github.salvagedisc.fallow.eveil.Eveil;
import java.util.function.Consumer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * L'usure d'un objet éveillé, vue au moment même : entretenu, il s'arrête à son dernier point au lieu de casser (même un
 * outil qui perd plusieurs points en une seconde) ; sinon il oublie son dernier éveil au point exact. L'usure est retenue
 * après {@code Item.damageItem}, où un mod peut encore l'augmenter. Seul un joueur le retient : dans un distributeur
 * ou une machine, l'objet s'use comme un autre.
 */
@Mixin(ItemStack.class)
abstract class ObjetEveilleMixin {
    private static final String HURT = "hurtAndBreak(ILnet/minecraft/server/level/ServerLevel;"
            + "Lnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)V";

    @ModifyVariable(method = HURT, at = @At(value = "INVOKE_ASSIGN", target = "Lnet/minecraft/world/item/Item;damageItem("
            + "Lnet/minecraft/world/item/ItemStack;ILnet/minecraft/world/entity/LivingEntity;Ljava/util/function/Consumer;)I"),
            argsOnly = true, ordinal = 0)
    private int fallow$retenir(int usure, @Local(argsOnly = true) LivingEntity porteur) {
        return Eveil.retenir((ItemStack) (Object) this, usure, porteur);
    }

    @Inject(method = HURT, at = @At("RETURN"))
    private void fallow$apresUsure(int usure, ServerLevel level, LivingEntity porteur, Consumer<Item> casse, CallbackInfo info) {
        Eveil.apresUsure(porteur, (ItemStack) (Object) this);
    }
}
