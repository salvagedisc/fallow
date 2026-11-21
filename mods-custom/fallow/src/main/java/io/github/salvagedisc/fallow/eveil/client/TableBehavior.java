package io.github.salvagedisc.fallow.eveil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.inventory.EnchantmentNames;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.EnchantingTableBlockEntity;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.NodePainter;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.eveil.Eclat;
import io.github.salvagedisc.fallow.eveil.EtatEveil;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import io.github.salvagedisc.fallow.eveil.TableEveil;

/**
 * La table d'enchantement en poste, sur le client, sur le modèle de celle de Hands-On Vanilla (MIT, Pocky) : le livre de la table
 * vient au joueur pendant que la caméra se penche, depuis l'endroit où il flotte, et s'ouvre devant elle. À gauche, la vie de
 * l'objet posé (ses éveils, ses plus grands hauts faits, son chemin vers le prochain) ; à droite, les deux voies que ses actes
 * ouvrent, chacune avec ce qu'elle fait et l'acte qui l'a fait naître, ou ce que l'éclat d'une grande bête lui donnerait. Un clic
 * sur une voie l'éveille : ses lettres coulent en runes dans l'objet, qui se soulève, tourne une fois et retombe éveillé. Ce que
 * les pages disent vient du serveur ({@link TableEveil}) ; chaque nouvelle page arrive par une page tournée, l'ancienne reste
 * écrite sur la feuille qui s'en va. En partant, le livre se referme à moitié et retourne flotter au-dessus de la table.
 */
final class TableBehavior implements StationBehavior {
    static final String CORPS = "corps";
    static final String LIVRE = "livre";

    /** La partie écrite d'une page en unités, la police d'une unité par pixel : comme chez Hands-On Vanilla, lisible en 1280 x 720. */
    private static final int LARGE = 100;
    private static final int HAUT = 156;
    private static final float GAUCHE_X = 3.0F;
    private static final float DROITE_X = 4.0F;
    private static final float LIGNE = 9.0F;
    private static final float PETIT = 0.72F;
    private static final float PETITE_LIGNE = 7.0F;
    private static final float PIED = 141.0F;

    /** Les encres : le texte en brun presque noir, les titres en sépia, les forces en violet d'encre, l'à-côté en brun pâli. */
    private static final int ENCRE = 0x2A2016;
    private static final int SEPIA = 0x7A5A2E;
    private static final int VIOLET = 0x45287A;
    private static final int PALE = 0x5E4D38;
    private static final int ROUGE = 0xAA0000;
    private static final int FILET = 0x7A6545;
    private static final int RUNES = 0x685E4A;
    private static final int BANDE = 0x1E8B5A2B;
    private static final int BANDE_VIVE = 0x2E6A3FB0;
    /** Les couleurs des runes qui coulent dans l'objet : celles de l'éclat des grandes bêtes et de la lueur des objets enchantés. */
    private static final int[] LUEURS = {0xB98CFF, 0x9FB8FF, 0xE6D9FF, 0xC9A8FF, 0x8F7BFF};
    private static final int GLINT = 0xFFC890FF;

    /** Secondes : le livre vient et repart, s'ouvre, l'objet en l'air, les runes qui coulent. */
    private static final float VENUE = 0.35F;
    private static final float DEPART = 0.3F;
    private static final float OUVERTURE = 0.25F;
    private static final float EN_L_AIR = 0.6F;
    private static final float ENVOL = 1.6F;
    private static final float COULEE = 0.7F;
    private static final float COULEE_PAR_SECONDE = 45.0F;
    static final float VOL_DE_RUNE = 0.5F;
    /** Un clic attend sa réponse au plus ce temps ; après, la voie se réécrit. */
    private static final float ATTENTE = 3.0F;
    private static final float VOL = 0.32F;

    /** Le livre ouvert comme celui de vanilla au-dessus de la table ; sa taille sur la table (vanilla) et à la lecture. */
    private static final float OUVERT = 1.4F;
    private static final float FEUILLE = 0.97F;
    private static final float DECALE = 1.3F;
    private static final float DEMI_FERME = 3.0F;
    private static final float TAILLE_TABLE = 1.0F;
    private static final float TAILLE_LECTURE = 0.8F;
    private static final int LUMIERE_MIN = 12;

    /** Le dernier clic droit sur un bloc, et quand : un poste ouvert juste après l'a été par ce clic. */
    private static BlockPos dernierUsage;
    private static long dernierUsageA;

    private final OpenBook book = new OpenBook(LARGE, HAUT, this::peindre);
    private StationView station;
    private float horloge;
    private float glisse;
    private boolean vient;
    private float ouvert;
    private float tourCorps = Float.NaN;

    /** Ce que chaque double page dit, par son numéro : la feuille qui s'en va garde ce qu'elle disait. */
    private final Map<Integer, Contenu> contenus = new HashMap<>();
    private Contenu enAttente;
    private int cleVue = Integer.MIN_VALUE;
    /** Les lignes des voies sur la page de droite, comme écrites à la dernière image (haut, bas), et celle sous le curseur. */
    private final float[][] voies = {{-1.0F, -1.0F}, {-1.0F, -1.0F}};
    private int survol = -1;
    private boolean pageSurvolee;
    /** La voie cliquée en dernier, dont les lettres coulent dans l'objet, et quand. */
    private int coulante = -1;
    private float clicA = -10.0F;
    private float couleeJusqua = -10.0F;
    private final Vector3f couleeDepuis = new Vector3f();
    private float couleeReste;
    private int couleeCouleur = -1;
    private float sautA = -1.0F;
    /** Les runes qui décorent les pages sans rien d'écrit, par page. */
    private final Map<Integer, List<FormattedCharSequence>> runes = new HashMap<>();
    private final List<float[]> bruits = new ArrayList<>();
    /** Ce que les places tenaient à la dernière image ; la case de l'inventaire d'où part la prochaine pile posée. */
    private ItemStack vuObjet = ItemStack.EMPTY;
    private ItemStack vuEclat = ItemStack.EMPTY;
    private int volObjet = -1;
    private int volEclat = -1;
    private int enMain = -1;
    private final RandomSource hasard = RandomSource.create();

    /** Ce qu'une double page dit : l'objet et l'éclat tels qu'ils étaient posés, et ce que le serveur en a dit. */
    private record Contenu(ItemStack objet, ItemStack eclat, CompoundTag data) {
        String etat() {
            return data.getString(TableEveil.ETAT);
        }

        boolean aEclat() {
            return data.contains(TableEveil.ECLAT_ETAT) && !eclat.isEmpty();
        }
    }

    TableBehavior() {
        book.setFiller(this::runes);
    }

