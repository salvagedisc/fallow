package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Vector3f;

/**
 * A sheet going over in a book, bent as paper bends: its free edge lifts first and leads, then trails behind and lands last. It is
 * drawn in strips from the fold to the edge, each turned a little further than the one before, with the picture of each of its two
 * faces ({@link PageTextures}) stretched across them, so that what is written goes over with the paper. The strips' angles never
 * leave the span between the two resting pages: the sheet arches over them and never cuts into them.
 */
public final class CurledSheet {
    /** How many strips from the fold to the edge. */
    private static final int STRIPS = 16;
    /** How far the edge turns ahead of (or behind) the fold at most, radians. */
    private static final float BEND = 1.15F;
    /** How the bend grows from the fold to the edge: the paper near the fold stays stiff. */
    private static final float STIFF = 1.7F;

    private CurledSheet() {
    }

    /** The book's own frame: where the fold is, which way the spine runs, and how its sheets turn about it. */
    public interface Frame {
        /** The way across a sheet from the fold, at {@code angle}. */
        Vector3f way(float angle);

        /** The face that shows the front picture looks this way at {@code angle}. */
        Vector3f front(float angle);
    }

    /**
     * The angle of each strip's boundary, from the fold ({@code 0}) to the edge ({@code STRIPS}), for a sheet going from the angle
     * {@code from} to {@code to}, {@code over} of the way there (0 to 1, eased as the book eases it).
     */
    public static float[] angles(float from, float to, float over) {
        float[] out = new float[STRIPS + 1];
        float base = Mth.lerp(over, from, to);
        // Ahead while it lifts, behind while it lands; a little ahead all along the middle of the turn.
        float lead = Mth.sin(over * Mth.TWO_PI) * 0.75F + Mth.sin(over * Mth.PI) * 0.25F;
        float sign = Math.signum(to - from);
        float low = Math.min(from, to);
        float high = Math.max(from, to);
        for (int i = 0; i <= STRIPS; i++) {
            float t = (float) i / STRIPS;
            float bend = BEND * lead * (float) Math.pow(t, STIFF);
            out[i] = Mth.clamp(base + sign * bend, low, high);
        }
        return out;
    }

    /**
     * Draws the sheet: {@code fold} its fold's middle, {@code top} half of the fold's length towards the head of the pages,
     * {@code wide} its width across, {@code angles} from {@link #angles}. The front picture runs from the fold (its left edge)
     * outwards, the back one from the edge (its left edge) to the fold, as each page reads.
     */
    public static void draw(PoseStack poseStack, MultiBufferSource buffers, Frame frame, Vector3f fold, Vector3f top, float wide,
            float[] angles, ResourceLocation front, ResourceLocation back, int light, float alpha) {
        int n = angles.length - 1;
        Vector3f[] at = new Vector3f[n + 1];
        at[0] = new Vector3f(fold);
        for (int i = 0; i < n; i++) {
            // Each strip goes on the way of the middle of its two boundaries.
            float mid = (angles[i] + angles[i + 1]) * 0.5F;
            at[i + 1] = frame.way(mid).mul(wide / n).add(at[i]);
        }
        int a = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
        boolean seeThrough = a < 255;
        VertexConsumer frontSide = buffers.getBuffer(seeThrough ? RenderType.entityTranslucentCull(front) : RenderType.entityCutout(front));
        strips(poseStack, frontSide, frame, at, top, angles, false, light, a);
        VertexConsumer backSide = buffers.getBuffer(seeThrough ? RenderType.entityTranslucentCull(back) : RenderType.entityCutout(back));
        strips(poseStack, backSide, frame, at, top, angles, true, light, a);
    }

    private static void strips(PoseStack poseStack, VertexConsumer consumer, Frame frame, Vector3f[] at, Vector3f top, float[] angles,
            boolean back, int light, int alpha) {
        int n = at.length - 1;
        PoseStack.Pose pose = poseStack.last();
        // A pose that mirrors (a negative scale) turns every winding round: the strips are wound the other way to match.
        boolean mirrored = pose.pose().determinant3x3() < 0.0F;
        Vector3f middle = frame.front((angles[0] + angles[n]) * 0.5F);
        for (int i = 0; i < n; i++) {
            float mid = (angles[i] + angles[i + 1]) * 0.5F;
            Vector3f normal = frame.front(mid);
            // The paper is shaded by how far it bends from the sheet's middle way: the curl reads, the page stays as light as
            // the resting ones, which no light shades.
            int shade = Math.round(255.0F * (SHADE_LOW + (1.0F - SHADE_LOW) * Math.max(0.0F, normal.dot(middle))));
            if (back) {
                normal.negate();
            }
            float u0 = (float) i / n;
            float u1 = (float) (i + 1) / n;
            if (back) {
                u0 = 1.0F - u0;
                u1 = 1.0F - u1;
            }
            Vector3f a0 = new Vector3f(at[i]).add(top);
            Vector3f b0 = new Vector3f(at[i]).sub(top);
            Vector3f b1 = new Vector3f(at[i + 1]).sub(top);
            Vector3f a1 = new Vector3f(at[i + 1]).add(top);
            // Wound to face its normal: the other face's strips are culled, never seen through this one. The pictures are painted
            // into frame buffers, whose first row is their bottom: the head of the page is at v 1.
            Vector3f turn = new Vector3f(b0).sub(a0).cross(new Vector3f(b1).sub(a0));
            if (turn.dot(normal) >= 0.0F != mirrored) {
                vertex(consumer, pose, a0, u0, 1.0F, shade, light, alpha);
                vertex(consumer, pose, b0, u0, 0.0F, shade, light, alpha);
                vertex(consumer, pose, b1, u1, 0.0F, shade, light, alpha);
                vertex(consumer, pose, a1, u1, 1.0F, shade, light, alpha);
            } else {
                vertex(consumer, pose, a1, u1, 1.0F, shade, light, alpha);
                vertex(consumer, pose, b1, u1, 0.0F, shade, light, alpha);
                vertex(consumer, pose, b0, u0, 0.0F, shade, light, alpha);
                vertex(consumer, pose, a0, u0, 1.0F, shade, light, alpha);
            }
        }
    }

    /**
     * A vertex whose normal points into the level's first light, as the icons on the pages have (Hands-On Vanilla's way): the
     * entity shader then leaves its colour as it is, and the shade is the strip's own.
     */
    private static void vertex(VertexConsumer consumer, PoseStack.Pose pose, Vector3f at, float u, float v, int shade, int light,
            int alpha) {
        consumer.addVertex(pose, at.x, at.y, at.z).setColor(shade, shade, shade, alpha).setUv(u, v).setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light).setNormal(LIT.x, LIT.y, LIT.z);
    }

    /** The level's first light, as the game sets it (Lighting.DIFFUSE_LIGHT_0). */
    private static final Vector3f LIT = new Vector3f(0.2F, 1.0F, -0.7F).normalize();
    /** The darkest a strip turned right across the sheet's middle way gets. */
    private static final float SHADE_LOW = 0.72F;
}
