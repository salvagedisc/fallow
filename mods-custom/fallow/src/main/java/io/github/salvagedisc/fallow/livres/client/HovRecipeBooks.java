package io.github.salvagedisc.fallow.livres.client;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.Mth;

import io.github.salvagedisc.fallow.Fallow;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.WeakHashMap;

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

    /** A page's size, in its units. */
    public static final float UNITS_WIDE = 72.0F;
    public static final float UNITS_HIGH = 96.0F;
    /** How far right what each book's right page, as last drawn at rest, shows reaches into its turned-down corner (none: 0). */
    private static final Map<Object, Float> TAKEN = new WeakHashMap<>();

    /**
     * The turned-down corner under the cursor ({@code x} across both pages, {@code y} down): -1 the left page's, 1 the right page's,
     * 0 none; a corner is there only where there is a page to turn to. The right page's reaches less far when what the page shows
     * is in it ({@link DogEar#SMALL_REACH}), so what is there still answers the cursor.
     */
    public static int footSide(Object book, float x, float y, int page, int pages) {
        if (pages <= 1 || y >= UNITS_HIGH) {
            return 0;
        }
        if (x >= 0.0F && x < DogEar.REACH && y >= UNITS_HIGH - DogEar.REACH && page > 0) {
            return -1;
        }
        float edge = TAKEN.getOrDefault(book, 0.0F);
        float reach = edge > 0.0F ? DogEar.SMALL_REACH : DogEar.REACH;
        return x >= UNITS_WIDE + DogEar.reachFrom(edge, UNITS_WIDE) && x < UNITS_WIDE * 2 && y >= UNITS_HIGH - reach
                && page < pages - 1 ? 1 : 0;
    }

    private static boolean sheetLooked;
    private static Field outer;
    private static Field page;
    private static Field cursorX;
    private static Field cursorY;
    private static Method pages;

    /** Whether a page of a recipe book has been seen drawn through {@code mixin.livres.HovRecipeSheetMixin}. */
    private static boolean pageSeen;

    /**
     * Whether the pages turn by their corners: only once the corners are drawn on a page (the sheet's mixin applied and the book's
     * members found), else the book's arrows stay and turn its pages as they did.
     */
    public static boolean cornersOn() {
        return pageSeen && outer != null;
    }

    /** Whether the corners can be drawn on the pages of the book that {@code sheet} paints: else its arrows stay. */
    public static boolean dogEarsReady(Object sheet) {
        if (!sheetLooked) {
            sheetLooked = true;
            try {
                outer = sheet.getClass().getDeclaredField("this$0");
                Class<?> book = outer.getType();
                page = book.getDeclaredField("page");
                cursorX = book.getDeclaredField("cursorX");
                cursorY = book.getDeclaredField("cursorY");
                pages = book.getDeclaredMethod("pages");
                if (shade == null) {
                    shade = book.getDeclaredField("shade");
                    shade.setAccessible(true);
                }
                for (var member : new java.lang.reflect.AccessibleObject[] {outer, page, cursorX, cursorY, pages}) {
                    member.setAccessible(true);
                }
            } catch (ReflectiveOperationException | RuntimeException e) {
                Fallow.LOGGER.warn("livres : les livres de Hands-On Vanilla gardent leurs flèches ({})", e.toString());
                outer = null;
            }
        }
        return outer != null;
    }

    /**
     * The turned-down corner on the page {@code sheet} has drawn: the left page's when a page comes before, the right page's when
     * one follows; lifted under the cursor. {@code taken}: how far right what the page shows reaches into the corner (0: nothing
     * does); the corner is then smaller and over it ({@link DogEar#SMALL}). {@code resting}: the page lies open (not a sheet going over), so the corner it has is the one
     * the cursor finds.
     */
    public static void dogEar(Object sheet, PoseStack poseStack, MultiBufferSource buffers, boolean left, float taken, boolean resting) {
        if (!dogEarsReady(sheet)) {
            return;
        }
        try {
            Object book = outer.get(sheet);
            if (!left && resting) {
                TAKEN.put(book, taken);
            }
            int now = page.getInt(book);
            int count = (int) pages.invoke(book);
            if (left ? now <= 0 : now >= count - 1) {
                return;
            }
            boolean under = resting && footSide(book, cursorX.getFloat(book), cursorY.getFloat(book), now, count) == (left ? -1 : 1);
            float size = taken > 0.0F && !under ? DogEar.SMALL : DogEar.SIZE;
            DogEar.draw(poseStack, buffers, shade.getInt(book), left ? 0.0F : UNITS_WIDE, UNITS_HIGH, size, left, under,
                    DogEar.Paper.CREAM, taken > 0.0F ? DogEar.OVER : DogEar.LIFT);
        } catch (ReflectiveOperationException | RuntimeException e) {
            Fallow.LOGGER.warn("livres : les livres de Hands-On Vanilla gardent leurs flèches ({})", e.toString());
            outer = null;
        }
    }

    /** The light the book that {@code sheet} paints is drawn in. */
    public static int light(Object sheet) {
        try {
            return shade.getInt(outer.get(sheet));
        } catch (ReflectiveOperationException | RuntimeException e) {
            return net.minecraft.client.renderer.LightTexture.FULL_BRIGHT;
        }
    }

    /** The canvas of the page being drawn, from the paper it lays until the page is done. */
    private static Object current;

    /** A page of a book starts: {@code sheet} has laid its paper. */
    public static void pageStarted(Object sheet) {
        current = sheet;
        pageSeen = true;
    }

    /** The page started last is done: its turned-down corner goes on it now, over or beside what it shows. */
    public static void pageDrawn(PoseStack poseStack, MultiBufferSource buffers, boolean resting) {
        Object sheet = current;
        current = null;
        if (sheet instanceof HovSheetState state) {
            dogEar(sheet, poseStack, buffers, state.fallow$left(), state.fallow$taken(), resting);
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
