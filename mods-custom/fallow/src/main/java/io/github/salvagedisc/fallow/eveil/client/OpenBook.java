// Adapted from Hands-On Vanilla by Pocky (MIT License, https://github.com/Pocky-l/handson-vanilla, branch 1.21.1,
// version 1.2.2). The licence travels with the mod: META-INF/LICENSE_handson_vanilla.
package io.github.salvagedisc.fallow.eveil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.StationView;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.livres.client.GutterShade;
import io.github.salvagedisc.fallow.livres.client.PageParts;
import io.github.salvagedisc.fallow.livres.client.OpenSheets;

import java.util.Optional;

/**
 * The book read at the enchanting table, where an item awakens ({@link AwakeningBookModel}), open, with pages to write on: two loose
 * sheets lie on its blocks of pages, arching up out of the gutter by the spine, and a turn sends a third over from one side to the
 * other with one page on its face and the next on its back, as {@code RecipeBook} turns its sheets. It takes over from vanilla's
 * own book (the table's floating one, the lectern's) by a short cross-fade at the same size and place ({@code own}). Drawn in the
 * model's own pixels, as vanilla lays its book out: the spine runs along y (the top of the pages at +y), the pages open towards +x,
 * where the reader is; the reader's right page lies towards -z. What is written lies on the flat part of a sheet, past the gutter,
 * in units of the user's choosing ({@code unitsWide} across it), drawn from the top left as on a screen.
 */
final class OpenBook {
    static final float FLIP_TIME = 0.4F;
    /** How much smaller vanilla's book is drawn inside this one during the hand-off. */
    private static final float HAND_OFF_SHRINK = 0.05F;
    /** A turn soon after the last one flips quicker, so that quick clicks are never kept waiting; and how soon that is, seconds. */
    static final float FLIP_QUICK = 0.16F;
    private static final float QUICK_GAP = 0.5F;
    /** A sheet, pixels: as vanilla's loose page. The first {@code GUTTER} of it from the fold arches up out of the gutter. */
    static final float PAGE_WIDE = 6.0F;
    static final float PAGE_HIGH = 8.0F;
    static final float GUTTER = 0.9F;
    /** How high over the fold the arch's first handle lies, pixels, and how many strips draw the arch. */
    private static final float GUTTER_RISE = 0.4F;
    private static final int GUTTER_STEPS = 4;
    /** How far the fold of the sheet being turned stands over the resting ones, pixels: it goes over them, never through. */
    private static final float SHEET_LIFT = 0.06F;
    /** How far the resting sheets' fold stands off the blocks of pages under them, pixels. */
    private static final float REST_LIFT = 0.02F;
    /**
     * How far (radians) a resting sheet must lift off its block of pages before a written sheet is laid under it: less, and the
     * two lie so close that the lower one's writing would show through the upper one.
     */
    private static final float UNDER_GAP = 0.08F;
    /** How far what is written across the spread stands over the fold, pixels: over the pages near it. */
    private static final float SPREAD_LIFT = 0.35F;
    /** Over the paper, in units: rules and bands, then items and text. */
    static final float MARK_LIFT = 0.3F;
    static final float INK_LIFT = 0.8F;
    private static final float ITEM_DEPTH = 8.0F;
    private static final float BLOCK_DEPTH = 0.5F;
    /** A page arrow: its size in units and where its head starts along it. */
    static final int ARROW_WIDE = 12;
    static final int ARROW_HIGH = 11;
    private static final int ARROW_HEAD = 5;
    /** The sheets' paper: a whole page (fold on the left) in the corner of a 128 x 256 texture, a texel to about a unit. */
    /** Glowing writing's rim: how far round each stroke, in the letter's own pixels, and how strong. */
    private static final float RIM = 0.5F;
    private static final float RIM_ALPHA = 0.55F;
    static final ResourceLocation PAPER = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/entity/livre_d_eveil_papier.png");
    private static final float PAPER_U1 = 115.0F / 128.0F;
    private static final float PAPER_V1 = 154.0F / 256.0F;

    /** Paints one page of the book; {@code resting}: the page lies still and answers the cursor. */
    @FunctionalInterface
    interface Pages {
        void paint(Canvas canvas, int page, boolean resting);
    }

    /**
     * Writes what stands on a page whatever the pages painter shows: {@code page} is its number, a riffle's sheet's
     * ({@link #isRiffleSheet}) or a real page's (negative too when the book has turned back past its first spread).
     * Drawn as solid as the book itself, not with the ink: it is there before the book is read and while it is carried.
     */
    @FunctionalInterface
    interface Filler {
        void paint(Canvas canvas, int page);
    }

    /** Paints across the foot of the spread, over the gutter: x 0 is the spine, the units are the pages'. */
    @FunctionalInterface
    interface Spread {
        void paint(Canvas canvas);
    }

    /** A point of a resting page under the cursor, in units from its top left; {@code page} is its number. */
    record Hit(int page, float x, float y, boolean right) {
    }

