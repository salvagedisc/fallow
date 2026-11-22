// Adapted from Hands-On Vanilla by Pocky (MIT License, https://github.com/Pocky-l/handson-vanilla, branch 1.21.1,
// version 1.2.2). The licence travels with the mod: META-INF/LICENSE_handson_vanilla.
package io.github.salvagedisc.fallow.eveil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.livres.client.BookShapes;

/**
 * The book of the enchanting table where it is read: vanilla's {@code BookModel} with its pages a
 * pixel wider, laid out and turning as vanilla's (closed along +x from the spine, opening about y), so vanilla's animation drives it.
 * Thin boards (planes) half a pixel larger than the pages all round, hinged a pixel off the middle shut and at the middle open
 * ({@link BookShapes#enchantingBook}), the spine a plane turned square to them, blocks of pages 6 x 8 and a pixel thick. The shape
 * is Hands-On Vanilla's enchanting book ({@code art/models/enchanting_book/enchanting_book.bbmodel}) with its lids cut to the pages;
 * the texture keeps its layout, drawn 16 times finer by {@code scripts/books/covers.py} (oxblood leather, gilt corners on the
 * outer side). The layer is built and baked here, so nothing
 * is registered.
 */
final class AwakeningBookModel {
    static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/entity/livre_d_eveil.png");

    private final ModelPart root;
    private final ModelPart leftLid;
    private final ModelPart rightLid;
    private final ModelPart leftPages;
    private final ModelPart rightPages;

    private AwakeningBookModel(ModelPart root) {
        this.root = root;
        leftLid = root.getChild("left_lid");
        rightLid = root.getChild("right_lid");
        leftPages = root.getChild("left_pages");
        rightPages = root.getChild("right_pages");
    }

    static AwakeningBookModel bake() {
        return new AwakeningBookModel(createBodyLayer().bakeRoot());
    }

    /** The shape, the lids cut to the pages ({@link BookShapes#enchantingBook}). */
    private static LayerDefinition createBodyLayer() {
        return BookShapes.enchantingBook();
    }

    /** Opens the book: {@code open} is the boards' angle from shut, radians, as vanilla's {@code BookModel} takes it. */
    void setupAnim(float open) {
        leftLid.yRot = Mth.PI + open;
        rightLid.yRot = -open;
        leftPages.yRot = open;
        rightPages.yRot = -open;
        leftPages.x = Mth.sin(open);
        rightPages.x = Mth.sin(open);
        BookShapes.enchantingLids(leftLid, rightLid, open);
    }

    void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float alpha) {
        root.render(poseStack, consumer, light, overlay, Math.round(alpha * 255.0F) << 24 | 0xFFFFFF);
    }
}
