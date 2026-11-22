package io.github.salvagedisc.fallow.livres.client;

import net.minecraft.util.Mth;

/**
 * The shade of a lectern-sized book's paper where it curves down into the fold: darkest at the fold, lifting smoothly to none
 * where the sheet lies flat, a little warm (paper in shadow yellows). Hands-On Vanilla shaded the curve in two flat bands of
 * colour; Fallow's awakening book takes the shade at each edge of its strips (so it runs smoothly across them), the lectern's
 * book (through {@code mixin.livres.HovOpenBookMixin}) at the middle of each of its four strips.
 */
public final class GutterShade {
    /** Pixels of the model from the fold to where the sheet lies flat (Hands-On Vanilla's {@code OpenBook.GUTTER}). */
    public static final float GUTTER = 0.9F;

    private GutterShade() {
    }

    /** The colour (0xRRGGBB, multiplied into the paper) {@code along} pixels from the fold. */
    public static int at(float along) {
        float t = Mth.clamp(along / GUTTER, 0.0F, 1.0F);
        float k = 0.84F + 0.16F * (float) Math.pow(t, 0.75);
        int r = Math.round(255.0F * k);
        int g = Math.round(255.0F * (k - (1.0F - k) * 0.06F));
        int b = Math.round(255.0F * (k - (1.0F - k) * 0.22F));
        return r << 16 | g << 8 | b;
    }
}
