package io.github.salvagedisc.fallow.bac.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.bac.PieuBlockEntity;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

/**
 * La corde de bac, dessinée par le pieu qui la porte jusqu'au pieu d'en face : deux rubans croisés qui pendent un
 * peu au milieu, éclairés le long du chemin, avec la texture de corde du pack.
 */
public class CordeRenderer implements BlockEntityRenderer<PieuBlockEntity> {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/block/corde_de_bac.png");
    private static final int SEGMENTS = 24;
    private static final float HALF_WIDTH = 0.045F;

    public CordeRenderer(BlockEntityRendererProvider.Context context) {
    }

    @Override
    public void render(PieuBlockEntity post, float partialTick, PoseStack poseStack, MultiBufferSource buffers, int light, int overlay) {
        BlockPos other = post.other();
        Level level = post.getLevel();
        if (!post.owner() || other == null || level == null) {
            return;
        }
        BlockPos pos = post.getBlockPos();
        Vec3 start = new Vec3(0.5, 0.9, 0.5);
        Vec3 end = new Vec3(other.getX() - pos.getX() + 0.5, other.getY() - pos.getY() + 0.9, other.getZ() - pos.getZ() + 0.5);
        double length = start.distanceTo(end);
        double sag = Math.max(0.3, 0.03 * length);
        VertexConsumer consumer = buffers.getBuffer(RenderType.entityCutoutNoCull(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        // Le ruban couché : sa largeur est à l'horizontale, en travers de la corde ; le ruban debout : à la verticale.
        double dx = end.x - start.x;
        double dz = end.z - start.z;
        double flat = Math.sqrt(dx * dx + dz * dz);
        float sideX = flat < 1.0e-6 ? HALF_WIDTH : (float) (-dz / flat) * HALF_WIDTH;
        float sideZ = flat < 1.0e-6 ? 0.0F : (float) (dx / flat) * HALF_WIDTH;
        ribbon(consumer, matrix, level, pos, start, end, sag, length, sideX, 0.0F, sideZ);
        ribbon(consumer, matrix, level, pos, start, end, sag, length, 0.0F, HALF_WIDTH, 0.0F);
    }

    private static void ribbon(VertexConsumer consumer, Matrix4f matrix, Level level, BlockPos origin, Vec3 start, Vec3 end,
            double sag, double length, float ox, float oy, float oz) {
        Vec3 previous = point(start, end, sag, 0.0);
        int previousLight = light(level, origin, previous);
        for (int i = 1; i <= SEGMENTS; i++) {
            double t = (double) i / SEGMENTS;
            Vec3 next = point(start, end, sag, t);
            int nextLight = light(level, origin, next);
            float v0 = (float) ((i - 1) * length / SEGMENTS);
            float v1 = (float) (i * length / SEGMENTS);
            vertex(consumer, matrix, previous, -ox, -oy, -oz, 0.0F, v0, previousLight);
            vertex(consumer, matrix, previous, ox, oy, oz, 1.0F, v0, previousLight);
            vertex(consumer, matrix, next, ox, oy, oz, 1.0F, v1, nextLight);
            vertex(consumer, matrix, next, -ox, -oy, -oz, 0.0F, v1, nextLight);
            previous = next;
            previousLight = nextLight;
        }
    }

    private static Vec3 point(Vec3 start, Vec3 end, double sag, double t) {
        return new Vec3(start.x + (end.x - start.x) * t, start.y + (end.y - start.y) * t - sag * 4.0 * t * (1.0 - t),
                start.z + (end.z - start.z) * t);
    }

    private static int light(Level level, BlockPos origin, Vec3 local) {
        return LevelRenderer.getLightColor(level, BlockPos.containing(origin.getX() + local.x, origin.getY() + local.y, origin.getZ() + local.z));
    }

    private static void vertex(VertexConsumer consumer, Matrix4f matrix, Vec3 at, float ox, float oy, float oz, float u, float v, int light) {
        consumer.addVertex(matrix, (float) at.x + ox, (float) at.y + oy, (float) at.z + oz)
                .setColor(255, 255, 255, 255)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0F, 1.0F, 0.0F);
    }

    @Override
    public boolean shouldRenderOffScreen(PieuBlockEntity post) {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 128;
    }

    @Override
    public AABB getRenderBoundingBox(PieuBlockEntity post) {
        BlockPos pos = post.getBlockPos();
        BlockPos other = post.other();
        if (other == null) {
            return new AABB(pos);
        }
        return new AABB(Vec3.atLowerCornerOf(pos), Vec3.atLowerCornerOf(other).add(1.0, 1.0, 1.0)).inflate(1.0, 4.0, 1.0);
    }
}
