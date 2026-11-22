// Adapted from Hands-On Vanilla by Pocky (MIT License, https://github.com/Pocky-l/handson-vanilla, branch 1.21.1,
// version 1.2.2). The licence travels with the mod: META-INF/LICENSE_handson_vanilla.
package io.github.salvagedisc.fallow.postes.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.ItemRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.BakedModel;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.stats.Stats;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.NodePainter;
import com.pockyl.diegesis.api.client.NodeView;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.scene.Transform;
import io.github.salvagedisc.fallow.postes.BookMath;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.livres.client.DogEar;
import io.github.salvagedisc.fallow.livres.client.RecipeSheets;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * The pot's recipes in a book hanging in the air beside it: a green cookbook ({@link RecipeBookModel}), opened out, with the recipes on
 * its pages, so the player reads and stirs at once. The page on the left lists the dishes, what can be cooked now first; the page on the
 * right shows the dish under the cursor or the one picked. Pages are measured in units of about a GUI pixel. Open, the player can move
 * it and size it by its top right corner, as in Hands-On Delight; that size and place are the hanging book's only, kept on the client
 * beside the open or shut key, and the book lying on the table and the one handed over to a neighbour go as they always did.
 */
final class RecipeBook implements NodePainter {
    /** The page's paper, a unit to a texel, and the ribbon beside it. */
    private static final ResourceLocation PAPER = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/entity/livre_de_recettes_papier.png");
    /**
     * The flat sprite that wells and bars are tinted from: this mod's own, in the off-white of white concrete that their
     * colours were chosen against, so that a resource pack that retextures concrete does not show through the book (Hands-On
     * Delight learnt it from a player's pack).
     */
    private static final ResourceLocation WHITE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "block/livre_blanc");
    private static final Quaternionf LYING = new Matrix3f(0.0F, 1.0F, 0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 0.0F, 0.0F).getNormalizedRotation(new Quaternionf());
    /** How big against the model's own pixels. */
    private static final float SIZE = 0.72F;
    /** The paper covers the model's blocks of pages exactly; the covers frame it with a pixel all round. */
    private static final float PAGE_WIDTH = 6.0F * SIZE;
    private static final float PAGE_HEIGHT = 8.0F * SIZE;
    private static final int UNITS_WIDE = 72;
    private static final int UNITS_HIGH = 96;
    private static final float UNIT = PAGE_WIDTH / UNITS_WIDE;
    /** How far the model opens, in its own measure, and the angle that leaves between a page and flat. */
    private static final float OPEN = 1.17F;
    /** The paper over the model's own pages, in the model's pixels; a sheet being turned over the pages under it, in pixels. */
    private static final float PAGE_GAP = 0.09F;
    private static final float SHEET_LIFT = 0.13F;
    /** Seconds the covers take to swing open or shut. */
    private static final float OPEN_TIME = 0.3F;
    /** How far the camera has come before the book starts to open: it arrives shut and opens before the player's eyes. */
    private static final float OPEN_FROM = 0.98F;
    /** How far the camera has come when the book starts to grow, flying in (and is gone, flying back). */
    private static final float APPEAR_FROM = 0.25F;
    /** Smaller than this the book is a speck, not a book: it is not drawn. */
    static final float SPECK = 0.08F;
    private static final float FLIP_TIME = 0.4F;
    /** A turn soon after the last one flips quicker, so that quick clicks are never kept waiting; and how soon that is, seconds. */
    private static final float FLIP_QUICK = 0.16F;
    private static final float QUICK_GAP = 0.5F;
    /** The least time between two page sounds, milliseconds: leafing quickly does not rattle. */
    private static final long TURN_SOUND_GAP = 80L;
    /** How far a turning sheet has gone over when the list it uncovers takes the cursor. */
    private static final float FLIP_LIVE = 0.6F;
    /** Over the paper, in units: wells, then what lies on them, then what covers that. */
    private static final float WELL_LIFT = 0.3F;
    private static final float ITEM_LIFT = 1.2F;
    /** How thick items are on the page: flat, with their faces well apart, or the two would flicker through each other. */
    private static final float ITEM_DEPTH = 8.0F;
    private static final float BLOCK_DEPTH = 0.5F;
    private static final float COVER_LIFT = 1.9F;
    private static final int COLUMNS = 3;
    private static final int ROWS = 4;
    private static final int PITCH = 18;
    private static final int WELL_SIZE = 17;
    private static final int PER_PAGE = COLUMNS * ROWS;
    /**
     * Each page is laid out about the middle of its open part, clear of the shade in the fold: the left page's fold is on its right,
     * the right page's on its left. The list's grid sits in the middle of the left page, its page number under it, and the foot of
     * the right page on the same line as the number. A page turns by its corner, turned down at the foot of the outer side.
     */
    /** The grid, 53 units wide, about the middle of the left page's open part, (72 - 5) / 2. */
    private static final int GRID_X = 7;
    private static final int GRID_Y = 5;
    /** The line of the list's page number and of the right page's small print: its top and its height. */
    private static final int ARROW_Y = 81;
    private static final int ARROW_HEIGHT = 11;
    /** The corner turned down at the foot of the outer side, as drawn, and how far round it the cursor takes it. */
    private static final float DOG_EAR = 14.0F;
    private static final float DOG_EAR_REACH = 16.0F;
    /** The widest the page number may be before it is drawn smaller. */
    private static final float NUMBER_ROOM = 27.0F;
    /** The list's grid across, and its middle: the page number stands there, the two halves of the foot meet there. */
    private static final float LIST_WIDE = COLUMNS * PITCH - (PITCH - WELL_SIZE);
    private static final float FOOT_MIDDLE = GRID_X + LIST_WIDE / 2.0F;
    private static final int INK = 0xFF4A3418;
    private static final int WELL = 0x1E5A3C18;
    /** A well whose recipe can be made now: washed with the cover's green. */
    private static final int WELL_READY = 0x603D7845;
    private static final int WELL_PICKED = 0xFF8A5A2B;
    private static final int FADED = 0x55FFFFFF;
    private static final int RULE = 0x405A3C18;
    /** Stars on a page: open (the cover's green), the part of the next one already earned, still shut. */
    private static final int STAR_OPEN = 0xFF306842;
    private static final int STAR_EARNING = 0x90306842;
    private static final int STAR_SHUT = 0x305A3C18;
    /** The paper texture: its size, the page in its corner (the fold on the left), the ribbon and the lit ribbon beside it. */
    private static final float PAPER_SIZE = 128.0F;
    private static final float PAPER_U1 = UNITS_WIDE / PAPER_SIZE;
    private static final float PAPER_V1 = UNITS_HIGH / PAPER_SIZE;
    private static final float RIBBON_U = 80.0F;
    private static final float RIBBON_LIT_U = 88.0F;
    /** Where the tabs take their paper from: clear of the page's darker edge. */
    private static final float TAB_PAPER_U = 8.0F;
    private static final float TAB_PAPER_V = 8.0F;
    private static final float REFRESH_TIME = 0.25F;
    /** How small it is closed. */
    private static final float CLOSED_SIZE = 0.62F;
    /** Closed, it shows its cover: the model turned about the spine, the spine moved to the left of the middle. */
    private static final float CLOSED_TURN = Mth.HALF_PI;
    private static final float CLOSED_SHIFT = 3.0F / 16.0F;
    /** The tabs over the list: where the row starts, each one's width and spacing, height (the others lower), icon, edge. */
    private static final float TABS_X = 2.0F;
    private static final float TAB_WIDE = 13.0F;
    private static final float TAB_PITCH = 14.0F;
    private static final float TAB_HIGH = 14.0F;
    private static final float TAB_DROP = 3.0F;
    private static final float TAB_ICON = 11.0F;
    /** More than five tabs are narrower, so that all of them stand side by side without covering each other. */
    private static final int TABS_FULL = 5;
    private static final float TAB_WIDE_NARROW = 11.0F;
    private static final float TAB_ICON_NARROW = 9.0F;
    private static final int TAB_EDGE = 0x905A3C18;
    /** The "only what can be made now" switch: a tab over the right page, just clear of the fold's shade. */
    private static final float FILTER_X = 6.0F;
    /** The search: a tab beside the switch; typing, the row of tabs over the list becomes one slip of paper with the text on it. */
    private static final float SEARCH_X = 21.0F;
    private static final float SLIP_HIGH = 11.0F;
    private static final float SLIP_TEXT_X = 11.0F;
    private static final int QUERY_MOST = 50;
    private static final float CARET_BLINK = 0.5F;
    /** How long a tab holding new recipes nods, seconds, and how high. */
    private static final float NOD_TIME = 0.8F;
    private static final float NOD_HEIGHT = 2.0F;
    /** The mark of a recipe new to the player: the ribbon's red, the one strong colour on the paper. */
    private static final int FRESH = 0xFFB5372B;
    /** The card the variants of one place lie on, opened over the list. */
    private static final int CARD = 0x165A3C18;
    /** The least room kept between the tabs and the page's edges once the row has to move over to fit. */
    private static final float TAB_MARGIN = 1.0F;
    /** The ribbon that closes the book, at the top of the right page: a bookmark coming out from under the head of the pages. */
    private static final int RIBBON_X = 62;
    private static final int RIBBON_WIDTH = 6;
    private static final int RIBBON_HEIGHT = 13;
    /** The shade in the fold of the pages. */
    private static final int FOLD = 0x285A3C18;
    private static final int FOLD_WIDTH = 5;
    /** The right page's middle, clear of the fold's shade on its left. */
    private static final float RIGHT_MIDDLE = (UNITS_WIDE + FOLD_WIDTH) / 2.0F;
    /** Small things at the foot of a page: how big, how far apart; the dish at the head of its page. */
    private static final float FOOT_SIZE = 12.0F;
    private static final float FOOT_PITCH = 15.0F;
    private static final float DISH_SIZE = 16.0F;
    private static final float DISH_Y = 2.0F;
    /** Where the dish's name starts, and the room between the rule under it and what goes in. */
    private static final float NAME_Y = 20.0F;
    /** A name longer than two lines: drawn this much smaller, in this many lines at most. */
    private static final float LONG_NAME_SCALE = 0.72F;
    private static final int LONG_NAME_LINES = 3;
    private static final float INGREDIENTS_GAP = 3.0F;
    /** How many recipes laid last the "made lately" tab keeps, and its kind. */
    private static final int RECENT_MOST = 24;
    static final String RECENT = "#recent";
    /** The ribbon takes the cursor a little round it: it is the only way to shut the book. */
    private static final float RIBBON_REACH = 2.0F;
    /** The least light the book is drawn in, block and sky: a torch-lit kitchen. */
    private static final int LIT = 12;
    /** How long a book handed over waits in the air for the next station to take it, seconds (the server lets the player in). */
    private static final float HANDOFF_WAIT = 1.0F;
    /** How small and how big the player can make the book, against its own size (Hands-On Delight's). */
    private static final float SIZE_LEAST = 0.75F;
    private static final float SIZE_MOST = 1.6F;
    /** Half the open book over its covers, node units: the pages and a model pixel of cover round them. */
    private static final float HALF_WIDE = 7.0F * SIZE;
    private static final float HALF_HIGH = 5.0F * SIZE;
    /** The corner that sizes the book, the top right one: its sides along the width (the right page at -x) and the height. */
    private static final int SIZING_CORNER_X = -1;
    private static final int SIZING_CORNER_Y = 1;
    /** A corner is taken on the cover's corner and the outer bit of the paper there, clear of the ribbon, node units. */
    private static final float CORNER_IN = 0.25F;
    /** How far the book can be moved from its place: a part of its width and of its height. */
    private static final float MOVE_WIDE = 0.75F;
    private static final float MOVE_HIGH = 0.5F;
    /** Kept between the book and the edges of the screen, node units. */
    private static final float SCREEN_MARGIN = 0.3F;
    /** How far a corner under the cursor rises towards the player, degrees, and how much a held book comes forward. */
    private static final float CORNER_RISE = 5.0F;
    private static final float HELD_GROWTH = 0.03F;
    /**
     * The keys of the book's size and place in the book's file (beside the open or shut key): the size, then the place along its
     * width and height, node units at its own size.
     */
    private static final String[] PLACE_KEYS = {"book.size", "book.x", "book.y"};
    /** The size and place the player gave the open book, the same at every station; read from the file at first use. */
    private static float[] kept;

    private RecipeBookModel model;
    /** The size and place drawn now, going after the kept ones (at once while the player holds the book). */
    private float shownSize;
    private float shownX;
    private float shownY;
    /** Where the cursor is on the book's own plane, node units (x across, z up the page); null: not over it. */
    private Vector3f pointer;
    /** The book in the player's hand, by a corner to size it or by the rest to move it; null when nobody holds it. */
    private Hold hold;
    /** How much the book is held (comes forward), and the corner raised towards the cursor, each -1 to 1 along width and height. */
    private float held;
    private float riseX;
    private float riseY;
    /** The move given to the node this frame, scene pixels: its place, the bob, the way to where it lies, the way over from another station. */
    private final Vector3f moved = new Vector3f();
    private List<Entry> entries = List.of();
    private boolean wanted;
    private float open;
    private float flip = 1.0F;
    private int flipWay = 1;
    /** How long the running flip takes, and when the last turn was, seconds of {@code time}; the last page sound, milliseconds. */
    private float flipTime = FLIP_TIME;
    private float lastTurn = -10.0F;
    private long lastTurnSound;
    /** What the left page listed when the running flip started: the sheet turned over shows it, not the list that is there now. */
    private Leaf flipFrom;
    /** A turned-down corner held: the way it turns (0: none), since when, and how many pages it has turned. */
    private int leafing;
    private float leafSince;
    private int leafDone;
    private int page;
    private Entry picked;
    private Entry hovered;
    /**
     * The last recipe of the list under the cursor: the right page keeps it while the cursor goes over to read it, until the cursor
     * leaves the book or the list changes.
     */
    private Entry previewed;
    /** What goes in, as last laid out on the right page, the one under the cursor, and the stack it showed when the cursor came. */
    private List<Spot> spots = List.of();
    private Spot hoveredSpot;
    private ItemStack spotShown = ItemStack.EMPTY;
    /** The recipe {@code hoveredSpot} is of, and the recipe that makes it (null: the book has none). */
    private Entry spotFor;
    private Page spotTarget;
    /** The cursor is on the dish at the head of the right page. */
    private boolean overDish;
    /** Every page of the book by its key, and the pages that make each item, as last read, whatever the tab and the switch. */
    private java.util.Map<Object, Page> allPages = java.util.Map.of();
    private java.util.Map<net.minecraft.world.item.Item, List<Page>> makes = java.util.Map.of();
    /** What the list was last read from (the pool, the grid, the pages, the switch, the tab): unchanged, a read can be skipped. */
    private int readFrom;
    private List<Page> readPages;
    /** The tooltip shown now on the book, set again only when it changes. */
    private Component tip;
    /** What the player searches for, and whether they are typing it now (a click on the search tab or on the slip). */
    private String query = "";
    private boolean typing;
    private float typingFrom;
    private boolean overSearch;
    private boolean overSlip;
    /** When the switch last emptied the list, to nod it. */
    private float switchNodFrom = -10.0F;
    private boolean emptiedBySwitch;
    /** The page and the recipe picked of each tab of each book, while the game runs: the book opens where the player left it. */
    private static final java.util.Map<String, Integer> PAGES = new java.util.HashMap<>();
    private static final java.util.Map<String, Object> PICKS = new java.util.HashMap<>();
    /**
     * The place on the list under the cursor and its variant shown there when the cursor came: it stays shown, so what a click lays is
     * what the player sees, though the place goes on through its variants while nobody points at it.
     */
    private Object hoverPlace;
    private Object hoverKey;
    /** The place on the list whose variants the left page shows instead of the list (a right click on it); null: the list. */
    private Object openPlace;
    private int pageBeforeOpen;
    /** The vanilla book's "only what can be made now" switch, a tab over the right page: the cursor is on it. */
    private boolean overFilter;
    /** Recipes new to the player when this book showed them: they keep a red mark while it is open. */
    private final java.util.Set<Object> fresh = new java.util.HashSet<>();
    /** The kinds holding recipes the player has not seen ("" for all), whose tabs nod when the book opens or gets more. */
    private java.util.Set<String> newKinds = java.util.Set.of();
    private float nodFrom = -10.0F;
    /** The tabs over the list (none for a book of one kind), the kind shown (null: all), the tab under the cursor. */
    private List<Tab> tabs = List.of();
    private String tab;
    /** The tab the player chose, kept in the file; {@code tab} is the one shown, "all" while the chosen one holds nothing. */
    private String chosenTab;
    private boolean tabRemembered;
    private Tab hoveredTab;
    private float refresh;
    private float reach;
    private float time;
    /** The light the book is drawn in: where it hangs, never darker than a lit kitchen, so it stays readable. */
    private int shade = LightTexture.FULL_BRIGHT;
    /** The last time the player's statistics were asked for, milliseconds. */
    private long statsAsked;
    /** The dish's name broken into lines, for the entry it was broken for. */
    private Entry nameFor;
    /** The scale the dish's name is drawn at: 1, or smaller for a name that needs more than two lines. */
    private float nameScale = 1.0F;
    private List<FormattedCharSequence> nameLines = List.of();
    /** Where the cursor is on the left page (units), or on the right one past its width; negative: not on the book. */
    private float cursorX = -1.0F;
    private float cursorY = -1.0F;
    private StationView station;
    /** The player closed the book by its ribbon: it stays shut at the next station until they open it again. */
    /**
     * Whether the player has the book open: shut until they open it, and kept as they left it, also over restarts (in the file of
     * the chosen tabs, as {@code <book>.open}).
     */
    private boolean keptOpen() {
        String kept = BookTabs.get(source.memory() + ".open");
        return source.openByDefault() ? !"false".equals(kept) : "true".equals(kept);
    }

    private void keepOpen(boolean value) {
        String kept = source.openByDefault() ? value ? null : "false" : value ? "true" : null;
        BookTabs.put(source.memory() + ".open", kept);
    }

    /** Whether the book is shown: the config can hide it, unless the station cannot be worked without it. */
    private boolean shown() {
        return source.required() || true;
    }
    /** The book in the air between two stations, while the camera flies over, so it does not vanish and open again. */
    private static Handoff handoff;
    /** The one this book handed over and still draws at its own place until the next station takes it. */
    private Handoff leaving;
    /** Where the book came from, this station's pixels from its own place, while it flies over with the camera; null at home. */
    private Vector3f arrivingFrom;
    /** How much of {@code arrivingFrom} is still to go, as drawn last; held: the player went on before it got here, it waits in the air. */
    private float arrivingLeft = 1.0F;
    private boolean arrivingHeld;
    private final Source source;
    /**
     * Applies a model's GUI transform and gives the model to draw after it: on Forge a model can give another one for the GUI
     * (separate transforms), so the loader sets its own; this one is vanilla's.
     */
    static java.util.function.BiFunction<BakedModel, PoseStack, BakedModel> guiModel = (model, poseStack) -> {
        model.getTransforms().getTransform(ItemDisplayContext.GUI).apply(false, poseStack);
        return model;
    };
    /**
     * Where the book lies shut, from the scene ({@code extra.rest} of {@code book_open}: {@code offset} pixels from where it hangs,
     * {@code turn} extra degrees, {@code scale}): put away, it lies on the station in small; null: it hangs shut as the kitchen's do.
     */
    private Rest rest;
    private boolean restRead;
    /** How far it has gone down to where it lies, 0 hanging to 1 lying; how it was last frame, to start it lying. */
    private float lie;
    private boolean wasVisible;
    /** Seconds the book takes to go down to its place or up from it. */
    private static final float LIE_TIME = 0.22F;
    /** How near its hanging place it is (as {@code lie}) when the covers start to swing, and how shut they are when it goes down. */
    private static final float LIE_OVERLAP = 0.3F;

    private record Rest(Vector3f offset, Vector3f turn, float scale) {
    }

    /** A book going over to the next station: where it hung in the world and how it was, open or put away. */
    private static final class Handoff {
        private final Vec3 where;
        private final float open;
        private final boolean wanted;
        private final float time;
        private final long since = Util.getMillis();
        private boolean taken;

        private Handoff(Vec3 where, float open, boolean wanted, float time) {
            this.where = where;
            this.open = open;
            this.wanted = wanted;
            this.time = time;
        }

        private boolean isStale() {
            return Util.getMillis() - since > HANDOFF_WAIT * 1000.0F;
        }
    }

    /**
     * The book taken by the player: by a corner ({@code cornerX}, {@code cornerY} the corner's side, -1 or 1, along the width and the
     * height) the corner across from it stays and the book grows or shrinks with the hand; by anything else it goes with the hand.
     * {@code grabX}, {@code grabY}: the point taken, node units; the rest is the book as it was taken.
     */
    private record Hold(int cornerX, int cornerY, float grabX, float grabY, float size, float x, float y) {
        boolean bySize() {
            return cornerX != 0;
        }
    }

    RecipeBook(Source source) {
        this.source = source;
        float[] place = kept();
        shownSize = place[0];
        shownX = place[1];
        shownY = place[2];
    }

    private static float[] kept() {
        if (kept == null) {
            kept = new float[] {Mth.clamp(number(PLACE_KEYS[0], 1.0F), SIZE_LEAST, SIZE_MOST), number(PLACE_KEYS[1], 0.0F),
                    number(PLACE_KEYS[2], 0.0F)};
        }
        return kept;
    }

    /** A number kept in the book's file under {@code key}, or {@code fallback} when there is none or it is not a number. */
    private static float number(String key, float fallback) {
        String value = BookTabs.get(key);
        if (value == null) {
            return fallback;
        }
        try {
            float number = Float.parseFloat(value);
            return Float.isFinite(number) ? number : fallback;
        } catch (NumberFormatException exception) {
            return fallback;
        }
    }

    /** Keeps the size and place, in memory and in the book's file; the book's own size and place leave no keys behind. */
    private static void keep(float size, float x, float y) {
        float[] place = kept();
        place[0] = size;
        place[1] = x;
        place[2] = y;
        boolean usual = Math.abs(size - 1.0F) < 0.005F && Math.abs(x) < 0.005F && Math.abs(y) < 0.005F;
        for (int i = 0; i < PLACE_KEYS.length; i++) {
            BookTabs.put(PLACE_KEYS[i], usual ? null : Float.toString(place[i]));
        }
    }

    /** What a station's book lists: its recipes, and what lies in the station already, which counts as the player's. */
    interface Source {
        List<Page> pages(StationView view);

        List<ItemStack> inStation(StationView view);

        /** The player picked a page by a click. */
        default void picked(StationView view, Page page) {
        }

        /** The key of the page the station is making now (what lies in it), to open the book there; null for none. */
        default Object current(StationView view) {
            return null;
        }

        /**
         * The kinds of recipe the book can show alone, as tabs over the list, the first one showing them all (kind null). Fewer than
         * two kinds: no tabs.
         */
        default List<Tab> tabs(StationView view) {
            return List.of();
        }

        /** Under which name the chosen tab is remembered on the client: one per station type. */
        default String memory() {
            return "";
        }

        /**
         * Whether the book has the vanilla book's "only what can be made now" switch, kept where the vanilla book keeps it (the
         * player's recipe book on the server), so both books show the same.
         */
        default boolean hasFilter() {
            return false;
        }

        /** The icon on the switch: the station whose work it filters. */
        default ItemStack filterIcon() {
            return new ItemStack(net.minecraft.world.item.Items.CRAFTING_TABLE);
        }

        default boolean filtering() {
            return false;
        }

        default void setFiltering(boolean value) {
        }

        /** Whether the book opens by itself until the player shuts it: a station worked only through its book. */
        default boolean openByDefault() {
            return false;
        }

        /** Whether the station cannot be worked without the book, so the config switch that hides books leaves it. */
        default boolean required() {
            return false;
        }

        /** Whether the player has not seen the page's recipe in a recipe book since it was unlocked. */
        default boolean isNew(Page page) {
            return false;
        }

        /** The page was shown: the recipe is no longer new, here and in the vanilla book. */
        default void seen(Page page) {
        }

        /**
         * Whether the list is ordered by name within its bands (new, ready, the rest); false keeps the order {@link #pages} gives,
         * the bands still applying: a station whose choices are numbered by their place, as the loom's patterns.
         */
        default boolean sorted() {
            return true;
        }

        /** The page under the cursor on the list or at the head of the right page, every frame; null for none. */
        default void hovered(StationView view, Page page) {
        }

        /**
         * Whether the right page shows the station's {@link #current} page when the one picked is no longer listed: a station whose
         * list changes with what lies in it (the loom with a pattern item, which offers one pattern).
         */
        default boolean followsCurrent() {
            return false;
        }
    }

    /**
     * A tab over the list: the kind of recipe it shows ({@code Page.kind}; null: all; {@link #RECENT}: the recipes laid last), drawn
     * as {@code icon}, named in its tooltip.
     */
    record Tab(String kind, ItemStack icon, Component name) {
    }

    /** What the left page lists: a page of the list, or of the variants of one place on a card. */
    private record Leaf(List<Entry> list, int page, boolean card) {
    }

    /** One thing that goes in, as laid out on the right page: its well, and which of the dish's ingredients it is. */
    private record Spot(float x, float y, int index) {
        boolean contains(float pointX, float pointY) {
            return pointX >= x && pointX < x + WELL_SIZE && pointY >= y && pointY < y + WELL_SIZE;
        }
    }

    /**
     * One recipe: the dish on the list, what goes in, and small things at the foot of its page, with a note after them. {@code key}
     * keeps the page picked while the list is read again; {@code stars}: the pot's stars of the dish are shown. Pages of one
     * {@code group} (and kind) share one place on the list, as the vanilla book shows the colours of wool as one button.
     */
    record Page(Object key, ItemStack dish, List<Ingredient> ingredients, List<ItemStack> foot, Component note, boolean stars,
            String kind, String group) {
        Page(Object key, ItemStack dish, List<Ingredient> ingredients, List<ItemStack> foot, Component note, boolean stars) {
            this(key, dish, ingredients, foot, note, stars, null, null);
        }

        /** What the page's place on the list is known by: its group, or the page itself when it has none. */
        Object place() {
            return group == null || group.isEmpty() ? key : kind + "/" + group;
        }
    }

    /**
     * A page, which of its ingredients the player has, and how many of the dish they have made, which opens its stars;
     * {@code variants}: the pages sharing its place on the list, itself among them, what can be made now first.
     */
    private static final class Entry {
        private final Page page;
        private final boolean[] found;
        private final int missing;
        private final int made;
        private final String name;
        /** Shared by all the variants; filled once they are all read. */
        private List<Entry> variants = List.of(this);

        private Entry(Page page, boolean[] found, int missing, int made) {
            this.page = page;
            this.found = found;
            this.missing = missing;
            this.made = made;
            name = page.dish().getHoverName().getString();
        }

        String name() {
            return name;
        }

        Page page() {
            return page;
        }

        boolean[] found() {
            return found;
        }

        int missing() {
            return missing;
        }

        int made() {
            return made;
        }

        List<Entry> variants() {
            return variants;
        }

        ItemStack dish() {
            return page.dish();
        }

        List<Ingredient> ingredients() {
            return page.ingredients();
        }

        boolean isReady() {
            return missing == 0;
        }

        /** Whether anything of its place on the list can be made now. */
        boolean anyReady() {
            return variants.get(0).isReady();
        }

        /** The least that is missing for any variant: how far down the list its place goes. */
        int leastMissing() {
            return variants.get(0).missing();
        }
    }

    /** The book of the station worked at, while it is open, for the dish under the cursor. */
    private static RecipeBook worked;

    /** Whether the player is typing into the search of the book of the station worked at: its letters are no viewer's keys. */
    static boolean typing() {
        RecipeBook book = worked;
        return book != null && book.typing && book.wanted && book.station != null && isWorkedAt(book.station);
    }

    /**
     * What is under the cursor in the open book of the station worked at, for the recipe viewers' keys: a dish of the list, the dish
     * at the head of the right page, or one of the things that go in; empty when none.
     */
    static ItemStack hoveredDish() {
        RecipeBook book = worked;
        if (book == null || !book.wanted || book.station == null || !isWorkedAt(book.station)) {
            return ItemStack.EMPTY;
        }
        if (book.hoveredSpot != null) {
            return book.spotShown;
        }
        if (book.hovered != null) {
            return book.hovered.dish();
        }
        Entry shown = book.rightShown();
        return book.overDish && shown != null ? shown.dish() : ItemStack.EMPTY;
    }

    /** The recipe the right page shows: the one under the cursor on the list, else the one last pointed at, else the one picked. */
    private Entry rightShown() {
        return hovered != null ? hovered : previewed != null ? previewed : picked;
    }

    boolean isOpen() {
        return wanted;
    }

    /** The player came to the pot: the book is open already, unless they put it away. */
    void opened(StationView view) {
        if (!wanted && keptOpen() && shown()) {
            toggle(view);
        }
    }

    void toggle(StationView view) {
        station = view;
        wanted = !wanted;
        if (wanted) {
            // How much of each dish the player has made is in their statistics, which the server sends when asked.
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            opening();
            read(view, true);
            restore(view);
        } else {
            openPlace = null;
            leafing = 0;
            endSearch();
            sound(SoundEvents.BOOK_PUT, 1.0F, 0.5F);
        }
    }

    /** Under which name the page and the pick of the tab shown are kept while the game runs. */
    private String memoryKey() {
        return source.memory() + "/" + (tab == null ? "" : tab);
    }

    /** Keeps the page of the list in view for its tab (not of a card of variants). */
    private void rememberPage() {
        if (openPlace == null && !searching()) {
            PAGES.put(memoryKey(), page);
        }
    }

    /**
     * Opens the list where the player left this tab: what the station makes now if anything, else the recipe picked there last and
     * the page they had turned to, else the first page.
     */
    private void restore(StationView view) {
        previewed = null;
        Object current = source.current(view);
        Entry now = current == null ? null : listedVariant(current);
        if (now != null) {
            picked = now;
            page = placeIndex(now) / PER_PAGE;
            return;
        }
        Object keptPick = PICKS.get(memoryKey());
        Entry pick = keptPick == null ? null : listedVariant(keptPick);
        picked = pick != null ? pick : entries.isEmpty() ? null : entries.get(0);
        Integer keptPage = PAGES.get(memoryKey());
        page = Mth.clamp(keptPage != null ? keptPage : pick != null ? placeIndex(pick) / PER_PAGE : 0, 0, pages() - 1);
    }

    /** The variant on the list whose page has {@code key}; null when the list does not hold it. */
    private Entry listedVariant(Object key) {
        for (Entry place : entries) {
            for (Entry variant : place.variants()) {
                if (variant.page().key().equals(key)) {
                    return variant;
                }
            }
        }
        return null;
    }

    /** One of the book's sounds, as loud as the config's sound volume says. */
    private static void sound(net.minecraft.sounds.SoundEvent sound, float pitch, float volume) {
        float loud = volume * io.github.salvagedisc.fallow.FallowConfig.postesSoundVolume();
        if (loud > 0.0F) {
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(sound, pitch, loud));
        }
    }

    /** The book opens: the list as it is, the new recipes marked afresh, the tabs holding them nodding. */
    private void opening() {
        openPlace = null;
        fresh.clear();
        nodFrom = time;
    }

    /** Where on the list a page is: the index of its place. */
    private int placeIndex(Entry variant) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).variants().contains(variant)) {
                return i;
            }
        }
        return 0;
    }

    /**
     * The player left the station. Going over to a neighbour, the book goes with them: it stays as it is until the next station's
     * book takes its place and flies from here to there with the camera. Leaving, it closes and goes away as the camera flies back.
     */
    void shut(StationView view) {
        if (hold != null) {
            letGo();
        }
        endSearch();
        Optional<NodeView> node = view.node("book_open");
        if (DiegesisClient.goingOver() < 1.0F && node.isPresent() && node.get().isVisible()) {
            passOn(node.get());
            return;
        }
        arrivingFrom = null;
        arrivingHeld = false;
        wanted = false;
    }

    /**
     * How much of the book is there, 0 to 1: it grows where it hangs as the camera flies in, and goes as the camera flies back, so it
     * neither pops up before the station is reached nor vanishes in the first frame of leaving. Going over, it is all there.
     */
    static float appear(StationView view) {
        boolean worked = DiegesisClient.focusedStation().map(focused -> focused.pos().equals(view.pos())).orElse(false);
        if (!worked || DiegesisClient.goingOver() < 1.0F) {
            return 1.0F;
        }
        // Over the last three quarters of the way in, and gone in the first part of the way back: far off it would be a speck.
        float t = Mth.clamp((DiegesisClient.arrival() - APPEAR_FROM) / (1.0F - APPEAR_FROM), 0.0F, 1.0F);
        return t * t * (3.0F - 2.0F * t);
    }

    /** Hands the book over to the next station from where it is now, also half way here (two quick presses of D). */
    private void passOn(NodeView node) {
        if (arrivingFrom != null && !arrivingHeld) {
            arrivingFrom.mul(arrivingLeft);
            arrivingHeld = true;
        }
        leaving = new Handoff(node.worldPosition(), open, wanted, time);
        handoff = leaving;
    }

    /** The station the player goes over to is this one: its book takes the one in the air, open as it was, and flies it here. */
    private void arrive(StationView view, NodeView node, Handoff passing) {
        handoff = null;
        passing.taken = true;
        station = view;
        arrivingHeld = false;
        wanted = passing.wanted;
        open = passing.open;
        time = passing.time;
        if (wanted) {
            var connection = Minecraft.getInstance().getConnection();
            if (connection != null) {
                connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
            }
            opening();
            read(view, true);
            restore(view);
            turnSound();
        }
        // The way back to where it hung, in this station's pixels: the scene may face another way than the one left.
        Vector3f home = new Vector3f(node.definition().transform().pos());
        Vec3 origin = view.toWorld(home.x, home.y, home.z);
        Vec3 alongX = view.toWorld(home.x + 1.0F, home.y, home.z).subtract(origin);
        Vec3 alongY = view.toWorld(home.x, home.y + 1.0F, home.z).subtract(origin);
        Vec3 alongZ = view.toWorld(home.x, home.y, home.z + 1.0F).subtract(origin);
        Vec3 away = passing.where.subtract(origin);
        arrivingFrom = new Vector3f((float) (away.dot(alongX) / alongX.lengthSqr()), (float) (away.dot(alongY) / alongY.lengthSqr()),
                (float) (away.dot(alongZ) / alongZ.lengthSqr()));
    }

    static boolean isWorkedAt(StationView view) {
        return DiegesisClient.focusedStation().map(focused -> focused.pos().equals(view.pos())).orElse(false);
    }

    private void turnSound() {
        turnSound(false);
    }

    /** A page turning; quick turns softer, and never so close together that leafing rattles. */
    private void turnSound(boolean quick) {
        long now = Util.getMillis();
        if (quick && now - lastTurnSound < TURN_SOUND_GAP) {
            return;
        }
        lastTurnSound = now;
        sound(SoundEvents.BOOK_PAGE_TURN, 1.0F, quick ? 0.35F : 0.6F);
    }

    /** What the player has made changed (a serving, a dish): the stars on the page follow once the server tells the new numbers. */
    void statsChanged() {
        long now = Util.getMillis();
        var connection = Minecraft.getInstance().getConnection();
        if (wanted && connection != null && now - statsAsked > 1000L) {
            statsAsked = now;
            connection.send(new ServerboundClientCommandPacket(ServerboundClientCommandPacket.Action.REQUEST_STATS));
        }
    }

    /**
     * Reads the list again. {@code resort}: the book was opened or the player changed what it shows (a tab, the switch, the search),
     * so the list is put in order afresh; otherwise (the timer) it is read only when something it is made from changed, and under
     * the cursor keeps its order, so a dish never moves away from where the player points.
     */
    private void read(StationView view, boolean resort) {
        List<Page> all = source.pages(view);
        List<ItemStack> pool = pool(view);
        boolean filtering = source.hasFilter() && source.filtering();
        if (!tabRemembered) {
            tabRemembered = true;
            chosenTab = BookTabs.get(source.memory());
        }
        int from = signature(pool, filtering) * 31 + query.hashCode();
        if (!resort && all == readPages && from == readFrom) {
            return;
        }
        readPages = all;
        readFrom = from;
        List<String> recent = recent();
        // Every page by its key and by what it makes, whatever is shown: a thing that goes in leads to its own recipe.
        java.util.Map<Object, Page> byKey = new java.util.HashMap<>();
        java.util.Map<net.minecraft.world.item.Item, List<Page>> making = new java.util.HashMap<>();
        java.util.Set<String> kinds = new java.util.HashSet<>();
        boolean anyRecent = false;
        for (Page page : all) {
            byKey.put(page.key(), page);
            making.computeIfAbsent(page.dish().getItem(), item -> new ArrayList<>()).add(page);
            if (page.kind() != null) {
                kinds.add(page.kind());
            }
            anyRecent |= recent.contains(page.key().toString());
        }
        // A tab with nothing in it is not offered (a new player has no redstone yet), as the vanilla book hides them.
        boolean hasRecent = anyRecent;
        List<Tab> offered = source.tabs(view).stream().filter(it -> it.kind() == null
                || (RECENT.equals(it.kind()) ? hasRecent : kinds.contains(it.kind()))).toList();
        tabs = offered.size() > 2 ? offered : List.of();
        tab = tabs.stream().anyMatch(it -> Objects.equals(it.kind(), chosenTab)) ? chosenTab : null;
        boolean searched = !query.isBlank();
        boolean lately = !searched && RECENT.equals(tab);
        // Pages of one group share a place, as in the vanilla book; with the switch on, only what can be made now is listed.
        java.util.Map<Object, List<Entry>> places = new java.util.LinkedHashMap<>();
        java.util.Set<String> unseen = new java.util.HashSet<>();
        java.util.Set<Object> newKeys = new java.util.HashSet<>();
        int inTab = 0;
        for (Page page : all) {
            if (source.isNew(page)) {
                unseen.add("");
                newKeys.add(page.key());
                if (page.kind() != null) {
                    unseen.add(page.kind());
                }
            }
            if (searched ? !BookMath.matches(query, page.dish().getHoverName().getString(), page.key().toString())
                    : lately ? !recent.contains(page.key().toString()) : tab != null && !tab.equals(page.kind())) {
                continue;
            }
            inTab++;
            Entry entry = entry(page, pool);
            if (!filtering || entry.isReady()) {
                // Made lately, each recipe is its own place: the one laid, not its group.
                places.computeIfAbsent(lately ? page.key() : page.place(), key -> new ArrayList<>()).add(entry);
            }
        }
        if (!unseen.equals(newKinds)) {
            if (!unseen.isEmpty() && !newKinds.containsAll(unseen)) {
                nodFrom = time;
            }
            newKinds = unseen;
        }
        List<Entry> found = new ArrayList<>();
        Comparator<Entry> byName = Comparator.comparing(Entry::name);
        for (List<Entry> variants : places.values()) {
            variants.sort(Comparator.comparingInt(Entry::missing).thenComparing(byName));
            List<Entry> shared = List.copyOf(variants);
            for (Entry variant : shared) {
                variant.variants = shared;
            }
            found.add(shared.get(0));
        }
        if (lately) {
            // Newest first, and they stay where they are: the point of the tab is that sticks are always in the same place.
            found.sort(Comparator.comparingInt(entry -> recent.indexOf(entry.page().key().toString())));
        } else {
            // New to the player first, then what can be made now, then the rest; by name within each, so a recipe has a place
            // (or in the source's own order, which a stable sort keeps).
            Comparator<Entry> bands = Comparator.comparingInt((Entry entry) -> band(entry, newKeys));
            found.sort(source.sorted() ? bands.thenComparing(byName) : bands);
        }
        if (!resort && cursorX >= 0.0F && !entries.isEmpty()) {
            java.util.Map<Object, Integer> before = new java.util.HashMap<>();
            for (int i = 0; i < entries.size(); i++) {
                before.putIfAbsent(entries.get(i).page().place(), i);
            }
            // Stable: what was not there before comes after, in its own order.
            found.sort(Comparator.comparingInt(entry -> before.getOrDefault(entry.page().place(), Integer.MAX_VALUE)));
        }
        entries = found;
        allPages = byKey;
        makes = making;
        if (picked != null) {
            // Kept while the book has its recipe, also when the list does not show it (an ingredient's recipe hidden by the switch).
            Object kept = picked.page().key();
            Entry listed = listedVariant(kept);
            Page page = byKey.get(kept);
            picked = listed != null ? listed : page != null ? entry(page, pool) : null;
        }
        if (source.followsCurrent() && (picked == null || listedVariant(picked.page().key()) == null)) {
            Object current = source.current(view);
            Entry now = current == null ? null : listedVariant(current);
            if (now != null) {
                picked = now;
            }
        }
        if (previewed != null) {
            previewed = listedVariant(previewed.page().key());
        }
        if (openPlace != null && opened() == null) {
            closePlace();
        }
        page = Math.min(page, pages() - 1);
        // The switch emptied the list: it nods, so the player sees why the page is blank.
        boolean emptied = filtering && found.isEmpty() && inTab > 0;
        if (emptied && !emptiedBySwitch) {
            switchNodFrom = time;
        }
        emptiedBySwitch = emptied;
    }

    /** Which band of the list a place goes in: new to the player, then what can be made now, then the rest. */
    private int band(Entry place, java.util.Set<Object> newKeys) {
        for (Entry variant : place.variants()) {
            if (newKeys.contains(variant.page().key()) || fresh.contains(variant.page().key())) {
                return 0;
            }
        }
        return place.anyReady() ? 1 : 2;
    }

    /** What the list is read from, in one number: the stacks the player has to craft with, the switch and the tab chosen. */
    private int signature(List<ItemStack> pool, boolean filtering) {
        int signature = Boolean.hashCode(filtering) * 31 + Objects.hashCode(chosenTab);
        for (ItemStack stack : pool) {
            signature = signature * 31 + System.identityHashCode(stack.getItem());
            signature = signature * 31 + stack.getCount();
            signature = signature * 31 + stack.getComponentsPatch().hashCode();
        }
        return signature;
    }

    /** The keys of the recipes laid from this book lately, newest first, kept in the book's file. */
    private List<String> recent() {
        String kept = BookTabs.get(source.memory() + ".recent");
        return kept == null || kept.isEmpty() ? List.of() : List.of(kept.split(","));
    }

    /** A recipe was laid from the book: it goes to the head of the recipes made lately. */
    private void made(Page page) {
        List<String> recent = new ArrayList<>(recent());
        String key = page.key().toString();
        recent.remove(key);
        recent.add(0, key);
        while (recent.size() > RECENT_MOST) {
            recent.remove(recent.size() - 1);
        }
        BookTabs.put(source.memory() + ".recent", String.join(",", recent));
        readPages = null;
    }

    /** The place whose variants the left page shows, as last read; null when none is open or it is gone. */
    private Entry opened() {
        if (openPlace == null) {
            return null;
        }
        for (Entry entry : entries) {
            if (entry.page().place().equals(openPlace)) {
                return entry;
            }
        }
        return null;
    }

    /** What the left page lists: the places, or the variants of the one opened. */
    private List<Entry> listed() {
        Entry place = opened();
        return place == null ? entries : place.variants();
    }

    /** Opens a place's variants on the left page, as the vanilla book's right click shows them over its list. */
    private void openPlace(Entry place) {
        flipFrom = leafNow();
        openPlace = place.page().place();
        pageBeforeOpen = page;
        page = 0;
        startFlip(1, false);
    }

    private void closePlace() {
        if (openPlace == null) {
            return;
        }
        flipFrom = leafNow();
        openPlace = null;
        page = Math.min(pageBeforeOpen, pages() - 1);
        startFlip(-1, false);
    }

    /** What the left page lists now. */
    private Leaf leafNow() {
        return new Leaf(listed(), page, opened() != null);
    }

    /** A sheet starts to go over, {@code way} 1 forward, -1 back; quick ones (soon after the last) are quicker and softer. */
    private void startFlip(int way, boolean quick) {
        flip = 0.0F;
        flipWay = way > 0 ? 1 : -1;
        flipTime = quick ? FLIP_QUICK : FLIP_TIME;
        previewed = null;
        turnSound(quick);
    }

    /**
     * The variant a place shows on the list: the one under the cursor stays; otherwise the place goes through the variants that can
     * be made now (all of them when none can), one a second and a half, as the vanilla book's buttons do.
     */
    private Entry shownAt(Entry place) {
        List<Entry> variants = place.variants();
        if (variants.size() == 1) {
            return place;
        }
        if (place.page().place().equals(hoverPlace)) {
            for (Entry variant : variants) {
                if (variant.page().key().equals(hoverKey)) {
                    return variant;
                }
            }
        }
        int ready = (int) variants.stream().filter(Entry::isReady).count();
        int choices = ready > 0 ? ready : variants.size();
        return variants.get((int) (Util.getMillis() / 1500L % choices));
    }

    /** What the player has to cook with: the inventory, the stack in the hand (a knife taken to cut), and what lies in the station. */
    private List<ItemStack> pool(StationView view) {
        List<ItemStack> pool = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        if (player != null) {
            for (int i = 0; i < Inventory.INVENTORY_SIZE; i++) {
                pool.add(player.getInventory().getItem(i));
            }
            pool.add(player.inventoryMenu.getCarried());
        }
        pool.addAll(source.inStation(view));
        pool.removeIf(ItemStack::isEmpty);
        return pool;
    }

    private static Entry entry(Page page, List<ItemStack> pool) {
        int[] left = pool.stream().mapToInt(ItemStack::getCount).toArray();
        List<Ingredient> ingredients = page.ingredients();
        boolean[] found = new boolean[ingredients.size()];
        int missing = 0;
        for (int i = 0; i < found.length; i++) {
            if (ingredients.get(i).isEmpty()) {
                // A gap in a shaped recipe's grid needs nothing.
                found[i] = true;
                continue;
            }
            for (int stack = 0; stack < left.length && !found[i]; stack++) {
                if (left[stack] > 0 && ingredients.get(i).test(pool.get(stack))) {
                    left[stack]--;
                    found[i] = true;
                }
            }
            missing += found[i] ? 0 : 1;
        }
        var player = Minecraft.getInstance().player;
        int made = player == null ? 0 : player.getStats().getValue(Stats.ITEM_CRAFTED.get(page.dish().getItem()));
        return new Entry(page, found, missing, made);
    }

    private int pages() {
        return Math.max(1, (listed().size() + PER_PAGE - 1) / PER_PAGE);
    }

    /** Every frame: opens or closes, hangs in the air, follows the cursor. */
    void frame(StationView view, float dt) {
        station = view;
        if (isWorkedAt(view)) {
            worked = this;
        }
        NodeView node = view.node("book_open").orElse(null);
        if (node == null) {
            return;
        }
        if (!shown()) {
            node.setVisible(false);
            return;
        }
        Handoff passing = handoff;
        if (leaving != null) {
            if (leaving.taken || leaving.isStale()) {
                if (handoff == leaving) {
                    handoff = null;
                }
                leaving = null;
                arrivingFrom = null;
                arrivingHeld = false;
                wanted = false;
                open = 0.0F;
            }
        } else if (passing != null && !passing.taken && !passing.isStale() && isWorkedAt(view)) {
            arrive(view, node, passing);
        }
        float flight = DiegesisClient.goingOver();
        if (arrivingFrom != null && !arrivingHeld && leaving == null && flight < 1.0F && !isWorkedAt(view)) {
            // The player went on before the book got here (the station was not theirs yet, so it was not shut).
            passOn(node);
        }
        if (arrivingFrom != null && !arrivingHeld && (flight >= 1.0F || !isWorkedAt(view))) {
            arrivingFrom = null;
        }
        float appear = appear(view);
        if (!restRead) {
            restRead = true;
            rest = readRest(node);
        }
        node.setVisible((view.isFocused() || leaving != null || arrivingFrom != null || isWorkedAt(view)) && appear > SPECK);
        if (node.isVisible() && !wasVisible && rest != null) {
            // It comes lying where the player left it; asked for, it then rises and opens.
            lie = 1.0F;
        }
        wasVisible = node.isVisible();
        if (!node.isVisible() && leaving == null) {
            open = 0.0F;
        }
        time += dt;
        // It comes in shut, growing with the camera, and opens once the camera is there: the covers swing out before the eyes.
        // Only a book that is shown opens: one left wanted at a station that is not drawn (quick A/D between neighbours) would
        // otherwise start opening again every frame, with a page sound each time (Hands-On Delight's open bug).
        boolean opening = node.isVisible() && wanted && appear >= OPEN_FROM && (rest == null || lie <= LIE_OVERLAP);
        if (rest != null) {
            // Mostly shut, then down to its place; mostly up from it, then open: the two run into each other, so it answers at once.
            float down = !wanted && open <= LIE_OVERLAP ? 1.0F : 0.0F;
            lie = Mth.clamp(lie + Math.signum(down - lie) * dt / LIE_TIME, 0.0F, 1.0F);
        }
        if (opening && open <= 0.0F) {
            turnSound();
        }
        open = Mth.clamp(open + (opening ? dt : -dt) / OPEN_TIME, 0.0F, 1.0F);
        flip = Math.min(1.0F, flip + dt / flipTime);
        // Grows and opens where it hangs, breathing a little.
        float eased = eased();
        reach = Mth.lerp(Math.min(1.0F, dt * 12.0F), reach, node.isHovered() && !wanted ? 1.0F : 0.0F);
        float lying = lie * lie * (3.0F - 2.0F * lie);
        // The size and place the player gave it are the hanging book's: lying on the table it is where it always lies.
        float hanging = 1.0F - lying;
        place(view, node, dt, appear >= 1.0F && arrivingFrom == null && lying <= 0.0F);
        float hangScale = Mth.lerp(eased, CLOSED_SIZE * (1.0F + 0.08F * reach), 1.0F) * drawnSize();
        float lieScale = rest == null ? hangScale : rest.scale() * (1.0F + 0.15F * reach);
        node.scaleTo(Mth.lerp(lying, hangScale, lieScale) * appear, 0.0F);
        float bob = Mth.sin(time * 1.3F) * 0.14F * hanging;
        Vector3f shift = onScene(node, shownX, shownY).mul(hanging);
        Quaternionf turn = facing(view, node, shift);
        if (arrivingFrom != null) {
            // With the camera, on the same easing: the book keeps its place on the screen while the kitchen moves under it, and comes
            // to the place the player gave it.
            float left = arrivingHeld ? 1.0F : 1.0F - flight;
            arrivingLeft = left;
            moved.set(arrivingFrom).mul(left).add(shift.x * (1.0F - left), shift.y * (1.0F - left) + bob, shift.z * (1.0F - left));
        } else if (rest != null) {
            moved.set(rest.offset()).mul(lying).add(shift.x, shift.y + bob, shift.z);
        } else {
            moved.set(shift.x, shift.y + bob, shift.z);
        }
        node.moveTo(moved.x, moved.y, moved.z, 0.0F);
        // Hanging, it faces the player wherever it was moved, sways a little, and a corner under the cursor rises towards the player,
        // as a page corner is lifted to be taken; lying, it is turned flat on the table as before.
        float swayX = Mth.sin(time * 0.9F) * 1.2F;
        float swayY = Mth.cos(time * 0.7F) * 1.5F;
        turn.mul(Transform.toQuaternion(new Vector3f(swayX - riseY * CORNER_RISE, swayY, riseX * CORNER_RISE), new Quaternionf()));
        Vector3f angles = turn.getEulerAnglesYXZ(new Vector3f()).mul(Mth.RAD_TO_DEG);
        if (rest != null) {
            node.rotateTo(Mth.lerp(lying, angles.x, rest.turn().x), Mth.lerp(lying, angles.y, rest.turn().y),
                    Mth.lerp(lying, angles.z, rest.turn().z), 0.0F);
        } else {
            node.rotateTo(angles.x, angles.y, angles.z, 0.0F);
        }
        refresh -= dt;
        if (refresh <= 0.0F && wanted) {
            refresh = REFRESH_TIME;
            read(view, false);
        }
        follow(view, node);
        leaf();
        if (wanted && open >= 1.0F && flip >= 1.0F) {
            seeShown();
        }
        // Words go into the tooltip, never onto the page: a tab's name, the switch, a thing that goes in.
        Component wantedTip = hoveredTab != null ? hoveredTab.name()
                : overFilter ? Component.translatable(source.filtering() ? "gui.recipebook.toggleRecipes.craftable"
                        : "gui.recipebook.toggleRecipes.all")
                : overSearch ? Component.translatable("gui.recipebook.search_hint")
                : hoveredSpot != null && !spotShown.isEmpty() ? spotShown.getHoverName() : null;
        if (!Objects.equals(wantedTip, tip)) {
            tip = wantedTip;
            node.setTooltip(tip);
        }
    }

    /** A turned-down corner held goes on turning pages, quicker and quicker, until it is let go, the cursor leaves it or the list ends. */
    private void leaf() {
        if (leafing == 0) {
            return;
        }
        if (!leftHeld() || !wanted || open < 1.0F || footSide() != leafing) {
            leafing = 0;
            return;
        }
        while (BookMath.repeatDue(time - leafSince, leafDone)) {
            leafDone++;
            int before = page;
            turn(leafing, false);
            if (page == before) {
                leafing = 0;
                return;
            }
        }
    }

    /** Whether the book shows a search: being typed, or its text standing on the slip. */
    private boolean searching() {
        return typing || !query.isEmpty();
    }

    /** The search is over: its text goes, the tabs come back (the book was shut or the station left). */
    private void endSearch() {
        typing = false;
        query = "";
    }

    private void setQuery(StationView view, String text) {
        if (text.equals(query)) {
            return;
        }
        query = text;
        openPlace = null;
        previewed = null;
        read(view, true);
        page = 0;
    }

    /**
     * A key while the player works at the table. Typing, the book takes every key (Enter or Escape ends the typing, Backspace takes
     * a letter back, with Ctrl all of them, Ctrl+V pastes), so that the station's own keys do not act on letters; otherwise only
     * Page Up and Page Down, Home and End, which turn the open book's pages.
     */
    boolean key(StationView view, int keyCode, int modifiers) {
        if (!wanted || open < 1.0F) {
            typing = false;
            return false;
        }
        boolean control = (modifiers & GLFW.GLFW_MOD_CONTROL) != 0;
        if (typing) {
            if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_KP_ENTER || keyCode == GLFW.GLFW_KEY_ESCAPE) {
                typing = false;
                if (query.isBlank()) {
                    stopSearch(view);
                }
            } else if (keyCode == GLFW.GLFW_KEY_BACKSPACE && !query.isEmpty()) {
                setQuery(view, control ? "" : query.substring(0, query.length() - 1));
            } else if (keyCode == GLFW.GLFW_KEY_V && control) {
                type(view, Minecraft.getInstance().keyboardHandler.getClipboard());
            } else {
                pageKey(keyCode);
            }
            return true;
        }
        return pageKey(keyCode);
    }

    /** A character typed: it goes into the search while the player is typing it. */
    boolean typed(StationView view, char character) {
        if (!typing || !wanted) {
            return false;
        }
        type(view, String.valueOf(character));
        return true;
    }

    private void type(StationView view, String text) {
        StringBuilder added = new StringBuilder(query);
        for (char character : text.toCharArray()) {
            if (net.minecraft.util.StringUtil.isAllowedChatCharacter(character) && added.length() < QUERY_MOST) {
                added.append(character);
            }
        }
        setQuery(view, added.toString());
    }

    /** Page Up and Down turn a page, Home and End go to the first and the last. */
    private boolean pageKey(int keyCode) {
        switch (keyCode) {
            case GLFW.GLFW_KEY_PAGE_DOWN -> turn(1, false);
            case GLFW.GLFW_KEY_PAGE_UP -> turn(-1, false);
            case GLFW.GLFW_KEY_HOME -> turn(-page, false);
            case GLFW.GLFW_KEY_END -> turn(pages() - 1 - page, false);
            default -> {
                return false;
            }
        }
        return true;
    }

    /** The search tab clicked again: the text goes and the tabs come back, on the page the tab was left at. */
    private void stopSearch(StationView view) {
        typing = false;
        query = "";
        openPlace = null;
        read(view, true);
        restore(view);
    }

    /**
     * The mouse wheel over the open book turns its pages, a page a notch, and stops at the ends. True whenever the book is open, so
     * that nothing behind it turns.
     */
    boolean scroll(double amount) {
        if (!wanted || open < 1.0F || amount == 0.0) {
            return wanted;
        }
        turn(amount > 0.0 ? -1 : 1, false);
        return true;
    }

    private static Rest readRest(NodeView node) {
        com.google.gson.JsonObject extra = node.extra();
        if (extra == null || !extra.has("rest")) {
            return null;
        }
        com.google.gson.JsonObject at = extra.getAsJsonObject("rest");
        return new Rest(vector(at, "offset"), vector(at, "turn"), at.has("scale") ? at.get("scale").getAsFloat() : CLOSED_SIZE);
    }

    private static Vector3f vector(com.google.gson.JsonObject from, String key) {
        if (!from.has(key)) {
            return new Vector3f();
        }
        com.google.gson.JsonArray values = from.getAsJsonArray(key);
        return new Vector3f(values.get(0).getAsFloat(), values.get(1).getAsFloat(), values.get(2).getAsFloat());
    }

    /** How far open the book is drawn: the covers lift slowly, swing over and settle flat, and close the same way back. */
    private float eased() {
        return open * open * (3.0F - 2.0F * open);
    }

    /** The angle between a lying page and flat at this opening: the pages rise a little towards their outer edges. */
    private float pageTilt() {
        return Mth.HALF_PI - 1.25F * OPEN * eased();
    }

    /** How high the fold of the pages stands over the node's plane, pixels. */
    private float foldHeight() {
        return (Mth.sin(1.25F * OPEN * eased()) + PAGE_GAP) * SIZE;
    }

    /**
     * The book's size and place: going after the hand while the player holds it, else easing to what was kept. Once the camera is in
     * and the book hangs, it is kept on the screen whatever the window: a smaller window moves it in (or makes it smaller) only while
     * it is that small, the kept place stays.
     */
    private void place(StationView view, NodeView node, float dt, boolean settled) {
        pointer = view.pointerOn("book_open", 0.0F).orElse(null);
        boolean taking = hold != null;
        if (taking && (!leftHeld() || !node.isVisible() || !wanted || open < 1.0F)) {
            letGo();
        } else if (taking && pointer != null) {
            follow(hold, settled ? onScreen(view, node, home(node)) : null);
        }
        float[] place = kept();
        float size = place[0];
        float x = place[1];
        float y = place[2];
        float[] screen = settled && hold == null ? onScreen(view, node, home(node)) : null;
        if (screen != null) {
            float[] fitted = fit(screen, size, x, y, 0, 0);
            size = fitted[0];
            x = fitted[1];
            y = fitted[2];
        }
        float ease = hold != null ? 1.0F : Math.min(1.0F, dt * 12.0F);
        shownSize = Mth.lerp(ease, shownSize, size);
        shownX = Mth.lerp(ease, shownX, x);
        shownY = Mth.lerp(ease, shownY, y);
        float step = Math.min(1.0F, dt * 14.0F);
        held = Mth.lerp(step, held, hold != null ? 1.0F : 0.0F);
        int[] corner = hold != null ? (hold.bySize() ? new int[] {hold.cornerX(), hold.cornerY()} : null)
                : onNothing() && node.isHovered() ? corner(riseX != 0.0F || riseY != 0.0F ? CORNER_IN * 0.6F : 0.0F) : null;
        riseX = Mth.lerp(step, riseX, corner == null ? 0.0F : corner[0]);
        riseY = Mth.lerp(step, riseY, corner == null ? 0.0F : corner[1]);
        if (Math.abs(riseX) < 0.002F && Math.abs(riseY) < 0.002F) {
            riseX = 0.0F;
            riseY = 0.0F;
        }
    }

    /** The book in the hand goes after it: sized from the corner across, or moved, then kept as far as the limits and the screen allow. */
    private void follow(Hold taken, float[] screen) {
        float size;
        float x;
        float y;
        if (taken.bySize()) {
            // The corner across stays; the size follows the hand along the line from that corner to where the book was taken. The
            // hand is on the plane the book hangs in at its own place and size: the node's units grow with the book.
            float handX = shownX + pointer.x * drawnSize();
            float handY = shownY + pointer.z * drawnSize();
            float stayX = taken.x() - taken.cornerX() * HALF_WIDE * taken.size();
            float stayY = taken.y() - taken.cornerY() * HALF_HIGH * taken.size();
            float fromX = taken.x() + taken.grabX() * taken.size() - stayX;
            float fromY = taken.y() + taken.grabY() * taken.size() - stayY;
            float along = ((handX - stayX) * fromX + (handY - stayY) * fromY) / Math.max(1.0E-4F, fromX * fromX + fromY * fromY);
            size = Mth.clamp(taken.size() * along, SIZE_LEAST, SIZE_MOST);
            x = stayX + taken.cornerX() * HALF_WIDE * size;
            y = stayY + taken.cornerY() * HALF_HIGH * size;
        } else {
            size = taken.size();
            // The point taken stays under the hand.
            x = shownX + (pointer.x - taken.grabX()) * drawnSize();
            y = shownY + (pointer.z - taken.grabY()) * drawnSize();
        }
        float[] fitted = screen == null ? limited(size, x, y) : fit(screen, size, x, y, taken.bySize() ? taken.cornerX() : 0,
                taken.bySize() ? taken.cornerY() : 0);
        kept()[0] = fitted[0];
        kept()[1] = fitted[1];
        kept()[2] = fitted[2];
    }

    /**
     * Whether the left button is down now. The game's own {@code MouseHandler.isLeftPressed} is only kept while no screen is open,
     * and the station is one: it always says up there.
     */
    private static boolean leftHeld() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    /** The size the open book is drawn at, against its own: as given, and a little bigger while it is held. */
    private float drawnSize() {
        return shownSize * (1.0F + HELD_GROWTH * held);
    }

    /** The place kept within the reach of its own: a part of the book's width and height from where it hangs. */
    private static float[] limited(float size, float x, float y) {
        return new float[] {size, Mth.clamp(x, -MOVE_WIDE * 2.0F * HALF_WIDE, MOVE_WIDE * 2.0F * HALF_WIDE),
                Mth.clamp(y, -MOVE_HIGH * 2.0F * HALF_HIGH, MOVE_HIGH * 2.0F * HALF_HIGH)};
    }

    /**
     * The book at {@code size} and {@code x, y} put where all of it is on the screen ({@code screen}: the screen's corners on the book's
     * plane), within its reach. A book too big for the screen is made smaller: from the corner across from {@code cornerX, cornerY}
     * when one is held, else about its middle.
     */
    private static float[] fit(float[] screen, float size, float x, float y, int cornerX, int cornerY) {
        float stayX = x - cornerX * HALF_WIDE * size;
        float stayY = y - cornerY * HALF_HIGH * size;
        float[] place = limited(size, x, y);
        for (int tries = 0; tries < 40; tries++) {
            place = limited(size, x, y);
            if (pushIn(screen, place) || size <= SIZE_LEAST) {
                return place;
            }
            size = Math.max(SIZE_LEAST, size - 0.02F);
            x = cornerX != 0 ? stayX + cornerX * HALF_WIDE * size : x;
            y = cornerY != 0 ? stayY + cornerY * HALF_HIGH * size : y;
        }
        return place;
    }

    /** Moves {@code place} (size, x, y) in from every edge of the screen it reaches over; true when all of it is on the screen then. */
    private static boolean pushIn(float[] screen, float[] place) {
        for (int pass = 0; pass < 3; pass++) {
            for (int edge = 0; edge < 4; edge++) {
                float[] in = inward(screen, edge);
                float clear = in == null ? SCREEN_MARGIN : clearance(in, place);
                if (clear < SCREEN_MARGIN) {
                    place[1] += in[0] * (SCREEN_MARGIN - clear);
                    place[2] += in[1] * (SCREEN_MARGIN - clear);
                }
            }
        }
        // Pushed in from one side and out again over the other: it does not fit.
        for (int edge = 0; edge < 4; edge++) {
            float[] in = inward(screen, edge);
            if (in != null && clearance(in, place) < SCREEN_MARGIN * 0.5F) {
                return false;
            }
        }
        return true;
    }

    /** An edge of the screen on the book's plane: its normal turned to the inside, and a point of it; null when it has no length. */
    private static float[] inward(float[] screen, int edge) {
        float fromX = screen[edge * 2];
        float fromY = screen[edge * 2 + 1];
        float alongX = screen[(edge + 1) % 4 * 2] - fromX;
        float alongY = screen[(edge + 1) % 4 * 2 + 1] - fromY;
        float length = Mth.sqrt(alongX * alongX + alongY * alongY);
        if (length < 1.0E-4F) {
            return null;
        }
        float inX = -alongY / length;
        float inY = alongX / length;
        float middleX = (screen[0] + screen[2] + screen[4] + screen[6]) / 4.0F;
        float middleY = (screen[1] + screen[3] + screen[5] + screen[7]) / 4.0F;
        float sign = inX * (middleX - fromX) + inY * (middleY - fromY) < 0.0F ? -1.0F : 1.0F;
        return new float[] {inX * sign, inY * sign, fromX, fromY};
    }

    /** How far all of the book at {@code place} is inside the edge {@code in}; negative: it reaches over. */
    private static float clearance(float[] in, float[] place) {
        return in[0] * (place[1] - in[2]) + in[1] * (place[2] - in[3]) - Math.abs(in[0]) * HALF_WIDE * place[0]
                - Math.abs(in[1]) * HALF_HIGH * place[0];
    }

    /**
     * The corners of the screen on the plane the book hangs in at its own place (node units at its own size: x across, y up the page),
     * in the order top left, top right, bottom right, bottom left; null while the camera cannot tell.
     */
    private static float[] onScreen(StationView view, NodeView node, Vector3f origin) {
        Transform own = node.definition().transform();
        Quaternionf turn = Transform.toQuaternion(own.rotation(), new Quaternionf());
        float base = own.scale().x();
        Vector3f across = turn.transform(new Vector3f(base, 0.0F, 0.0F));
        Vector3f up = turn.transform(new Vector3f(0.0F, 0.0F, base));
        Vector3f normal = turn.transform(new Vector3f(0.0F, 1.0F, 0.0F));
        float[] corners = new float[8];
        for (int i = 0; i < 4; i++) {
            float screenX = i == 1 || i == 2 ? 1.0F : 0.0F;
            float screenY = i >= 2 ? 1.0F : 0.0F;
            // The line from the eye through that corner of the screen, where it goes through the book's plane.
            Vector3f[] line = line(view, screenX, screenY, origin.y);
            if (line == null) {
                return null;
            }
            float towards = line[1].dot(normal);
            if (Math.abs(towards) < 1.0E-4F) {
                return null;
            }
            Vector3f hit = new Vector3f(line[1]).mul(new Vector3f(origin).sub(line[0]).dot(normal) / towards).add(line[0]).sub(origin);
            corners[i * 2] = hit.dot(across) / across.lengthSquared();
            corners[i * 2 + 1] = hit.dot(up) / up.lengthSquared();
        }
        return corners;
    }

    /** Where the book hangs in the scene at its own place, pixels: where it is drawn less the move given to it. */
    private Vector3f home(NodeView node) {
        return node.scenePosition().sub(moved);
    }

    /**
     * The turn that keeps the book facing the player wherever it is moved: from the way it looks at the eye from its own place to the
     * way it would from where it is, in the node's own axes (its scene turn comes first); none while the camera cannot tell.
     */
    private Quaternionf facing(StationView view, NodeView node, Vector3f move) {
        Vector3f origin = home(node);
        Vector3f eye = eye(view, origin.y);
        if (eye == null) {
            return new Quaternionf();
        }
        Vector3f fromHome = new Vector3f(eye).sub(origin).normalize();
        Vector3f fromHere = new Vector3f(eye).sub(origin).sub(move).normalize();
        Quaternionf own = Transform.toQuaternion(node.definition().transform().rotation(), new Quaternionf());
        Quaternionf turn = new Quaternionf().rotationTo(fromHome, fromHere);
        return new Quaternionf(own).conjugate().mul(turn).mul(own);
    }

    /** Where the player's eye is in the scene, pixels: where the lines through two points of the screen meet; null when not known. */
    private static Vector3f eye(StationView view, float height) {
        Vector3f[] middle = line(view, 0.5F, 0.5F, height);
        Vector3f[] corner = line(view, 0.1F, 0.1F, height);
        if (middle == null || corner == null) {
            return null;
        }
        // The closest points of the two lines; through one eye they meet.
        Vector3f across = new Vector3f(middle[0]).sub(corner[0]);
        float aa = middle[1].dot(middle[1]);
        float ab = middle[1].dot(corner[1]);
        float bb = corner[1].dot(corner[1]);
        float ad = middle[1].dot(across);
        float bd = corner[1].dot(across);
        float under = aa * bb - ab * ab;
        if (Math.abs(under) < 1.0E-6F) {
            return null;
        }
        float along = (ab * bd - bb * ad) / under;
        return new Vector3f(middle[1]).mul(along).add(middle[0]);
    }

    /** The line from the eye through a point of the screen: a point of it and its way, scene pixels; null when not known. */
    private static Vector3f[] line(StationView view, float x, float y, float height) {
        Optional<Vector3f> near = view.screenOn(x, y, height);
        Optional<Vector3f> far = view.screenOn(x, y, height - 8.0F);
        if (near.isEmpty() || far.isEmpty()) {
            return null;
        }
        return new Vector3f[] {far.get(), new Vector3f(near.get()).sub(far.get())};
    }

    /** {@code x, y} on the book's plane (node units at its own size) as a move of the node in the scene, pixels. */
    private static Vector3f onScene(NodeView node, float x, float y) {
        Transform home = node.definition().transform();
        float base = home.scale().x();
        return Transform.toQuaternion(home.rotation(), new Quaternionf()).transform(new Vector3f(x * base, 0.0F, y * base));
    }

    /**
     * The corner of the open book under the cursor that sizes it, its sides along the width and the height (-1 or 1); null off it.
     * Only the top right one does, one place to find: the left page lies at +x, the top of the pages at +z. It is the plain cover's
     * corner and the outer bit of the paper there. A corner already raised is kept a little further out ({@code more}), so that
     * rising does not take it from under the cursor.
     */
    private int[] corner(float more) {
        if (pointer == null) {
            return null;
        }
        float x = Math.abs(pointer.x);
        float y = Math.abs(pointer.z);
        float in = CORNER_IN + more;
        boolean onCorner = x > PAGE_WIDTH - in && y > PAGE_HEIGHT / 2.0F - in && x < HALF_WIDE + in && y < HALF_HIGH + in;
        return onCorner && pointer.x < 0.0F && pointer.z > 0.0F ? new int[] {SIZING_CORNER_X, SIZING_CORNER_Y} : null;
    }

    /**
     * The corner turned down under the cursor: -1 the left page's (back), 1 the right page's (on), 0 none. A corner is there only
     * where there is a page to turn to, as in a book whose last page has nothing after it.
     */
    private int footSide() {
        int pages = pages();
        if (pages <= 1 || cursorY < UNITS_HIGH - DOG_EAR_REACH || cursorY >= UNITS_HIGH) {
            return 0;
        }
        if (cursorX >= 0.0F && cursorX < DOG_EAR_REACH && page > 0) {
            return -1;
        }
        return cursorX >= UNITS_WIDE * 2 - DOG_EAR_REACH && cursorX < UNITS_WIDE * 2 && page < pages - 1 ? 1 : 0;
    }

    /**
     * Whether the open book is under the cursor where nothing of its own is: no place on the list, tab, switch, ribbon, turned-down corner,
     * dish or thing that goes in, and no card of variants open (a click beside the card puts it away first). There the book is
     * taken, sized and put back.
     */
    private boolean onNothing() {
        return wanted && open >= 1.0F && flip >= 1.0F && openPlace == null && hovered == null && hoveredTab == null && !overFilter
                && !onRibbon() && footSide() == 0 && hoveredSpot == null && !overDish && !overSearch && !overSlip;
    }

    /**
     * A press on the open book where nothing of the recipes is: the left button takes it (a corner to size it, the rest to move it), the
     * right one puts it back to its own size and place.
     */
    private void take(int button) {
        if (pointer == null) {
            return;
        }
        float[] place = kept();
        if (button == 1) {
            if (place[0] != 1.0F || place[1] != 0.0F || place[2] != 0.0F) {
                keep(1.0F, 0.0F, 0.0F);
                sound(SoundEvents.BOOK_PUT, 1.1F, 0.4F);
            }
            return;
        }
        if (button != 0) {
            return;
        }
        // Taken as it is drawn, which a small window may have moved in from the kept place.
        int[] corner = corner(CORNER_IN * 0.6F);
        hold = new Hold(corner == null ? 0 : corner[0], corner == null ? 0 : corner[1], pointer.x, pointer.z, shownSize, shownX, shownY);
        place[0] = shownSize;
        place[1] = shownX;
        place[2] = shownY;
        sound(SoundEvents.BOOK_PAGE_TURN, 1.3F, 0.35F);
    }

    /** The player let the book go: it settles where it is, and the place is kept for the next time. */
    private void letGo() {
        hold = null;
        float[] place = kept();
        keep(place[0], place[1], place[2]);
        sound(SoundEvents.BOOK_PUT, 1.25F, 0.3F);
    }

    private void follow(StationView view, NodeView node) {
        cursorX = -1.0F;
        cursorY = -1.0F;
        hovered = null;
        hoveredTab = null;
        overFilter = false;
        overSearch = false;
        overSlip = false;
        followHover(null);
        Spot wasSpot = hoveredSpot;
        Entry wasSpotFor = spotFor;
        hoveredSpot = null;
        overDish = false;
        followCursor(view);
        if (hovered != null) {
            previewed = hovered;
        } else if (cursorX < 0.0F) {
            previewed = null;
        }
        Entry under = hovered;
        // The right page: the dish at its head and what goes in, under the cursor.
        Entry right = rightShown();
        if (right != null && cursorX >= UNITS_WIDE && cursorY >= 0.0F) {
            float x = cursorX - UNITS_WIDE;
            overDish = x >= RIGHT_MIDDLE - DISH_SIZE / 2.0F && x < RIGHT_MIDDLE + DISH_SIZE / 2.0F && cursorY >= DISH_Y
                    && cursorY < DISH_Y + DISH_SIZE;
            for (Spot spot : layout(right)) {
                if (spot.contains(x, cursorY)) {
                    hoveredSpot = spot;
                }
            }
        }
        source.hovered(view, under != null ? under.page() : overDish && right != null ? right.page() : null);
        if (hoveredSpot == null) {
            spotFor = null;
            spotTarget = null;
        } else if (wasSpot == null || wasSpot.index() != hoveredSpot.index() || wasSpotFor != right) {
            // Under the cursor it stops going through its choices: the one it showed when the cursor came stays.
            spotFor = right;
            spotShown = choice(right, hoveredSpot.index());
            spotTarget = target(right, hoveredSpot.index(), spotShown);
        }
    }

    private void followCursor(StationView view) {
        if (open < 1.0F) {
            return;
        }
        // The pages rise from the fold towards their outer edges: the cursor's ray meets a page above the fold's plane, the higher
        // the further out. A few steps find the height of the page where the ray comes down on it.
        float tilt = pageTilt();
        float fold = foldHeight();
        float height = fold;
        Vector3f point = null;
        for (int step = 0; step < 4; step++) {
            Optional<Vector3f> cursor = view.pointerOn("book_open", height);
            if (cursor.isEmpty()) {
                return;
            }
            point = cursor.get();
            height = fold + Math.abs(point.x) * Mth.sin(tilt) / Mth.cos(tilt);
        }
        float y = (PAGE_HEIGHT / 2.0F - point.z) / UNIT;
        // Along the page from the fold; the left page runs from its edge at +x to the fold at 0, the right one on from there.
        float along = Math.abs(point.x) / Mth.cos(tilt);
        float x = point.x > 0.0F ? (PAGE_WIDTH - along) / UNIT : UNITS_WIDE + along / UNIT;
        boolean overRow = x >= 0.0F && x < UNITS_WIDE && y >= -TAB_HIGH && y < 0.0F;
        boolean onSlip = searching() && overRow && x >= TABS_X && x < UNITS_WIDE - TAB_MARGIN && y >= -SLIP_HIGH;
        boolean overTabs = !searching() && !tabs.isEmpty() && overRow;
        boolean onSearch = x >= UNITS_WIDE + SEARCH_X && x < UNITS_WIDE + SEARCH_X + TAB_WIDE && y >= -TAB_HIGH && y < 0.0F;
        boolean onFilter = source.hasFilter() && x >= UNITS_WIDE + FILTER_X && x < UNITS_WIDE + FILTER_X + TAB_WIDE
                && y >= -TAB_HIGH && y < 0.0F;
        // The ribbon stands a little over the head of the page, and takes the cursor a little round it.
        boolean nearRibbon = x >= UNITS_WIDE + RIBBON_X - RIBBON_REACH && x < UNITS_WIDE + RIBBON_X + RIBBON_WIDTH + RIBBON_REACH
                && y >= -RIBBON_REACH && y < 0.0F;
        if (x >= 0.0F && x < UNITS_WIDE * 2 && (y >= 0.0F || overTabs || onFilter || nearRibbon || onSlip || onSearch)
                && y < UNITS_HIGH) {
            cursorX = x;
            cursorY = y;
            // While a sheet goes over, the list it uncovers takes the cursor once it is mostly in view; corners and tabs at once.
            followHover(flip >= FLIP_LIVE && y >= 0.0F ? entryAt(x, y) : null);
            hoveredTab = overTabs ? tabAt(x) : null;
            overFilter = onFilter;
            overSearch = onSearch;
            overSlip = onSlip;
        }
    }

    /** The stack a thing that goes in shows now: it goes through its choices, one a second. */
    private static ItemStack choice(Entry entry, int index) {
        ItemStack[] choices = entry.ingredients().get(index).getItems();
        return choices.length == 0 ? ItemStack.EMPTY : choices[(int) (Util.getMillis() / 1000L % choices.length)];
    }

    /**
     * The recipe that makes a thing going into {@code entry}: of the stack shown, else of any of its choices; one that can be made
     * now first. Null when the book has none (logs, ores), or only the dish's own.
     */
    private Page target(Entry entry, int index, ItemStack shown) {
        List<Page> making = shown.isEmpty() ? null : makes.get(shown.getItem());
        if (making == null) {
            for (ItemStack choice : entry.ingredients().get(index).getItems()) {
                making = makes.get(choice.getItem());
                if (making != null) {
                    break;
                }
            }
        }
        if (making == null) {
            return null;
        }
        Page first = null;
        for (Page page : making) {
            if (!page.key().equals(entry.page().key())) {
                first = first == null ? page : first;
            }
        }
        return first;
    }

    /** The recipe a click on a thing going in leads to: one that can be made now when there is one. */
    private Page readyTarget(StationView view, Page target) {
        List<Page> making = makes.getOrDefault(target.dish().getItem(), List.of());
        List<ItemStack> pool = pool(view);
        for (Page page : making) {
            if (!page.key().equals(target.key()) && entry(page, pool).isReady()) {
                return page;
            }
        }
        return target;
    }

    /** Where the things that go in lie on the right page for {@code entry}: under its name, in rows of three. */
    private List<Spot> layout(Entry entry) {
        List<Spot> laid = new ArrayList<>();
        int count = entry.ingredients().size();
        float top = ingredientsTop(entry);
        for (int i = 0; i < count; i++) {
            if (entry.ingredients().get(i).getItems().length == 0) {
                continue;
            }
            int inRow = Math.min(3, count - i / 3 * 3);
            float x = RIGHT_MIDDLE - (inRow * PITCH - (PITCH - WELL_SIZE)) / 2.0F + i % 3 * PITCH;
            laid.add(new Spot(x, top + i / 3 * PITCH, i));
        }
        return laid;
    }

    /** Where the rule under the dish's name is, and the things that go in start just under it. */
    private float ruleY(Entry entry) {
        List<FormattedCharSequence> name = nameLines(entry);
        return NAME_Y + Math.min(nameScale < 1.0F ? LONG_NAME_LINES : 2, name.size()) * Minecraft.getInstance().font.lineHeight * nameScale;
    }

    private float ingredientsTop(Entry entry) {
        return ruleY(entry) + INGREDIENTS_GAP;
    }

    /**
     * The dish's name broken into lines: two at most, or a name that needs more (a banner's "Blue Creeper Charge") smaller in three,
     * so that a long modded name still leaves room for what goes in.
     */
    private List<FormattedCharSequence> nameLines(Entry entry) {
        if (nameFor != entry) {
            nameFor = entry;
            Font font = Minecraft.getInstance().font;
            float wide = 2.0F * (UNITS_WIDE - RIGHT_MIDDLE) - 6.0F;
            nameScale = 1.0F;
            nameLines = font.split(entry.dish().getHoverName(), Math.round(wide));
            if (nameLines.size() > 2) {
                nameScale = LONG_NAME_SCALE;
                nameLines = font.split(entry.dish().getHoverName(), Math.round(wide / LONG_NAME_SCALE));
            }
        }
        return nameLines;
    }

    /**
     * What is under the cursor on the list: on a place of several variants, the one it showed when the cursor came stays shown; on
     * an opened place, the variant itself.
     */
    private void followHover(Entry under) {
        if (under == null || openPlace != null) {
            hoverPlace = null;
            hovered = under;
            return;
        }
        if (!under.page().place().equals(hoverPlace)) {
            hoverPlace = null;
            hoverKey = shownAt(under).page().key();
            hoverPlace = under.page().place();
        }
        hovered = shownAt(under);
    }

    /** Marks the recipes on the list's page in view as seen, as the vanilla book does when it shows them; they keep a red mark. */
    private void seeShown() {
        List<Entry> list = listed();
        int first = page * PER_PAGE;
        for (int i = first; i < Math.min(list.size(), first + PER_PAGE); i++) {
            List<Entry> shown = openPlace == null ? list.get(i).variants() : List.of(list.get(i));
            for (Entry variant : shown) {
                if (source.isNew(variant.page())) {
                    fresh.add(variant.page().key());
                    source.seen(variant.page());
                    // Read again: the marks on the tabs follow what is still unseen.
                    readPages = null;
                }
            }
        }
    }

    private boolean isFresh(Entry place, boolean card) {
        if (fresh.isEmpty()) {
            return false;
        }
        List<Entry> variants = card ? List.of(place) : place.variants();
        return variants.stream().anyMatch(variant -> fresh.contains(variant.page().key()));
    }

    private Entry entryAt(float x, float y) {
        int column = Mth.floor((x - GRID_X) / PITCH);
        int row = Mth.floor((y - GRID_Y) / PITCH);
        int index = page * PER_PAGE + row * COLUMNS + column;
        List<Entry> list = listed();
        return column >= 0 && column < COLUMNS && row >= 0 && row < ROWS && index < list.size() ? list.get(index) : null;
    }

    private Tab tabAt(float x) {
        float pitch = tabPitch();
        // A tab drawn later lies over the one before it: each shows from its own left edge to the next one's.
        int index = Math.min(Mth.floor((x - TABS_X) / pitch), tabs.size() - 1);
        float into = x - TABS_X - index * pitch;
        return index >= 0 && into < tabWide() ? tabs.get(index) : null;
    }

    /** How wide a tab is: narrower when more than five stand over the page. */
    private float tabWide() {
        return tabs.size() > TABS_FULL ? TAB_WIDE_NARROW : TAB_WIDE;
    }

    private float tabIcon() {
        return tabs.size() > TABS_FULL ? TAB_ICON_NARROW : TAB_ICON;
    }

    /** How far apart the tabs stand: as usual, or closer, so that however many there are all stay over the left page. */
    private float tabPitch() {
        if (tabs.size() < 2) {
            return TAB_PITCH;
        }
        return Math.min(TAB_PITCH, (UNITS_WIDE - TAB_MARGIN - TABS_X - tabWide()) / (tabs.size() - 1));
    }

    /** The ribbon that shuts the book, with a little room round it: it is the only way to shut it, so it is easy to hit. */
    private boolean onRibbon() {
        return cursorX >= UNITS_WIDE + RIBBON_X - RIBBON_REACH && cursorX < UNITS_WIDE + RIBBON_X + RIBBON_WIDTH + RIBBON_REACH
                && cursorY >= -RIBBON_REACH && cursorY < RIBBON_HEIGHT + RIBBON_REACH + 1.0F;
    }

    /**
     * A click on the book: opens it; open it lays a recipe, turns the page, switches what is shown, or closes by its ribbon. A right
     * click on a place of several variants shows them all on the left page, as the vanilla book's right click does; a click
     * anywhere else on the book puts them away again. With no card open, a left press where nothing of the book's own is takes the
     * book in the hand (by its top right corner to size it), and a right click there puts it back to its own size and place.
     */
    void click(StationView view, int button) {
        int foot = footSide();
        if (typing && !overSlip && !overSearch) {
            // A click anywhere else ends the typing, and does what it does.
            typing = false;
            if (query.isBlank()) {
                stopSearch(view);
            }
        }
        if (overSearch && wanted) {
            if (searching()) {
                stopSearch(view);
            } else {
                rememberPage();
                typing = true;
                typingFrom = time;
            }
            turnSound();
        } else if (overSlip && wanted) {
            typing = true;
            typingFrom = time;
        } else if (onNothing()) {
            take(button);
        } else if (!wanted || onRibbon()) {
            toggle(view);
            keepOpen(wanted);
        } else if (overFilter) {
            // The switch starts its list at the first page: the other list's page number means nothing in this one.
            flipFrom = leafNow();
            openPlace = null;
            source.setFiltering(!source.filtering());
            read(view, true);
            page = 0;
            rememberPage();
            startFlip(1, false);
        } else if (button == 1) {
            if (openPlace == null && hovered != null && hovered.variants().size() > 1) {
                openPlace(hovered);
            } else {
                closePlace();
            }
        } else if (hoveredTab != null) {
            if (!Objects.equals(tab, hoveredTab.kind())) {
                rememberPage();
                openPlace = null;
                chosenTab = hoveredTab.kind();
                BookTabs.put(source.memory(), chosenTab);
                flip = 1.0F;
                read(view, true);
                // The tab opens where the player left it this game, else on its first page.
                restore(view);
                turnSound();
            }
        } else if (hoveredSpot != null) {
            if (spotTarget != null) {
                goTo(view, readyTarget(view, spotTarget));
            }
        } else if (overDish) {
            Entry shown = rightShown();
            if (shown != null) {
                lay(view, shown);
            }
        } else if (hovered != null) {
            // A place of several variants asks which one when there is a choice; with one that can be made, it lays that one.
            if (openPlace == null && hovered.variants().size() > 1 && readyCount(hovered) != 1) {
                openPlace(hovered);
            } else {
                lay(view, hovered);
            }
        } else if (foot != 0) {
            // A click goes round the ends (on from the last page to the first); held, it leafs on and stops at the end.
            int before = page;
            turn(foot, true);
            boolean wrapped = foot > 0 ? page < before : page > before;
            leafing = wrapped ? 0 : foot;
            leafSince = time;
            leafDone = 1;
        } else {
            closePlace();
        }
    }

    private static int readyCount(Entry place) {
        int ready = 0;
        for (Entry variant : place.variants()) {
            ready += variant.isReady() ? 1 : 0;
        }
        return ready;
    }

    /** Lays a recipe into the station (shift: as many as fit), and keeps it as picked and as made lately. */
    private void lay(StationView view, Entry entry) {
        picked = entry;
        previewed = null;
        PICKS.put(memoryKey(), entry.page().key());
        source.picked(view, entry.page());
        made(entry.page());
        closePlace();
    }

    /**
     * Shows the recipe of a thing that goes in: picked, nothing laid, and the list turned to it. A tab that hides it gives way to
     * all recipes for now (the one chosen stays kept); when only the switch hides it, the right page alone shows it.
     */
    private void goTo(StationView view, Page target) {
        if (openPlace != null) {
            closePlace();
        }
        Entry listed = listedVariant(target.key());
        if (listed == null && tab != null) {
            rememberPage();
            chosenTab = null;
            read(view, true);
            listed = listedVariant(target.key());
        }
        if (listed != null) {
            picked = listed;
            int at = placeIndex(listed) / PER_PAGE;
            if (at != page) {
                flipFrom = leafNow();
                int way = at > page ? 1 : -1;
                page = at;
                startFlip(way, false);
                rememberPage();
            } else {
                turnSound();
            }
        } else {
            picked = entry(target, pool(view));
            turnSound();
        }
        previewed = null;
        PICKS.put(memoryKey(), target.key());
    }

    /**
     * Turns the list's page by {@code by}, within the list or round its ends ({@code wrap}). A turn is never refused: one asked for
     * while a sheet is still going over lands that sheet and starts the next, quicker.
     */
    private void turn(int by, boolean wrap) {
        int next = BookMath.turned(page, by, pages(), wrap);
        if (next == page) {
            return;
        }
        flipFrom = leafNow();
        page = next;
        startFlip(by, time - lastTurn < QUICK_GAP);
        lastTurn = time;
        rememberPage();
    }

    /** It hangs before the camera and sways: drawn by the scene, its edges showed steps. */
    @Override
    public boolean smooth() {
        return true;
    }

    @Override
    public void paint(PoseStack poseStack, MultiBufferSource buffers, int light) {
        shade = LightTexture.pack(Math.max(LightTexture.block(light), LIT), Math.max(LightTexture.sky(light), LIT));
        if (model == null) {
            model = RecipeBookModel.bake();
        }
        float eased = eased();
        float closed = 1.0F - eased;
        // How far each cover has turned from shut, as the enchanting table's book turns them.
        float angle = 1.25F * OPEN * eased;
        poseStack.pushPose();
        poseStack.mulPose(LYING);
        poseStack.scale(16.0F * SIZE, 16.0F * SIZE, 16.0F * SIZE);
        poseStack.mulPose(new Quaternionf().rotationY(CLOSED_TURN * closed));
        poseStack.translate(-CLOSED_SHIFT * closed, 0.0F, 0.0F);
        model.setupAnim(angle);
        // Cut out, so the clear gutters of the sheet stay clear; the cloth and the pages are opaque.
        model.render(poseStack, buffers.getBuffer(RenderType.entityCutout(RecipeBookModel.TEXTURE)), shade, OverlayTexture.NO_OVERLAY);
        poseStack.popPose();
        if (open > 0.0F) {
            // The sheets go with the model: the same turn from closed, the same angle of the pages all the way open.
            poseStack.pushPose();
            poseStack.mulPose(new Quaternionf().rotationZ(CLOSED_TURN * closed));
            poseStack.translate(0.0F, -CLOSED_SHIFT * 16.0F * SIZE * closed, 0.0F);
            spread(poseStack, buffers, Mth.HALF_PI - angle, (Mth.sin(angle) + PAGE_GAP) * SIZE);
            poseStack.popPose();
        }
    }

    /**
     * The open pages. Turning, three are on show: a sheet goes over from one side to the other with one page on its face and the next on
     * its back, and the page it uncovers lies ready under it, so nothing appears or goes away. The page turned away is the one that was
     * there when the turn started ({@code flipFrom}), whatever the list became since.
     */
    private void spread(PoseStack poseStack, MultiBufferSource buffers, float tilt, float height) {
        Leaf now = leafNow();
        if (flip >= 1.0F) {
            side(poseStack, buffers, true, tilt, height, now, true);
            side(poseStack, buffers, false, -tilt, height, now, true);
            return;
        }
        Leaf from = flipFrom == null ? now : flipFrom;
        float over = flip * flip * (3.0F - 2.0F * flip);
        // Forward the old list stays on the left until the sheet covers it with the new one; back the old list itself lifts off.
        boolean forward = flipWay > 0;
        side(poseStack, buffers, true, tilt, height, forward ? from : now, true);
        side(poseStack, buffers, false, -tilt, height, now, true);
        float right = -tilt;
        float left = tilt - Mth.PI;
        float turn = forward ? Mth.lerp(over, right, left) : Mth.lerp(over, left, right);
        float lifted = height + SHEET_LIFT;
        // The sheet going over bends as paper does, what is written on it going with it; the first frame of a turn it goes flat.
        Leaf back = forward ? now : from;
        if (RecipeSheets.turn(poseStack, buffers, shade, lifted, forward ? right : left, forward ? left : right, over, this,
                (pose, drawn, onLeft) -> paintFace(pose, drawn, onLeft, onLeft ? back : now))) {
            return;
        }
        if (turn > -Mth.HALF_PI) {
            side(poseStack, buffers, false, turn, lifted, now, false);
        } else {
            side(poseStack, buffers, true, turn + Mth.PI, lifted, forward ? now : from, false);
        }
    }

    /**
     * Paints one face of the sheet going over flat at the fold, at full light (the world's light falls on the bent sheet): the
     * right page ({@code left} false) or the left one.
     */
    private void paintFace(PoseStack poseStack, MultiBufferSource buffers, boolean left, Leaf leaf) {
        int lit = shade;
        shade = LightTexture.FULL_BRIGHT;
        side(poseStack, buffers, left, 0.0F, 0.0F, leaf, false);
        shade = lit;
    }

    /** One side of a sheet, turned about the fold: units run right and down as on a screen. {@code resting}: the tabs stand on it. */
    private void side(PoseStack poseStack, MultiBufferSource buffers, boolean left, float turn, float height, Leaf leaf, boolean resting) {
        poseStack.pushPose();
        poseStack.translate(0.0F, height, 0.0F);
        poseStack.mulPose(new Quaternionf().rotationZ(turn));
        Matrix4f sheet = new Matrix4f(-UNIT, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F, -UNIT, 0.0F, 0.0F, UNIT, 0.0F, 0.0F, left ? PAGE_WIDTH : 0.0F, 0.0F, PAGE_HEIGHT / 2.0F, 1.0F);
        poseStack.last().pose().mul(sheet);
        Sheet canvas = new Sheet(poseStack, buffers);
        canvas.paper(left);
        canvas.rect(left ? UNITS_WIDE - FOLD_WIDTH : 0.0F, 0.0F, FOLD_WIDTH, UNITS_HIGH, 0.1F, FOLD);
        canvas.rect(left ? UNITS_WIDE - 2.0F : 0.0F, 0.0F, 2.0F, UNITS_HIGH, 0.15F, FOLD);
        // Only the resting pages answer the cursor: a sheet going over shows what it holds, unlit.
        boolean live = resting && (flip >= 1.0F || !left || leaf.page() == page && leaf.card() == (opened() != null));
        // The corner turned down where there is a page to turn to: before this one on the left, after it on the right.
        int leafPages = Math.max(1, (leaf.list().size() + PER_PAGE - 1) / PER_PAGE);
        if (left ? leaf.page() > 0 : leaf.page() < leafPages - 1) {
            boolean under = live && footSide() == (left ? -1 : 1);
            DogEar.draw(poseStack, buffers, shade, left ? 0.0F : UNITS_WIDE, UNITS_HIGH, DOG_EAR, left, under, DogEar.Paper.CREAM, DogEar.LIFT);
        }
        if (left) {
            if (resting) {
                drawTabs(canvas);
            }
            drawList(canvas, leaf, live);
        } else {
            if (resting && source.hasFilter()) {
                // On, it is washed with the green of what can be made, so it reads as a switch, not as one more kind of recipe.
                float since = time - switchNodFrom;
                float nod = since >= 0.0F && since < NOD_TIME ? Math.abs(Mth.sin(since / NOD_TIME * Mth.TWO_PI)) * NOD_HEIGHT : 0.0F;
                canvas.tab(FILTER_X, TAB_WIDE, TAB_ICON, source.filtering(), overFilter,
                        source.filterIcon(), nod, source.filtering(), false);
            }
            if (resting) {
                canvas.tab(SEARCH_X, TAB_WIDE, TAB_ICON, searching(), overSearch, ItemStack.EMPTY, 0.0F, false, false);
                canvas.magnifier(SEARCH_X + (TAB_WIDE - 7.0F) / 2.0F, -(searching() ? TAB_HIGH : TAB_HIGH - TAB_DROP) + 3.0F);
            }
            Entry shown = rightShown();
            if (shown != null) {
                drawDish(canvas, shown, resting);
            }
            if (resting) {
                // The ribbon is there whatever the page shows, also when the list is empty: it is the way to shut the book.
                canvas.ribbon(onRibbon());
            }
        }
        poseStack.popPose();
    }

    /** The slip the search is typed on, over the list where the tabs were: the text, its tail when long, and a blinking caret. */
    private void drawSlip(Sheet canvas) {
        Font font = Minecraft.getInstance().font;
        float wide = UNITS_WIDE - TAB_MARGIN - TABS_X;
        canvas.slip(TABS_X, wide, SLIP_HIGH, overSlip && !typing);
        canvas.magnifier(TABS_X + 2.0F, -SLIP_HIGH + 2.0F);
        float room = wide - SLIP_TEXT_X - 3.0F;
        String shown = query;
        while (!shown.isEmpty() && font.width(shown) > room) {
            shown = shown.substring(1);
        }
        float textY = -SLIP_HIGH + 2.0F;
        canvas.text(shown, TABS_X + SLIP_TEXT_X, textY);
        if (typing && (time - typingFrom) % (CARET_BLINK * 2.0F) < CARET_BLINK) {
            canvas.rect(TABS_X + SLIP_TEXT_X + font.width(shown), textY - 1.0F, 1.0F, 9.0F, COVER_LIFT, INK);
        }
    }

    private void drawTabs(Sheet canvas) {
        if (searching()) {
            drawSlip(canvas);
            return;
        }
        // A tab holding recipes the player has not seen nods twice when the book opens or gets more of them, and keeps a red mark.
        float since = time - nodFrom;
        float nod = since >= 0.0F && since < NOD_TIME ? Math.abs(Mth.sin(since / NOD_TIME * Mth.TWO_PI)) * NOD_HEIGHT : 0.0F;
        for (int i = 0; i < tabs.size(); i++) {
            Tab shown = tabs.get(i);
            boolean unseen = !RECENT.equals(shown.kind()) && newKinds.contains(shown.kind() == null ? "" : shown.kind());
            canvas.tab(TABS_X + i * tabPitch(), tabWide(), tabIcon(), Objects.equals(shown.kind(), tab), shown == hoveredTab,
                    shown.icon(), unseen ? nod : 0.0F, false, unseen);
        }
    }

    private void drawList(Sheet canvas, Leaf leaf, boolean live) {
        List<Entry> list = leaf.list();
        int first = leaf.page() * PER_PAGE;
        int count = Math.max(0, Math.min(list.size(), first + PER_PAGE) - first);
        boolean card = leaf.card();
        if (card && count > 0) {
            // The variants of one place lie on a card over the list, as the vanilla book lays them over its own.
            float wide = Math.min(count, COLUMNS) * PITCH - (PITCH - WELL_SIZE) + 4.0F;
            float high = (count + COLUMNS - 1) / COLUMNS * PITCH - (PITCH - WELL_SIZE) + 4.0F;
            canvas.rect(GRID_X - 2.0F, GRID_Y - 2.0F, wide, high, WELL_LIFT * 0.5F, CARD);
            canvas.outline(GRID_X - 2.0F, GRID_Y - 2.0F, wide, high, WELL_LIFT * 0.5F, RULE);
        }
        Entry pointed = hovered != null ? hovered : previewed;
        for (int i = first; i < first + count; i++) {
            Entry place = list.get(i);
            Entry shown = card ? place : shownAt(place);
            boolean ready = card ? place.isReady() : place.anyReady();
            int x = GRID_X + (i - first) % COLUMNS * PITCH;
            int y = GRID_Y + (i - first) / COLUMNS * PITCH;
            if (!card && place.variants().size() > 1) {
                // More than one recipe here: a second well peeks out from under it, as a stack of cards.
                canvas.rect(x + 1.5F, y + 1.5F, WELL_SIZE, WELL_SIZE, WELL_LIFT * 0.5F, ready ? WELL_READY : WELL);
            }
            canvas.rect(x, y, WELL_SIZE, WELL_SIZE, WELL_LIFT, ready ? WELL_READY : WELL);
            canvas.item(shown.dish(), x + 0.5F, y + 0.5F, 16.0F, !shown.isReady());
            // Null-safe: the lists of variants are immutable and refuse to look for null.
            boolean here = card ? place == picked || place == pointed
                    : picked != null && place.variants().contains(picked) || pointed != null && place.variants().contains(pointed);
            if (here) {
                canvas.frame(x, y, WELL_SIZE);
            }
            if (isFresh(place, card)) {
                canvas.rect(x + 1.0F, y + 2.0F, 3.0F, 1.0F, COVER_LIFT, FRESH);
                canvas.rect(x + 2.0F, y + 1.0F, 1.0F, 3.0F, COVER_LIFT, FRESH);
            }
        }
        int leafPages = Math.max(1, (list.size() + PER_PAGE - 1) / PER_PAGE);
        if (leafPages > 1) {
            // "7/52" under the list, smaller when it would not fit.
            Font font = Minecraft.getInstance().font;
            String number = (leaf.page() + 1) + "/" + leafPages;
            float wide = font.width(number) - 1.0F;
            float scale = Math.min(1.0F, NUMBER_ROOM / wide);
            float high = (font.lineHeight - 2.0F) * scale;
            canvas.text(number, FOOT_MIDDLE - wide * scale / 2.0F, ARROW_Y + (ARROW_HEIGHT - high) / 2.0F, scale);
        }
    }

    /** The dish, its name, what goes in, and the small print at the foot. {@code live}: the resting page, which answers the cursor. */
    private void drawDish(Sheet canvas, Entry entry, boolean live) {
        Font font = Minecraft.getInstance().font;
        float middle = RIGHT_MIDDLE;
        canvas.item(entry.dish(), middle - DISH_SIZE / 2.0F, DISH_Y, DISH_SIZE);
        if (live && overDish) {
            // The dish lays its recipe at a click, as a place on the list does.
            canvas.frame(middle - DISH_SIZE / 2.0F - 0.5F, DISH_Y - 0.5F, DISH_SIZE + 1.0F);
        }
        if (entry.dish().getCount() > 1) {
            // How many one craft makes, at the dish's foot as on a slot.
            String made = String.valueOf(entry.dish().getCount());
            canvas.text(made, middle + DISH_SIZE / 2.0F - font.width(made) + 2.0F, DISH_Y + DISH_SIZE - font.lineHeight + 2.0F);
        }
        float line = NAME_Y;
        List<FormattedCharSequence> name = nameLines(entry);
        for (FormattedCharSequence text : name.subList(0, Math.min(nameScale < 1.0F ? LONG_NAME_LINES : 2, name.size()))) {
            canvas.text(text, middle - font.width(text) * nameScale / 2.0F, line, nameScale);
            line += font.lineHeight * nameScale;
        }
        canvas.rect(middle - LIST_WIDE / 2.0F, ruleY(entry) + 0.5F, LIST_WIDE, 0.6F, WELL_LIFT, RULE);
        for (Spot spot : layout(entry)) {
            int i = spot.index();
            boolean pointed = live && hoveredSpot != null && hoveredSpot.index() == i && spotFor == entry;
            ItemStack stack = pointed ? spotShown : choice(entry, i);
            canvas.rect(spot.x(), spot.y(), WELL_SIZE, WELL_SIZE, WELL_LIFT, entry.found()[i] ? WELL_READY : WELL);
            canvas.item(stack, spot.x() + 0.5F, spot.y() + 0.5F, 16.0F, !entry.found()[i]);
            if (pointed && spotTarget != null) {
                // What the book can make lights under the cursor, and a click shows its recipe; the rest only names itself.
                canvas.frame(spot.x(), spot.y(), WELL_SIZE);
            }
        }
        // The small print goes where there is room left, in the middle, on the line of the list's page number.
        int count = entry.ingredients().size();
        float footMiddle = ARROW_Y + ARROW_HEIGHT / 2.0F;
        float bottom = footMiddle - FOOT_SIZE / 2.0F;
        int rows = (count + 2) / 3;
        if (ingredientsTop(entry) + rows * PITCH - (PITCH - WELL_SIZE) <= bottom) {
            List<ItemStack> foot = entry.page().foot();
            Component noted = entry.page().note();
            FormattedCharSequence note = noted == null ? null : noted.getVisualOrderText();
            // The note stands close after the last thing, as a time after its clock.
            float noteWide = note == null ? 0.0F : font.width(note) + 1.0F;
            float wide = foot.isEmpty() ? noteWide : (foot.size() - 1) * FOOT_PITCH + FOOT_SIZE + noteWide;
            float x = middle - wide / 2.0F;
            for (ItemStack small : foot) {
                canvas.item(small, x, bottom, FOOT_SIZE);
                x += FOOT_PITCH;
            }
            if (note != null) {
                float noteX = foot.isEmpty() ? x : x - FOOT_PITCH + FOOT_SIZE + 1.0F;
                canvas.text(note, noteX, footMiddle - font.lineHeight / 2.0F + 1.0F);
            }
        }
    }


    /** Drawing on a page. */
    private final class Sheet {
        private final PoseStack poseStack;
        private final MultiBufferSource buffers;
        private final TextureAtlasSprite white;

        Sheet(PoseStack poseStack, MultiBufferSource buffers) {
            this.poseStack = poseStack;
            this.buffers = buffers;
            white = Minecraft.getInstance().getTextureAtlas(TextureAtlas.LOCATION_BLOCKS).apply(WHITE);
        }

        void paper(boolean mirrored) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.text(PAPER));
            quad(consumer, 0.0F, 0.0F, UNITS_WIDE, UNITS_HIGH, 0.0F, 0xFFFFFFFF, mirrored ? PAPER_U1 : 0.0F, mirrored ? 0.0F : PAPER_U1, 0.0F, PAPER_V1);
        }

        void rect(float x, float y, float width, float height, float lift, int argb) {
            VertexConsumer consumer = buffers.getBuffer(RenderType.text(TextureAtlas.LOCATION_BLOCKS));
            quad(consumer, x, y, x + width, y + height, lift, argb, white.getU0(), white.getU1(), white.getV0(), white.getV1());
        }

        /**
         * A paper tab standing over the page's top edge, as vanilla's recipe book has them: the chosen one taller and in the page's own
         * paper, the others a shade darker behind it, each with its kind's icon.
         */
        void tab(float x, float wide, float iconSize, boolean chosen, boolean lit, ItemStack icon, float nod, boolean washed,
                boolean marked) {
            float high = chosen ? TAB_HIGH : TAB_HIGH - TAB_DROP;
            int paper = chosen ? 0xFFFFFFFF : lit ? 0xFFEDE4D0 : 0xFFD6C9AC;
            // Down into the page by a unit: the chosen one runs on into it, with no edge between.
            float bottom = chosen ? 1.0F : 0.0F;
            float u0 = TAB_PAPER_U / PAPER_SIZE;
            float v0 = TAB_PAPER_V / PAPER_SIZE;
            quad(buffers.getBuffer(RenderType.text(PAPER)), x, -high, x + wide, bottom, chosen ? 0.05F : 0.0F, paper, u0,
                    u0 + wide / PAPER_SIZE, v0, v0 + (bottom + high) / PAPER_SIZE);
            if (washed) {
                rect(x + 1.0F, -high + 1.0F, wide - 2.0F, high - 1.0F + bottom, 0.08F, WELL_READY);
            }
            rect(x + 1.0F, -high, wide - 2.0F, 1.0F, 0.1F, TAB_EDGE);
            rect(x, -high + 1.0F, 1.0F, high - 1.0F, 0.1F, TAB_EDGE);
            rect(x + wide - 1.0F, -high + 1.0F, 1.0F, high - 1.0F, 0.1F, TAB_EDGE);
            if (!icon.isEmpty()) {
                item(icon, x + (wide - iconSize) / 2.0F, -high + 2.0F - nod, iconSize, !chosen && !lit && !washed);
            }
            if (marked) {
                // Recipes the player has not seen are in it: the red mark of the list's wells, in its top right corner.
                rect(x + wide - 4.0F, -high - nod + 2.0F, 3.0F, 1.0F, COVER_LIFT, FRESH);
                rect(x + wide - 3.0F, -high - nod + 1.0F, 1.0F, 3.0F, COVER_LIFT, FRESH);
            }
        }

        /** The search's slip: the page's own paper standing over its top edge and running on into it, as the chosen tab does. */
        void slip(float x, float wide, float high, boolean lit) {
            float u0 = TAB_PAPER_U / PAPER_SIZE;
            float v0 = TAB_PAPER_V / PAPER_SIZE;
            quad(buffers.getBuffer(RenderType.text(PAPER)), x, -high, x + wide, 1.0F, 0.05F, lit ? 0xFFF6F0E2 : 0xFFFFFFFF, u0,
                    u0 + wide / PAPER_SIZE, v0, v0 + (high + 1.0F) / PAPER_SIZE);
            rect(x + 1.0F, -high, wide - 2.0F, 1.0F, 0.1F, TAB_EDGE);
            rect(x, -high + 1.0F, 1.0F, high - 1.0F, 0.1F, TAB_EDGE);
            rect(x + wide - 1.0F, -high + 1.0F, 1.0F, high - 1.0F, 0.1F, TAB_EDGE);
        }

        /** A magnifying glass in ink, 7 units square: a ring and its handle going down to the right. */
        void magnifier(float x, float y) {
            rect(x + 1.0F, y, 3.0F, 1.0F, COVER_LIFT, INK);
            rect(x + 1.0F, y + 4.0F, 3.0F, 1.0F, COVER_LIFT, INK);
            rect(x, y + 1.0F, 1.0F, 3.0F, COVER_LIFT, INK);
            rect(x + 4.0F, y + 1.0F, 1.0F, 3.0F, COVER_LIFT, INK);
            rect(x + 4.0F, y + 4.0F, 1.0F, 1.0F, COVER_LIFT, INK);
            rect(x + 5.0F, y + 5.0F, 2.0F, 1.0F, COVER_LIFT, INK);
            rect(x + 6.0F, y + 6.0F, 1.0F, 1.0F, COVER_LIFT, INK);
        }

        void outline(float x, float y, float width, float height, float lift, int argb) {
            rect(x, y, width, 1.0F, lift, argb);
            rect(x, y + height - 1.0F, width, 1.0F, lift, argb);
            rect(x, y + 1.0F, 1.0F, height - 2.0F, lift, argb);
            rect(x + width - 1.0F, y + 1.0F, 1.0F, height - 2.0F, lift, argb);
        }

        /** The ribbon from the paper texture, its forked end frayed; lighter under the cursor. */
        void ribbon(boolean lit) {
            float u = (lit ? RIBBON_LIT_U : RIBBON_U) / PAPER_SIZE;
            quad(buffers.getBuffer(RenderType.text(PAPER)), RIBBON_X, 0.0F, RIBBON_X + RIBBON_WIDTH, RIBBON_HEIGHT, COVER_LIFT, 0xFFFFFFFF, u,
                    u + RIBBON_WIDTH / PAPER_SIZE, 0.0F, RIBBON_HEIGHT / PAPER_SIZE);
        }

        void frame(float x, float y, float size) {
            rect(x, y, size, 1.0F, COVER_LIFT, WELL_PICKED);
            rect(x, y + size - 1.0F, size, 1.0F, COVER_LIFT, WELL_PICKED);
            rect(x, y, 1.0F, size, COVER_LIFT, WELL_PICKED);
            rect(x + size - 1.0F, y, 1.0F, size, COVER_LIFT, WELL_PICKED);
        }

        private void quad(VertexConsumer consumer, float x0, float y0, float x1, float y1, float lift, int argb, float u0, float u1, float v0, float v1) {
            Matrix4f matrix = poseStack.last().pose();
            int a = argb >>> 24;
            int r = argb >> 16 & 0xFF;
            int g = argb >> 8 & 0xFF;
            int b = argb & 0xFF;
            consumer.addVertex(matrix, x0, y0, lift).setColor(r, g, b, a).setUv(u0, v0).setLight(shade);
            consumer.addVertex(matrix, x0, y1, lift).setColor(r, g, b, a).setUv(u0, v1).setLight(shade);
            consumer.addVertex(matrix, x1, y1, lift).setColor(r, g, b, a).setUv(u1, v1).setLight(shade);
            consumer.addVertex(matrix, x1, y0, lift).setColor(r, g, b, a).setUv(u1, v0).setLight(shade);
        }

        void item(ItemStack stack, float x, float y, float size) {
            item(stack, x, y, size, false);
        }

        /** {@code faded}: what the player does not have yet, the icon itself see-through, as nothing laid over it would stay in its place. */
        void item(ItemStack stack, float x, float y, float size, boolean faded) {
            poseStack.pushPose();
            poseStack.translate(x + size / 2.0F, y + size / 2.0F, ITEM_LIFT);
            ItemRenderer renderer = Minecraft.getInstance().getItemRenderer();
            BakedModel model = renderer.getModel(stack, station.level(), null, 0);
            Matrix3f facing = new Matrix3f(poseStack.last().normal());
            poseStack.scale(size, -size, model.isGui3d() ? BLOCK_DEPTH : ITEM_DEPTH);
            // As in the GUI, the flip and the depth leave the normals alone: IconLight lights the faces as the GUI does.
            poseStack.last().normal().set(facing);
            // The icon looks as in a GUI, but is drawn as no GUI item: mods that decorate every GUI item (Shadow Drop's shadows and
            // its shift) take its world pose for a screen's and move or cover the icon.
            BakedModel shown = guiModel.apply(model, poseStack);
            MultiBufferSource lit = IconLight.of(buffers, facing, model.usesBlockLight());
            renderer.render(stack, ItemDisplayContext.NONE, false, poseStack, faded ? DiegesisClient.tinted(lit, FADED) : lit, shade,
                    OverlayTexture.NO_OVERLAY, shown);
            poseStack.popPose();
        }

        void text(String text, float x, float y) {
            text(Component.literal(text).getVisualOrderText(), x, y);
        }

        /** Text drawn smaller ({@code scale} under 1) from its top left corner: scaled as a whole, never wrapped. */
        void text(String text, float x, float y, float scale) {
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(x, y, ITEM_LIFT).scale(scale, scale, 1.0F);
            Minecraft.getInstance().font.drawInBatch(Component.literal(text).getVisualOrderText(), 0.0F, 0.0F, INK, false, matrix, buffers,
                    Font.DisplayMode.NORMAL, 0, shade);
        }

        void text(FormattedCharSequence text, float x, float y) {
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(0.0F, 0.0F, ITEM_LIFT);
            Minecraft.getInstance().font.drawInBatch(text, x, y, INK, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, shade);
        }

        /** Text drawn at {@code scale} from its top left corner. */
        void text(FormattedCharSequence text, float x, float y, float scale) {
            Matrix4f matrix = new Matrix4f(poseStack.last().pose()).translate(x, y, ITEM_LIFT).scale(scale, scale, 1.0F);
            Minecraft.getInstance().font.drawInBatch(text, 0.0F, 0.0F, INK, false, matrix, buffers, Font.DisplayMode.NORMAL, 0, shade);
        }
    }
}