    /** Appelé quand la touche d'usage part : retient le bloc visé, pour savoir si l'on est venu au poste par ce clic. */
    static void usage(BlockPos pos) {
        dernierUsage = pos;
        dernierUsageA = Util.getMillis();
    }

    @Override
    public void onSceneReady(StationView view) {
        station = view;
        tourCorps = Float.NaN;
        view.setInventoryFilter(stack -> TableEveil.objet(stack) || TableEveil.eclat(stack));
        view.node(LIVRE).ifPresent(node -> node.setPainter(new NodePainter() {
            @Override
            public void paint(PoseStack poseStack, MultiBufferSource buffers, int light) {
                dessiner(view, poseStack, buffers);
            }

            @Override
            public boolean smooth() {
                return true;
            }
        }));
    }

    @Override
    public void onOpen(StationView view) {
        station = view;
        vient = true;
        tourCorps = Float.NaN;
        contenus.clear();
        enAttente = null;
        cleVue = Integer.MIN_VALUE;
        coulante = -1;
        couleeJusqua = -10.0F;
        sautA = -1.0F;
        bruits.clear();
        survol = -1;
        vuObjet = view.state().getItem(TableEveil.OBJET_REF).copy();
        vuEclat = view.state().getItem(TableEveil.ECLAT_REF).copy();
        volObjet = -1;
        volEclat = -1;
        LocalPlayer player = Minecraft.getInstance().player;
        boolean venu = view.pos().equals(dernierUsage) && Util.getMillis() - dernierUsageA < 1500L;
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        enMain = venu && (TableEveil.objet(main) || TableEveil.eclat(main)) && player.inventoryMenu.getCarried().isEmpty()
                ? player.getInventory().selected : -1;
        // Le livre s'ouvre sur les pages que celui de la table montre, comme s'il était celui-là.
        if (view.level().getBlockEntity(view.pos()) instanceof EnchantingTableBlockEntity entity) {
            book.jumpTo(OpenBook.flipTurns(entity.flip));
        }
    }

    @Override
    public void onClose(StationView view) {
        vient = false;
        survol = -1;
        enMain = -1;
        view.node(LIVRE).ifPresent(node -> node.setTooltip(null));
    }

    /** Le livre retourne encore à la table : la scène tient lieu du bloc jusqu'à ce qu'il y soit. */
    @Override
    public boolean holdsBlock(StationView view) {
        return !view.isInUse() && glisse > 0.0F;
    }

    @Override
    public void tick(StationView view) {
        regarderLesPlaces(view);
    }

    /** Ce qui se pose sur la table sonne et fait courir les pages ; ce qui vient de la barre y vole. */
    private void regarderLesPlaces(StationView view) {
        ItemStack objet = view.state().getItem(TableEveil.OBJET_REF);
        ItemStack eclat = view.state().getItem(TableEveil.ECLAT_REF);
        if (!ItemStack.isSameItem(objet, vuObjet) || objet.isEmpty() != vuObjet.isEmpty()) {
            if (!objet.isEmpty()) {
                volerDepuisLaBarre(view, TableEveil.OBJET, volObjet);
                joue(view, SoundEvents.ITEM_FRAME_ADD_ITEM, TableEveil.OBJET, 0.3F, 0.9F + 0.2F * hasard.nextFloat());
                feuilleter(view, 8, 0.5F, 1);
            } else {
                joue(view, SoundEvents.ITEM_FRAME_REMOVE_ITEM, TableEveil.OBJET, 0.3F, 0.9F + 0.2F * hasard.nextFloat());
                feuilleter(view, 4, 0.3F, -1);
            }
            volObjet = -1;
        }
        if (eclat.isEmpty() != vuEclat.isEmpty()) {
            if (!eclat.isEmpty()) {
                volerDepuisLaBarre(view, TableEveil.ECLAT, volEclat);
                joue(view, SoundEvents.AMETHYST_CLUSTER_PLACE, TableEveil.ECLAT, 0.35F, 1.2F + 0.2F * hasard.nextFloat());
            } else if (sautA < 0.0F) {
                joue(view, SoundEvents.ITEM_FRAME_REMOVE_ITEM, TableEveil.ECLAT, 0.3F, 1.3F);
            }
            volEclat = -1;
        }
        vuObjet = objet.copy();
        vuEclat = eclat.copy();
    }

    /** La pile envoyée de la case {@code depuis} de la barre (Maj + clic, la main en arrivant) y vole, en arc, comme chez Hands-On. */
    private static void volerDepuisLaBarre(StationView view, String noeud, int depuis) {
        if (depuis < 0 || !view.isFocused()) {
            return;
        }
        view.node(noeud).ifPresent(node -> {
            Vector3fc base = node.definition().transform().pos();
            var window = Minecraft.getInstance().getWindow();
            float large = window.getGuiScaledWidth();
            float haut = window.getGuiScaledHeight();
            float x = depuis < Inventory.getSelectionSize() ? (large / 2.0F - 80.0F + 20.0F * depuis) / large : 0.5F;
            view.screenOn(x, (haut - 11.0F) / haut, base.y() + 2.0F).map(on -> on.sub(base, new Vector3f()))
                    .filter(from -> from.length() <= 40.0F).ifPresent(from -> {
                        node.moveTo(from.x, from.y, from.z, 0.0F);
                        node.hopTo(0.0F, 0.0F, 0.0F, 2.5F, VOL);
                    });
        });
    }

    /** Les feuilles courent sous le pouce, avec le froissement de quelques pages, de plus en plus aigu. */
    private void feuilleter(StationView view, int feuilles, float secondes, int sens) {
        if (!view.isFocused() || book.isRiffling()) {
            return;
        }
        book.riffle(feuilles, secondes, sens);
        int froissements = feuilles > 5 ? 3 : 2;
        bruits.clear();
        for (int i = 0; i < froissements; i++) {
            bruits.add(new float[] {horloge + i * secondes / froissements, 1.1F + 0.2F * i + 0.1F * hasard.nextFloat()});
        }
    }

