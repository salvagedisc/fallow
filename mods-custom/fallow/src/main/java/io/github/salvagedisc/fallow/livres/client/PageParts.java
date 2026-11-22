package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import io.github.salvagedisc.fallow.Fallow;

/**
 * The furniture of a recipe book's pages, drawn on the page in its own units (x across from its left edge, y down from its head):
 * the tabs over the head of the left page, the search slip, the wells the recipes sit in, the frame round a picked one, the card a
 * group of recipes opens on, the magnifier, the mark of a new recipe, the rule under a recipe's name. All come from one texture,
 * {@code livre_elements.png} (the gutter's shade with them), two texels to a unit, drawn by {@code scripts/books/papers.py}; the tabs, the slip and the card are
 * drawn in nine pieces so they take any size. Used by the pack's own books and, through {@code mixin.livres.HovRecipeSheetMixin},
 * by Hands-On Vanilla's.
 */
public final class PageParts {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/entity/livre_elements.png");
    private static final float ATLAS = 128.0F;
    /** Texels of the texture to a unit of the page. */
    private static final float DENSITY = 2.0F;

    /** A tab behind the page, under the cursor, or open (the page's paper, meeting the page). */
    public enum Tab {
        REST, LIT, OPEN
    }

    private PageParts() {
    }

    /** A tab from {@code top} down to {@code bottom}, {@code x} to {@code x + wide}. */
    public static void tab(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float top, float wide, float bottom,
            float lift, Tab state) {
        nine(poseStack, buffers, light, x, top, wide, bottom - top, lift, 16 * state.ordinal(), 0, 16, 16, 5);
    }

    /** The search slip, a field from {@code top} down to {@code bottom}. */
    public static void slip(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float top, float wide, float bottom,
            float lift, boolean lit) {
        nine(poseStack, buffers, light, x, top, wide, bottom - top, lift, lit ? 64 : 48, 0, 16, 16, 5);
    }

    /** A well {@code size} units square, pressed into the paper: faint at rest, a soft green when its recipe can be made. */
    public static void well(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float size, float lift,
            boolean ready) {
        sprite(poseStack, buffers, light, x, y, size, size, lift, ready ? 34 : 0, 16, 34, 34);
    }

    /** The frame round a picked well. */
    public static void frame(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float size, float lift) {
        sprite(poseStack, buffers, light, x, y, size, size, lift, 68, 16, 34, 34);
    }

    /** The card a group of recipes opens on. */
    public static void card(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float wide, float high,
            float lift) {
        nine(poseStack, buffers, light, x, y, wide, high, lift, 0, 52, 24, 24, 8);
    }

    /** The magnifier, 7 units square from ({@code x}, {@code y}), as the books' own was. */
    public static void magnifier(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float lift) {
        sprite(poseStack, buffers, light, x - 0.5F, y - 0.5F, 8.0F, 8.0F, lift, 24, 52, 16, 16);
    }

    /** The mark of a new recipe, 4 units square centred on ({@code x}, {@code y}). */
    public static void mark(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float lift) {
        sprite(poseStack, buffers, light, x - 2.0F, y - 2.0F, 4.0F, 4.0F, lift, 40, 52, 8, 8);
    }

    /**
     * The gutter's shade over a page, its full height: a fine line where the page goes down into the fold and a soft shadow
     * fading out over 8 units; {@code left}: the left page, the fold on its right.
     */
    public static void gutter(PoseStack poseStack, MultiBufferSource buffers, int light, boolean left, float wide, float high,
            float lift) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(TEXTURE));
        float x0 = left ? wide - 8.0F : 0.0F;
        quad(consumer, poseStack.last().pose(), light, x0, 0.0F, x0 + 8.0F, high, lift, left ? 64 : 48, 52, left ? 48 : 64, 60);
    }

    /** The rule under a recipe's name, {@code wide} across, its line at {@code y}. */
    public static void rule(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float wide, float lift) {
        sprite(poseStack, buffers, light, x, y - 2.0F, wide, 4.0F, lift, 0, 76, 106, 8);
    }

    private static void sprite(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float wide, float high,
            float lift, int u, int v, int w, int h) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(TEXTURE));
        quad(consumer, poseStack.last().pose(), light, x, y, x + wide, y + high, lift, u, v, u + w, v + h);
    }

    /**
     * A box drawn in nine pieces from the {@code w} x {@code h} texels at ({@code u}, {@code v}): corners of {@code border} texels
     * kept as they are, sides and middle stretched. A box smaller than its corners takes them shrunk.
     */
    private static void nine(PoseStack poseStack, MultiBufferSource buffers, int light, float x, float y, float wide, float high,
            float lift, int u, int v, int w, int h, int border) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(TEXTURE));
        Matrix4f matrix = poseStack.last().pose();
        float side = border / DENSITY;
        float bx = Math.min(side, wide / 2.0F);
        float by = Math.min(side, high / 2.0F);
        float[] xs = {x, x + bx, x + wide - bx, x + wide};
        float[] ys = {y, y + by, y + high - by, y + high};
        float[] us = {u, u + border, u + w - border, u + w};
        float[] vs = {v, v + border, v + h - border, v + h};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (xs[col + 1] - xs[col] <= 0.0F || ys[row + 1] - ys[row] <= 0.0F) {
                    continue;
                }
                quad(consumer, matrix, light, xs[col], ys[row], xs[col + 1], ys[row + 1], lift, us[col], vs[row], us[col + 1], vs[row + 1]);
            }
        }
    }

    private static void quad(VertexConsumer consumer, Matrix4f matrix, int light, float x0, float y0, float x1, float y1, float lift,
            float u0, float v0, float u1, float v1) {
        u0 /= ATLAS;
        u1 /= ATLAS;
        v0 /= ATLAS;
        v1 /= ATLAS;
        consumer.addVertex(matrix, x0, y0, lift).setColor(255, 255, 255, 255).setUv(u0, v0).setLight(light);
        consumer.addVertex(matrix, x0, y1, lift).setColor(255, 255, 255, 255).setUv(u0, v1).setLight(light);
        consumer.addVertex(matrix, x1, y1, lift).setColor(255, 255, 255, 255).setUv(u1, v1).setLight(light);
        consumer.addVertex(matrix, x1, y0, lift).setColor(255, 255, 255, 255).setUv(u1, v0).setLight(light);
    }
}
