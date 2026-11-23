package io.github.salvagedisc.fallow.hotes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import io.github.salvagedisc.fallow.hotes.visite.NatteBlock;
import io.github.salvagedisc.fallow.hotes.visite.NatteBlockEntity;
import java.util.List;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ce qui est posé sur la natte, couché à plat : trois rangs de trois au plus, chaque objet un peu tourné, comme posé à
 * la main. Rien sur une natte roulée.
 */
public class NatteRenderer implements BlockEntityRenderer<NatteBlockEntity> {
    private static final float[][] PLACES = {
            {0.5F, 0.5F}, {0.27F, 0.3F}, {0.73F, 0.32F}, {0.28F, 0.71F}, {0.72F, 0.7F},
            {0.5F, 0.24F}, {0.5F, 0.77F}, {0.24F, 0.5F}, {0.76F, 0.5F}};
    private final ItemRenderer items;

    public NatteRenderer(BlockEntityRendererProvider.Context context) {
        this.items = context.getItemRenderer();
    }

    @Override
    public void render(NatteBlockEntity natte, float partialTick, PoseStack pose, MultiBufferSource buffers, int light, int overlay) {
        BlockState state = natte.getBlockState();
        if (state.getValue(NatteBlock.ROULEE)) {
            return;
        }
        List<ItemStack> poses = natte.poses();
        for (int i = 0; i < poses.size() && i < PLACES.length; i++) {
            ItemStack pile = poses.get(i);
            if (pile.isEmpty()) {
                continue;
            }
            // Un tas : jusqu'à trois exemplaires empilés, selon le nombre.
            int couches = pile.getCount() >= 16 ? 3 : pile.getCount() >= 4 ? 2 : 1;
            long graine = natte.getBlockPos().asLong() * 31 + i;
            for (int c = 0; c < couches; c++) {
                pose.pushPose();
                float dx = ((graine >> (c * 3)) & 7) / 7.0F * 0.04F - 0.02F;
                pose.translate(PLACES[i][0] + dx, 0.07F + c * 0.03F + i * 0.002F, PLACES[i][1] - dx);
                pose.mulPose(Axis.YP.rotationDegrees((graine * 47 + c * 37) % 360));
                pose.mulPose(Axis.XP.rotationDegrees(90.0F));
                pose.scale(0.38F, 0.38F, 0.38F);
                items.renderStatic(pile, ItemDisplayContext.FIXED, light, overlay, pose, buffers, natte.getLevel(), (int) graine + c);
                pose.popPose();
            }
        }
    }
}
