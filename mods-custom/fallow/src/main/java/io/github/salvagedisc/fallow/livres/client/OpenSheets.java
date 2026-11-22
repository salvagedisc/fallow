package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The sheets of the books laid out as the enchanting table's (Hands-On Vanilla's {@code OpenBook} and the awakening book ported
 * from it): sheets 6 x 8 pixels turning about the y axis, the fold at {@code sin(open)} along x, the right page's face towards +z.
 * A sheet going over is bent ({@link CurledSheet}) with the picture of each of its faces on it.
 */
public final class OpenSheets {
    public static final float PAGE_WIDE = 6.0F;
    public static final float PAGE_HIGH = 8.0F;
    /** The picture of a face: texels to the model's pixel, and its size. */
    private static final int SCALE = 48;
    private static final int WIDE = Math.round(PAGE_WIDE * SCALE);
    private static final int HIGH = Math.round(PAGE_HIGH * SCALE);
    /** A sheet drawn flat at the fold for its picture: a hair off flat, so that it reads as on the right or the left. */
    public static final float FLAT = 1.0E-5F;
    /** The model's pixels of a flat sheet to its face's picture: the right one from the fold, the left one from the far edge. */
    private static final Matrix4f FRONT_PICTURE = new Matrix4f(SCALE, 0.0F, 0.0F, 0.0F,
            0.0F, -SCALE, 0.0F, 0.0F,
            0.0F, 0.0F, SCALE, 0.0F,
            0.0F, PAGE_HIGH / 2.0F * SCALE, 0.0F, 1.0F);
    private static final Matrix4f BACK_PICTURE = new Matrix4f(-SCALE, 0.0F, 0.0F, 0.0F,
            0.0F, -SCALE, 0.0F, 0.0F,
            0.0F, 0.0F, -SCALE, 0.0F,
            PAGE_WIDE * SCALE, PAGE_HIGH / 2.0F * SCALE, 0.0F, 1.0F);

    private static final CurledSheet.Frame FRAME = new CurledSheet.Frame() {
        @Override
        public Vector3f way(float angle) {
            return new Vector3f(Mth.cos(angle), 0.0F, -Mth.sin(angle));
        }

        @Override
        public Vector3f front(float angle) {
            return new Vector3f(Mth.sin(angle), 0.0F, Mth.cos(angle));
        }
    };

    private OpenSheets() {
    }

    /**
     * Draws a page of the book flat at the fold, on the right ({@code front}) or the left, at full light: the book's own way of
     * drawing a sheet, at the angle {@code front ? FLAT : -FLAT} with the fold at 0.
     */
    @FunctionalInterface
    public interface FacePainter {
        void paint(PoseStack poseStack, MultiBufferSource buffers, int page, boolean front);
    }

    /** A face of a sheet of a book, for {@link PageTextures}: the book by identity, the page, which face. */
    private record Face(Object book, int page, boolean front) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Face face && face.book == book && face.page == page && face.front == front;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(book) * 31 + page * 2 + (front ? 1 : 0);
        }
    }

    /**
     * A sheet of {@code book} going over from the angle {@code from} to {@code to}, {@code over} of the way, bent, with {@code front}
     * on its face (read on the right) and {@code back} on its back. False until the pictures of its faces are painted (the first
     * frame of a turn): the book then draws the sheet flat, as it always did.
     */
    public static boolean turn(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float lift, float from, float to,
            float over, Object book, int front, int back, FacePainter painter, float alpha) {
        ResourceLocation frontPicture = PageTextures.request(new Face(book, front, true), WIDE, HIGH, (pose, drawn) -> {
            pose.mulPose(FRONT_PICTURE);
            painter.paint(pose, drawn, front, true);
        });
        ResourceLocation backPicture = PageTextures.request(new Face(book, back, false), WIDE, HIGH, (pose, drawn) -> {
            pose.mulPose(BACK_PICTURE);
            painter.paint(pose, drawn, back, false);
        });
        if (frontPicture == null || backPicture == null) {
            return false;
        }
        Vector3f fold = new Vector3f(Mth.sin(open) + lift, 0.0F, 0.0F);
        CurledSheet.draw(poseStack, buffers, FRAME, fold, new Vector3f(0.0F, PAGE_HIGH / 2.0F, 0.0F), PAGE_WIDE,
                CurledSheet.angles(from, to, over), frontPicture, backPicture, light, alpha);
        return true;
    }
}
