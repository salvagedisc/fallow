package io.github.salvagedisc.fallow.mixin.livres;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import io.github.salvagedisc.fallow.livres.client.BookShapes;

/**
 * Hands-On Vanilla's recipe book: its boards cut to its pages, half a pixel beyond them all round, their hinges coming in to
 * the middle as it opens ({@link BookShapes#recipeBook}); the texture the pack gives it is drawn for that shape.
 */
@Pseudo
@Mixin(targets = "com.pockyl.handson_vanilla.client.RecipeBookModel", remap = false)
abstract class HovRecipeBookModelMixin {
    @Shadow(remap = false)
    @Final
    private ModelPart frontCover;
    @Shadow(remap = false)
    @Final
    private ModelPart backCover;

    @Inject(method = "createBodyLayer", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void fallow$forme(CallbackInfoReturnable<LayerDefinition> callback) {
        callback.setReturnValue(BookShapes.recipeBook());
    }

    @Inject(method = "setupAnim", at = @At("TAIL"), remap = false, require = 0)
    private void fallow$plats(float angle, CallbackInfo callback) {
        BookShapes.recipeCovers(frontCover, backCover, angle);
    }
}
