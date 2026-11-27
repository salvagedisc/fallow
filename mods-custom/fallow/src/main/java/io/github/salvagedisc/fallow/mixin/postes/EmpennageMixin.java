package io.github.salvagedisc.fallow.mixin.postes;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.util.TriState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.postes.client.PostesTables;

/**
 * Overgeared ouvre sa table d'empennage depuis son propre écouteur, avant tous les autres, et annule l'usage du bloc : sur le
 * client, Diegesis (qui écoute en dernier) ne voyait jamais le clic, et la fenêtre d'Overgeared passait au lieu du poste. Sur le
 * client seulement, quand le poste d'empennage s'ouvre, l'écouteur d'Overgeared laisse passer l'usage du bloc : Diegesis arme le
 * poste, puis {@code PostesTables.apresDiegesis} arrête le clic comme Overgeared l'aurait fait (ni l'objet tenu ni la main gauche
 * ne servent), et le serveur, où rien ne change, ouvre la fenêtre que le poste prend.
 * Appliqué seulement avec Overgeared et Diegesis.
 */
@Mixin(targets = "net.stirdrem.overgeared.event.ModItemInteractEvents", remap = false)
abstract class EmpennageMixin {
    @Inject(method = "onUseFletching", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fallow$auPoste(PlayerInteractEvent.RightClickBlock event, Player player, Level level, CallbackInfo info) {
        if (level.isClientSide() && PostesTables.empennage(player)) {
            event.setUseItem(TriState.FALSE);
            info.cancel();
        }
    }
}
