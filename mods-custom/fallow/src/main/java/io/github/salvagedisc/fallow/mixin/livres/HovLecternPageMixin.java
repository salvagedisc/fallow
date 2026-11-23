package io.github.salvagedisc.fallow.mixin.livres;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/**
 * Les pages du livre du pupitre de Hands-On Vanilla, mises comme celles des autres livres du pack : le texte commence un peu plus
 * bas (Hands-On Vanilla le pose à 12 unités du haut de la page, il commence ici à 20, où la courbure du papier le serre contre la tête), et les numéros
 * de page, rouge et gris chez lui, sont dans les encres des livres : l'encre douce pour la page ouverte, la pâle pour l'autre.
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
