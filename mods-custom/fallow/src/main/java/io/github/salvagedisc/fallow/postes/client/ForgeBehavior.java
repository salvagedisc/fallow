package io.github.salvagedisc.fallow.postes.client;

import java.util.IdentityHashMap;
import java.util.Map;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SmithingTemplateItem;
import net.minecraft.world.item.crafting.Ingredient;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.MenuAccess;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.scene.Visual;
import com.pockyl.diegesis.api.state.SlotRef;

import net.stirdrem.overgeared.AnvilTier;
import net.stirdrem.overgeared.screen.AbstractSmithingAnvilMenu;
import net.stirdrem.overgeared.util.ModTags;

/**
 * Le poste d'une enclume de forge, sur le modèle de la table de Hands-On Vanilla : les pièces se posent dans la grille sur
 * l'enclume, le marteau et le plan à leur place, le livre de forge à côté. Une pièce posée tinte sur l'enclume (et grésille si
 * elle sort du feu), une pièce reprise glisse. Les pièces chaudes fument dans leur case. Quand la grille fait une recette, ce
 * qu'elle fera tourne, pâle, là où l'ouvrage arrivera, et la légende dit de frapper : accroupi, le marteau en main, comme
 * Overgeared le veut. L'ouvrage forgé arrive dans une gerbe d'étincelles et flotte au-dessus de sa place ; un clic dessus
 * fait venir le marteau, qui frappe l'enclume, et l'ouvrage, pris au coup, s'en lève (la chorégraphie de l'enclume de Hands-On
 * Vanilla). L'inventaire grise ce qui ne se forge pas ici.
 */
final class ForgeBehavior implements StationBehavior {
    private final RecipeBook book;
    private final ForgeBook source;
    private final int[] grille = new int[9];
    private boolean connu;
    /** La recette que fait la grille, la dernière fois : la légende ne revient qu'à une recette nouvelle. */
    private ItemStack annonce = ItemStack.EMPTY;
    /** Ce qui sert ici, objet par objet, lu une fois : le livre de cette enclume, le marteau, le plan. */
    private final Map<Item, Boolean> utiles = new IdentityHashMap<>();
    private final RandomSource random = RandomSource.create();
    private float horloge;
    private float fumee;
    /** L'objet que montre le fantôme de l'ouvrage, pour ne changer son dessin qu'à une recette nouvelle. */
    private Item fantome;
    private int ouvrage;
    /** Vrai quand l'ouvrage attend dans sa case : le fantôme de ce que fera la grille se tait. */
    private boolean fini;
    /** L'ouvrage fini, qui flotte au-dessus de sa place. */
    private final Flottant flottant = new Flottant(3.6F);
    /** La case du menu où Overgeared met l'ouvrage : juste après la grille. */
    private final int caseOuvrage;
    /** Où en est le coup de marteau qui prend l'ouvrage. */
    private Coup coup = Coup.REPOS;
    private float tempsCoup;
    private boolean tout;
    private boolean revenu = true;
    /** L'ouvrage pris au coup, qui se lève de l'enclume, et quand. */
    private ItemStack leve = ItemStack.EMPTY;
    private float leveA;

    private enum Coup { REPOS, ENTRE, FRAPPE, SONNE, SORT }

    /** La case de la main quand on est venu à l'enclume avec une pièce : elle s'y pose quand la caméra est arrivée. */
    private int enMain = -1;

    /** La pièce que montre, pâle, chaque case vide de la dernière page choisie. */
    private final Item[] manques = new Item[9];

    /** Ce que le curseur tenait au tick d'avant : s'il a baissé, la pièce posée vient de la main et ne vole pas. */
    private int porte;

    /** Le marteau au repos, à sa place à droite de l'enclume, relevé, et frappant (décalages du nœud {@code coup}). */
    private static final Vector3f DEPUIS = new Vector3f(-12.0F, 6.0F, -4.0F);
    private static final Vector3f LEVE = new Vector3f(0.0F, 1.5F, 0.0F);