    @Override
    public void frame(StationView view, float dt) {
        station = view;
        horloge += dt;
        book.frame(dt);
        for (int i = bruits.size() - 1; i >= 0; i--) {
            if (horloge >= bruits.get(i)[0]) {
                joue(view, SoundEvents.BOOK_PAGE_TURN, LIVRE, 0.3F, bruits.get(i)[1]);
                bruits.remove(i);
            }
        }
        if (enMain >= 0 && view.isFocused() && DiegesisClient.arrival() >= 1.0F) {
            // Comme le four de Hands-On Vanilla : envoyée à l'ouverture, la pile arriverait au serveur avant le poste et se perdrait.
            poser(view, enMain);
            enMain = -1;
        }
        boolean cache = view.isBlockHidden();
        view.node(CORPS).ifPresent(node -> node.setVisible(cache));
        view.node(LIVRE).ifPresent(node -> node.setVisible(cache));
        if (view.turn() != tourCorps) {
            // La scène fait face au joueur sous son angle ; la table reste d'équerre avec le monde, comme le bloc.
            tourCorps = view.turn();
            view.node(CORPS).ifPresent(node -> node.rotateTo(0.0F, tourCorps, 0.0F, 0.0F));
        }
        if (vient && view.isFocused()) {
            glisse = cache ? Math.min(1.0F, glisse + dt / VENUE) : glisse;
        } else {
            glisse = Math.max(0.0F, glisse - dt / DEPART);
        }
        ouvert = Mth.approach(ouvert, vient ? (view.isFocused() ? 1.0F : 0.0F) : 0.5F, dt / OUVERTURE);
        lire(view);
        couler(view, dt);
        retomber(view);
        if (coulante >= 0 && horloge - clicA > ATTENTE) {
            coulante = -1;
        }
        int avant = survol;
        survol = -1;
        pageSurvolee = false;
        Contenu ici = affiche();
        if (ici != null && glisse >= 1.0F && ouvert >= 1.0F && coulante < 0) {
            Optional<OpenBook.Hit> hit = sousLeCurseur(view);
            if (hit.isPresent() && hit.get().right()) {
                if (ici.aEclat()) {
                    pageSurvolee = TableEveil.ECLAT_OK.equals(ici.data().getString(TableEveil.ECLAT_ETAT));
                } else if (TableEveil.ETAT_PRET.equals(ici.etat())) {
                    survol = voieSous(hit.get().y());
                }
            }
        }
        if (survol >= 0 && survol != avant) {
            joue(view, SoundEvents.BOOK_PAGE_TURN, LIVRE, 0.08F, 1.8F);
        }
    }

    /**
     * Ce que le serveur dit de la table, quand il change : la page tourne et la nouvelle s'écrit (pendant que les feuilles
     * courent, la dernière l'apporte ; une fois qu'elles se sont arrêtées, une feuille de plus).
     */
    private void lire(StationView view) {
        CompoundTag data = view.state().data();
        if (!data.contains(TableEveil.CLE)) {
            return;
        }
        int cle = data.getInt(TableEveil.CLE);
        if (cle != cleVue) {
            Contenu contenu = new Contenu(view.state().getItem(TableEveil.OBJET_REF).copy(),
                    view.state().getItem(TableEveil.ECLAT_REF).copy(), data.copy());
            boolean premier = cleVue == Integer.MIN_VALUE;
            cleVue = cle;
            if (premier || glisse < 1.0F || ouvert < 0.9F) {
                // Le livre arrive encore fermé : il s'ouvre sur ce qu'il a à dire.
                contenus.put(book.spread(), contenu);
                enAttente = null;
            } else if (book.isRiffling() && !book.riffleUncovered()) {
                book.jumpTo(book.spread() + 1);
                contenus.put(book.spread(), contenu);
                enAttente = null;
            } else if (book.isRiffling() || book.isFlipping()) {
                enAttente = contenu;
            } else {
                tourner(view, contenu);
            }
        }
        if (enAttente != null && !book.isRiffling() && !book.isFlipping()) {
            tourner(view, enAttente);
            enAttente = null;
        }
        contenus.keySet().removeIf(spread -> Math.abs(spread - book.spread()) > 3);
    }

    private void tourner(StationView view, Contenu contenu) {
        book.turnTo(book.spread() + 1, false);
        contenus.put(book.spread(), contenu);
        joue(view, SoundEvents.BOOK_PAGE_TURN, LIVRE, 0.35F, 1.0F + 0.2F * hasard.nextFloat());
    }

    /** Ce que dit la double page ouverte, quand elle est au repos ; null pendant que les feuilles tournent. */
    private Contenu affiche() {
        return book.isRiffling() || book.isFlipping() ? null : contenus.get(book.spread());
    }

    private int voieSous(float y) {
        for (int i = 0; i < voies.length; i++) {
            if (voies[i][0] >= 0.0F && y >= voies[i][0] && y < voies[i][1]) {
                return i;
            }
        }
        return -1;
    }

    private Optional<OpenBook.Hit> sousLeCurseur(StationView view) {
        Pose pose = pose(view);
        return book.hit(view, LIVRE, pose.versLeNoeud(view), pose.open, pose.right, pose.left);
    }

