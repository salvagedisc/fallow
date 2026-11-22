package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import io.github.salvagedisc.fallow.Fallow;

/**
 * The corner turned down at the foot of a page, where a book has more to turn: at the outer corner of the right page when a page
 * follows, of the left page when one comes before. It is where the page is taken to be turned; under the cursor it lifts a little
 * more. Drawn over the paper, under what is written, in a page's own units (x across from its left edge, y down from its head).
 * The picture is {@code coin_de_page.png}, drawn by {@code scripts/books/papers.py}.
 */
public final class DogEar {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/entity/coin_de_page.png");

    /**
     * How far over the paper it is drawn, in the units of a page's depth: above the wells and rules (0.3), under the icons and the
     * words (1.2), and far enough off the paper not to sink into it at a distance.
     */
    public static final float LIFT = 0.9F;

    /** The recipe and lectern books' paper, or the awakening book's. */
    public enum Paper {
        CREAM, MYSTIC
    }

    private DogEar() {
    }

    /**
     * Draws the corner {@code size} units square at the page's corner ({@code cornerX}, {@code cornerY}): the bottom left one of a
     * left page ({@code left}), the bottom right one of a right page. {@code lift}: how far over the paper, in the page's depth.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, int light, float cornerX, float cornerY, float size,
            boolean left, boolean lit, Paper paper, float lift) {
        float u0 = lit ? 0.5F : 0.0F;
        float u1 = u0 + 0.5F;
        float v0 = paper == Paper.MYSTIC ? 0.5F : 0.0F;
        float v1 = v0 + 0.5F;
        float x0 = left ? cornerX : cornerX - size;
        float x1 = x0 + size;
        float y0 = cornerY - size;
        float y1 = cornerY;
        if (left) {
            float u = u0;
            u0 = u1;
            u1 = u;
        }
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        consumer.addVertex(matrix, x0, y0, lift).setColor(255, 255, 255, 255).setUv(u0, v0).setLight(light);
        consumer.addVertex(matrix, x0, y1, lift).setColor(255, 255, 255, 255).setUv(u0, v1).setLight(light);
        consumer.addVertex(matrix, x1, y1, lift).setColor(255, 255, 255, 255).setUv(u1, v1).setLight(light);
        consumer.addVertex(matrix, x1, y0, lift).setColor(255, 255, 255, 255).setUv(u1, v0).setLight(light);
    }
}
