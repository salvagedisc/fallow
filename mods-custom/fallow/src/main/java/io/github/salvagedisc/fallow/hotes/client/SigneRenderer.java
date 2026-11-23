package io.github.salvagedisc.fallow.hotes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.salvagedisc.fallow.hotes.HotesModule;
import io.github.salvagedisc.fallow.hotes.visite.SigneBlock;
import io.github.salvagedisc.fallow.hotes.visite.SigneBlockEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/** Le masque du peuple qui viendra, pendu en haut du piquet, tourné vers la clairière ; il se balance un peu au vent. */
public class SigneRenderer implements BlockEntityRenderer<SigneBlockEntity> {
    private final ItemRenderer items;

    public SigneRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(SigneBlockEntity signe, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        ItemStack masque = new ItemStack(HotesModule.MASQUES.get(signe.peuple()).get());
        var face = signe.getBlockState().getValue(SigneBlock.FACING);
        float lacet = face.toYRot();
        long temps = signe.getLevel() == null ? 0 : signe.getLevel().getGameTime();
        float balance = (float) Math.sin((temps + partialTick) * 0.05F + signe.getBlockPos().asLong() % 7) * 3.0F;
        pose.pushPose();
        // Du côté de la clairière, contre le piquet, sous le bras qui le tient.
        pose.translate(0.5F + face.getStepX() * 0.25F, 0.52F, 0.5F + face.getStepZ() * 0.25F);
        pose.mulPose(Axis.YP.rotationDegrees(-lacet));
        pose.mulPose(Axis.XP.rotationDegrees(balance));
        pose.scale(0.62F, 0.62F, 0.62F);
        items.renderStatic(masque, ItemDisplayContext.FIXED, light, overlay, pose, buffers, signe.getLevel(), 0);
        pose.popPose();
    }
}
