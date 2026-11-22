package io.github.salvagedisc.fallow.eveil.client;

import com.mojang.blaze3d.vertex.PoseStack;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.ChatFormatting;
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
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.lwjgl.glfw.GLFW;

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
import io.github.salvagedisc.fallow.eveil.Lien;
import io.github.salvagedisc.fallow.eveil.PartDeBete;

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
    /**
     * Les marges : le papier du livre porte un cadre filé (vers 3 et 5 unités des bords, 2 et 4 du haut) ; ce qui s'écrit reste à
     * {@link #MARGE} des côtés et entre {@link #HAUT_ECRIT} et {@link #BAS_ECRIT} de haut en bas.
     */
    private static final float MARGE = 8.0F;
    private static final float HAUT_ECRIT = 7.0F;
    private static final float BAS_ECRIT = 149.0F;
    private static final float GAUCHE_X = MARGE;
    private static final float DROITE_X = MARGE;
    private static final float LIGNE = 9.0F;
    private static final float PETIT = 0.72F;
    private static final float PETITE_LIGNE = 7.0F;
    private static final float PIED = 140.0F;

    /**
     * Les encres, celles des autres livres : le texte en brun presque noir, les titres en sépia, les forces à l'encre rouge des
     * rubriques, l'à-côté en brun pâli.
     */
    private static final int ENCRE = 0x2A2016;
    private static final int SEPIA = 0x7A5A2E;
    private static final int RUBRIQUE = 0x8A2E20;
    private static final int PALE = 0x5E4D38;
    private static final int ROUGE = 0xAA0000;
    private static final int FILET = 0x7A6545;
    private static final int RUNES = 0x685E4A;
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
    private float volutesReste;
    /** La case de l'inventaire d'où l'encre vient à l'encrier en arrivant, et celle d'où elle vole (Maj + clic). */
    private int encreEnArrivant = -1;
    private int volEncre = -1;
    private ItemStack vuEncre = ItemStack.EMPTY;
    private final RandomSource hasard = RandomSource.create();
    /** Les bibliothèques qui nourrissent la table (comme celles de vanilla), en pixels de la scène, et les runes qui en partent. */
    private final List<Vector3f> bibliotheques = new ArrayList<>();
    private float runesReste;
    private float poussiereReste;
    /** Le temps que met la poussière de la brume à venir au livre, secondes (la vie de sa particule). */
    private static final float VOL_DE_POUSSIERE = 2.2F;
    /** Combien la pièce s'assombrit autour de la table, de 0 à 1 (lu par l'écran du poste, {@link TableClient}). */
    private static float obscurite;
    /** L'éclair blanc quand l'éveil s'accomplit, de 1 à 0. */
    private static float eclair;
    /** La voie sur laquelle le clic est tenu (-1 : aucune), si c'est l'éclat, et depuis combien de temps le rituel monte. */
    private int tenue = -1;
    private boolean tenueEclat;
    private float tenueT;
    /** Combien de temps tenir le clic, une fois la voie déchiffrée, secondes. */
    private static final float TENUE = 1.8F;
    /** Combien de temps une voie met à se déchiffrer sous le curseur, secondes. */
    private static final float DECHIFFRE = 1.4F;
    private float sombre;

    /** Ce qu'une double page dit : l'objet et l'éclat tels qu'ils étaient posés, et ce que le serveur en a dit. */
    private record Contenu(ItemStack objet, ItemStack eclat, CompoundTag data, float[] revele) {
        Contenu(ItemStack objet, ItemStack eclat, CompoundTag data) {
            this(objet, eclat, data, new float[2]);
        }

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
    static float obscurite() {
        return obscurite;
    }

    static void usage(BlockPos pos) {
        dernierUsage = pos;
        dernierUsageA = Util.getMillis();
    }

    @Override
    public void onSceneReady(StationView view) {
        station = view;
        tourCorps = Float.NaN;
        view.setInventoryFilter(stack -> TableEveil.objet(stack) || TableEveil.eclat(stack) || TableEveil.encre(stack));
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
        Isolement.arriver();
        // En s'asseyant, une houle grave : le monde recule.
        joue(view, SoundEvents.BEACON_POWER_SELECT, LIVRE, 0.3F, 0.5F);
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
        volEncre = -1;
        vuEncre = view.state().getItem(TableEveil.ENCRE_REF).copy();
        LocalPlayer player = Minecraft.getInstance().player;
        boolean venu = view.pos().equals(dernierUsage) && Util.getMillis() - dernierUsageA < 1500L;
        ItemStack main = player == null ? ItemStack.EMPTY : player.getMainHandItem();
        enMain = -1;
        if (venu && player.inventoryMenu.getCarried().isEmpty()) {
            // Ce qui a éveillé la table vient s'y poser : l'objet en main s'il est prêt (ou l'éclat), sinon le premier qui l'est.
            if (TableEveil.attendu(main)) {
                enMain = player.getInventory().selected;
            } else {
                for (int i = 0; i < player.getInventory().items.size() && enMain < 0; i++) {
                    if (TableEveil.attendu(player.getInventory().getItem(i)) && !TableEveil.eclat(player.getInventory().getItem(i))) {
                        enMain = i;
                    }
                }
            }
        }
        // L'encre qu'on porte vient d'elle-même à l'encrier, s'il n'est pas plein.
        encreEnArrivant = -1;
        if (venu && vuEncre.getCount() < 16) {
            for (int i = 0; i < player.getInventory().items.size() && encreEnArrivant < 0; i++) {
                if (TableEveil.encre(player.getInventory().getItem(i))) {
                    encreEnArrivant = i;
                }
            }
        }
        tenue = -1;
        tenueT = 0.0F;
        bibliotheques.clear();
        Matrix4f scene = OpenBook.sceneFromBlock(view);
        for (BlockPos decalage : net.minecraft.world.level.block.EnchantingTableBlock.BOOKSHELF_OFFSETS) {
            if (net.minecraft.world.level.block.EnchantingTableBlock.isValidBookShelf(view.level(), view.pos(), decalage)) {
                bibliotheques.add(scene.transformPosition(new Vector3f(decalage.getX() * 16.0F + 8.0F, decalage.getY() * 16.0F + 8.0F,
                        decalage.getZ() * 16.0F + 8.0F)));
            }
        }
        // Le livre s'ouvre sur les pages que celui de la table montre, comme s'il était celui-là.
        if (view.level().getBlockEntity(view.pos()) instanceof EnchantingTableBlockEntity entity) {
            book.jumpTo(OpenBook.flipTurns(entity.flip));
        }
    }

    @Override
    public void onClose(StationView view) {
        vient = false;
        obscurite = 0.0F;
        sombre = 0.0F;
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
        ItemStack encre = view.state().getItem(TableEveil.ENCRE_REF);
        if (encre.getCount() > vuEncre.getCount()) {
            volerDepuisLaBarre(view, TableEveil.ENCRE, volEncre);
            joue(view, SoundEvents.BOTTLE_FILL, TableEveil.ENCRE, 0.3F, 0.7F + 0.1F * hasard.nextFloat());
            volEncre = -1;
        } else if (encre.isEmpty() && !vuEncre.isEmpty()) {
            // La dernière fiole est partie dans les runes : l'encrier sonne creux.
            joue(view, SoundEvents.BOTTLE_EMPTY, TableEveil.ENCRE, 0.35F, 0.6F);
        }
        vuObjet = objet.copy();
        vuEclat = eclat.copy();
        vuEncre = encre.copy();
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
        } else if (encreEnArrivant >= 0 && view.isFocused() && DiegesisClient.arrival() >= 1.0F) {
            poser(view, encreEnArrivant);
            encreEnArrivant = -1;
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
        ambiance(view, dt);
        eclair = Math.max(0.0F, eclair - dt / 0.6F);
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
        // Sous le regard, la voie se déchiffre lettre à lettre ; une fois lue, elle le reste.
        int lue = survol >= 0 ? survol : pageSurvolee ? 0 : -1;
        if (ici != null && lue >= 0 && ici.revele()[lue] < 1.0F && manque(view, ici) == null) {
            float avantLue = ici.revele()[lue];
            if (avantLue <= 0.0F) {
                // Un murmure quand la table commence à dire une voie.
                joue(view, SoundEvents.SOUL_ESCAPE.value(), LIVRE, 0.4F, 0.6F + 0.2F * hasard.nextFloat());
            }
            volutes(view, ici.aEclat() ? 60.0F : (voies[lue][0] + voies[lue][1]) / 2.0F, dt);
            ici.revele()[lue] = Math.min(1.0F, avantLue + dt / DECHIFFRE);
            if ((int) (avantLue * 12.0F) != (int) (ici.revele()[lue] * 12.0F)) {
                joue(view, SoundEvents.ENCHANTMENT_TABLE_USE, LIVRE, 0.05F, 1.6F + 0.4F * hasard.nextFloat());
            }
        }
        tenir(view, dt);
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
        if (!TableEveil.objet(stack) && !TableEveil.eclat(stack) && !TableEveil.encre(stack)) {
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
        } else if (TableEveil.encre(stack)) {
            volEncre = slot;
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
        if (manque(view, ici) != null && (ici.aEclat() || TableEveil.ETAT_PRET.equals(ici.etat()))) {
            refus(view);
            view.node(sansEncre(view) ? TableEveil.ENCRE : TableEveil.OBJET).ifPresent(node -> node.shake(0.4F, 0.25F));
            return;
        }
        if (ici.aEclat()) {
            if (!TableEveil.ECLAT_OK.equals(ici.data().getString(TableEveil.ECLAT_ETAT))) {
                refus(view);
                return;
            }
            commencerTenue(view, 0, true);
            return;
        }
        if (!TableEveil.ETAT_PRET.equals(ici.etat())) {
            refus(view);
            return;
        }
        int voie = voieSous(hit.get().y());
        if (voie >= 0) {
            commencerTenue(view, voie, false);
        }
    }

    /** Le clic est tenu sur une voie : le rituel commence ; il ne s'accomplit que si l'on tient jusqu'au bout. */
    private void commencerTenue(StationView view, int voie, boolean eclat) {
        tenue = voie;
        tenueEclat = eclat;
        tenueT = 0.0F;
        joue(view, SoundEvents.BEACON_POWER_SELECT, LIVRE, 0.3F, 0.6F);
    }

    /**
     * Le bouton gauche, lu sur la fenêtre : vanilla ne suit plus le bouton tant qu'un écran est ouvert (celui du poste l'est).
     */
    private static boolean gaucheEnfoncee() {
        return GLFW.glfwGetMouseButton(Minecraft.getInstance().getWindow().getWindow(), GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
    }

    /**
     * Le rituel tenu : la voie se déchiffre si elle ne l'est pas encore, puis l'objet se soulève et tourne, des runes s'enroulent
     * autour de lui en se resserrant, le bourdon monte, la pièce s'assombrit. Relâché avant la fin, tout retombe ; tenu jusqu'au
     * bout, un éclair blanc, un son profond, et la voie coule dans l'objet.
     */
    private void tenir(StationView view, float dt) {
        if (tenue < 0) {
            return;
        }
        Contenu ici = affiche();
        boolean tenu = gaucheEnfoncee() && ici != null && view.isFocused()
                && (tenueEclat ? pageSurvolee : survol == tenue);
        if (!tenu) {
            if (tenueT > 0.2F) {
                joue(view, SoundEvents.BEACON_DEACTIVATE, LIVRE, 0.25F, 1.2F);
                view.node(TableEveil.OBJET).ifPresent(objet -> {
                    objet.moveTo(0.0F, 0.0F, 0.0F, 0.3F);
                    objet.rotateTo(0.0F, 0.0F, 0.0F, 0.3F);
                });
            }
            tenue = -1;
            tenueT = 0.0F;
            return;
        }
        float[] revele = ici.revele();
        if (revele[tenue] < 1.0F) {
            revele[tenue] = Math.min(1.0F, revele[tenue] + dt / 0.5F);
            return;
        }
        tenueT += dt;
        float k = Math.min(1.0F, tenueT / TENUE);
        view.node(TableEveil.OBJET).ifPresent(objet -> {
            objet.moveTo(0.0F, 3.5F * k, 0.0F, 0.05F);
            objet.rotateTo(0.0F, 540.0F * k * k, 0.0F, 0.05F);
            Vector3f centre = new Vector3f(objet.definition().transform().pos()).add(0.0F, 1.0F + 3.5F * k, 0.0F);
            // Un anneau de runes qui tourne et se resserre autour de l'objet.
            int nombre = 1 + (int) (k * 4.0F);
            for (int i = 0; i < nombre; i++) {
                float angle = horloge * (4.0F + 10.0F * k) + i * Mth.TWO_PI / nombre;
                float rayon = 9.0F * (1.0F - 0.7F * k);
                Vector3f depuis = new Vector3f(centre).add(Mth.cos(angle) * rayon, (hasard.nextFloat() - 0.5F) * 2.0F, Mth.sin(angle) * rayon);
                Vector3f vitesse = new Vector3f(centre).sub(depuis).mul(1.2F).add(-Mth.sin(angle) * 10.0F, 0.0F, Mth.cos(angle) * 10.0F);
                view.spawnParticles(TableClient.glyphe(hasard), depuis, vitesse, 0.3F, 1, 0xFF000000 | lueur());
            }
        });
        if (hasard.nextFloat() < dt * 6.0F) {
            view.node(LIVRE).ifPresent(node -> node.shake(0.08F + 0.25F * k, 0.15F));
        }
        if (tenueT >= TENUE) {
            accomplir(view, ici);
        }
    }

    /** Le rituel est allé au bout : la voie (ou l'éclat) s'éveille dans l'objet. */
    private void accomplir(StationView view, Contenu ici) {
        Pose pose = pose(view);
        if (tenueEclat) {
            view.sendAction(TableEveil.VERSER, new CompoundTag());
            Eclat eclat = ici.eclat().get(EveilModule.ECLAT.get());
            commencerCoulee(view, pose, LARGE / 2.0F, 60.0F, 0, eclat == null ? -1 : eclat.couleur());
        } else {
            CompoundTag how = new CompoundTag();
            how.putInt(TableEveil.VOIE, tenue);
            view.sendAction(TableEveil.CHOISIR, how);
            commencerCoulee(view, pose, LARGE / 2.0F, (voies[tenue][0] + voies[tenue][1]) / 2.0F, tenue, -1);
        }
        eclair = 1.0F;
        Isolement.relacher();
        // Une onde de runes part de l'objet, des étincelles restent, et l'encrier tressaille : une fiole est partie dans les mots.
        view.node(TableEveil.OBJET).ifPresent(objet -> {
            Vector3f centre = new Vector3f(objet.definition().transform().pos()).add(0.0F, 4.0F, 0.0F);
            for (int i = 0; i < 28; i++) {
                float angle = i * Mth.TWO_PI / 28.0F;
                Vector3f vitesse = new Vector3f(Mth.cos(angle) * 22.0F, (hasard.nextFloat() - 0.3F) * 6.0F, Mth.sin(angle) * 22.0F);
                view.spawnParticles(TableClient.glypheVolant(hasard), centre, vitesse, 0.2F, 1, 0xFF000000 | lueur());
            }
            view.spawnParticles(TableClient.ETINCELLE, centre, new Vector3f(0.0F, 2.0F, 0.0F), 3.0F, 18, 0xFFFFFFFF);
        });
        view.node(TableEveil.ENCRE).ifPresent(node -> node.shake(0.5F, 0.3F));
        joue(view, SoundEvents.BEACON_ACTIVATE, LIVRE, 0.8F, 0.5F);
        joue(view, SoundEvents.AMETHYST_BLOCK_RESONATE, LIVRE, 1.0F, 0.5F);
        tenue = -1;
        tenueT = 0.0F;
    }

    static float eclair() {
        return eclair;
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

    /** L'encre monte de l'encrier en volutes jusqu'à la ligne qu'on lit : c'est elle qui fait les lettres. */
    private void volutes(StationView view, float y, float dt) {
        volutesReste += dt * 22.0F;
        if (volutesReste < 1.0F) {
            return;
        }
        Pose pose = pose(view);
        Vector3f vers = pose.versLaScene(book.onPage(true, LARGE / 2.0F, y, pose.open, pose.right, pose.left));
        view.node(TableEveil.ENCRE).ifPresent(encrier -> {
            Vector3f depuis = new Vector3f(encrier.definition().transform().pos()).add(0.0F, 2.0F, 0.0F);
            while (volutesReste >= 1.0F) {
                volutesReste -= 1.0F;
                Vector3f ici = new Vector3f(depuis).add((hasard.nextFloat() - 0.5F) * 1.5F, hasard.nextFloat(), (hasard.nextFloat() - 0.5F) * 1.5F);
                Vector3f la = new Vector3f(vers).add((hasard.nextFloat() - 0.5F) * 30.0F * pose.scale(), (hasard.nextFloat() - 0.5F) * 3.0F, 0.0F);
                view.spawnParticles(TableClient.VOLUTE, ici, la.sub(ici).div(0.8F), 0.2F, 1, 0xFF000000 | lueur());
            }
        });
    }

    /** L'encrier est vide : la table ne peut rien écrire (le mode créatif n'use pas d'encre). */
    private static boolean sansEncre(StationView view) {
        LocalPlayer player = Minecraft.getInstance().player;
        return view.state().getItem(TableEveil.ENCRE_REF).isEmpty() && (player == null || !player.getAbilities().instabuild);
    }

    /**
     * Ce qui manque pour écrire, dit au pied de la page : l'encre d'abord, puis, pour une armure d'Archie, la part de sa bête
     * que le joueur doit porter ({@link PartDeBete}) ; {@code null} si rien ne manque.
     */
    private static Component manque(StationView view, Contenu ici) {
        if (sansEncre(view)) {
            return Component.translatable("eveil.fallow.livre.sans_encre");
        }
        LocalPlayer player = Minecraft.getInstance().player;
        if (ici != null && player != null && !PartDeBete.porte(player, ici.objet())) {
            return Component.translatable("eveil.fallow.livre.sans_part",
                    PartDeBete.de(ici.objet()).map(item -> item.getDescription()).orElse(Component.empty()));
        }
        return null;
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

    /**
     * Ce qui fait sentir que la table n'est pas un meuble : la pièce s'assombrit autour d'elle, un bourdon grave monte (plus fort
     * quand un éveil attend), et des runes quittent les bibliothèques pour venir au livre, comme au-dessus de la table de vanilla
     * mais en nombre ; pendant l'éveil, tout s'appuie.
     */
    private void ambiance(StationView view, float dt) {
        Contenu ici = contenus.get(book.spread());
        boolean attend = ici != null && (TableEveil.ETAT_PRET.equals(ici.etat())
                || TableEveil.ECLAT_OK.equals(ici.data().getString(TableEveil.ECLAT_ETAT)));
        boolean rituel = coulante >= 0 || sautA >= 0.0F;
        float vise = !vient || !view.isFocused() ? 0.0F : rituel ? 1.0F : attend ? 0.75F : 0.5F;
        sombre = Mth.approach(sombre, vise * glisse, dt / (rituel ? 0.25F : 1.2F));
        // Une autre table à portée, au repos, ne doit pas effacer l'ombre de celle où l'on est assis.
        boolean assis = vient && view.isFocused();
        if (assis || sombre > 0.0F) {
            obscurite = sombre;
        }
        float volume = 0.0F;
        if (vient && view.isFocused()) {
            volume = (rituel ? 0.7F : attend ? 0.45F : 0.25F) * glisse;
        }
        boucle(view, "bourdon", SoundEvents.BEACON_AMBIENT, volume, rituel ? 0.6F : 0.5F);
        boucle(view, "souffle", SoundEvents.PORTAL_AMBIENT, volume * 0.12F, 0.5F);
        // Le monde s'en va autour de la table : voilé quand on s'assoit, presque parti quand un éveil attend, effacé pendant le rite.
        float monte = tenue >= 0 ? Math.min(1.0F, tenueT / TENUE) : 0.0F;
        float isole = !vient || !view.isFocused() ? 0.0F : rituel ? 1.0F : tenue >= 0 ? 0.8F + 0.2F * monte : attend ? 0.75F : 0.6F;
        if (assis) {
            // Seule la table où l'on est assis mène l'isolement ; partie, il retombe de lui-même en quelques secondes.
            Isolement.viser(isole, Vec3.atCenterOf(view.pos()), monte);
        }
        poussiere(view, dt, isole * glisse);
        if (bibliotheques.isEmpty() || glisse < 1.0F) {
            return;
        }
        // Deux runes par seconde et par bibliothèque, trois fois plus quand un éveil attend, un flot pendant l'éveil.
        runesReste += dt * bibliotheques.size() * (rituel ? 8.0F : attend ? 2.0F : 0.6F);
        Vector3fc livre = view.node(LIVRE).map(n -> n.definition().transform().pos()).orElse(new Vector3f(8.0F, 18.5F, 9.5F));
        while (runesReste >= 1.0F) {
            runesReste -= 1.0F;
            Vector3f depuis = new Vector3f(bibliotheques.get(hasard.nextInt(bibliotheques.size())))
                    .add((hasard.nextFloat() - 0.5F) * 12.0F, (hasard.nextFloat() - 0.2F) * 12.0F, (hasard.nextFloat() - 0.5F) * 12.0F);
            Vector3f vers = new Vector3f(livre).add((hasard.nextFloat() - 0.5F) * 6.0F, (hasard.nextFloat() - 0.5F) * 3.0F, 0.0F);
            view.spawnParticles(TableClient.glyphe(hasard), depuis, new Vector3f(vers).sub(depuis).div(VOL_DE_RUNE), 0.5F, 1,
                    0xFF000000 | lueur());
        }
    }

    /** Une poussière pâle sort de la brume de tous côtés et vient au livre, comme aspirée par la table. */
    private void poussiere(StationView view, float dt, float k) {
        poussiereReste += dt * 7.0F * k;
        if (poussiereReste < 1.0F) {
            return;
        }
        Vector3fc livre = view.node(LIVRE).map(n -> n.definition().transform().pos()).orElse(new Vector3f(8.0F, 18.5F, 9.5F));
        while (poussiereReste >= 1.0F) {
            poussiereReste -= 1.0F;
            float angle = hasard.nextFloat() * Mth.TWO_PI;
            float rayon = 56.0F + hasard.nextFloat() * 32.0F;
            Vector3f depuis = new Vector3f(livre).add(Mth.cos(angle) * rayon, (hasard.nextFloat() - 0.35F) * 48.0F, Mth.sin(angle) * rayon);
            Vector3f vers = new Vector3f(livre).add((hasard.nextFloat() - 0.5F) * 4.0F, (hasard.nextFloat() - 0.5F) * 4.0F, 0.0F);
            view.spawnParticles(TableClient.POUSSIERE, depuis, new Vector3f(vers).sub(depuis).div(VOL_DE_POUSSIERE), 0.0F, 1, 0xFFFFFFFF);
        }
    }

    private static void boucle(StationView view, String nom, SoundEvent sound, float volume, float pitch) {
        view.loopSound(nom, sound, volume * FallowConfig.postesSoundVolume(), pitch);
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
        canvas.item(station.level(), objet, GAUCHE_X, HAUT_ECRIT, 12.0F);
        float nomX = GAUCHE_X + 15.0F;
        List<FormattedCharSequence> nom = font.split(objet.getHoverName().copy().withStyle(Style.EMPTY), (int) (LARGE - nomX - MARGE));
        float y = nom.size() > 1 ? HAUT_ECRIT : HAUT_ECRIT + 3.0F;
        for (FormattedCharSequence ligne : nom.subList(0, Math.min(2, nom.size()))) {
            canvas.text(ligne, nomX, y, ENCRE, 1.0F);
            y += LIGNE;
        }
        EtatEveil etat = objet.get(EveilModule.ETAT.get());
        y = Math.max(y, HAUT_ECRIT + 15.0F);
        if (etat != null) {
            // Les places en signes, pleins pour les éveils portés, creux pour ceux qui restent : pas de compte.
            StringBuilder signes = new StringBuilder();
            for (int i = 0; i < etat.places(); i++) {
                signes.append(i < etat.occupees() ? '\u25C6' : '\u25C7').append(' ');
            }
            String places = signes.toString().trim();
            canvas.text(Component.literal(places).getVisualOrderText(), nomX, y - 1.0F, RUBRIQUE, 1.0F);
            // Et le mot de son attachement, à la suite : négligé, entretenu, chéri.
            int lien = Lien.de(objet);
            if (lien != Lien.AUCUN) {
                canvas.text(Component.translatable("eveil.fallow.lien." + Lien.cle(lien)).withStyle(ChatFormatting.ITALIC)
                        .getVisualOrderText(), nomX + font.width(places) + 4.0F, y, PETIT, lien == Lien.NEGLIGE ? PALE : SEPIA, 1.0F);
            }
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
                y = paragraphe(canvas, nomEncre(trace.enchantement(), trace.niveau()), GAUCHE_X + 2.0F, y, 1.0F, RUBRIQUE, 2) - 1.0F;
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
                Component ligne = majuscule(Component.translatable(fait.getString(TableEveil.NOM), quantite(fait.getDouble(TableEveil.FOIS))));
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
            mots = Component.translatable(etat.occupees() >= etat.places() ? "eveil.fallow.livre.comble" : "eveil.fallow.livre.repos");
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
            canvas.rect(GAUCHE_X, PIED + 7.2F, large, 0.5F, OpenBook.MARK_LIFT, 0x60000000 | FILET);
            canvas.rect(GAUCHE_X, PIED + 6.95F, large * Mth.clamp(avancee, 0.0F, 1.0F), 1.0F, OpenBook.MARK_LIFT * 1.2F,
                    0xE0000000 | (avancee >= 1.0F ? RUBRIQUE : SEPIA));
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
        float y = titre(canvas, Component.translatable(liste.size() > 1 ? "eveil.fallow.livre.voies" : "eveil.fallow.livre.voie"), HAUT_ECRIT);
        filet(canvas, y + 1.0F);
        y += 6.0F;
        Component manque = station == null ? null : manque(station, contenu);
        boolean sec = manque != null;
        Component piedTexte = sec ? manque : Component.translatable("eveil.fallow.livre.choisir");
        float bas = PIED + PETITE_LIGNE - lignesDuPied(piedTexte) * PETITE_LIGNE - 4.0F;
        int n = Math.min(2, liste.size());
        // Chaque voie est d'abord mesurée sur ses mots lus : sa bande la couvre toute, et rien ne bouge pendant qu'on la déchiffre.
        List<List<Ligne>> textes = new ArrayList<>();
        float pris = 0.0F;
        for (int i = 0; i < n; i++) {
            List<Ligne> texte = texteDeVoie(liste.getCompound(i), contenu.revele()[i]);
            textes.add(texte);
            pris += hauteur(texte) + 6.0F;
        }
        float marge = Math.max(0.0F, (bas - y - pris) / Math.max(1, n));
        float haut = y;
        for (int i = 0; i < n; i++) {
            List<Ligne> texte = textes.get(i);
            float hauteur = hauteur(texte) + 6.0F + marge;
            if (courante) {
                voies[i][0] = haut;
                voies[i][1] = haut + hauteur;
            }
            boolean vive = courante && i == survol;
            boolean coule = courante && i == coulante;
            if (i > 0) {
                filet(canvas, haut - 2.5F);
            }
            float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
            float ly = haut + 3.0F + marge / 2.0F;
            float tenu = courante && i == tenue && !tenueEclat ? Math.min(1.0F, tenueT / TENUE) : 0.0F;
            ecrireLignes(canvas, texte, ly, vive, courante ? 0.16F + 0.12F * Mth.sin(horloge * 2.2F + i * 1.7F) : 0.0F, 0xE6D9FF, lettres,
                    tenu);
            haut += hauteur;
        }
        pied(canvas, piedTexte, sec ? ROUGE : PALE);
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
        float revele = contenu.revele()[0];
        canvas.item(station.level(), stack, (LARGE - 14.0F) / 2.0F, HAUT_ECRIT, 14.0F);
        float y = HAUT_ECRIT + 16.0F;
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
        // La force de l'éclat, en runes jusqu'à ce qu'on la lise ; la bande couvre exactement ce qui se lit.
        List<Ligne> force = new ArrayList<>();
        ajouter(force, nomEncre(eclat.enchantement(), eclat.niveau()), revele, 1.0F, RUBRIQUE, Style.EMPTY, 2, 0.0F);
        String pouvoir = "eveil.fallow.pouvoir." + eclat.enchantement().getNamespace() + "." + eclat.enchantement().getPath();
        if (I18n.exists(pouvoir)) {
            ajouter(force, Component.translatable(pouvoir), revele, PETIT, ENCRE, Style.EMPTY, 3, 1.0F);
        }
        float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
        float tenu = courante && tenueEclat && tenue >= 0 ? Math.min(1.0F, tenueT / TENUE) : 0.0F;
        y = ecrireLignes(canvas, force, y, vive, courante && ok ? 0.16F + 0.12F * Mth.sin(horloge * 2.2F) : 0.0F,
                0xFF000000 | eclat.couleur(), lettres, tenu);
        paragraphe(canvas, Component.translatable("eveil.fallow.livre.eclat.place"), DROITE_X, y + 3.0F, PETIT, PALE, 3);
        Component manque = station == null ? null : manque(station, contenu);
        String cle = switch (contenu.data().getString(TableEveil.ECLAT_ETAT)) {
            case TableEveil.ECLAT_OK -> "eveil.fallow.livre.eclat.verser";
            case TableEveil.ECLAT_SANS_OBJET -> "eveil.fallow.livre.eclat.sans_objet";
            case TableEveil.ECLAT_DEJA -> "eveil.fallow.eclat.deja";
            case TableEveil.ECLAT_PLEIN -> "eveil.fallow.table.plein";
            default -> "eveil.fallow.eclat.ne_prend_pas";
        };
        boolean manqueIci = ok && manque != null;
        pied(canvas, manqueIci ? manque : Component.translatable(cle), ok && !manqueIci ? SEPIA : ROUGE);
    }

    // ---- l'écriture ----

    /**
     * Un titre au milieu de la page, sur une ligne s'il y tient à peine plus petit, sinon coupé ; renvoie le bas de sa dernière
     * ligne.
     */
    private static float titre(OpenBook.Canvas canvas, Component texte, float y) {
        Font font = Minecraft.getInstance().font;
        float large = font.width(texte);
        float place = LARGE - 2.0F * MARGE;
        if (large <= place / 0.75F) {
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
    /**
     * Les lignes d'une voie (ou de la force d'un éclat) : son nom respire d'une lueur faible ({@code souffle}) et s'allume sous le
     * regard ({@code vive}) ; pendant le clic maintenu, un trait d'encre violette se tire sous le nom, de gauche à droite
     * ({@code tenu}, de 0 à 1). Pas de fond : rien que l'encre et la lumière. Renvoie le bas de la dernière ligne.
     */
    private static float ecrireLignes(OpenBook.Canvas canvas, List<Ligne> texte, float y, boolean vive, float souffle, int lueur,
            float lettres, float tenu) {
        float sousLeNom = -1.0F;
        for (Ligne ligne : texte) {
            y += ligne.avant();
            boolean nom = ligne.taille() == 1.0F && ligne.encre() == RUBRIQUE;
            if (nom) {
                canvas.brightText(ligne.texte(), DROITE_X, y, 1.0F, RUBRIQUE, lueur, vive ? 0.75F : souffle, 0.05F);
            }
            canvas.text(ligne.texte(), DROITE_X, y, ligne.taille(), vive && !nom ? sombre(ligne.encre()) : ligne.encre(),
                    nom ? lettres : 1.0F);
            y += ligne.taille() < 1.0F ? PETITE_LIGNE : LIGNE;
            if (nom) {
                sousLeNom = y - 1.0F;
            }
        }
        if (tenu > 0.0F && sousLeNom >= 0.0F) {
            float large = LARGE - DROITE_X - MARGE;
            canvas.rect(DROITE_X, sousLeNom, large * tenu, 1.0F, OpenBook.MARK_LIFT * 1.2F, 0xE0000000 | RUBRIQUE);
        }
        return y;
    }

    /** Une encre un peu plus soutenue : sous le regard, ce que la voie dit se lit mieux. */
    private static int sombre(int encre) {
        int r = (encre >> 16 & 0xFF) * 3 / 4;
        int g = (encre >> 8 & 0xFF) * 3 / 4;
        int b = (encre & 0xFF) * 3 / 4;
        return r << 16 | g << 8 | b;
    }

    private static int lignesDuPied(Component texte) {
        return Minecraft.getInstance().font.split(texte, (int) ((LARGE - DROITE_X - MARGE) / PETIT)).size();
    }

    /** La phrase du pied de la page de droite, entière : elle monte d'autant de lignes qu'il lui en faut. */
    private static void pied(OpenBook.Canvas canvas, Component texte, int encre) {
        List<FormattedCharSequence> lignes = Minecraft.getInstance().font.split(texte, (int) ((LARGE - DROITE_X - MARGE) / PETIT));
        float y = PIED + PETITE_LIGNE - lignes.size() * PETITE_LIGNE;
        for (FormattedCharSequence ligne : lignes) {
            canvas.text(ligne, DROITE_X, y, PETIT, encre, 1.0F);
            y += PETITE_LIGNE;
        }
    }

    /** Une ligne écrite sur une page : son texte, sa taille, son encre, et l'espace laissé au-dessus d'elle. */
    private record Ligne(FormattedCharSequence texte, float taille, int encre, float avant) {
    }

    /** Ce qu'une voie écrit : son nom, ce qu'elle portait avant, ce qu'elle fait, et l'acte qui l'a éveillée. */
    private static List<Ligne> texteDeVoie(CompoundTag voie, float revele) {
        List<Ligne> texte = new ArrayList<>();
        ResourceLocation id = ResourceLocation.tryParse(voie.getString(TableEveil.ENCHANTEMENT));
        int niveau = voie.getInt(TableEveil.NIVEAU);
        ajouter(texte, id == null ? Component.literal("?") : nomEncre(id, niveau), revele, 1.0F, RUBRIQUE, Style.EMPTY, 2, 0.0F);
        if (id != null && niveau > 1) {
            ajouter(texte, Component.translatable("eveil.fallow.livre.avant", nomEncre(id, niveau - 1)), revele, PETIT, PALE,
                    Style.EMPTY, 1, 0.0F);
        }
        String pouvoir = id == null ? "" : "eveil.fallow.pouvoir." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(pouvoir)) {
            ajouter(texte, Component.translatable(pouvoir), revele, PETIT, ENCRE, Style.EMPTY, 3, 1.0F);
        }
        if (voie.contains(TableEveil.NE_DE)) {
            Component ne = Component.translatable("eveil.fallow.livre.ne_de",
                    Component.translatable(voie.getString(TableEveil.NE_DE), quantite(voie.getDouble(TableEveil.FOIS))));
            ajouter(texte, ne, revele, PETIT, SEPIA, Style.EMPTY.withItalic(true), 3, 2.0F);
        }
        return texte;
    }

    private static void ajouter(List<Ligne> texte, Component mots, float revele, float taille, int encre, Style style, int auPlus,
            float avant) {
        List<FormattedCharSequence> lignes = lignes(mots, revele, (LARGE - DROITE_X - MARGE) / taille, style);
        for (int i = 0; i < Math.min(auPlus, lignes.size()); i++) {
            texte.add(new Ligne(lignes.get(i), taille, encre, i == 0 ? avant : 0.0F));
        }
    }

    private static float hauteur(List<Ligne> texte) {
        float h = 0.0F;
        for (Ligne ligne : texte) {
            h += ligne.avant() + (ligne.taille() < 1.0F ? PETITE_LIGNE : LIGNE);
        }
        return h;
    }

    /**
     * Le texte coupé en lignes sur ses mots lus (en clair), puis chaque ligne écrite en runes au-delà de ce qui est déchiffré :
     * les lignes restent où elles sont pendant qu'on lit. Une ligne en runes trop large est resserrée à sa place.
     */
    private static List<FormattedCharSequence> lignes(Component texte, float revele, float largeur, Style style) {
        Font font = Minecraft.getInstance().font;
        String mots = texte.getString();
        int clair = Mth.clamp(Math.round(revele * mots.length()), 0, mots.length());
        List<FormattedCharSequence> sortie = new ArrayList<>();
        int debut = 0;
        for (FormattedText coupe : font.getSplitter().splitLines(mots, (int) largeur, style)) {
            String ligne = coupe.getString();
            int a = mots.indexOf(ligne, debut);
            a = a < 0 ? debut : a;
            int lu = Mth.clamp(clair - a, 0, ligne.length());
            net.minecraft.network.chat.MutableComponent ecrite = Component.literal(ligne.substring(0, lu)).withStyle(style);
            if (lu < ligne.length()) {
                ecrite.append(Component.literal(ligne.substring(lu)).withStyle(style.withFont(Minecraft.ALT_FONT)));
            }
            sortie.add(ecrite.getVisualOrderText());
            debut = a + ligne.length();
        }
        return sortie;
    }

    private static float paragraphe(OpenBook.Canvas canvas, Component texte, float x, float y, float taille, int encre, int auPlus) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lignes = font.split(texte, (int) ((LARGE - x - MARGE) / taille));
        float pas = taille < 1.0F ? PETITE_LIGNE : LIGNE;
        for (FormattedCharSequence ligne : lignes.subList(0, Math.min(auPlus, lignes.size()))) {
            canvas.text(ligne, x, y, taille, encre, 1.0F);
            y += pas;
        }
        return y;
    }

    /** Le filet des livres, un losange au milieu, comme entre deux parties d'un livre. */
    private static void filet(OpenBook.Canvas canvas, float y) {
        canvas.rule(18.0F, y + 0.35F, LARGE - 36.0F);
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
            for (int i = 0; i < 13; i++) {
                FormattedText mots = EnchantmentNames.getInstance().getRandomName(font, (int) (LARGE - 2.0F * MARGE));
                faites.add(net.minecraft.locale.Language.getInstance().getVisualOrder(mots));
            }
            return faites;
        });
        if (runes.size() > 64) {
            runes.clear();
        }
        float y = HAUT_ECRIT;
        for (FormattedCharSequence ligne : lignes) {
            canvas.text(ligne, MARGE, y, RUNES, 0.45F);
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

    /**
     * Combien de fois, sans chiffre : le livre ne compte pas, il se souvient (« quelques », « des dizaines de », « des centaines
     * de », « des milliers de »), en minuscules ; {@link #majuscule} pour un début de phrase.
     */
    private static Component quantite(double fois) {
        String ordre = fois < 10.0 ? "quelques" : fois < 100.0 ? "dizaines" : fois < 1000.0 ? "centaines" : "milliers";
        return Component.translatable("eveil.fallow.livre.quantite." + ordre);
    }

    private static Component majuscule(Component texte) {
        String mots = texte.getString();
        return mots.isEmpty() ? texte : Component.literal(Character.toUpperCase(mots.charAt(0)) + mots.substring(1));
    }

    private static void joue(StationView view, SoundEvent sound, String node, float volume, float pitch) {
        float scaled = volume * FallowConfig.postesSoundVolume();
        if (scaled > 0.0F) {
            view.playSound(sound, node, scaled, pitch);
        }
    }
}
