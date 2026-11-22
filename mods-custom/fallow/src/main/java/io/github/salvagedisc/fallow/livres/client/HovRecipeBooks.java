package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

import io.github.salvagedisc.fallow.Fallow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Hands-On Vanilla's recipe books (the crafting table's, the stonecutter's, the loom's…) turn their sheets bent, as the stations'
 * cookbook does: its {@code spread} is drawn here while a sheet goes over ({@code mixin.livres.HovRecipeBookMixin}), the same as
 * its own but for the sheet going over. Its page and sheet types are its own and private, so they are reached by reflection; if
 * a version of the mod has changed them, this steps aside and the book turns its sheets flat, as it always did.
 */
public final class HovRecipeBooks {
    private static boolean looked;
    private static Method leafNow;
    private static Method side;
    private static Field flipFrom;
    private static Field shade;

    private HovRecipeBooks() {
    }

    private static boolean find(Class<?> book) {
        if (!looked) {
            looked = true;
            try {
                leafNow = book.getDeclaredMethod("leafNow");
                Class<?> leaf = leafNow.getReturnType();
                side = book.getDeclaredMethod("side", PoseStack.class, MultiBufferSource.class, boolean.class, float.class, float.class,
                        leaf, boolean.class);
                flipFrom = book.getDeclaredField("flipFrom");
                shade = book.getDeclaredField("shade");
                leafNow.setAccessible(true);
                side.setAccessible(true);
                flipFrom.setAccessible(true);
                shade.setAccessible(true);
            } catch (ReflectiveOperationException | RuntimeException e) {
                Fallow.LOGGER.warn("livres : les livres de Hands-On Vanilla tournent leurs pages à plat ({})", e.toString());
                side = null;
            }
        }
        return side != null;
    }

    /**
     * Draws the open pages of {@code book} while a sheet goes over ({@code flip} of the way, {@code forward} or back), as its own
     * {@code spread(poseStack, buffers, tilt, height)} does but with the sheet bent. False: nothing was drawn, the book draws them.
     */
    public static boolean spread(Object book, PoseStack poseStack, MultiBufferSource buffers, float tilt, float height, float flip,
            boolean forward) {
        if (!find(book.getClass())) {
            return false;
        }
        try {
            Object now = leafNow.invoke(book);
            Object fromLeaf = flipFrom.get(book);
            Object from = fromLeaf == null ? now : fromLeaf;
            Object back = forward ? now : from;
            int light = shade.getInt(book);
            float over = flip * flip * (3.0F - 2.0F * flip);
            float right = -tilt;
            float left = tilt - Mth.PI;
            boolean bent = RecipeSheets.turn(poseStack, buffers, light, height + RecipeSheets.SHEET_LIFT, forward ? right : left,
                    forward ? left : right, over, book, (pose, drawn, onLeft) -> paintSide(book, pose, drawn, onLeft, onLeft ? back : now));
            if (!bent) {
                return false;
            }
            // The resting pages as the book draws them: forward the old list stays on the left until the sheet has covered it.
            side.invoke(book, poseStack, buffers, true, tilt, height, forward ? from : now, true);
            side.invoke(book, poseStack, buffers, false, -tilt, height, now, true);
            return true;
        } catch (ReflectiveOperationException | RuntimeException e) {
            Fallow.LOGGER.warn("livres : les livres de Hands-On Vanilla tournent leurs pages à plat ({})", e.toString());
            side = null;
            return false;
        }
    }

    private static void paintSide(Object book, PoseStack poseStack, MultiBufferSource buffers, boolean left, Object leaf) {
        try {
            int lit = shade.getInt(book);
            shade.setInt(book, LightTexture.FULL_BRIGHT);
            try {
                side.invoke(book, poseStack, buffers, left, 0.0F, 0.0F, leaf, false);
            } finally {
                shade.setInt(book, lit);
            }
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
