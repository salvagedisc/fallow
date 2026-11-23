package io.github.salvagedisc.fallow.mixin.livres;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.DogEar;
import io.github.salvagedisc.fallow.livres.client.HovRecipeBooks;
import io.github.salvagedisc.fallow.livres.client.HovSheetState;
import io.github.salvagedisc.fallow.livres.client.PageParts;

/**
 * Les pages des livres de recettes de Hands-On Vanilla : leurs flèches ne sont plus dessinées, le coin corné l'est sur le papier
 * de chaque page qui a une page à tourner de son côté ({@link HovRecipeBooks#dogEar}) une fois la page finie, plus petit et
 * par-dessus si ce qu'elle montre atteint le coin (une recette de trois rangs), et le mobilier de la page (onglets, fiche
 * de recherche, loupe, cases, cadre de la case choisie, carte d'un groupe, filet sous le nom, marque des nouveautés) est dessiné
 * par {@link PageParts}. Les cases, la carte, le filet et la marque passent par {@code rect} : on les reconnaît à leur couleur,
 * celles de Hands-On Vanilla 1.2.2 ; les deux bandes du pli aussi, remplacées par l'ombre de la gouttière.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.RecipeBook$Sheet", remap = false)
abstract class HovRecipeSheetMixin implements HovSheetState {
    private static final int WELL = 0x1E5A3C18;
    private static final int WELL_READY = 0x603D7845;
    private static final int CARD = 0x165A3C18;
    private static final int RULE = 0x405A3C18;
    private static final int FRESH = 0xFFB5372B;
    private static final int FOLD = 0x285A3C18;
    /** The count of a dish: the books' ink. */
    private static final int COUNT_INK = 0xFF3B2A1A;
    /** Where Hands-On Vanilla 1.2.2 writes the count of a dish: its top, DISH_Y + DISH_SIZE - the line's height + 2. */
    private static final float DISH_COUNT_Y = 2.0F + 16.0F - 9.0F + 2.0F;
    /** The count's size, and its gap from the dish. */
    private static final float COUNT_SCALE = 0.75F;
    private static final float COUNT_GAP = 1.0F;
    /** As Hands-On Vanilla writes on its pages: at the icons' lift. */
    private static final float COUNT_LIFT = 1.2F;
    /** The slip starts this far in from the page's edge. */
    private static final float SLIP_INSET = 2.0F;
    /** Where Hands-On Vanilla 1.2.2 writes what is typed in the slip. */
    private static final float SLIP_TEXT_X = 13.0F;
    private static final float SLIP_TEXT_Y = -9.0F;
    /** Hands-On Vanilla 1.2.2's well behind one holding variants: how far behind, and how far over the paper. */
    private static final float STACK_OFFSET = 1.5F;
    private static final float STACK_LIFT = 0.15F;
    /** The mark of variants drawn over the well (0.3), under the icon. */
    private static final float WELL_LIFT_OVER = 0.32F;
    /** A tab's height over the page: behind it, and open. */
    private static final float TAB_REST = 12.0F;
    private static final float TAB_OPEN = 14.0F;
    /** How far under a tab's head what it shows begins: its line and a unit of paper. */
    private static final float TAB_HEAD = 2.0F;
    /** Where Hands-On Vanilla 1.2.2 puts the magnifier of its search slip. */
    private static final float SLIP_MAGNIFIER_Y = -9.0F;
    /** Where Hands-On Vanilla 1.2.2 puts the magnifier of the search's tab behind the page. */
    private static final float SEARCH_REST_Y = -8.0F;
    /** Where Hands-On Vanilla 1.2.2 puts the filter's tab, on the right page. */
    private static final float FILTER_X = 6.0F;
    /** How much lower than Hands-On Vanilla's the rule under a name is drawn. */
    private static final float RULE_DROP = 1.0F;

    @Unique
    private boolean fallow$left;
    @Unique
    private float fallow$taken;

    @Shadow(remap = false)
    @Final
    private PoseStack poseStack;
    @Shadow(remap = false)
    @Final
    private MultiBufferSource buffers;

    @Shadow(remap = false)
    abstract void item(ItemStack stack, float x, float y, float size, boolean faded);

    @Shadow(remap = false)
    abstract void rect(float x, float y, float width, float height, float lift, int argb);

    @Shadow(remap = false)
    abstract void text(String text, float x, float y);

    @Inject(method = "arrow", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$sansFleche(float x, float y, boolean next, boolean over, CallbackInfo callback) {
        if (HovRecipeBooks.dogEarsReady(this)) {
            callback.cancel();
        }
    }

    /** Le papier posé : une page commence ; son coin corné viendra quand elle sera finie ({@link HovRecipeBooks#pageDrawn}). */
    @Inject(method = "paper", at = @At("TAIL"), remap = false, require = 0)
    private void fallow$page(boolean left, CallbackInfo callback) {
        fallow$left = left;
        fallow$taken = 0.0F;
        HovRecipeBooks.pageStarted(this);
    }

    @Override
    public boolean fallow$left() {
        return fallow$left;
    }

    @Override
    public float fallow$taken() {
        return fallow$taken;
    }

    /** Ce qui est dessiné dans le coin de la page de droite : le coin s'y fera plus petit, par-dessus. */
    private void fallow$occupe(float x, float y, float wide, float high) {
        if (!fallow$left && DogEar.inCorner(x, y, wide, high, HovRecipeBooks.UNITS_WIDE, HovRecipeBooks.UNITS_HIGH)) {
            fallow$taken = Math.max(fallow$taken, x + wide);
        }
    }

    @Inject(method = "item(Lnet/minecraft/world/item/ItemStack;FFFZ)V", at = @At("HEAD"), remap = false, require = 0)
    private void fallow$icone(ItemStack stack, float x, float y, float size, boolean faded, CallbackInfo callback) {
        fallow$occupe(x, y, size, size);
    }

    @Inject(method = "text(Lnet/minecraft/util/FormattedCharSequence;FF)V", at = @At("HEAD"), remap = false, require = 0)
    private void fallow$texte(FormattedCharSequence text, float x, float y, CallbackInfo callback) {
        fallow$occupe(x, y, Minecraft.getInstance().font.width(text), Minecraft.getInstance().font.lineHeight);
    }

    @Inject(method = "tab", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$onglet(float x, float wide, float iconSize, boolean chosen, boolean lit, ItemStack icon, float nod,
            boolean washed, boolean marked, CallbackInfo callback) {
        if (!HovRecipeBooks.dogEarsReady(this)) {
            return;
        }
        int light = HovRecipeBooks.light(this);
        float high = chosen ? TAB_OPEN : TAB_REST;
        // Every tab stops at the page's edge: nothing of them lies on the page. The filter, when on, is drawn as the open tab
        // (the page's paper): no colour of its own.
        PageParts.tab(poseStack, buffers, light, x, -high - nod, wide, 0.0F, chosen ? 0.25F : 0.05F,
                chosen || washed ? PageParts.Tab.OPEN : lit ? PageParts.Tab.LIT : PageParts.Tab.REST);
        if (marked) {
            // Something new behind it: its head in the rubric red, clear of what it shows.
            PageParts.cap(poseStack, buffers, light, x, -high - nod, wide, chosen ? 0.3F : 0.1F);
        }
        // What it shows sits two units under its head, in the middle of its width: a pictogram of the book's inks when the tab is
        // one Hands-On Vanilla's books know (the filter of the right page among them), else the item, as small, inside it.
        float top = -high - nod + TAB_HEAD;
        PageParts.Glyph glyph = !fallow$left && x == FILTER_X ? PageParts.Glyph.FILTER : HovRecipeBooks.glyph(icon);
        if (glyph != null) {
            PageParts.glyph(poseStack, buffers, light, glyph, x + (wide - PageParts.GLYPH) / 2.0F, top, chosen ? 0.35F : 0.15F);
        } else if (!icon.isEmpty()) {
            float size = Math.min(iconSize, PageParts.GLYPH);
            item(icon, x + (wide - size) / 2.0F, top + (PageParts.GLYPH - size) / 2.0F, size, false);
        }
        callback.cancel();
    }

    @Inject(method = "slip", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$fiche(float x, float wide, float high, boolean lit, CallbackInfo callback) {
        if (HovRecipeBooks.dogEarsReady(this)) {
            // The slip starts on the page, two units in from its outer edge, never over the cover.
            float from = Math.max(x, SLIP_INSET);
            PageParts.slip(poseStack, buffers, HovRecipeBooks.light(this), from, -high, wide - (from - x), 0.0F, 0.25F, lit);
            callback.cancel();
        }
    }

    @Inject(method = "magnifier", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$loupe(float x, float y, CallbackInfo callback) {
        if (HovRecipeBooks.dogEarsReady(this)) {
            // On the search's tab behind the page (Hands-On Vanilla puts it 3 under an 11 high tab's head), it keeps to where the
            // pictograms of the other tabs sit.
            float at = y == SEARCH_REST_Y ? -TAB_REST + TAB_HEAD + 1.0F : y;
            // On the search slip, half a unit right: a unit clear of the slip's edge and of the words typed after it.
            float from = y == SLIP_MAGNIFIER_Y ? x + 0.5F : x;
            PageParts.magnifier(poseStack, buffers, HovRecipeBooks.light(this), from, at, 1.9F);
            callback.cancel();
        }
    }

    @Inject(method = "frame", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$cadre(float x, float y, float size, CallbackInfo callback) {
        if (HovRecipeBooks.dogEarsReady(this)) {
            PageParts.frame(poseStack, buffers, HovRecipeBooks.light(this), x, y, size, 1.9F);
            callback.cancel();
        }
    }

    @Inject(method = "outline", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$contour(float x, float y, float width, float height, float lift, int argb, CallbackInfo callback) {
        // Only the card of a group has an outline; the card draws its own.
        if (argb == RULE && HovRecipeBooks.dogEarsReady(this)) {
            callback.cancel();
        }
    }

    @Inject(method = "rect", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$rectangle(float x, float y, float width, float height, float lift, int argb, CallbackInfo callback) {
        if (argb == WELL || argb == WELL_READY) {
            fallow$occupe(x, y, width, height);
        }
        if (argb != WELL && argb != WELL_READY && argb != CARD && argb != RULE && argb != FRESH && argb != FOLD) {
            return;
        }
        if (!HovRecipeBooks.dogEarsReady(this)) {
            return;
        }
        int light = HovRecipeBooks.light(this);
        if ((argb == WELL || argb == WELL_READY) && width == 17.0F && height == 17.0F && lift == STACK_LIFT) {
            // Hands-On Vanilla's second well a unit and a half behind one holding variants reached into the gap and touched the
            // well beside it: a second rim inside the well instead.
            PageParts.stack(poseStack, buffers, light, x - STACK_OFFSET, y - STACK_OFFSET, width, WELL_LIFT_OVER);
        } else if ((argb == WELL || argb == WELL_READY) && width == 17.0F && height == 17.0F) {
            PageParts.well(poseStack, buffers, light, x, y, width, lift, argb == WELL_READY);
        } else if (argb == CARD) {
            PageParts.card(poseStack, buffers, light, x, y, width, height, lift);
        } else if (argb == RULE && height < 1.0F) {
            // A unit lower than the book's: clear of the descenders of the name above it.
            PageParts.rule(poseStack, buffers, light, x, y + height / 2.0F + RULE_DROP, width, lift);
        } else if (argb == FRESH && width == 3.0F && height == 1.0F) {
            PageParts.mark(poseStack, buffers, light, x + 1.5F, y + 0.5F, 1.95F);
        } else if (argb == FOLD && height == 96.0F && width > 2.0F) {
            // The gutter's shade, from the wider of the two bands the book lays along the fold (its left page's: at x 67).
            PageParts.gutter(poseStack, buffers, light, x > 0.0F, 72.0F, 96.0F, 0.15F);
        } else if (argb == FOLD && height == 96.0F) {
            // The narrower band: the gutter's shade has its line.
        } else if (argb == FRESH && width == 1.0F && height == 3.0F) {
            // The upright stroke of the cross the mark replaces.
        } else {
            return;
        }
        callback.cancel();
    }

    /**
     * Le nombre que fait une recette, au pied du plat (le seul texte fait de chiffres écrit à cette hauteur) : juste à droite de
     * l'icône, plus sur son coin, où l'icône d'un bloc, dessinée après, le cachait. Et ce qui est tapé dans la fiche de recherche,
     * une unité plus haut.
     */
    @Inject(method = "text(Ljava/lang/String;FF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$nombre(String text, float x, float y, CallbackInfo callback) {
        if (x == SLIP_TEXT_X && y == SLIP_TEXT_Y && HovRecipeBooks.dogEarsReady(this)) {
            // What is typed in the slip, a unit higher: a "p" or a "g" no longer sits on its lower line.
            text(text, x, y - 1.0F);
            callback.cancel();
            return;
        }
        if (y != DISH_COUNT_Y || text.isEmpty() || !text.chars().allMatch(Character::isDigit) || !HovRecipeBooks.dogEarsReady(this)) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        // Small, a unit right of the dish's foot: Hands-On Vanilla writes it over the icon's corner, ending 2 units past it.
        float left = x + font.width(text) - 2.0F + COUNT_GAP;
        float top = DISH_COUNT_Y - 2.0F + font.lineHeight - font.lineHeight * COUNT_SCALE + 1.0F;
        Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(left, top, COUNT_LIFT).scale(COUNT_SCALE, COUNT_SCALE, 1.0F);
        font.drawInBatch(text, 0.0F, 0.0F, COUNT_INK, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, HovRecipeBooks.light(this));
        callback.cancel();
    }
}