    final int unitsWide;
    final int unitsHigh;
    private final float unit;
    private final Pages pages;
    private Spread spreadPainter;
    private Filler filler;
    /** The paper of each page, by its number ({@link #isRiffleSheet}: a riffle's sheet). */
    private java.util.function.IntFunction<ResourceLocation> paperOf = page -> PAPER;
    private AwakeningBookModel model;
    private ModelPart vanillaRoot;
    /** The spread shown: its left page is {@code 2 * spread}, its right one the next. */
    private int spread;
    /** The spread the running turn started from: the sheet going over shows its page, whatever the book shows now. */
    private int fromSpread;
    private float flip = 1.0F;
    private int flipWay = 1;
    private float flipTime = FLIP_TIME;
    private float time;
    private float lastTurn = -10.0F;
    /** A riffle: many blank sheets going over one after the other, as a thumb lets them go; when, how many, how long, which way. */
    private float riffleAt = -10.0F;
    private int riffleSheets;
    private float riffleSpan;
    private int riffleWay = 1;
    /** The spread lying open when the riffle started: the side its sheets land on shows its page until the first one lands. */
    private int riffleFrom;
    /** Riffles so far: each riffle's sheets get page numbers of their own, so their writing differs from the last one's. */
    private int riffles;

    OpenBook(int unitsWide, int unitsHigh, Pages pages) {
        this.unitsWide = unitsWide;
        this.unitsHigh = unitsHigh;
        this.unit = (PAGE_WIDE - GUTTER) / unitsWide;
        this.pages = pages;
    }

    int spread() {
        return spread;
    }

    /** What is written across the foot of the spread while it rests (null: nothing). */
    void setSpreadPainter(Spread painter) {
        spreadPainter = painter;
    }

    /** What is written on every page under what the pages painter shows (null: nothing). */
    void setFiller(Filler filler) {
        this.filler = filler;
    }

    /** The paper each page is drawn on, by its number ({@link #isRiffleSheet}: a riffle's sheet); plain paper by default. */
    void setPaper(java.util.function.IntFunction<ResourceLocation> paper) {
        paperOf = paper;
    }

    boolean isFlipping() {
        return flip < 1.0F;
    }

    void frame(float dt) {
        time += dt;
        flip = Math.min(1.0F, flip + dt / flipTime);
    }

    /**
     * The spread vanilla's flip has turned to: each time one of its two flip pages goes over the middle (at a flip of a quarter
     * and three quarters, each whole flip) is a page turned, so the pages it shows are new ones, not the same two going back and
     * forth.
     */
    static int flipTurns(float flip) {
        return Mth.floor(flip * 2.0F + 0.5F);
    }

