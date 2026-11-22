package io.github.salvagedisc.fallow.mixin.eveil;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import io.github.salvagedisc.fallow.eveil.client.Isolement;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * À la table d'éveil, les bruits du monde se taisent presque ({@link Isolement#volume}) : au départ d'un bruit, et pour ceux qui
 * jouent déjà quand le silence bouge. Jamais jusqu'à zéro, pour qu'aucun bruit ne soit coupé et que tout revienne en partant.
 */
@Mixin(SoundEngine.class)
abstract class SilenceDeTableMixin {
    @ModifyReturnValue(method = "calculateVolume(Lnet/minecraft/client/resources/sounds/SoundInstance;)F", at = @At("RETURN"))
    private float fallow$silenceEnJouant(float volume, SoundInstance sound) {
        return volume * Isolement.volume(sound);
    }

    @WrapOperation(method = "play", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/sounds/SoundEngine;calculateVolume(FLnet/minecraft/sounds/SoundSource;)F"))
    private float fallow$silenceAuDepart(SoundEngine engine, float volume, SoundSource source, Operation<Float> original,
            @Local(argsOnly = true) SoundInstance sound) {
        return original.call(engine, volume, source) * Isolement.volume(sound);
    }
}