    ForgeBehavior(AnvilTier tier, int debutGrille) {
        this.source = new ForgeBook(tier, debutGrille);
        this.book = new RecipeBook(source);
        this.caseOuvrage = debutGrille + 9;
        this.pierre = tier == AnvilTier.STONE;
    }

    /** L'enclume de pierre sonne la pierre et jette de la poussière ; les autres, le métal et des étincelles. */
    private final boolean pierre;
    /** Vrai quand l'ouvrage, encore à forger, porte la consigne du marteau en infobulle. */
    private boolean consigne;

    @Override
    public void onSceneReady(StationView view) {
        view.node("book_open").ifPresent(node -> node.setPainter(book));
        view.node(PostesForge.OUVRAGE).ifPresent(node -> node.setPainter((pose, buffers, light) -> {
            if (!enCoup()) {
                flottant.paint(view.level(), ouvrage(view), horloge, pose, buffers, light);
            }
        }));
        view.node("leve").ifPresent(node -> node.setPainter((pose, buffers, light) ->
                Flottant.leve(view.level(), leve, horloge - leveA, 3.6F, pose, buffers, light)));
    }

    private static ItemStack ouvrage(StationView view) {
        return view.menu() != null && view.menu().handle() instanceof AbstractSmithingAnvilMenu menu ? menu.getResultSlot().getItem()
                : ItemStack.EMPTY;
    }

    /** Vrai du moment où le marteau part jusqu'au coup : l'ouvrage ne flotte plus, il est sous le marteau. */
    private boolean enCoup() {
        return coup == Coup.ENTRE || coup == Coup.FRAPPE;
    }

    @Override
    public void onOpen(StationView view) {
        utiles.clear();
        view.setInventoryFilter(stack -> utile(view, stack));
        book.opened(view);
        annonce = ItemStack.EMPTY;
        fantome = null;
        ouvrage = -1;
        LocalPlayer venu = Minecraft.getInstance().player;
        enMain = venu != null && PostesClient.venuParUsage(view.pos()) && !venu.getMainHandItem().is(ModTags.Items.SMITHING_HAMMERS)
                && !venu.getMainHandItem().isEmpty() && utile(view, venu.getMainHandItem()) ? venu.getInventory().selected : -1;
        coup = Coup.REPOS;
        leve = ItemStack.EMPTY;
        view.node("coup").ifPresent(node -> node.setVisible(false));
    }

    private boolean utile(StationView view, ItemStack stack) {
        if (stack.is(ModTags.Items.SMITHING_HAMMERS)) {
            return true;
        }
        if (stack.getItem() instanceof SmithingTemplateItem
                || net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().equals("blueprint")) {
            // Le plan a sa case sur les enclumes de métal ; l'enclume de pierre n'en a pas : il irait dans la grille.
            return !pierre;
        }
        Boolean deja = utiles.get(stack.getItem());
        if (deja != null) {
            return deja;
        }
        java.util.List<RecipeBook.Page> pages = source.pages(view);
        ItemStack one = new ItemStack(stack.getItem());
        boolean sert = false;
        for (RecipeBook.Page page : pages) {
            for (Ingredient ingredient : page.ingredients()) {
                sert |= !ingredient.isEmpty() && ingredient.test(one);
            }
        }
        // Les recettes pas encore reçues ne disent rien : la réponse n'est gardée qu'une fois le livre rempli.
        if (!pages.isEmpty()) {
            utiles.put(stack.getItem(), sert);
        }
        return sert;
    }

    @Override
    public void onClose(StationView view) {
        book.shut(view);
        source.oublie();
        connu = false;
        coup = Coup.REPOS;
        view.node("coup").ifPresent(node -> node.setVisible(false));
    }

