// The recipe book's and the enchanting book's shapes are adapted from Hands-On Vanilla by Pocky (MIT License,
// https://github.com/Pocky-l/handson-vanilla, branch 1.21.1, version 1.2.2). The licence travels with the mod:
// META-INF/LICENSE_handson_vanilla.
package io.github.salvagedisc.fallow.livres.client;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.util.Mth;

/**
 * The shapes of the books worked in the world, the boards cut to their pages. Hands-On Vanilla's boards stand a pixel and more
 * beyond the paper all round (10 pixels high for pages of 8, hinged a pixel out from the middle), so an open book showed a wide
 * frame of cloth round small pages. Here the boards are half a pixel larger than the pages on every side, as a bound book's
 * squares are, and their hinges come in to the middle as the book opens (they stay a pixel out, round the pages, while it is
 * shut): open, the boards lie under the pages with their half pixel showing beyond them; shut, they still close over the
 * block of pages. The spine is the boards' height, with headbands at its ends. Texture layouts (64 x 32 units) in
 * {@code scripts/books/covers.py}.
 * Used by Fallow's own books and, through {@code mixin.livres}, by Hands-On Vanilla's.
 */
public final class BookShapes {
    /** How far the boards stand beyond the pages, in pixels of the model, on the three outer sides. */
    public static final float SQUARE = 0.5F;
    /**
     * The recipe book's boards: from an eighth past the hinge to a square beyond the 6 pixels of pages (a little more, so the
     * face is a whole number of the texture's texels, 80 at 12 to a pixel).
     */
    public static final float RECIPE_BOARD_WIDE = 80.0F / 12.0F;
    /** Every board's height: 8 pixels of pages and a square above and below. */
    public static final float BOARD_HIGH = 8.0F + 2.0F * SQUARE;
    /**
     * The spine's height: the boards'. Its ends show between the pages at the head and the foot of the fold: the texture draws
     * headbands there (a dark sliver of leather before, among the tabs; cut to the pages, the spine left a gap there instead).
     */
    public static final float SPINE_HIGH = BOARD_HIGH;
    /**
     * How much the recipe book's spine stops short of the boards at each end (its texture keeps the boards' height), so its
     * head and foot do not lie in the plane of the open boards' edges under the fold and flicker against them.
     */
    private static final float SPINE_INSET = 0.01F;
    /** The enchanting book's lids: 6 pixels of pages and a square beyond. */
    public static final float LID_WIDE = 6.0F + SQUARE;

    private BookShapes() {
    }

    /**
     * Hands-On Vanilla's recipe book (and Fallow's copy): a spine of three layers, two boards, two blocks of pages and two loose
     * pages, each a part (closed along +x from the spine, the front cover on +z, opening about y).
     */
    public static LayerDefinition recipeBook() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float half = BOARD_HIGH / 2.0F;
        float spine = SPINE_HIGH / 2.0F;
        root.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(28, 0).addBox(0.0F, -spine, -1.0F, 0.25F, SPINE_HIGH, 2.0F, new CubeDeformation(0.0F, -SPINE_INSET, 0.0F))
                .texOffs(0, 12).addBox(0.3125F, -spine, -1.0F, 0.0F, SPINE_HIGH, 2.0F, new CubeDeformation(0.0625F, -SPINE_INSET, 0.0625F))
                .texOffs(6, 12).addBox(0.4375F, -spine, -0.5F, 0.0F, SPINE_HIGH, 1.0F, new CubeDeformation(0.0625F, -SPINE_INSET, 0.0625F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        // A board: a flat box made a board's thickness only (its faces exactly the board's size, so the texture's net is exact).
        CubeDeformation board = new CubeDeformation(0.0F, 0.0F, 0.135F);
        root.addOrReplaceChild("front_cover", CubeListBuilder.create()
                .texOffs(0, 1).addBox(0.125F - RECIPE_BOARD_WIDE, -half, 0.125F, RECIPE_BOARD_WIDE, BOARD_HIGH, 0.0F, board),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        root.addOrReplaceChild("back_cover", CubeListBuilder.create()
                .texOffs(14, 1).addBox(0.125F - RECIPE_BOARD_WIDE, -half, -0.125F, RECIPE_BOARD_WIDE, BOARD_HIGH, 0.0F, board),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("front_pages", CubeListBuilder.create()
                .texOffs(34, 0).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("back_pages", CubeListBuilder.create()
                .texOffs(49, 0).addBox(-6.0F, -4.0F, -1.0F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("front_flip", CubeListBuilder.create()
                .texOffs(34, 10).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("back_flip", CubeListBuilder.create()
                .texOffs(49, 10).addBox(-6.0F, -4.0F, 0.0F, 6.0F, 8.0F, 0.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * The recipe book's boards at {@code angle} from shut (radians, as its model turns them): their hinges a pixel out from the
     * middle shut, at the middle less an eighth open, so the two boards meet under the fold. The hinge follows
     * {@code cos + sin / 8}, which keeps each board's inner face on the outer face of its block of pages at every angle (a hinge
     * following the sine alone let the pages show through the boards while the book opened).
     */
    public static void recipeCovers(ModelPart front, ModelPart back, float angle) {
        float out = Mth.cos(angle) + 0.125F * Mth.sin(angle);
        front.z = out;
        back.z = -out;
    }

    /**
     * Hands-On Vanilla's enchanting book (the lectern's, and Fallow's awakening book): vanilla's book with its lids and pages laid
     * out as vanilla's (closed along +x from the spine, opening about y), the lids now a square larger than the pages.
     */
    public static LayerDefinition enchantingBook() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        float half = BOARD_HIGH / 2.0F;
        root.addOrReplaceChild("left_lid", CubeListBuilder.create()
                .texOffs(0, 0).addBox(-LID_WIDE, -half, -0.01F, LID_WIDE, BOARD_HIGH, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, -1.0F));
        root.addOrReplaceChild("right_lid", CubeListBuilder.create()
                .texOffs(19, 0).addBox(0.0F, -half, -0.01F, LID_WIDE, BOARD_HIGH, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 1.0F));
        root.addOrReplaceChild("spine", CubeListBuilder.create()
                .texOffs(14, 0).addBox(-1.0F, -SPINE_HIGH / 2.0F, 0.0F, 2.0F, SPINE_HIGH, 0.01F, new CubeDeformation(0.0F)),
                PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, Mth.HALF_PI, 0.0F));
        root.addOrReplaceChild("left_pages", CubeListBuilder.create()
                .texOffs(0, 10).addBox(0.0F, -4.0F, -0.99F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("right_pages", CubeListBuilder.create()
                .texOffs(14, 10).addBox(0.0F, -4.0F, -0.01F, 6.0F, 8.0F, 1.0F, new CubeDeformation(0.0F)),
                PartPose.offset(0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 64, 32);
    }

    /**
     * The enchanting book's lids at {@code open} from shut: hinged a pixel out shut, at the middle open flat. The hinge follows
     * the cosine, which keeps each lid flat on the outer face of its block of pages at every angle (one following
     * {@code 1 - sin} sank the lids into the pages short of a right angle, as at the table).
     */
    public static void enchantingLids(ModelPart left, ModelPart right, float open) {
        float out = Math.max(0.0F, Mth.cos(open));
        left.z = -out;
        right.z = out;
    }
}
