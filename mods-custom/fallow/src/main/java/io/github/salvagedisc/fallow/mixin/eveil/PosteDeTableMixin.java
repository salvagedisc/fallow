package io.github.salvagedisc.fallow.mixin.eveil;

import java.util.Optional;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.eveil.TableEveil;

/**
 * La table d'enchantement est toujours notre poste ({@link TableEveil}). Hands-On Vanilla enregistre aussi un poste pour la table,
 * coupé dans la configuration du pack ; Diegesis prend le premier poste enregistré pour un bloc, et si c'était le sien, rien ne
 * s'ouvrait et la fenêtre d'enchantement de vanilla passait. Ici, que l'ordre de chargement soit l'un ou l'autre, la table
 * répond par le nôtre, côté serveur comme côté client.
 */
@Pseudo
@Mixin(targets = "com.pockyl.diegesis.impl.StationRegistry", remap = false)
abstract class PosteDeTableMixin {
    @Inject(method = "find", at = @At("HEAD"), cancellable = true, remap = false)
    private static void fallow$notrePoste(BlockState state, CallbackInfoReturnable<Optional<StationType>> callback) {
        if (state.is(Blocks.ENCHANTING_TABLE)) {
            Optional<StationType> notre = Diegesis.stationType(TableEveil.ID);
            if (notre.isPresent()) {
                callback.setReturnValue(notre);
            }
        }
    }
}