    @Override
    public void frame(StationView view, float deltaSeconds) {
        // Venu avec une pièce en main : elle se pose sur l'enclume (comme le four de Hands-On Vanilla), une fois le menu là.
        if (enMain >= 0 && view.menu() != null && com.pockyl.diegesis.api.client.DiegesisClient.arrival() >= 1.0F) {
            LocalPlayer joueur = Minecraft.getInstance().player;
            // La main a pu changer pendant l'approche : on ne pose que ce qui sert encore.
            ItemStack tenu = joueur == null ? ItemStack.EMPTY : joueur.getInventory().getItem(enMain);
            if (!tenu.isEmpty() && !tenu.is(ModTags.Items.SMITHING_HAMMERS) && utile(view, tenu)) {
                Arrivee.depuis(view, enMain);
                view.menu().quickMoveFromInventory(enMain);
            }
            enMain = -1;
        }
        book.frame(view, deltaSeconds);
        horloge += deltaSeconds;
        tempsCoup += deltaSeconds;
        ItemStack fait = ouvrage(view);
        view.node(PostesForge.OUVRAGE).ifPresent(node -> {
            flottant.frame(node, fait, !enCoup(), deltaSeconds);
            // Tant que la pièce reste à forger, son emplacement rappelle comment faire (la légende ne dure que trois secondes).
            boolean dire = fait.isEmpty() && !annonce.isEmpty();
            if (dire != consigne) {
                consigne = dire;
                node.setTooltip(dire ? Component.translatable("postes.fallow.forge.pret", annonce.getHoverName()) : null);
            }
        });
        view.node("leve").ifPresent(node -> node.setVisible(!leve.isEmpty() && horloge - leveA < 0.3F));
        // Le marteau quitte sa place pendant le coup.
        view.node(PostesForge.MARTEAU).ifPresent(node -> node.setVisible(coup == Coup.REPOS));
        choregraphie(view);
        Item fera = view.isFocused() && !fini && !annonce.isEmpty() ? annonce.getItem() : null;
        view.node("fera").ifPresent(node -> {
            node.setVisible(fera != null);
            if (fera != fantome && fera != null) {
                node.setVisual(Visual.ofItem(fera).withTint(0x80FFFFFF).asTranslucent());
            }
            if (fera != null) {
                node.moveTo(0.0F, 0.35F * Mth.sin(horloge * 2.2F), 0.0F, 0.0F);
                node.rotateTo(-60.0F, horloge * 40.0F % 360.0F, 0.0F, 0.0F);
            }
        });
        fantome = fera;
        // Les pièces qui manquent à la page choisie, pâles dans leurs cases vides, comme la table de Hands-On Vanilla ; quand
        // l'ingrédient en admet plusieurs, elles se relaient chaque seconde.
        java.util.List<Ingredient> attendus = source.attendus();
        if (fini) {
            source.oublie();
        }
        for (int cell = 0; cell < 9; cell++) {
            Ingredient want = cell < attendus.size() ? attendus.get(cell) : Ingredient.EMPTY;
            ItemStack[] choix = want.isEmpty() ? new ItemStack[0] : want.getItems();
            boolean vide = view.menu() != null && view.state().getItem(SlotRef.of(PostesForge.GRILLE, cell)).isEmpty();
            Item montre = view.isFocused() && vide && choix.length > 0 ? choix[(int) horloge % choix.length].getItem() : null;
            final int index = cell;
            view.node("manque_" + cell).ifPresent(node -> {
                node.setVisible(montre != null);
                if (montre != null && montre != manques[index]) {
                    node.setVisual(Visual.ofItem(montre).withTint(0x70FFFFFF).asTranslucent());
                }
            });
            manques[cell] = montre;
        }
        // Les pièces chaudes fument dans leur case.
        if (!view.isFocused()) {
            return;
        }
        int chaudes = 0;
        for (int cell = 0; cell < grille.length; cell++) {
            if (chaude(view.state().getItem(SlotRef.of(PostesForge.GRILLE, cell)))) {
                chaudes++;
            }
        }
        fumee += deltaSeconds * Math.min(3, chaudes) * 1.5F;
        while (fumee >= 1.0F && chaudes > 0) {
            fumee -= 1.0F;
            int pick = random.nextInt(chaudes);
            for (int cell = 0; cell < grille.length; cell++) {
                if (chaude(view.state().getItem(SlotRef.of(PostesForge.GRILLE, cell))) && pick-- == 0) {
                    view.spawnParticles(DiegesisParticles.SMOKE, "grille_" + cell, 1, 0x50D8D0C8);
                }
            }
        }
    }

