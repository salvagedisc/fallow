// Adapted from Hands-On Vanilla by Pocky (MIT License, https://github.com/Pocky-l/handson-vanilla, branch 1.21.1,
// version 1.2.2). The licence travels with the mod: META-INF/LICENSE_handson_vanilla.
package io.github.salvagedisc.fallow.eveil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

import io.github.salvagedisc.fallow.Fallow;

/**
 * The book of the enchanting table where it is read: vanilla's {@code BookModel} with its pages a
 * pixel wider, laid out and turning as vanilla's (closed along +x from the spine, opening about y), so vanilla's animation drives it.
 * Thin boards (planes) 7 x 10 hinged a pixel off the middle, the spine a 2 x 10 plane turned square to them, blocks of pages 6 x 8
 * and a pixel thick (vanilla: boards 6 x 10, pages 5 x 8). The shape is Blockbench's Modded Entity export of Hands-On Vanilla's
 * {@code art/models/enchanting_book/enchanting_book.bbmodel}; the texture keeps its layout, drawn 16 times finer by
 * {@code scripts/books/covers.py} (violet leather, gilt corners on the outer side). The layer is built and baked here, so nothing
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

    /** The export, unchanged but for line breaks and {@code Mth.HALF_PI}: a part per piece, its cubes by texture offset, in pixels. */
    private static LayerDefinition createBodyLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        root.addOrReplaceChild("left_lid", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-7.0F, -5.0F, -0.005F, 7.0F, 10.0F, 0.005F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("right_lid", CubeListBuilder.create()
                .texOffs(18, 0).addBox(0.0F, -5.0F, -0.005F, 7.0F, 10.0F, 0.005F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        root.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(14, 0).addBox(-1.0F, -5.0F, 0.0F, 2.0F, 10.0F, 0.005F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F));
        root.addOrReplaceChild("left_pages", CubeListBuilder.create()
                .texOffs(0, 10).addBox(0.0F, -4.0F, -0.99F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_pages", CubeListBuilder.create()
                .texOffs(14, 10).addBox(0.0F, -4.0F, -0.01F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /** Opens the book: {@code open} is the boards' angle from shut, radians, as vanilla's {@code BookModel} takes it. */
    void setupAnim(float open) {
        leftLid.yRot = Mth.PI + open;
        rightLid.yRot = -open;
        leftPages.yRot = open;
        rightPages.yRot = -open;
        leftPages.x = Mth.sin(open);
        rightPages.x = Mth.sin(open);
    }

    void render(PoseStack poseStack, VertexConsumer consumer, int light, int overlay, float alpha) {
        root.render(poseStack, consumer, light, overlay, Math.round(alpha * 255.0F) << 24 | 0xFFFFFF);
    }
}