    /**
     * The angles of the reader's right and left sheets for vanilla's {@code flip} with sheets resting at {@code rest}: of its
     * two flip pages, the one that went over last is the left sheet (showing the spread's left page, the back of the sheet that
     * turned), the other the right one (the spread's right page, to go over next).
     */
    static float[] flipAngles(float flip, float rest) {
        float first = Mth.clamp(Mth.frac(flip + 0.25F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float second = Mth.clamp(Mth.frac(flip + 0.75F) * 1.6F - 0.3F, 0.0F, 1.0F);
        float one = rest - rest * 2.0F * first;
        float two = rest - rest * 2.0F * second;
        float phase = Mth.frac(flip);
        boolean firstWentOver = phase >= 0.25F && phase < 0.75F;
        return firstWentOver ? new float[] {two, one} : new float[] {one, two};
    }

    /** A sheet's flight in a riffle, seconds: quicker than a turn by hand, so that many go over in half a second. */
    static final float RIFFLE_SHEET = 0.2F;

    /**
     * Riffles {@code sheets} blank sheets over in {@code seconds}, forwards ({@code way} 1) or back (-1), one after the other, over the
     * pages lying open; what the pages show is the caller's.
     */
    void riffle(int sheets, float seconds, int way) {
        riffleFrom = isRiffling() ? riffleFrom : spread;
        riffleAt = time;
        riffleSheets = Math.max(1, sheets);
        riffleSpan = Math.max(RIFFLE_SHEET, seconds);
        riffleWay = way >= 0 ? 1 : -1;
        riffles++;
    }

    /** Whether sheets of a riffle are still going over. */
    boolean isRiffling() {
        return time - riffleAt < riffleSpan;
    }

    /**
     * Whether the last sheet of the running riffle has left the side it started from: the page under it lies open, and what is
     * written there can no longer change unseen.
     */
    boolean riffleUncovered() {
        return isRiffling() && time - riffleAt > riffleGap() * (riffleSheets - 1);
    }

    /** How long after the last one each sheet of a riffle sets off, seconds. */
    private float riffleGap() {
        return riffleSheets > 1 ? (riffleSpan - RIFFLE_SHEET) / (riffleSheets - 1) : 0.0F;
    }

    /**
     * The page on the face of a riffle's sheet {@code i} before it goes over ({@code back} false) or after (true): the first one
     * lifts the page that lay open there.
     */
    private int rifflePage(int i, boolean back) {
        if (i == 0 && !back) {
            return 2 * riffleFrom + (riffleWay > 0 ? 1 : 0);
        }
        return RIFFLE_SHEETS - (riffles % 64) * 32 - 2 * i - (back ? 1 : 0);
    }

    /**
     * The page numbers of riffles' sheets lie at and below this, far from the real pages: those go below zero too, as vanilla's
     * flip the table's book follows turns back past where it started.
     */
    private static final int RIFFLE_SHEETS = Integer.MIN_VALUE / 2;

    /** Whether {@code page} is a sheet of a riffle, not a page of the book. */
    static boolean isRiffleSheet(int page) {
        return page <= RIFFLE_SHEETS;
    }

    /** Whether the running riffle goes forwards. */
    boolean riffleForward() {
        return riffleWay > 0;
    }

    /** Shows {@code to} at once, no sheet going over: the book opened somewhere else, or caught up with the server. */
    void jumpTo(int to) {
        spread = to;
        fromSpread = to;
        flip = 1.0F;
    }

    /** Turns to the spread {@code to}, a sheet going over (quickly when {@code quick}, or soon after the last turn). */
    void turnTo(int to, boolean quick) {
        if (to == spread) {
            return;
        }
        fromSpread = isFlipping() && flip < 0.5F ? fromSpread : spread;
        flipWay = to > spread ? 1 : -1;
        spread = to;
        flipTime = quick || time - lastTurn < QUICK_GAP ? FLIP_QUICK : FLIP_TIME;
        flip = 0.0F;
        lastTurn = time;
    }

    /**
     * Draws the book, in its model's pixels: the boards and the blocks of pages at {@code open} (the boards' angle from shut, radians,
     * as vanilla's book takes it), the sheet resting on the reader's right at the angle {@code right} (positive) and the one on the
     * left at {@code left} (negative), and a sheet going over during a turn. {@code ink}: how much of what is written shows, 0 to 1;
     * {@code own}: 0 draws vanilla's book exactly as the table or the lectern draws it (its loose pages at the two angles), 1 this
     * one, between them this one fades in over vanilla's.
     */
    void draw(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float right, float left, float ink, float own) {
        if (own < 1.0F) {
            // Vanilla's book stays whole under this one while it fades in over it: nothing behind shows through the two. It shrinks
            // a little inside this one as that one comes, so their faces (the same boards and pages at the same place) never fight.
            poseStack.pushPose();
            float inside = 1.0F - HAND_OFF_SHRINK * Math.min(1.0F, own * 4.0F);
            poseStack.scale(inside, inside, inside);
            drawVanilla(poseStack, buffers, light, open, right, left, 1.0F);
            poseStack.popPose();
        }
        if (own <= 0.0F) {
            return;
        }
        if (model == null) {
            model = AwakeningBookModel.bake();
        }
        model.setupAnim(open);
        boolean fading = own < 1.0F;
        VertexConsumer cloth = buffers.getBuffer(fading ? RenderType.entityTranslucent(AwakeningBookModel.TEXTURE)
                : RenderType.entityCutout(AwakeningBookModel.TEXTURE));
        poseStack.pushPose();
        poseStack.scale(16.0F, 16.0F, 16.0F);
        model.render(poseStack, cloth, light, OverlayTexture.NO_OVERLAY, own);
        poseStack.popPose();
        float shown = ink * own;
        int left0 = 2 * spread;
        if (filler != null) {
            // A resting sheet lifted off its block of pages (the table's book flips its sheets) shows the page under it, written
            // too, never a bare block: on the right the next spread's right page, on the left the last spread's left page.
            if (open - right > UNDER_GAP) {
                sheet(poseStack, buffers, light, open, open, REST_LIFT, true, left0 + 3, false, 0.0F, own);
            }
            if (left + open > UNDER_GAP) {
                sheet(poseStack, buffers, light, open, -open, REST_LIFT, true, left0 - 2, false, 0.0F, own);
            }
        }
        if (isRiffling()) {
            drawRiffle(poseStack, buffers, light, open, right, left, shown, own);
            return;
        }
        if (flip >= 1.0F) {
            sheet(poseStack, buffers, light, open, right, REST_LIFT, true, left0 + 1, true, shown, own);
            sheet(poseStack, buffers, light, open, left, REST_LIFT, true, left0, true, shown, own);
            if (spreadPainter != null && shown > 0.0F) {
                // Flat across the spread, a little over the fold, so it clears the pages rising on either side of the gutter.
                poseStack.pushPose();
                poseStack.mulPose(new Matrix4f(0.0F, 0.0F, -unit, 0.0F,
                        0.0F, -unit, 0.0F, 0.0F,
                        unit, 0.0F, 0.0F, 0.0F,
                        Mth.sin(open) + REST_LIFT + SPREAD_LIFT, PAGE_HIGH / 2.0F, 0.0F, 1.0F));
                spreadPainter.paint(new Canvas(poseStack, buffers, light, shown, unitsWide * 2, unitsHigh));
                poseStack.popPose();
            }
            return;
        }
        int from0 = 2 * fromSpread;
        boolean forward = flipWay > 0;
        // Forward the old left page stays until the sheet covers it with the new one; back the old right page stays.
        sheet(poseStack, buffers, light, open, right, REST_LIFT, true, forward ? left0 + 1 : from0 + 1, false, shown, own);
        sheet(poseStack, buffers, light, open, left, REST_LIFT, true, forward ? from0 : left0, false, shown, own);
        float over = flip * flip * (3.0F - 2.0F * flip);
        // The sheet going over shows the page it lifts on its face and the one it brings on its back.
        int front = forward ? from0 + 1 : left0 + 1;
        int back = forward ? left0 : from0;
        turning(poseStack, buffers, light, open, forward ? right : left, forward ? left : right, over, SHEET_LIFT, front, back, shown,
                shown, own);
    }

    /**
     * A sheet going over from the angle {@code from} to {@code to}, {@code over} of the way: bent as paper bends, with each face's
     * picture on it ({@link OpenSheets}), {@code front} the page on its face (read on the right), {@code back} the one on its back.
     * Until the pictures are painted (the first frame of a turn) it goes over flat, as it did before.
     */
    private void turning(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float from, float to, float over,
            float lift, int front, int back, float frontInk, float backInk, float own) {
        boolean bent = OpenSheets.turn(poseStack, buffers, light, open, lift, from, to, over, this, front, back,
                (pose, drawn, page, onFront) -> sheet(pose, drawn, LightTexture.FULL_BRIGHT, 0.0F,
                        onFront ? OpenSheets.FLAT : -OpenSheets.FLAT, 0.0F, false, page, false, onFront ? frontInk : backInk, 1.0F),
                own);
        if (!bent) {
            float turn = Mth.lerp(over, from, to);
            boolean onFront = turn > 0.0F;
            sheet(poseStack, buffers, light, open, turn, lift, false, onFront ? front : back, false, onFront ? frontInk : backInk, own);
        }
    }

    /**
     * A riffle: its sheets in flight, each a little higher over the pages than the one before it, so two passing never cut through
     * each other; each sets off a little after the last and lands as the riffle ends. The side they leave shows the next sheet to
     * go, and once the last has gone the page the riffle runs to; the side they land on shows the last one landed. The last sheet
     * carries the page the riffle runs to on its back: what is written there comes with the paper, never fades in.
     */
    private void drawRiffle(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float right, float left, float ink,
            float own) {
        float since = time - riffleAt;
        float gap = riffleGap();
        boolean forward = riffleWay > 0;
        int target = 2 * spread + (forward ? 0 : 1);
        int targetFrom = 2 * spread + (forward ? 1 : 0);
        int waiting = -1;
        int landed = -1;
        for (int i = 0; i < riffleSheets; i++) {
            float t = (since - i * gap) / RIFFLE_SHEET;
            if (t <= 0.0F && waiting < 0) {
                waiting = i;
            }
            if (t >= 1.0F) {
                landed = i;
            }
            if (t <= 0.0F || t >= 1.0F) {
                continue;
            }
            float over = t * t * (3.0F - 2.0F * t);
            boolean last = i == riffleSheets - 1;
            int lifted = rifflePage(i, false);
            int brought = last ? target : rifflePage(i, true);
            // Forwards a sheet lifts off the right, so the page it lifts is on its face; back it lifts off the left, on its back.
            int front = forward ? lifted : brought;
            int back = forward ? brought : lifted;
            turning(poseStack, buffers, light, open, forward ? right : left, forward ? left : right, over, SHEET_LIFT + 0.03F * (i + 1),
                    front, back, isRiffleSheet(front) ? 0.0F : ink, isRiffleSheet(back) ? 0.0F : ink, own);
        }
        int leaving = waiting >= 0 ? rifflePage(waiting, false) : targetFrom;
        int landing = landed == riffleSheets - 1 ? target : landed >= 0 ? rifflePage(landed, true)
                : 2 * riffleFrom + (forward ? 0 : 1);
        int rightPage = forward ? leaving : landing;
        int leftPage = forward ? landing : leaving;
        sheet(poseStack, buffers, light, open, right, REST_LIFT, true, rightPage, false, rightPage >= 0 ? ink : 0.0F, own);
        sheet(poseStack, buffers, light, open, left, REST_LIFT, true, leftPage, false, leftPage >= 0 ? ink : 0.0F, own);
    }

    /** Vanilla's book with its texture (a resource pack's), as the table's and the lectern's renderers draw it, faded by {@code alpha}. */
    private void drawVanilla(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float right, float left,
            float alpha) {
        if (vanillaRoot == null) {
            vanillaRoot = Minecraft.getInstance().getEntityModels().bakeLayer(ModelLayers.BOOK);
        }
        ModelPart leftLid = vanillaRoot.getChild("left_lid");
        ModelPart rightLid = vanillaRoot.getChild("right_lid");
        ModelPart leftPages = vanillaRoot.getChild("left_pages");
        ModelPart rightPages = vanillaRoot.getChild("right_pages");
        ModelPart flip1 = vanillaRoot.getChild("flip_page1");
        ModelPart flip2 = vanillaRoot.getChild("flip_page2");
        leftLid.yRot = Mth.PI + open;
        rightLid.yRot = -open;
        leftPages.yRot = open;
        rightPages.yRot = -open;
        flip1.yRot = right;
        flip2.yRot = left;
        float rise = Mth.sin(open);
        leftPages.x = rise;
        rightPages.x = rise;
        flip1.x = rise;
        flip2.x = rise;
        VertexConsumer consumer = alpha >= 1.0F ? EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entitySolid)
                : EnchantTableRenderer.BOOK_LOCATION.buffer(buffers, RenderType::entityTranslucent);
        poseStack.pushPose();
        poseStack.scale(16.0F, 16.0F, 16.0F);
        vanillaRoot.render(poseStack, consumer, light, OverlayTexture.NO_OVERLAY, Math.round(alpha * 255.0F) << 24 | 0xFFFFFF);
        poseStack.popPose();
    }

    /** Where a point {@code along} pixels from the fold lies on a sheet at {@code angle}, arching out of the gutter when it rests. */
    private static Vector3f alongSheet(float open, float angle, float lift, boolean curved, float along) {
        Vector3f way = new Vector3f(Mth.cos(angle), 0.0F, -Mth.sin(angle));
        Vector3f fold = new Vector3f(Mth.sin(open) + lift, 0.0F, 0.0F);
        if (!curved || along >= GUTTER) {
            return new Vector3f(way).mul(along).add(fold);
        }
        // A cubic curve leaving the fold straight up out of the gutter and meeting the flat sheet along its own way: the paper arches
        // over the blocks of pages, which meet at the fold, and never dips into them.
        Vector3f up = new Vector3f(fold).add(GUTTER_RISE, 0.0F, 0.0F);
        Vector3f on = new Vector3f(way).mul(GUTTER * 0.5F).add(fold);
        Vector3f end = new Vector3f(way).mul(GUTTER).add(fold);
        float t = along / GUTTER;
        float s = 1.0F - t;
        return new Vector3f(fold).mul(s * s * s).add(new Vector3f(up).mul(3.0F * s * s * t)).add(new Vector3f(on).mul(3.0F * s * t * t))
                .add(new Vector3f(end).mul(t * t * t));
    }

    /**
     * One sheet at {@code angle} about the fold, showing {@code page} on the face the reader sees: as a right page while it lies on
     * the right half (positive angle), else as a left one. Its paper runs over the whole sheet, the gutter's curve included; what is
     * written lies on its flat part.
     */
    private void sheet(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float angle, float lift, boolean curved,
            int page, boolean resting, float ink, float alpha) {
        boolean rightSide = angle > 0.0F;
        paper(poseStack, buffers, light, open, angle, lift, curved, rightSide, alpha, paperOf.apply(page));
        boolean filled = filler != null && alpha > 0.0F;
        if (ink <= 0.0F && !filled) {
            return;
        }
        poseStack.pushPose();
        poseStack.mulPose(canvasMatrix(open, angle, lift));
        if (filled) {
            filler.paint(new Canvas(poseStack, buffers, light, alpha, unitsWide, unitsHigh), page);
        }
        if (ink > 0.0F && !isRiffleSheet(page)) {
            pages.paint(new Canvas(poseStack, buffers, light, ink, unitsWide, unitsHigh), page, resting);
        }
        poseStack.popPose();
    }

    /** The paper of a sheet in strips from the fold to its edge: the gutter's curve, shaded towards the fold, then the flat part. */
    private void paper(PoseStack poseStack, MultiBufferSource buffers, int light, float open, float angle, float lift, boolean curved,
            boolean rightSide, float alpha, ResourceLocation texture) {
        VertexConsumer consumer = buffers.getBuffer(RenderType.text(texture));
        Matrix4f pose = poseStack.last().pose();
        int steps = curved ? GUTTER_STEPS + 1 : 1;
        float[] stops = new float[steps + 1];
        for (int i = 0; i <= steps; i++) {
            stops[i] = !curved ? i * PAGE_WIDE : i <= GUTTER_STEPS ? GUTTER * i / GUTTER_STEPS : PAGE_WIDE;
        }
        float half = PAGE_HIGH / 2.0F;
        int a8 = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
        for (int i = 0; i < steps; i++) {
            Vector3f from = alongSheet(open, angle, lift, curved, stops[i]);
            Vector3f to = alongSheet(open, angle, lift, curved, stops[i + 1]);
            float uFrom = stops[i] / PAGE_WIDE * PAPER_U1;
            float uTo = stops[i + 1] / PAGE_WIDE * PAPER_U1;
            // The shade runs smoothly up out of the gutter, from one edge of the strip to the other.
            int shadeFrom = curved ? GutterShade.at(stops[i]) : 0xFFFFFF;
            int shadeTo = curved ? GutterShade.at(stops[i + 1]) : 0xFFFFFF;
            // Wound as the text on it, so the face towards the reader is the front: the fold is the right page's left edge, the left
            // page's right edge (its paper mirrored).
            if (rightSide) {
                vertex(consumer, pose, from, half, uFrom, 0.0F, shadeFrom, a8, light);
                vertex(consumer, pose, from, -half, uFrom, PAPER_V1, shadeFrom, a8, light);
                vertex(consumer, pose, to, -half, uTo, PAPER_V1, shadeTo, a8, light);
                vertex(consumer, pose, to, half, uTo, 0.0F, shadeTo, a8, light);
            } else {
                vertex(consumer, pose, to, half, uTo, 0.0F, shadeTo, a8, light);
                vertex(consumer, pose, to, -half, uTo, PAPER_V1, shadeTo, a8, light);
                vertex(consumer, pose, from, -half, uFrom, PAPER_V1, shadeFrom, a8, light);
                vertex(consumer, pose, from, half, uFrom, 0.0F, shadeFrom, a8, light);
            }
        }
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, Vector3f at, float y, float u, float v, int rgb, int alpha,
            int light) {
        consumer.addVertex(pose, at.x, at.y + y, at.z).setColor(rgb >> 16 & 0xFF, rgb >> 8 & 0xFF, rgb & 0xFF, alpha).setUv(u, v)
                .setLight(light);
    }

    /**
     * Units on the flat part of a sheet at {@code angle} to the model's pixels: x across the page as the reader sees it, y down it, z out
     * of the paper towards the reader (one unit is one unit there too).
     */
    private Matrix4f canvasMatrix(float open, float angle, float lift) {
        boolean rightSide = angle > 0.0F;
        float cos = Mth.cos(angle);
        float sin = Mth.sin(angle);
        Vector3f along = new Vector3f(cos, 0.0F, -sin);
        Vector3f out = rightSide ? new Vector3f(sin, 0.0F, cos) : new Vector3f(-sin, 0.0F, -cos);
        Vector3f origin = alongSheet(open, angle, lift, false, rightSide ? GUTTER : PAGE_WIDE).add(0.0F, PAGE_HIGH / 2.0F, 0.0F);
        Vector3f across = rightSide ? along : new Vector3f(along).negate();
        return new Matrix4f(across.x * unit, across.y * unit, across.z * unit, 0.0F,
                0.0F, -unit, 0.0F, 0.0F,
                out.x * unit, out.y * unit, out.z * unit, 0.0F,
                origin.x, origin.y, origin.z, 1.0F);
    }

    /** Where a point of the resting right or left page is in the model's pixels ({@code x, y} units). */
    Vector3f onPage(boolean rightPage, float x, float y, float open, float right, float left) {
        return canvasMatrix(open, rightPage ? right : left, REST_LIFT).transformPosition(new Vector3f(x, y, 0.0F));
    }

    /** The foot of the spine between the pages, in the model's pixels, at the bottom of the gutter: a ribbon comes out there. */
    static Vector3f spineFoot(float open) {
        return new Vector3f(Mth.sin(open), -PAGE_HIGH / 2.0F, 0.0F);
    }

    /**
     * The resting page under the cursor and the point on it, units; empty when the cursor is on neither. {@code toNode} takes the
     * model's pixels to the node's {@code node}, whose plane the station reads the cursor on.
     */
    Optional<Hit> hit(StationView view, String node, Matrix4f toNode, float open, float right, float left) {
        Optional<Vector3f> near = view.pointerOn(node, 0.0F);
        Optional<Vector3f> far = view.pointerOn(node, -4.0F);
        if (near.isEmpty() || far.isEmpty()) {
            return Optional.empty();
        }
        // Two points of the cursor's ray, from the camera down through the node's plane: the ray in the book's own pixels.
        Matrix4f toModel = new Matrix4f(toNode).invert();
        Vector3f from = toModel.transformPosition(new Vector3f(near.get()));
        Vector3f way = toModel.transformPosition(new Vector3f(far.get())).sub(from);
        Hit best = null;
        float bestT = Float.MAX_VALUE;
        for (boolean rightPage : new boolean[] {true, false}) {
            Matrix4f page = canvasMatrix(open, rightPage ? right : left, REST_LIFT);
            Vector3f origin = page.transformPosition(new Vector3f());
            Vector3f normal = page.transformDirection(new Vector3f(0.0F, 0.0F, 1.0F));
            float facing = way.dot(normal);
            if (Math.abs(facing) < 1.0E-6F) {
                continue;
            }
            float t = new Vector3f(origin).sub(from).dot(normal) / facing;
            Vector3f point = new Vector3f(way).mul(t).add(from);
            Vector3f units = new Matrix4f(page).invert().transformPosition(point);
            if (units.x >= 0.0F && units.x < unitsWide && units.y >= 0.0F && units.y < unitsHigh && t < bestT) {
                bestT = t;
                best = new Hit(2 * spread + (rightPage ? 1 : 0), units.x, units.y, rightPage);
            }
        }
        return Optional.ofNullable(best);
    }

    /** The block's own pixels (0..16 from its corner, as a block entity renderer draws) to the scene's. */
    static Matrix4f sceneFromBlock(StationView view) {
        Vec3 origin = view.toWorld(0.0F, 0.0F, 0.0F);
        Vec3 x = view.toWorld(1.0F, 0.0F, 0.0F).subtract(origin);
        Vec3 y = view.toWorld(0.0F, 1.0F, 0.0F).subtract(origin);
        Vec3 z = view.toWorld(0.0F, 0.0F, 1.0F).subtract(origin);
        Vec3 corner = origin.subtract(Vec3.atLowerCornerOf(view.pos()));
        Matrix4f blockFromScene = new Matrix4f(
                (float) x.x * 16.0F, (float) x.y * 16.0F, (float) x.z * 16.0F, 0.0F,
                (float) y.x * 16.0F, (float) y.y * 16.0F, (float) y.z * 16.0F, 0.0F,
                (float) z.x * 16.0F, (float) z.y * 16.0F, (float) z.z * 16.0F, 0.0F,
                (float) corner.x * 16.0F, (float) corner.y * 16.0F, (float) corner.z * 16.0F, 1.0F);
        return blockFromScene.invert();
    }

    /** Drawing on a page, in units from its top left. */
    static final class Canvas {
        private static final net.minecraft.resources.ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "block/livre_blanc");
        private final PoseStack poseStack;
        private final MultiBufferSource buffers;
        private final int light;
        private final float ink;
        private final int wide;
        private final int high;
        private final TextureAtlasSprite white;

        Canvas(PoseStack poseStack, MultiBufferSource buffers, int light, float ink, int wide, int high) {
            this.poseStack = poseStack;
            this.buffers = buffers;
            this.light = light;
            this.ink = Mth.clamp(ink, 0.0F, 1.0F);
            this.wide = wide;
            this.high = high;
            white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        }

        int wide() {
            return wide;
        }

        int high() {
            return high;
        }

        /** The rule of the books' pages ({@link PageParts#rule}), {@code wide} across, its line at {@code y}; it fades with the ink. */
        void rule(float x, float y, float wide) {
            int alpha = Math.round(255.0F * ink);
            if (alpha >= 4) {
                PageParts.rule(poseStack, buffers, light, x, y, wide, MARK_LIFT, alpha);
            }
        }

        /** A flat colour; its alpha goes with the ink. */
        void rect(float x, float y, float width, float height, float lift, int argb) {
            quad(buffers.getBuffer(RenderType.text(TextureAtlas.LOCATION_BLOCKS)), x, y, x + width, y + height, lift, argb,
                    white.getU0(), white.getU1(), white.getV0(), white.getV1(), true);
        }

        void text(FormattedCharSequence text, float x, float y, int rgb, float alpha) {
            int a = Math.round(255.0F * Mth.clamp(alpha * ink, 0.0F, 1.0F));
            if (a < 4) {
                return;
            }
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(0.0F, 0.0F, INK_LIFT);
            Minecraft.getInstance().font.drawInBatch(text, x, y, a << 24 | rgb & 0xFFFFFF, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, light);
        }

        void text(Component text, float x, float y, int rgb, float alpha) {
            text(text.getVisualOrderText(), x, y, rgb, alpha);
        }

        /**
         * Text drawn at {@code scale} from its left, about the middle of the line it would take at full size: scaled as a whole,
         * never wrapped.
         */
        void text(FormattedCharSequence text, float x, float y, float scale, int rgb, float alpha) {
            if (scale == 1.0F) {
                text(text, x, y, rgb, alpha);
                return;
            }
            poseStack.pushPose();
            poseStack.translate(x, y + Minecraft.getInstance().font.lineHeight * (1.0F - scale) * 0.5F, 0.0F);
            poseStack.scale(scale, scale, 1.0F);
            text(text, 0.0F, 0.0F, rgb, alpha);
            poseStack.popPose();
        }

        /**
         * A letter of glowing ink: {@code text} in {@code rgb} at full light (it gives its own light), with a thin rim of
         * {@code rim} round its strokes, as glow-ink writing has; drawn over the plain ink, raised by {@code rise} units.
         */
        void brightText(FormattedCharSequence text, float x, float y, float scale, int rgb, int rim, float alpha, float rise) {
            float strength = Mth.clamp(alpha, 0.0F, 1.0F) * ink;
            if (strength < 0.02F) {
                return;
            }
            Font font = Minecraft.getInstance().font;
            int a = Math.round(255.0F * strength);
            int rimA = Math.round(255.0F * strength * RIM_ALPHA);
            poseStack.pushPose();
            poseStack.translate(x, y - rise + font.lineHeight * (1.0F - scale) * 0.5F, 0.0F);
            poseStack.scale(scale, scale, 1.0F);
            Matrix4f base = new Matrix4f(poseStack.last().pose());
            if (rimA >= 4) {
                Matrix4f under = new Matrix4f(base).translate(0.0F, 0.0F, INK_LIFT * 1.1F);
                for (int side = 0; side < 4; side++) {
                    float dx = side == 0 ? RIM : side == 1 ? -RIM : 0.0F;
                    float dy = side == 2 ? RIM : side == 3 ? -RIM : 0.0F;
                    font.drawInBatch(text, dx, dy, rimA << 24 | rim & 0xFFFFFF, false, under, buffers, Font.DisplayMode.NORMAL, 0,
                            LightTexture.FULL_BRIGHT);
                }
            }
            Matrix4f over = new Matrix4f(base).translate(0.0F, 0.0F, INK_LIFT * 1.25F);
            font.drawInBatch(text, 0.0F, 0.0F, a << 24 | rgb & 0xFFFFFF, false, over, buffers, Font.DisplayMode.NORMAL, 0,
                    LightTexture.FULL_BRIGHT);
            poseStack.popPose();
        }

        /** A part ({@code u0..v1}) of a texture at full light over the writing, tinted {@code rgb}: a drawing's line awake. */
        void brightSprite(ResourceLocation texture, float x, float y, float wide, float high, float u0, float v0, float u1, float v1,
                int rgb, float alpha) {
            int a = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
            if (a < 4) {
                return;
            }
            quad(buffers.getBuffer(RenderType.text(texture)), x, y, x + wide, y + high, INK_LIFT * 1.2F, a << 24 | rgb & 0xFFFFFF,
                    u0, u1, v0, v1, true, LightTexture.FULL_BRIGHT);
        }

        /** A flat colour at full light, over the writing: a spark's ray. */
        void brightRect(float x, float y, float width, float height, int rgb, float alpha) {
            int a = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
            if (a < 4) {
                return;
            }
            quad(buffers.getBuffer(RenderType.text(TextureAtlas.LOCATION_BLOCKS)), x, y, x + width, y + height, INK_LIFT * 1.4F,
                    a << 24 | rgb & 0xFFFFFF, white.getU0(), white.getU1(), white.getV0(), white.getV1(), true,
                    LightTexture.FULL_BRIGHT);
        }

        /**
         * A whole texture tinted {@code rgb} at {@code alpha} (and the ink), {@code wide} x {@code high} units from its top left;
         * {@code bright}: at full light and over the ink, as glowing writing.
         */
        void sprite(ResourceLocation texture, float x, float y, float wide, float high, int rgb, float alpha, boolean bright) {
            sprite(texture, x, y, wide, high, 1.0F, 1.0F, rgb, alpha, bright);
        }

        /** The top left part {@code u1} x {@code v1} of a texture, as {@link #sprite}. */
        void sprite(ResourceLocation texture, float x, float y, float wide, float high, float u1, float v1, int rgb, float alpha,
                boolean bright) {
            int a = Math.round(255.0F * Mth.clamp(alpha, 0.0F, 1.0F));
            quad(buffers.getBuffer(RenderType.text(texture)), x, y, x + wide, y + high, bright ? INK_LIFT * 1.2F : MARK_LIFT,
                    a << 24 | rgb & 0xFFFFFF, 0.0F, u1, 0.0F, v1, true, bright ? LightTexture.FULL_BRIGHT : light);
        }

        /** Text drawn as the HUD draws the experience level: a black edge round it, then the colour. */
        void outlined(String text, float x, float y, int rgb, float alpha) {
            FormattedCharSequence line = Component.literal(text).getVisualOrderText();
            int a = Math.round(255.0F * Mth.clamp(alpha * ink, 0.0F, 1.0F));
            if (a < 4) {
                return;
            }
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(0.0F, 0.0F, INK_LIFT);
            Font font = Minecraft.getInstance().font;
            font.drawInBatch(line, x + 1.0F, y, a << 24, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
            font.drawInBatch(line, x - 1.0F, y, a << 24, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
            font.drawInBatch(line, x, y + 1.0F, a << 24, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
            font.drawInBatch(line, x, y - 1.0F, a << 24, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, light);
            Matrix4f over = new Matrix4f(matrix).translate(0.0F, 0.0F, INK_LIFT * 0.5F);
            font.drawInBatch(line, x, y, a << 24 | rgb & 0xFFFFFF, false, over, buffers, Font.DisplayMode.NORMAL, 0, light);
        }

        /**
         * A page arrow in ink, 12 x 11 units from its top left, pointing on ({@code next}) or back; drawn here, not taken from
         * vanilla's book screen, whose arrows a resource pack restyles for its own pages.
         */
        void arrow(float x, float y, boolean next, int argb) {
            int middle = ARROW_HIGH / 2;
            for (int row = 0; row < ARROW_HIGH; row++) {
                int off = Math.abs(row - middle);
                float from = off <= 1 ? 1.0F : ARROW_HEAD;
                float to = ARROW_WIDE - 1 - off;
                if (to > from) {
                    rect(next ? x + from : x + ARROW_WIDE - to, y + row, to - from, 1.0F, MARK_LIFT, argb);
                }
            }
        }

        /** A part ({@code u0..v1}) of a texture, {@code wide} x {@code high} units from its top left, tinted; it fades with the ink. */
        void image(ResourceLocation texture, float x, float y, float wide, float high, float u0, float v0, float u1, float v1,
                int rgb) {
            quad(buffers.getBuffer(RenderType.text(texture)), x, y, x + wide, y + high, INK_LIFT, 0xFF000000 | rgb, u0, u1, v0, v1,
                    true);
        }

        /** An item as in a GUI, {@code size} units square from its top left; it fades with the ink. */
        void item(Level level, ItemStack stack, float x, float y, float size) {
            item(level, stack, x, y, size, 1.0F);
        }

        /** An item as in a GUI, faded by {@code alpha} as well as the ink. */
        void item(Level level, ItemStack stack, float x, float y, float size, float alpha) {
            float ink = this.ink * Mth.clamp(alpha, 0.0F, 1.0F);
            if (stack.isEmpty() || ink <= 0.02F) {
                return;
            }
            poseStack.pushPose();
            poseStack.translate(x + size / 2.0F, y + size / 2.0F, INK_LIFT);
            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            BakedModel model = renderer.getModel(stack, level, null, 0);
            poseStack.scale(size, -size, model.isGui3d() ? BLOCK_DEPTH : ITEM_DEPTH);
            model.getTransforms().getTransform(ItemDisplayContext.GUI).apply(false, poseStack);
            BakedModel shown = model;
            MultiBufferSource drawn = ink < 1.0F ? DiegesisClient.tinted(buffers, Math.round(255.0F * ink) << 24 | 0xFFFFFF) : buffers;
            // Drawn in ink, never with the enchantment glint: its render type is not one the book's buffers carry.
            ItemStack plain = stack.copy();
            plain.set(net.minecraft.core.component.DataComponents.ENCHANTMENT_GLINT_OVERRIDE, false);
            renderer.render(plain, ItemDisplayContext.NONE, false, poseStack, drawn, light, OverlayTexture.NO_OVERLAY, shown);
            poseStack.popPose();
        }

        private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1, float lift, int argb, float u0, float u1,
                float v0, float v1, boolean inked) {
            quad(consumer, x0, y0, x1, y1, lift, argb, u0, u1, v0, v1, inked, light);
        }

        private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1, float lift, int argb, float u0, float u1,
                float v0, float v1, boolean inked, int light) {
            Matrix4f matrix = poseStack.last().pose();
            int a = inked ? Math.round((argb >>> 24) * ink) : argb >>> 24;
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            consumer.addVertex(matrix, x0, y0, lift).setColor(r, g, b, a).setUv(u0, v0).setLight(light);
            consumer.addVertex(matrix, x0, y1, lift).setColor(r, g, b, a).setUv(u0, v1).setLight(light);
            consumer.addVertex(matrix, x1, y1, lift).setColor(r, g, b, a).setUv(u1, v1).setLight(light);
            consumer.addVertex(matrix, x1, y0, lift).setColor(r, g, b, a).setUv(u1, v0).setLight(light);
        }
    }
}
