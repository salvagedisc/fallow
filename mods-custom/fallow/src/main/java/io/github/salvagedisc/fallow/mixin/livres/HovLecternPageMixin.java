package io.github.salvagedisc.fallow.mixin.livres;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * Les pages du livre du pupitre de Hands-On Vanilla, mises comme celles des autres livres du pack : le texte commence un peu plus
 * bas (Hands-On Vanilla le pose à 12 unités du haut de la page, il commence ici à 20, où la courbure du papier le serre contre la tête), et les numéros
 * de page, rouge et gris chez lui, sont dans les encres des livres : l'encre douce pour la page ouverte, la pâle pour l'autre.
 * Le texte et le numéro sont aussi centrés sur la page qu'on voit, non sur sa partie plate : le papier écrit commence après la
 * courbe de la gouttière (près d'un pixel du modèle, 27 unités), si bien que chez Hands-On Vanilla le texte de chaque page penche
 * vers la tranche ; ici la page de gauche écrit à 34 unités de sa tranche et celle de droite à 7 de la courbe, et chaque numéro
 * est décalé d'autant.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.LecternBehavior", remap = false)
abstract class HovLecternPageMixin {
    @Unique
    private static final float FALLOW$TEXT_TOP = 20.0F;
    @Unique
    private static final int FALLOW$OWN_PAGE = 0x8A6B45;
    @Unique
    private static final int FALLOW$OTHER_PAGE = 0xB9A27C;

    /** Où commence le texte de la page de gauche (depuis sa tranche) et de la page de droite (depuis la courbe du pli). */
    @Unique
    private static final float FALLOW$LEFT_X = 34.0F;
    @Unique
    private static final float FALLOW$RIGHT_X = 7.0F;
    /** Hands-On Vanilla 1.2.2 : où il commence, et la largeur d'une page sur laquelle il centre le numéro. */
    @Unique
    private static final float FALLOW$HOV_X = 20.0F;
    @Unique
    private static final int FALLOW$HOV_WIDE = 155;
    @Unique
    private boolean fallow$droite;
    @Unique
    private static Field fallow$hit;
    @Unique
    private static Method fallow$right;

    @ModifyVariable(method = "paintPage", at = @At("HEAD"), argsOnly = true, ordinal = 0, remap = false, require = 0)
    private int fallow$page(int page) {
        fallow$droite = Math.floorMod(page, 2) == 1;
        return page;
    }

    @ModifyConstant(method = "paintPage", constant = @Constant(floatValue = 20.0F), remap = false, require = 0)
    private float fallow$gaucheDuTexte(float x) {
        return fallow$droite ? FALLOW$RIGHT_X : FALLOW$LEFT_X;
    }

    /** Le numéro, centré sur la largeur qu'on lui donne : décalé comme le texte. */
    @ModifyConstant(method = "paintPage", constant = @Constant(intValue = 155), remap = false, require = 0)
    private int fallow$milieuDuNumero(int wide) {
        float decale = (fallow$droite ? FALLOW$RIGHT_X : FALLOW$LEFT_X) - FALLOW$HOV_X;
        return FALLOW$HOV_WIDE + Math.round(2.0F * decale);
    }

    /** Sous le curseur, la page est celle du point visé ({@code hit}, que le pupitre passe à {@code styleAt}). */
    @ModifyConstant(method = "styleAt", constant = @Constant(floatValue = 20.0F), remap = false, require = 0)
    private float fallow$gaucheDuTexteSousLeCurseur(float x) {
        try {
            if (fallow$hit == null) {
                fallow$hit = getClass().getDeclaredField("hit");
                fallow$hit.setAccessible(true);
            }
            Object hit = fallow$hit.get(this);
            if (hit == null) {
                return x;
            }
            if (fallow$right == null) {
                fallow$right = hit.getClass().getDeclaredMethod("right");
                fallow$right.setAccessible(true);
            }
            return (Boolean) fallow$right.invoke(hit) ? FALLOW$RIGHT_X : FALLOW$LEFT_X;
        } catch (ReflectiveOperationException | RuntimeException e) {
            return x;
        }
    }

    @ModifyConstant(method = "paintPage", constant = @Constant(floatValue = 12.0F), remap = false, require = 0)
    private float fallow$hautDuTexte(float top) {
        return FALLOW$TEXT_TOP;
    }

    /** Où le pupitre cherche ce qui est sous le curseur (un lien, une infobulle) : les lignes là où elles sont écrites. */
    @ModifyConstant(method = "styleAt", constant = @Constant(floatValue = 12.0F), remap = false, require = 0)
    private float fallow$hautDuTexteSousLeCurseur(float top) {
        return FALLOW$TEXT_TOP;
    }

    @ModifyConstant(method = "paintPage", constant = @Constant(intValue = 11141120), remap = false, require = 0)
    private int fallow$pageOuverte(int colour) {
        return FALLOW$OWN_PAGE;
    }

    @ModifyConstant(method = "paintPage", constant = @Constant(intValue = 7039851), remap = false, require = 0)
    private int fallow$autrePage(int colour) {
        return FALLOW$OTHER_PAGE;
    }
}