    // ---- les gestes ----

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Click click && LIVRE.equals(click.node()) && click.button() == 0) {
            presser(view);
        }
    }

    /** Une pile en main n'empêche pas de choisir : le livre prend le clic, la pile reste où elle est. */
    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if (LIVRE.equals(node)) {
            if (button == 0) {
                presser(view);
            }
            return true;
        }
        return false;
    }

    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        if (!TableEveil.objet(stack) && !TableEveil.eclat(stack)) {
            return false;
        }
        poser(view, slot);
        return true;
    }

    /** La pile de la case {@code slot} va à sa place sur la table, en volant depuis la barre. */
    private void poser(StationView view, int slot) {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (TableEveil.objet(stack)) {
            volObjet = slot;
        } else if (TableEveil.eclat(stack)) {
            volEclat = slot;
        } else {
            return;
        }
        CompoundTag how = new CompoundTag();
        how.putInt(TableEveil.DEPUIS, slot);
        view.sendAction(TableEveil.POSER, how);
    }

    /**
     * Un clic sur la page de droite : la voie survolée s'éveille, ou l'éclat se verse ; ses lettres se détachent et coulent dans
     * l'objet. Sur une page qui n'a rien à donner, le livre frémit.
     */
    private void presser(StationView view) {
        Contenu ici = affiche();
        if (ici == null || ouvert < 0.5F || coulante >= 0) {
            return;
        }
        Optional<OpenBook.Hit> hit = sousLeCurseur(view);
        if (hit.isEmpty() || !hit.get().right()) {
            return;
        }
        Pose pose = pose(view);
        if (ici.aEclat()) {
            if (!TableEveil.ECLAT_OK.equals(ici.data().getString(TableEveil.ECLAT_ETAT))) {
                refus(view);
                return;
            }
            view.sendAction(TableEveil.VERSER, new CompoundTag());
            Eclat eclat = ici.eclat().get(EveilModule.ECLAT.get());
            commencerCoulee(view, pose, LARGE / 2.0F, 60.0F, 0, eclat == null ? -1 : eclat.couleur());
            return;
        }
        if (!TableEveil.ETAT_PRET.equals(ici.etat())) {
            refus(view);
            return;
        }
        int voie = voieSous(hit.get().y());
        if (voie < 0) {
            return;
        }
        CompoundTag how = new CompoundTag();
        how.putInt(TableEveil.VOIE, voie);
        view.sendAction(TableEveil.CHOISIR, how);
        commencerCoulee(view, pose, LARGE / 2.0F, (voies[voie][0] + voies[voie][1]) / 2.0F, voie, -1);
    }

    private void commencerCoulee(StationView view, Pose pose, float x, float y, int voie, int couleur) {
        couleeDepuis.set(pose.versLaScene(book.onPage(true, x, y, pose.open, pose.right, pose.left)));
        couleeJusqua = horloge + COULEE;
        couleeReste = 0.0F;
        couleeCouleur = couleur;
        coulante = voie;
        clicA = horloge;
        Minecraft.getInstance().player.swing(net.minecraft.world.InteractionHand.MAIN_HAND);
        joue(view, SoundEvents.BOOK_PAGE_TURN, LIVRE, 0.4F, 0.7F);
        joue(view, SoundEvents.ENCHANTMENT_TABLE_USE, LIVRE, 0.25F, 1.4F);
    }

    private void refus(StationView view) {
        view.node(LIVRE).ifPresent(node -> node.shake(0.3F, 0.2F));
        joue(view, SoundEvents.BOOK_PUT, LIVRE, 0.25F, 0.8F);
    }

    /**
     * Un objet s'est éveillé (chez tous ceux qui voient la table) : il se soulève, tourne une fois dans l'air et retombe dans une
     * gerbe de lueurs ; ceux qui n'ont pas cliqué voient aussi les runes couler du livre.
     */
    @Override
    public void onEffect(StationView view, ResourceLocation effect, String node, CompoundTag data) {
        if (!TableEveil.EVEIL.equals(effect)) {
            return;
        }
        LocalPlayer player = Minecraft.getInstance().player;
        boolean moi = player != null && data.hasUUID(TableEveil.ACTEUR) && player.getUUID().equals(data.getUUID(TableEveil.ACTEUR));
        if (!moi) {
            Pose pose = pose(view);
            commencerCoulee(view, pose, LARGE / 2.0F, HAUT / 2.0F, 0, -1);
        }
        coulante = -1;
        sautA = horloge;
        view.node(TableEveil.OBJET).ifPresent(objet -> {
            objet.moveTo(0.0F, 3.0F, 0.0F, 0.2F);
            objet.rotateTo(0.0F, 360.0F, 0.0F, EN_L_AIR);
            Vector3f at = new Vector3f(objet.definition().transform().pos()).add(0.0F, 1.5F, 0.0F);
            view.spawnParticles(DiegesisParticles.GLINT, at, new Vector3f(0.0F, 6.0F, 0.0F), 1.5F, 10, GLINT);
        });
        if (data.getBoolean(TableEveil.PAR_ECLAT)) {
            view.node(TableEveil.ECLAT).ifPresent(eclat -> {
                Vector3f at = new Vector3f(eclat.definition().transform().pos()).add(0.0F, 1.0F, 0.0F);
                view.spawnParticles(DiegesisParticles.GLINT, at, new Vector3f(0.0F, 4.0F, 0.0F), 1.2F, 12, 0xFF000000 | Eclat.COULEUR);
            });
            joue(view, SoundEvents.AMETHYST_BLOCK_BREAK, TableEveil.ECLAT, 0.5F, 1.1F);
        }
        joue(view, SoundEvents.ENCHANTMENT_TABLE_USE, TableEveil.OBJET, 0.6F, 1.0F);
    }

    /** L'objet retombe, éveillé, avec un carillon. */
    private void retomber(StationView view) {
        if (sautA < 0.0F || horloge - sautA < EN_L_AIR) {
            return;
        }
        sautA = -1.0F;
        view.node(TableEveil.OBJET).ifPresent(objet -> {
            objet.moveTo(0.0F, 0.0F, 0.0F, 0.15F);
            objet.rotateTo(0.0F, 0.0F, 0.0F, 0.0F);
            Vector3f at = new Vector3f(objet.definition().transform().pos()).add(0.0F, 1.0F, 0.0F);
            view.spawnParticles(DiegesisParticles.GLINT, at, new Vector3f(0.0F, 3.0F, 0.0F), 4.0F, 18, GLINT);
            // Quelques runes s'envolent de l'objet éveillé, comme d'un livre qu'on referme.
            for (int i = 0; i < 8; i++) {
                Vector3f vitesse = new Vector3f((hasard.nextFloat() - 0.5F) * 4.0F, 3.0F + 3.0F * hasard.nextFloat(),
                        (hasard.nextFloat() - 0.5F) * 4.0F);
                view.spawnParticles(TableClient.glypheVolant(hasard), at, vitesse, 0.5F, 1, 0xFF000000 | lueur());
            }
        });
        joue(view, SoundEvents.AMETHYST_BLOCK_CHIME, TableEveil.OBJET, 0.7F, 1.2F + 0.2F * hasard.nextFloat());
        feuilleter(view, 6, 0.5F, 1);
    }

    /** Les runes de la voie cliquée, de la page dans l'objet, un flot continu un moment après le clic. */
    private void couler(StationView view, float dt) {
        if (horloge >= couleeJusqua) {
            return;
        }
        couleeReste += dt * COULEE_PAR_SECONDE;
        int nombre = (int) couleeReste;
        couleeReste -= nombre;
        if (nombre <= 0) {
            return;
        }
        view.node(TableEveil.OBJET).ifPresent(objet -> {
            Vector3f vers = new Vector3f(objet.definition().transform().pos()).add(0.0F, 1.0F, 0.0F);
            for (int i = 0; i < nombre; i++) {
                Vector3f depuis = new Vector3f(couleeDepuis).add((hasard.nextFloat() - 0.5F) * 6.0F, (hasard.nextFloat() - 0.5F) * 1.5F,
                        (hasard.nextFloat() - 0.5F) * 2.0F);
                Vector3f vitesse = new Vector3f(vers).sub(depuis).div(VOL_DE_RUNE);
                int couleur = couleeCouleur >= 0 && hasard.nextBoolean() ? couleeCouleur : lueur();
                view.spawnParticles(TableClient.glyphe(hasard), depuis, vitesse, 0.6F, 1, 0xFF000000 | couleur);
            }
        });
    }

    private int lueur() {
        return LUEURS[hasard.nextInt(LUEURS.length)];
    }

    // ---- le livre ----

    /** La place, la tournure et l'ouverture du livre à cette image, du livre de la table (0) à l'endroit où on le lit (1). */
    private record Pose(Vector3f at, Quaternionf turn, float scale, float open, float right, float left, float ink, int light) {
        Matrix4f versLaScene() {
            return new Matrix4f().translation(at).rotate(turn).scale(scale);
        }

        Vector3f versLaScene(Vector3f model) {
            return versLaScene().transformPosition(model);
        }

        Matrix4f versLeNoeud(StationView view) {
            Vector3fc node = view.node(LIVRE).map(n -> n.definition().transform().pos()).orElse(new Vector3f());
            return new Matrix4f().translation(new Vector3f(at).sub(node)).rotate(turn).scale(scale);
        }
    }

    private Pose pose(StationView view) {
        float k = vient ? sortieCubique(glisse) : 1.0F - sortieCubique(1.0F - glisse);
        float partial = Minecraft.getInstance().getTimer().getGameTimeDeltaPartialTick(false);
        // À la lecture : couché sur l'air, ses pages face à extra.faces (la caméra), comme un livre qu'on tient pour le lire ;
        // fermé, tourné d'un quart sur son dos pour que sa couverture regarde la caméra.
        Vector3fc centre = view.node(LIVRE).map(n -> n.definition().transform().pos()).orElse(new Vector3f(8.0F, 18.5F, 9.5F));
        Vector3f oeil = regarde(view);
        Vector3f versOeil = new Vector3f(oeil).sub(centre).normalize();
        Vector3f haut = new Vector3f(0.0F, 1.0F, 0.0F);
        haut.sub(new Vector3f(versOeil).mul(haut.dot(versOeil))).normalize();
        Vector3f cote = new Vector3f(versOeil).cross(haut);
        float ferme = 1.0F - ouvert;
        Matrix4f lecture = new Matrix4f().translation(new Vector3f(centre))
                .mul(new Matrix4f(versOeil.x, versOeil.y, versOeil.z, 0.0F, haut.x, haut.y, haut.z, 0.0F, cote.x, cote.y, cote.z, 0.0F,
                        0.0F, 0.0F, 0.0F, 1.0F))
                .translate(-DECALE * TAILLE_LECTURE * ouvert, 0.0F, 0.0F)
                .rotateY(Mth.HALF_PI * ferme * k)
                .translate(-DEMI_FERME * TAILLE_LECTURE * ferme * k, 0.0F, 0.0F);
        float ouvertLecture = OUVERT * ouvert;
        float repos = Math.max(0.001F, ouvertLecture * FEUILLE);
        Matrix4f table = lecture;
        float ouvertTable = ouvertLecture;
        float droiteTable = repos;
        float gaucheTable = -repos;
        int lumiereTable = LightTexture.FULL_BRIGHT;
        if (view.level().getBlockEntity(view.pos()) instanceof EnchantingTableBlockEntity entity) {
            // Exactement ce que dessine EnchantTableRenderer en ce moment, en pixels de la scène.
            float temps = entity.time + partial;
            float tour = entity.rot - entity.oRot;
            while (tour >= Mth.PI) {
                tour -= Mth.TWO_PI;
            }
            while (tour < -Mth.PI) {
                tour += Mth.TWO_PI;
            }
            Matrix4f monde = new Matrix4f().translation(8.0F, 12.0F + 1.6F + Mth.sin(temps * 0.1F) * 0.16F, 8.0F)
                    .rotateY(-(entity.oRot + tour * partial)).rotateZ(80.0F * Mth.DEG_TO_RAD);
            table = OpenBook.sceneFromBlock(view).mul(monde);
            ouvertTable = (Mth.sin(temps * 0.02F) * 0.1F + 1.25F) * Mth.lerp(partial, entity.oOpen, entity.open);
            float[] feuilles = OpenBook.flipAngles(Mth.lerp(partial, entity.oFlip, entity.flip), ouvertTable);
            droiteTable = feuilles[0];
            gaucheTable = feuilles[1];
            lumiereTable = LevelRenderer.getLightColor(view.level(), view.pos());
        }
        Vector3f at = table.getTranslation(new Vector3f()).lerp(lecture.getTranslation(new Vector3f()), k);
        Quaternionf turn = table.getNormalizedRotation(new Quaternionf()).slerp(lecture.getNormalizedRotation(new Quaternionf()), k);
        int lumiere = LightTexture.FULL_BRIGHT;
        int light = k < 0.5F ? lumiereTable : LightTexture.pack(Math.max(LightTexture.block(lumiere), LUMIERE_MIN), LightTexture.sky(lumiere));
        float ink = Mth.clamp((k - 0.6F) / 0.4F, 0.0F, 1.0F);
        return new Pose(at, turn, Mth.lerp(k, TAILLE_TABLE, TAILLE_LECTURE), Mth.lerp(k, ouvertTable, ouvertLecture),
                Mth.lerp(k, droiteTable, repos), Mth.lerp(k, gaucheTable, -repos), ink, light);
    }

    private static float sortieCubique(float x) {
        float y = 1.0F - Mth.clamp(x, 0.0F, 1.0F);
        return 1.0F - y * y * y;
    }

    /** Le point que regardent les pages du livre lu ({@code extra.faces} du nœud), en pixels de la scène. */
    private static Vector3f regarde(StationView view) {
        return view.node(LIVRE).map(node -> {
            var extra = node.extra();
            if (extra != null && extra.has("faces")) {
                var at = extra.getAsJsonArray("faces");
                return new Vector3f(at.get(0).getAsFloat(), at.get(1).getAsFloat(), at.get(2).getAsFloat());
            }
            return new Vector3f(8.0F, 37.5F, -9.5F);
        }).orElse(new Vector3f(8.0F, 37.5F, -9.5F));
    }

    private void dessiner(StationView view, PoseStack poseStack, MultiBufferSource buffers) {
        Pose pose = pose(view);
        Vector3fc node = view.node(LIVRE).map(n -> n.definition().transform().pos()).orElse(new Vector3f());
        poseStack.pushPose();
        poseStack.translate(pose.at.x - node.x(), pose.at.y - node.y(), pose.at.z - node.z());
        poseStack.mulPose(pose.turn);
        poseStack.scale(pose.scale, pose.scale, pose.scale);
        book.draw(poseStack, buffers, pose.light, pose.open, pose.right, pose.left, pose.ink, 1.0F);
        poseStack.popPose();
    }

    // ---- les pages ----

    /** Une page : à gauche la vie de l'objet, à droite ses voies ou l'éclat. Rien sur les feuilles qui courent. */
    private void peindre(OpenBook.Canvas canvas, int page, boolean repos) {
        if (OpenBook.isRiffleSheet(page)) {
            return;
        }
        int spread = Math.floorDiv(page, 2);
        Contenu contenu = contenus.get(spread);
        if (contenu == null) {
            return;
        }
        boolean droite = Math.floorMod(page, 2) == 1;
        boolean courante = repos && spread == book.spread();
        if (droite) {
            if (courante) {
                voies[0][0] = -1.0F;
                voies[1][0] = -1.0F;
            }
            if (contenu.aEclat()) {
                pageEclat(canvas, contenu, courante);
            } else {
                pageVoies(canvas, contenu, courante);
            }
        } else {
            pageVie(canvas, contenu);
        }
    }

    /** La page de gauche : l'objet, ses éveils et quand, ses plus grands hauts faits, son chemin vers le prochain éveil. */
    private void pageVie(OpenBook.Canvas canvas, Contenu contenu) {
        Font font = Minecraft.getInstance().font;
        ItemStack objet = contenu.objet();
        if (objet.isEmpty()) {
            float y = titre(canvas, Component.translatable("eveil.fallow.livre.titre"), 8.0F);
            filet(canvas, y + 1.0F);
            y = paragraphe(canvas, Component.translatable("eveil.fallow.livre.invite"), GAUCHE_X, y + 6.0F, 1.0F, ENCRE, 12);
            paragraphe(canvas, Component.translatable("eveil.fallow.livre.invite_eclat"), GAUCHE_X, y + 6.0F, PETIT, PALE, 6);
            return;
        }
        canvas.item(station.level(), objet, GAUCHE_X, 3.0F, 14.0F);
        List<FormattedCharSequence> nom = font.split(objet.getHoverName().copy().withStyle(Style.EMPTY), (int) ((LARGE - 22.0F) / 1.0F));
        float y = nom.size() > 1 ? 2.0F : 6.0F;
        for (FormattedCharSequence ligne : nom.subList(0, Math.min(2, nom.size()))) {
            canvas.text(ligne, 20.0F, y, ENCRE, 1.0F);
            y += LIGNE;
        }
        EtatEveil etat = objet.get(EveilModule.ETAT.get());
        y = Math.max(y, 18.0F);
        if (etat != null) {
            canvas.text(Component.translatable("eveil.fallow.tooltip.places", etat.occupees(), etat.places()).getVisualOrderText(),
                    20.0F, y - 1.0F, PETIT, PALE, 1.0F);
        }
        y += 9.0F;
        filet(canvas, y);
        y += 5.0F;
        y = entete(canvas, Component.translatable("eveil.fallow.livre.eveils"), y);
        if (etat == null || etat.traces().isEmpty()) {
            canvas.text(Component.translatable("eveil.fallow.livre.aucun_eveil").getVisualOrderText(), GAUCHE_X + 2.0F, y, PETIT, PALE, 1.0F);
            y += PETITE_LIGNE + 2.0F;
        } else {
            for (EtatEveil.Trace trace : etat.traces()) {
                if (y > 86.0F) {
                    break;
                }
                y = paragraphe(canvas, nomEncre(trace.enchantement(), trace.niveau()), GAUCHE_X + 2.0F, y, 1.0F, VIOLET, 2) - 1.0F;
                Component detail = trace.eclat().isPresent()
                        ? Component.translatable("eveil.fallow.livre.trace.eclat", trace.jour(), trace.par(),
                                Component.translatable(trace.eclat().get()))
                        : Component.translatable("eveil.fallow.livre.trace", trace.jour(), trace.par());
                y = paragraphe(canvas, detail, GAUCHE_X + 4.0F, y, PETIT, PALE, 3) + 1.5F;
            }
        }
        y += 2.0F;
        ListTag faits = TableEveil.liste(contenu.data(), TableEveil.FAITS);
        y = entete(canvas, Component.translatable("eveil.fallow.livre.faits"), y);
        if (faits.isEmpty()) {
            canvas.text(Component.translatable("eveil.fallow.livre.aucun_fait").getVisualOrderText(), GAUCHE_X + 2.0F, y, PETIT, PALE, 1.0F);
        } else {
            for (int i = 0; i < faits.size() && y < PIED - 14.0F; i++) {
                CompoundTag fait = faits.getCompound(i);
                Component ligne = Component.translatable(fait.getString(TableEveil.NOM), nombre(fait.getDouble(TableEveil.FOIS)));
                y = paragraphe(canvas, ligne, GAUCHE_X + 2.0F, y, PETIT, ENCRE, 2) + 1.0F;
            }
        }
        chemin(canvas, contenu, etat);
    }

    /** Au pied de la page de gauche : où en est l'objet, en mots, et le chemin vers le prochain éveil en un trait d'encre. */
    private void chemin(OpenBook.Canvas canvas, Contenu contenu, EtatEveil etat) {
        String dit = contenu.etat();
        Component mots;
        float avancee = -1.0F;
        if (etat == null) {
            return;
        } else if (TableEveil.ETAT_PRET.equals(dit)) {
            mots = Component.translatable("eveil.fallow.livre.pret");
            avancee = 1.0F;
        } else if (TableEveil.ETAT_PLEIN.equals(dit) || TableEveil.ETAT_RIEN_A_APPRENDRE.equals(dit)) {
            mots = Component.translatable("eveil.fallow.livre.places_libres", Math.max(0, etat.places() - etat.occupees()));
        } else {
            mots = Component.translatable("eveil.fallow.tooltip.palier." + Mth.clamp(etat.palier(), 0, 3));
            avancee = contenu.data().getFloat(TableEveil.AVANCEE);
        }
        // Sur une ligne, un peu plus petit s'il le faut : le trait du chemin est juste dessous.
        float place = LARGE - 2.0F * GAUCHE_X;
        float taille = Math.min(PETIT, place / Math.max(1.0F, Minecraft.getInstance().font.width(mots)));
        canvas.text(mots.getVisualOrderText(), GAUCHE_X, PIED - 2.0F, taille, SEPIA, 1.0F);
        if (avancee >= 0.0F) {
            float large = LARGE - 2.0F * GAUCHE_X;
            canvas.rect(GAUCHE_X, PIED + 7.0F, large, 0.8F, OpenBook.MARK_LIFT, 0x70000000 | FILET);
            canvas.rect(GAUCHE_X, PIED + 6.6F, large * Mth.clamp(avancee, 0.0F, 1.0F), 1.6F, OpenBook.MARK_LIFT * 1.2F,
                    0xE0000000 | (avancee >= 1.0F ? VIOLET : SEPIA));
        }
    }

    /** La page de droite sans éclat : les voies, ou ce que la table dit de l'objet. */
    private void pageVoies(OpenBook.Canvas canvas, Contenu contenu, boolean courante) {
        String dit = contenu.etat();
        if (!TableEveil.ETAT_PRET.equals(dit)) {
            String cle = switch (dit) {
                case TableEveil.ETAT_VIDE -> "eveil.fallow.table.vide";
                case TableEveil.ETAT_RIEN -> "eveil.fallow.table.rien";
                case TableEveil.ETAT_PLEIN -> "eveil.fallow.table.plein";
                case TableEveil.ETAT_RIEN_A_APPRENDRE -> "eveil.fallow.table.rien_a_apprendre";
                default -> "eveil.fallow.table.pas_encore";
            };
            // Au milieu de la page, comme une phrase seule dans un livre.
            List<FormattedCharSequence> lignes = Minecraft.getInstance().font.split(Component.translatable(cle), (int) (LARGE - 2.0F * DROITE_X - 8.0F));
            float y = HAUT / 2.0F - lignes.size() * LIGNE / 2.0F;
            for (FormattedCharSequence ligne : lignes) {
                float x = (LARGE - Minecraft.getInstance().font.width(ligne)) / 2.0F;
                canvas.text(ligne, x, y, PALE, 1.0F);
                y += LIGNE;
            }
            fleuron(canvas, y + 4.0F);
            return;
        }
        ListTag liste = TableEveil.liste(contenu.data(), TableEveil.VOIES);
        float y = titre(canvas, Component.translatable(liste.size() > 1 ? "eveil.fallow.livre.voies" : "eveil.fallow.livre.voie"), 4.0F);
        filet(canvas, y + 1.0F);
        y += 6.0F;
        float bas = PIED - 6.0F;
        float hauteur = (bas - y) / Math.max(1, liste.size());
        for (int i = 0; i < Math.min(2, liste.size()); i++) {
            CompoundTag voie = liste.getCompound(i);
            float haut = y + i * hauteur;
            if (courante) {
                voies[i][0] = haut;
                voies[i][1] = haut + hauteur;
            }
            boolean vive = courante && i == survol;
            boolean coule = courante && i == coulante;
            if (vive || coule) {
                canvas.rect(1.0F, haut - 1.0F, LARGE - 2.0F, hauteur - 2.0F, OpenBook.MARK_LIFT * 0.5F, coule ? BANDE_VIVE : BANDE);
            }
            if (i > 0) {
                filet(canvas, haut - 2.5F);
            }
            ResourceLocation id = ResourceLocation.tryParse(voie.getString(TableEveil.ENCHANTEMENT));
            int niveau = voie.getInt(TableEveil.NIVEAU);
            Component nom = id == null ? Component.literal("?") : nomEncre(id, niveau);
            float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
            float ly = haut + 3.0F;
            for (FormattedCharSequence ligne : Minecraft.getInstance().font.split(nom, (int) (LARGE - 2.0F * DROITE_X))) {
                if (vive) {
                    canvas.brightText(ligne, DROITE_X, ly, 1.0F, VIOLET, 0xE6D9FF, 0.6F, 0.05F);
                }
                canvas.text(ligne, DROITE_X, ly, VIOLET, lettres);
                ly += LIGNE;
            }
            if (id != null && niveau > 1) {
                ly = paragraphe(canvas, Component.translatable("eveil.fallow.livre.avant", nomEncre(id, niveau - 1)), DROITE_X, ly, PETIT,
                        PALE, 1);
            }
            String pouvoir = id == null ? "" : "eveil.fallow.pouvoir." + id.getNamespace() + "." + id.getPath();
            if (I18n.exists(pouvoir)) {
                ly = paragraphe(canvas, Component.translatable(pouvoir), DROITE_X, ly + 1.0F, PETIT, ENCRE, 3);
            }
            if (voie.contains(TableEveil.NE_DE)) {
                Component ne = Component.translatable("eveil.fallow.livre.ne_de",
                        Component.translatable(voie.getString(TableEveil.NE_DE), nombre(voie.getDouble(TableEveil.FOIS))));
                paragraphe(canvas, ne.copy().withStyle(s -> s.withItalic(true)), DROITE_X, ly + 2.0F, PETIT, SEPIA, 2);
            }
        }
        paragraphe(canvas, Component.translatable("eveil.fallow.livre.choisir"), DROITE_X, PIED, PETIT, PALE, 2);
    }

    /** La page de droite avec un éclat : la bête, son récit, la force qu'il donnerait, et ce que la table en dit. */
    private void pageEclat(OpenBook.Canvas canvas, Contenu contenu, boolean courante) {
        ItemStack stack = contenu.eclat();
        Eclat eclat = stack.get(EveilModule.ECLAT.get());
        if (eclat == null) {
            return;
        }
        boolean ok = TableEveil.ECLAT_OK.equals(contenu.data().getString(TableEveil.ECLAT_ETAT));
        boolean vive = courante && pageSurvolee;
        boolean coule = courante && coulante >= 0;
        if (vive || coule) {
            canvas.rect(1.0F, 20.0F, LARGE - 2.0F, 92.0F, OpenBook.MARK_LIFT * 0.5F, coule ? BANDE_VIVE : BANDE);
        }
        canvas.item(station.level(), stack, (LARGE - 16.0F) / 2.0F, 2.0F, 16.0F);
        float y = 20.0F;
        for (FormattedCharSequence ligne : Minecraft.getInstance().font.split(stack.getHoverName().copy().withStyle(Style.EMPTY),
                (int) (LARGE - 2.0F * DROITE_X))) {
            canvas.text(ligne, (LARGE - Minecraft.getInstance().font.width(ligne)) / 2.0F, y, ENCRE, 1.0F);
            y += LIGNE;
        }
        filet(canvas, y + 1.0F);
        y += 5.0F;
        if (!eclat.recit().isEmpty()) {
            y = paragraphe(canvas, Component.translatable(eclat.recit()).copy().withStyle(s -> s.withItalic(true)), DROITE_X, y, PETIT,
                    PALE, 4) + 3.0F;
        }
        Component nom = nomEncre(eclat.enchantement(), eclat.niveau());
        float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
        for (FormattedCharSequence ligne : Minecraft.getInstance().font.split(nom, (int) (LARGE - 2.0F * DROITE_X))) {
            if (vive) {
                canvas.brightText(ligne, DROITE_X, y, 1.0F, VIOLET, 0xFF000000 | eclat.couleur(), 0.6F, 0.05F);
            }
            canvas.text(ligne, DROITE_X, y, VIOLET, lettres);
            y += LIGNE;
        }
        String pouvoir = "eveil.fallow.pouvoir." + eclat.enchantement().getNamespace() + "." + eclat.enchantement().getPath();
        if (I18n.exists(pouvoir)) {
            y = paragraphe(canvas, Component.translatable(pouvoir), DROITE_X, y + 1.0F, PETIT, ENCRE, 3);
        }
        paragraphe(canvas, Component.translatable("eveil.fallow.livre.eclat.place"), DROITE_X, y + 3.0F, PETIT, PALE, 3);
        String cle = switch (contenu.data().getString(TableEveil.ECLAT_ETAT)) {
            case TableEveil.ECLAT_OK -> "eveil.fallow.livre.eclat.verser";
            case TableEveil.ECLAT_SANS_OBJET -> "eveil.fallow.livre.eclat.sans_objet";
            case TableEveil.ECLAT_DEJA -> "eveil.fallow.eclat.deja";
            case TableEveil.ECLAT_PLEIN -> "eveil.fallow.table.plein";
            default -> "eveil.fallow.eclat.ne_prend_pas";
        };
        paragraphe(canvas, Component.translatable(cle), DROITE_X, PIED - 6.0F, PETIT, ok ? SEPIA : ROUGE, 3);
    }

    // ---- l'écriture ----

    /**
     * Un titre au milieu de la page, sur une ligne s'il y tient à peine plus petit, sinon coupé ; renvoie le bas de sa dernière
     * ligne.
     */
    private static float titre(OpenBook.Canvas canvas, Component texte, float y) {
        Font font = Minecraft.getInstance().font;
        float large = font.width(texte);
        float place = LARGE - 6.0F;
        if (large <= place / 0.82F) {
            float taille = Math.min(1.0F, place / large);
            canvas.text(texte.getVisualOrderText(), (LARGE - large * taille) / 2.0F, y, taille, SEPIA, 1.0F);
            return y + LIGNE;
        }
        for (FormattedCharSequence ligne : font.split(texte, (int) place)) {
            canvas.text(ligne, (LARGE - font.width(ligne)) / 2.0F, y, SEPIA, 1.0F);
            y += LIGNE;
        }
        return y;
    }

    private static float entete(OpenBook.Canvas canvas, Component texte, float y) {
        canvas.text(texte.getVisualOrderText(), GAUCHE_X, y, 0.85F, SEPIA, 1.0F);
        return y + LIGNE;
    }

    /** Un paragraphe coupé à la largeur de la page, à la taille {@code taille} ; renvoie le bas de sa dernière ligne. */
    private static float paragraphe(OpenBook.Canvas canvas, Component texte, float x, float y, float taille, int encre, int auPlus) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lignes = font.split(texte, (int) ((LARGE - x - 3.0F) / taille));
        float pas = taille < 1.0F ? PETITE_LIGNE : LIGNE;
        for (FormattedCharSequence ligne : lignes.subList(0, Math.min(auPlus, lignes.size()))) {
            canvas.text(ligne, x, y, taille, encre, 1.0F);
            y += pas;
        }
        return y;
    }

    /** Un filet d'encre pâlie avec un point au milieu, comme entre deux parties d'un livre. */
    private static void filet(OpenBook.Canvas canvas, float y) {
        canvas.rect(18.0F, y, LARGE - 36.0F, 0.7F, OpenBook.MARK_LIFT, 0x70000000 | FILET);
        canvas.rect(LARGE / 2.0F - 1.4F, y - 1.05F, 2.8F, 2.8F, OpenBook.MARK_LIFT, 0x70000000 | FILET);
    }

    /** Trois points sous une phrase seule. */
    private static void fleuron(OpenBook.Canvas canvas, float y) {
        for (int i = -1; i <= 1; i++) {
            canvas.rect(LARGE / 2.0F - 1.0F + i * 6.0F, y, 2.0F, 2.0F, OpenBook.MARK_LIFT, 0x90000000 | FILET);
        }
    }

    /**
     * Les runes de la table sur les pages où rien n'est écrit (les feuilles qui courent, celles d'avant) : les mots de
     * l'alphabet de la table de vanilla, pâles, comme sur le livre qui flotte au-dessus d'elle.
     */
    private void runes(OpenBook.Canvas canvas, int page) {
        if (!OpenBook.isRiffleSheet(page) && contenus.containsKey(Math.floorDiv(page, 2))) {
            return;
        }
        List<FormattedCharSequence> lignes = runes.computeIfAbsent(page, p -> {
            Font font = Minecraft.getInstance().font;
            EnchantmentNames.getInstance().initSeed(p * 31L + 7L);
            List<FormattedCharSequence> faites = new ArrayList<>();
            for (int i = 0; i < 14; i++) {
                FormattedText mots = EnchantmentNames.getInstance().getRandomName(font, LARGE - 8);
                faites.add(net.minecraft.locale.Language.getInstance().getVisualOrder(mots));
            }
            return faites;
        });
        if (runes.size() > 64) {
            runes.clear();
        }
        float y = 6.0F;
        for (FormattedCharSequence ligne : lignes) {
            canvas.text(ligne, 4.0F, y, RUNES, 0.45F);
            y += 10.5F;
        }
    }

    /** Le nom d'un enchantement à l'encre : sans le gris des infobulles, qui ne se lit pas sur le papier ; une malédiction garde son rouge. */
    private static Component nomEncre(ResourceLocation id, int niveau) {
        var level = Minecraft.getInstance().level;
        Optional<Holder.Reference<Enchantment>> holder = level == null ? Optional.empty()
                : level.registryAccess().registryOrThrow(Registries.ENCHANTMENT).getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
        if (holder.isEmpty()) {
            return Component.literal(id.toString());
        }
        boolean malediction = holder.get().is(EnchantmentTags.CURSE);
        return Enchantment.getFullname(holder.get(), niveau).copy()
                .withStyle(style -> style.withColor(malediction ? TextColor.fromRgb(ROUGE) : null));
    }

    /** Un nombre de hauts faits, entier, groupé à la façon de la langue du jeu (« 1 204 », « 1,204 »). */
    private static String nombre(double fois) {
        String code = Minecraft.getInstance().getLanguageManager().getSelected();
        Locale locale = Locale.forLanguageTag(code.replace('_', '-'));
        return NumberFormat.getIntegerInstance(locale).format(Math.max(1L, Math.round(fois)));
    }

    private static void joue(StationView view, SoundEvent sound, String node, float volume, float pitch) {
        float scaled = volume * FallowConfig.postesSoundVolume();
        if (scaled > 0.0F) {
            view.playSound(sound, node, scaled, pitch);
        }
    }
}
