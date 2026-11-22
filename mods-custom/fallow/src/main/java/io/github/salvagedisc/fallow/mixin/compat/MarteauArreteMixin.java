package io.github.salvagedisc.fallow.mixin.compat;

import io.github.salvagedisc.fallow.eveil.Eveil;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Overgeared lance la forge au marteau depuis son propre écouteur, avant tous les autres : un marteau éveillé entretenu
 * arrêté à son dernier point (il ne s'use plus) y forgerait sans fin. Il attend d'être réparé, comme partout ailleurs.
 * Appliqué seulement avec Overgeared.
 */
@Mixin(targets = "net.stirdrem.overgeared.event.ModItemInteractEvents", remap = false)
abstract class MarteauArreteMixin {
    @Inject(method = "onUseSmithingHammer", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fallow$arrete(ItemStack marteau, PlayerInteractEvent.RightClickBlock event, Player player, Level level,
            BlockState state, CallbackInfo info) {
        if (Eveil.arrete(marteau)) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            EveilModule.refuser(player, marteau);
            info.cancel();
        }
    }
}
