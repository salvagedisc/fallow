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
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import io.github.salvagedisc.fallow.livres.client.DogEar;
import io.github.salvagedisc.fallow.livres.client.HovRecipeBooks;
import io.github.salvagedisc.fallow.livres.client.HovSheetState;
import io.github.salvagedisc.fallow.livres.client.PageParts;

/**
 * Les pages des livres de recettes de Hands-On Vanilla : leurs flèches ne sont plus dessinées, le coin corné l'est sur le papier
 * de chaque page qui a une page à tourner de son côté ({@link HovRecipeBooks#dogEar}) une fois la page finie, plus petit et
 * par-dessus si ce qu'elle montre atteint le coin (une recette de trois rangs), et une partie du mobilier de la page (cases,
 * carte d'un groupe, filet sous le nom, marque des nouveautés) est dessinée par {@link PageParts}. Le papier des onglets, la
 * fiche de recherche et le cadre de la case choisie restent ceux de Hands-On Vanilla ; l'icône d'un onglet est plus petite et
 * tient dedans, et la loupe est l'icône de recherche du jeu. Les cases, la carte, le filet et la marque passent par {@code rect} : on les reconnaît à leur couleur,
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
    /** Hands-On Vanilla 1.2.2's tabs: their height over the page, behind it and open; the search tab's width. */
    private static final float TAB_REST = 11.0F;
    private static final float TAB_OPEN = 14.0F;
    private static final float SEARCH_TAB_WIDE = 13.0F;
    /** Hands-On Vanilla 1.2.2's search slip: its height over the page, and where it puts the magnifier on it. */
    private static final float SLIP_HIGH = 11.0F;
    private static final float SLIP_MAGNIFIER_Y = -9.0F;
    /** The texture width the slip's paper is read against: 128 would take it to the page's edge (see fallow$papierDeLaFiche). */
    private static final float SLIP_PAPER = 160.0F;
    /** The count's size, and its gap from the dish. */
    private static final float COUNT_SCALE = 0.75F;
    private static final float COUNT_GAP = 1.0F;
    /** As Hands-On Vanilla writes on its pages: at the icons' lift. */
    private static final float COUNT_LIFT = 1.2F;
    /** Hands-On Vanilla 1.2.2's well behind one holding variants: how far behind, and how far over the paper. */
    private static final float STACK_OFFSET = 1.5F;
    private static final float STACK_LIFT = 0.15F;
    /** The mark of variants drawn over the well (0.3), under the icon. */
    private static final float WELL_LIFT_OVER = 0.32F;
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
    abstract void tab(float x, float wide, float iconSize, boolean chosen, boolean lit, ItemStack icon, float nod, boolean washed,
            boolean marked);

    /** While Hands-On Vanilla draws a tab's paper for {@link #fallow$onglet}. */
    @Unique
    private boolean fallow$papierDOnglet;

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

    /**
     * The search slip, as Hands-On Vanilla draws it, its paper taken from a narrower band of the page's texture: at its full width
     * it reached the page's outer edge in Fallow's paper and showed it as a line across the slip.
     */
    @ModifyConstant(method = "slip", constant = @Constant(floatValue = 128.0F, ordinal = 0), remap = false, require = 0)
    private float fallow$papierDeLaFiche(float atlas) {
        return SLIP_PAPER;
    }

    /**
     * A tab: its paper and line as Hands-On Vanilla draws them, its icon smaller ({@link PageParts#tabIcon}) and in the middle of
     * the part of the tab over the page, in full colour, so it never runs past the tab; the mark of something new on its top
     * right corner.
     */
    @Inject(method = "tab", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$onglet(float x, float wide, float iconSize, boolean chosen, boolean lit, ItemStack icon, float nod,
            boolean washed, boolean marked, CallbackInfo callback) {
        if (fallow$papierDOnglet || !HovRecipeBooks.dogEarsReady(this)) {
            return;
        }
        fallow$papierDOnglet = true;
        try {
            tab(x, wide, iconSize, chosen, lit, ItemStack.EMPTY, nod, false, false);
        } finally {
            fallow$papierDOnglet = false;
        }
        float high = chosen ? TAB_OPEN : TAB_REST;
        if (washed) {
            // The filter on: washed with the green of what can be made, down to the page's edge, never over the page.
            rect(x + 1.0F, -high + 1.0F, wide - 2.0F, high - 1.0F, 0.08F, WELL_READY);
        }
        if (!icon.isEmpty()) {
            float size = PageParts.tabIcon(wide);
            item(icon, x + (wide - size) / 2.0F, PageParts.tabIconTop(high, size) - nod, size, false);
        }
        if (marked) {
            PageParts.mark(poseStack, buffers, HovRecipeBooks.light(this), x + wide - 3.0F, -high - nod + 3.0F, 1.95F);
        }
        callback.cancel();
    }

    /**
     * The magnifier: the game's own search icon instead of Hands-On Vanilla's few strokes of ink, on the search's tab (in the
     * middle of it, as a tab's icon) and at the head of the search slip.
     */
    @Inject(method = "magnifier", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$loupe(float x, float y, CallbackInfo callback) {
        if (!HovRecipeBooks.dogEarsReady(this)) {
            return;
        }
        float size = PageParts.SEARCH_ICON;
        int light = HovRecipeBooks.light(this);
        if (y == SLIP_MAGNIFIER_Y) {
            // On the slip, a unit in from its edge and clear of the words typed after it.
            PageParts.search(poseStack, buffers, light, x + 1.0F, -SLIP_HIGH + (SLIP_HIGH + 1.0F - size) / 2.0F, size, 1.9F);
        } else {
            // Hands-On Vanilla puts it 3 units in from its tab's left side and 3 under the tab's head.
            float high = 3.0F - y;
            PageParts.search(poseStack, buffers, light, x - 3.0F + (SEARCH_TAB_WIDE - size) / 2.0F, PageParts.tabIconTop(high, size), size,
                    1.9F);
        }
        callback.cancel();
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
     * l'icône, plus sur son coin, où l'icône d'un bloc, dessinée après, le cachait.
     */
    @Inject(method = "text(Ljava/lang/String;FF)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void fallow$nombre(String text, float x, float y, CallbackInfo callback) {
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