    private static boolean chaude(ItemStack stack) {
        return !stack.isEmpty() && (stack.is(ModTags.Items.HOT_ITEMS) || stack.is(ModTags.Items.HEATED_METALS));
    }

    @Override
    public void tick(StationView view) {
        if (!view.isFocused() || view.menu() == null) {
            return;
        }
        int pose = 0;
        int pris = 0;
        boolean grille_chaude = false;
        LocalPlayer joueur = Minecraft.getInstance().player;
        int tenu = joueur == null ? 0 : joueur.containerMenu.getCarried().getCount();
        boolean deLaMain = tenu < porte;
        porte = tenu;
        for (int cell = 0; cell < grille.length; cell++) {
            ItemStack stack = view.state().getItem(SlotRef.of(PostesForge.GRILLE, cell));
            int now = stack.getCount();
            if (connu && now > grille[cell]) {
                // Posée par Maj + clic ou par le livre : la pièce vole de la barre à sa case.
                if (!deLaMain) {
                    Arrivee.caseDepuisLaBarre(view, "grille_" + cell, stack.getItem(), pose);
                }
                pose++;
                grille_chaude |= chaude(stack);
            } else if (connu && now < grille[cell]) {
                pris++;
            }
            grille[cell] = now;
        }
        connu = true;
        // Comme l'enclume de Hands-On Vanilla : un tintement clair quand une pièce se pose, un glissement quand elle part.
        if (pose > 0) {
            play(view, pierre ? SoundEvents.STONE_HIT : SoundEvents.ANVIL_PLACE, pierre ? 0.3F : 0.08F, 1.7F + random.nextFloat() * 0.2F);
            if (grille_chaude) {
                play(view, SoundEvents.FIRE_EXTINGUISH, 0.12F, 1.6F + random.nextFloat() * 0.2F);
            }
        } else if (pris > 0) {
            play(view, SoundEvents.ITEM_FRAME_REMOVE_ITEM, 0.3F, 1.0F + random.nextFloat() * 0.1F);
        }
        AbstractSmithingAnvilMenu enclume = view.menu().handle() instanceof AbstractSmithingAnvilMenu menu ? menu : null;
        int forge = enclume == null ? 0 : enclume.getResultSlot().getItem().getCount();
        fini = forge > 0;
        if (ouvrage >= 0 && forge > ouvrage) {
            // L'ouvrage forgé arrive : une gerbe d'étincelles (il apparaît en grandissant au-dessus de sa place).
            view.spawnParticles(DiegesisParticles.SPARK, new Vector3f(-2.0F, 17.5F, 11.5F), new Vector3f(0.0F, 14.0F, 0.0F), 6.0F, 14, 0xFFFFC870);
            view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(-2.0F, 18.0F, 11.5F), new Vector3f(0.0F, 2.0F, 0.0F), 3.0F, 6, 0xFFFFF0D0);
            Son.joue(view, pierre ? SoundEvents.STONE_PLACE : SoundEvents.ANVIL_PLACE, PostesForge.OUVRAGE, 0.12F, 1.4F + random.nextFloat() * 0.2F);
        }
        ouvrage = forge;
        ItemStack fera = enclume != null ? enclume.getGhostResult() : ItemStack.EMPTY;
        if (!fera.isEmpty() && !ItemStack.isSameItem(fera, annonce)) {
            view.showCaption(Component.translatable("postes.fallow.forge.pret", fera.getHoverName()), 3.0F);
            view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(-2.0F, 18.5F, 11.5F), new Vector3f(0.0F, 1.5F, 0.0F), 2.5F, 5, 0xFFFFF0D0);
            Son.joue(view, SoundEvents.AMETHYST_BLOCK_CHIME, "fera", 0.4F, 1.4F + random.nextFloat() * 0.2F);
        }
        annonce = fera.copy();
    }

    /**
     * Un clic sur l'ouvrage : le marteau vient de sa place, se lève et frappe le milieu de l'enclume ; l'ouvrage est pris au coup
     * (Maj : tout droit dans l'inventaire). Rien à prendre, ou la main pleine d'autre chose : l'ouvrage tremble, un son mat.
     */
    private void frappe(StationView view, boolean all) {
        if (coup != Coup.REPOS && coup != Coup.SORT) {
            return;
        }
        if (!prenable(view, all)) {
            if (!ouvrage(view).isEmpty()) {
                view.node(PostesForge.OUVRAGE).ifPresent(node -> node.shake(0.35F, 0.2F));
                Son.joue(view, pierre ? SoundEvents.STONE_HIT : SoundEvents.ANVIL_LAND, PostesForge.OUVRAGE, pierre ? 0.4F : 0.08F,
                        pierre ? 0.7F : 1.9F);
            }
            return;
        }
        tout = all;
        view.node("coup").ifPresent(node -> {
            if (coup == Coup.REPOS) {
                node.moveTo(DEPUIS.x, DEPUIS.y, DEPUIS.z, 0.0F);
                node.rotateTo(0.0F, 0.0F, 80.0F, 0.0F);
            }
            node.setVisible(true);
            node.hopTo(LEVE.x, LEVE.y, LEVE.z, 3.0F, 0.22F);
            node.rotateTo(0.0F, 0.0F, 60.0F, 0.22F);
        });
        aller(Coup.ENTRE);
    }

    private void choregraphie(StationView view) {
        switch (coup) {
            case ENTRE -> {
                if (tempsCoup >= 0.22F) {
                    view.node("coup").ifPresent(node -> {
                        node.moveTo(0.0F, 0.0F, 0.0F, 0.07F);
                        node.rotateTo(0.0F, 0.0F, 0.0F, 0.07F);
                    });
                    aller(Coup.FRAPPE);
                }
            }
            case FRAPPE -> {
                if (tempsCoup >= 0.07F) {
                    prend(view);
                    aller(Coup.SONNE);
                }
            }
            case SONNE -> {
                if (!revenu && tempsCoup >= 0.04F) {
                    revenu = true;
                    view.node("coup").ifPresent(node -> node.rotateTo(0.0F, 0.0F, 0.0F, 0.06F));
                }
                if (tempsCoup >= 0.12F) {
                    view.node("coup").ifPresent(node -> {
                        node.hopTo(DEPUIS.x, DEPUIS.y, DEPUIS.z, 3.0F, 0.2F);
                        node.rotateTo(0.0F, 0.0F, 80.0F, 0.2F);
                    });
                    aller(Coup.SORT);
                }
            }
            case SORT -> {
                if (tempsCoup >= 0.2F) {
                    view.node("coup").ifPresent(node -> node.setVisible(false));
                    aller(Coup.REPOS);
                }
            }
            case REPOS -> {
            }
        }
    }

    /** Le coup tombe : le vrai clic sur la case de l'ouvrage part, les étincelles jaillissent, l'ouvrage se lève. */
    private void prend(StationView view) {
        MenuAccess menu = view.menu();
        ItemStack fait = ouvrage(view);
        leve = ItemStack.EMPTY;
        if (menu != null && !fait.isEmpty() && prenable(view, tout)) {
            leve = fait.copy();
            leveA = horloge;
            menu.click(caseOuvrage, 0, tout ? ClickType.QUICK_MOVE : ClickType.PICKUP);
        }
        Vector3f at = new Vector3f(8.0F, 16.6F, 8.0F);
        if (pierre) {
            // Sur la pierre, le coup est sourd et soulève de la poussière ; seule la pièce chaude crache quelques étincelles.
            view.spawnParticles(DiegesisParticles.DUST, at, new Vector3f(0.0F, 5.0F, 0.0F), 6.0F, tout ? 14 : 9, 0xFF9A9590);
            view.spawnParticles(DiegesisParticles.SPARK, at, new Vector3f(0.0F, 10.0F, 0.0F), 6.0F, chaude(leve) ? 6 : 0, 0xFFFFC870);
            Son.joue(view, SoundEvents.STONE_HIT, "leve", 0.6F, 0.8F + random.nextFloat() * 0.2F);
            Son.joue(view, SoundEvents.ANVIL_LAND, "leve", 0.06F, 1.6F + random.nextFloat() * 0.2F);
        } else {
            view.spawnParticles(DiegesisParticles.SPARK, at, new Vector3f(0.0F, 14.0F, 0.0F), 10.0F, tout ? 20 : 12, 0xFFFFC870);
            view.spawnParticles(DiegesisParticles.SPARK, at, new Vector3f(12.0F, 4.0F, 0.0F), 4.0F, 2, 0xFFFFC870);
            view.spawnParticles(DiegesisParticles.SPARK, at, new Vector3f(-12.0F, 4.0F, 0.0F), 4.0F, 2, 0xFFFFC870);
            Son.joue(view, SoundEvents.ANVIL_USE, "leve", 0.25F, 1.0F + random.nextFloat() * 0.2F);
        }
        view.node("leve").ifPresent(node -> node.shake(0.25F, 0.1F));
        view.node("coup").ifPresent(node -> node.rotateTo(0.0F, 0.0F, 10.0F, 0.04F));
        revenu = false;
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        }
    }

    /** L'ouvrage se prend : main vide ou la même chose qui s'empile, et de la place dans l'inventaire avec Maj. */
    private boolean prenable(StationView view, boolean all) {
        ItemStack fait = ouvrage(view);
        if (fait.isEmpty() || view.menu() == null) {
            return false;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return false;
        }
        if (all) {
            return player.getInventory().getFreeSlot() >= 0 || player.getInventory().getSlotWithRemainingSpace(fait) >= 0;
        }
        ItemStack held = player.containerMenu.getCarried();
        return held.isEmpty() || ItemStack.isSameItemSameComponents(held, fait) && held.getCount() + fait.getCount() <= held.getMaxStackSize();
    }

    private void aller(Coup suivant) {
        coup = suivant;
        tempsCoup = 0.0F;
    }

    private static void play(StationView view, SoundEvent sound, float volume, float pitch) {
        Son.joue(view, sound, view.node("grille").isPresent() ? "grille" : null, volume, pitch);
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Click click && "book_open".equals(click.node())) {
            book.click(view, click.button());
        } else if (event instanceof SceneEvent.Click click && PostesForge.OUVRAGE.equals(click.node())) {
            frappe(view, Screen.hasShiftDown());
        }
    }

    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if ("book_open".equals(node)) {
            book.click(view, button);
            return true;
        }
        if (PostesForge.OUVRAGE.equals(node)) {
            frappe(view, shift);
            return true;
        }
        return false;
    }

    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        Arrivee.depuis(view, slot);
        return false;
    }

    @Override
    public boolean onScroll(StationView view, String node, double amount) {
        return "book_open".equals(node) && book.scroll(amount);
    }

    @Override
    public boolean onKey(StationView view, int keyCode, int scanCode, int modifiers) {
        return book.key(view, keyCode, modifiers);
    }

    @Override
    public boolean onChar(StationView view, char character, int modifiers) {
        return book.typed(view, character);
    }
}
