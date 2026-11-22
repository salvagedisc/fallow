package io.github.salvagedisc.fallow.mixin.compat;

import io.github.salvagedisc.fallow.eveil.Eveil;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import java.util.List;
import java.util.stream.Stream;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
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
    private static final List<TagKey<Block>> BASES = Stream.of("stone", "iron", "tier_a", "tier_b")
            .map(t -> TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("overgeared", t + "_anvil_bases")))
            .toList();

    private static boolean enclume(BlockEntity entite) {
        for (Class<?> c = entite == null ? null : entite.getClass(); c != null; c = c.getSuperclass()) {
            if (c.getName().equals("net.stirdrem.overgeared.block.entity.AbstractSmithingAnvilBlockEntity")) {
                return true;
            }
        }
        return false;
    }

    @Inject(method = "onUseSmithingHammer", at = @At("HEAD"), cancellable = true, require = 0)
    private static void fallow$arrete(ItemStack marteau, PlayerInteractEvent.RightClickBlock event, Player player, Level level,
            BlockState state, CallbackInfo info) {
        // Seulement là où le marteau forge : une enclume de forge, ou, accroupi, une base d'enclume qu'il changerait en enclume.
        // Ailleurs (une porte, un coffre, la fonderie d'Overgeared), le bloc répond comme à une main vide.
        if (Eveil.arrete(marteau) && (enclume(level.getBlockEntity(event.getPos()))
                || player.isCrouching() && BASES.stream().anyMatch(state::is))) {
            event.setCanceled(true);
            event.setCancellationResult(InteractionResult.FAIL);
            EveilModule.refuser(player, marteau);
            info.cancel();
        }
    }
}
