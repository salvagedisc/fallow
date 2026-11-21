package io.github.salvagedisc.fallow.mixin.eveil;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.pockyl.diegesis.api.client.DiegesisClient;

/**
 * Au poste de la table d'enchantement, le livre qui flotte au-dessus de la table est celui de la scène : il en part, vient au
 * joueur et y retourne. Le livre de vanilla n'est pas dessiné en plus tant que la scène est là, comme Hands-On Vanilla le fait
 * pour le sien.
 */
@Mixin(EnchantTableRenderer.class)
abstract class LivreDeTableMixin {
    @Inject(method = "render(Lnet/minecraft/world/level/block/entity/EnchantingTableBlockEntity;FLcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/MultiBufferSource;II)V",
            at = @At("HEAD"), cancellable = true)
    private void fallow$livreDeLaScene(EnchantingTableBlockEntity table, float partialTick, PoseStack poseStack, MultiBufferSource buffers,
            int light, int overlay, CallbackInfo callback) {
        if (DiegesisClient.isSceneDrawn(table.getBlockPos())) {
            callback.cancel();
        }
    }
}
