package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * The sheets of the recipe books laid out as Hands-On Vanilla's {@code RecipeBook} (its own, and the stations' cookbook ported
 * from it): pages 72 x 96 units of 0.06 pixel, turning about the z axis at the fold, the right page across -x with its face up
 * (+y). A sheet going over is bent ({@link CurledSheet}) with the picture of each of its faces on it.
 */
public final class RecipeSheets {
    private static final int UNITS_WIDE = 72;
    private static final int UNITS_HIGH = 96;
    private static final float UNIT = 0.72F * 6.0F / UNITS_WIDE;
    private static final float PAGE_WIDTH = UNITS_WIDE * UNIT;
    private static final float PAGE_HEIGHT = UNITS_HIGH * UNIT;
    /** How far the sheet going over stands over the resting ones, pixels, as the books lift it. */
    public static final float SHEET_LIFT = 0.13F;
    /** The picture of a face: texels to a unit, and its size. */
    private static final int SCALE = 4;
    private static final int WIDE = UNITS_WIDE * SCALE;
    private static final int HIGH = UNITS_HIGH * SCALE;
    /** What undoes a book's own placing of a page flat at the fold, so that its units fall on the picture's texels. */
    private static final Matrix4f RIGHT_PICTURE = picture(false);
    private static final Matrix4f LEFT_PICTURE = picture(true);

    private static final CurledSheet.Frame FRAME = new CurledSheet.Frame() {
        @Override
        public Vector3f way(float angle) {
            return new Vector3f(-Mth.cos(angle), -Mth.sin(angle), 0.0F);
        }

        @Override
        public Vector3f front(float angle) {
            return new Vector3f(-Mth.sin(angle), Mth.cos(angle), 0.0F);
        }
    };

    private RecipeSheets() {
    }

    private static Matrix4f picture(boolean left) {
        Matrix4f sheet = new Matrix4f(-UNIT, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -UNIT, 0.0F, 0.0F, UNIT, 0.0F, 0.0F, left ? PAGE_WIDTH : 0.0F,
                0.0F, PAGE_HEIGHT / 2.0F, 1.0F);
        return new Matrix4f().scaling(SCALE).mul(sheet.invert());
    }

    /**
     * Draws one side of a sheet as the book's own {@code side} does, at the turn 0 and the height 0 (flat at the fold), at full
     * light: the right page ({@code left} false) or the left one.
     */
    @FunctionalInterface
    public interface SidePainter {
        void paint(PoseStack poseStack, MultiBufferSource buffers, boolean left);
    }

    /** A face of a book's sheet going over, for {@link PageTextures}: the book by identity and which face. */
    private record Face(Object book, boolean front) {
        @Override
        public boolean equals(Object other) {
            return other instanceof Face face && face.book == book && face.front == front;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(book) * 2 + (front ? 1 : 0);
        }
    }

    /**
     * The sheet of {@code book} going over, {@code over} of the way from the angle {@code from} to {@code to}, its fold {@code height}
     * over the spine (the sheet's lift included): bent, the right page on its face, the left one on its back. False until the
     * pictures of its faces are painted (the first frame of a turn): the book then draws the sheet flat, as it always did.
     */
    public static boolean turn(PoseStack poseStack, MultiBufferSource buffers, int light, float height, float from, float to, float over,
            Object book, SidePainter painter) {
        ResourceLocation frontPicture = PageTextures.request(new Face(book, true), WIDE, HIGH, (pose, drawn) -> {
            pose.mulPose(RIGHT_PICTURE);
            painter.paint(pose, drawn, false);
        });
        ResourceLocation backPicture = PageTextures.request(new Face(book, false), WIDE, HIGH, (pose, drawn) -> {
            pose.mulPose(LEFT_PICTURE);
            painter.paint(pose, drawn, true);
        });
        if (frontPicture == null || backPicture == null) {
            return false;
        }
        CurledSheet.draw(poseStack, buffers, FRAME, new Vector3f(0.0F, height, 0.0F), new Vector3f(0.0F, 0.0F, PAGE_HEIGHT / 2.0F),
                PAGE_WIDTH, CurledSheet.angles(from, to, over), frontPicture, backPicture, light, 1.0F);
        return true;
    }
}
