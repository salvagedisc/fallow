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
import net.minecraft.world.level.Level;
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
     * Les marges. Le papier écrit commence après la courbe de la gouttière (près d'un pixel du modèle, 17 unités) : du côté du
     * pli, {@link #DEDANS} suffit, la courbe fait le reste de la marge ; du côté de la tranche, {@link #DEHORS}. Ainsi ce qui est
     * centré l'est sur la page qu'on voit, non sur sa partie plate. {@link #bordG} et {@link #bordD} sont ceux de la page qu'on
     * peint ({@link #peindre}) ; de haut en bas, ce qui s'écrit reste entre {@link #HAUT_ECRIT} et le pied.
     */
    private static final float DEDANS = 1.0F;
    private static final float DEHORS = 12.0F;
    private static float bordG = DEHORS;
    private static float bordD = LARGE - DEDANS;
    private static final float HAUT_ECRIT = 7.0F;
    private static final float LIGNE = 9.0F;
    private static final float PETIT = 0.72F;
    private static final float PETITE_LIGNE = 7.0F;
    private static final float PIED = 140.0F;
    /** Le médaillon où un objet est en majesté, en haut de page : son cercle d'or, et ce qui l'entoure jusqu'à ses pointes. */
    private static final float MEDAILLON = 11.0F;
    private static final float EMBLEME = 23.0F;
    /** D'où partent les lignes sous un intitulé, et l'écart entre deux places d'éveil. */
    private static final float RETRAIT = 6.0F;
    private static final float PAS_DE_PLACE = 7.0F;
    /** Les losanges du chemin vers le prochain éveil. */
    private static final int PAS_DU_CHEMIN = 9;
    /** La fourche : la tige dans la marge, les cartouches à sa droite, leur texte, leur intérieur et ce qui les sépare. */
    private static final float TIGE = 3.0F;
    private static final float CARTOUCHE = 8.0F;
    /** L'air laissé sous les cartouches, au-dessus de la phrase du pied (la racine de la fourche finit au bas du dernier). */
    private static final float RACINE = 2.0F;
    /** Le temps entre deux reflets sur un filet d'or, secondes. */
    private static final float REFLET = 5.0F;
    private static final float CARTOUCHE_DEDANS = 3.0F;
    private static final float ENTRE_VOIES = 3.0F;
    private static final float VOIE = 4.0F;
    /** Le milieu de la page de droite, celle des voies, d'où l'encre part. */
    private static final float MILIEU_DROITE = (DEDANS + LARGE - DEHORS) / 2.0F;

    /** Les bords de ce qui s'écrit sur une page : la page de droite a le pli à sa gauche. */
    private static void cadrer(boolean droite) {
        bordG = droite ? DEDANS : DEHORS;
        bordD = droite ? LARGE - DEHORS : LARGE - DEDANS;
    }

    private static float milieu() {
        return (bordG + bordD) / 2.0F;
    }

    private static float largeur() {
        return bordD - bordG;
    }

    private static float corpsX() {
        return bordG + RETRAIT;
    }

    private static float cartoucheX() {
        return bordG + CARTOUCHE;
    }

    private static float cartoucheLarge() {
        return bordD - cartoucheX();
    }

    private static float voieX() {
        return cartoucheX() + VOIE;
    }

    private static float voieLarge() {
        return bordD - 3.0F - voieX();
    }

    /**
     * Les encres, celles des autres livres : le texte et les titres en brun d'encre (jamais de noir), l'à-côté en encre douce,
     * les forces, les intitulés et ce qui se lit à l'encre rouge des rubriques ; ce qui manque, d'un rouge plus sombre.
     */
    private static final int ENCRE = 0x3B2A1A;
    private static final int SEPIA = 0x8A6B45;
    private static final int RUBRIQUE = 0x8F2A22;
    private static final int PALE = 0x6E5A40;
    private static final int PALE_FILET = 0xC9B184;
    private static final int ROUGE = 0x8E1C14;
    private static final int RUNES = 0x9C8462;
    /** Les runes de la couronne autour de l'objet, éteintes : une sépia assez soutenue pour se lire sur le vélin. */
    private static final int RUNES_DE_COURONNE = 0x7A5E3C;
    /**
     * L'or du livre : ses dorures (cercles, filets, coins des cartouches, nœuds de la fourche), d'un or bruni qui se lit sur le
     * vélin, et la lueur qu'elles jettent, à pleine lumière, quand la pièce s'assombrit autour de la table.
     */
    private static final int OR = 0xB0802E;
    private static final int OR_CLAIR = 0xE2B95E;
    private static final int OR_VIF = 0xC98A1E;
    private static final int LUEUR_D_OR = 0xFFE3A0;
    /** La lueur du nom d'une voie : l'or pâle d'une dorure, sur l'encre rouge. */
    private static final int LUEUR_DE_RUBRIQUE = 0xF2D27A;
    /** Les couleurs de l'encre qui monte de l'encrier et coule dans l'objet : les rouges de la rubrique et l'or. */
    private static final int[] LUEURS = {0xC8463A, 0xE07A4E, 0xF2D27A, 0xA3322A, 0xFFE7B0};
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
    /** L'horloge du livre, pour ce que la page anime d'elle-même (le médaillon, les dorures, les paillettes). */
    private static float battement;
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
            commencerCoulee(view, pose, MILIEU_DROITE, 60.0F, 0, eclat == null ? -1 : eclat.couleur());
        } else {
            CompoundTag how = new CompoundTag();
            how.putInt(TableEveil.VOIE, tenue);
            view.sendAction(TableEveil.CHOISIR, how);
            commencerCoulee(view, pose, MILIEU_DROITE, (voies[tenue][0] + voies[tenue][1]) / 2.0F, tenue, -1);
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
        Vector3f vers = pose.versLaScene(book.onPage(true, MILIEU_DROITE, y, pose.open, pose.right, pose.left));
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
            commencerCoulee(view, pose, MILIEU_DROITE, HAUT / 2.0F, 0, -1);
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
        cadrer(droite);
        battement = horloge;
        paillettes(canvas, page);
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

    /**
     * La page de gauche : l'objet en majesté dans un losange de rubrique, son nom, ses places d'éveil en losanges (pleins pour ce
     * qu'il porte), son attachement ; puis ses éveils et ses plus grands hauts faits ; au pied, son chemin vers le prochain éveil.
     */
    private void pageVie(OpenBook.Canvas canvas, Contenu contenu) {
        Font font = Minecraft.getInstance().font;
        ItemStack objet = contenu.objet();
        if (objet.isEmpty()) {
            float y = titre(canvas, Component.translatable("eveil.fallow.livre.titre"), 10.0F);
            filet(canvas, y + 2.0F);
            y = paragraphe(canvas, Component.translatable("eveil.fallow.livre.invite"), bordG, y + 8.0F, 1.0F, ENCRE, 12);
            paragraphe(canvas, Component.translatable("eveil.fallow.livre.invite_eclat"), bordG, y + 6.0F, PETIT, PALE, 6);
            return;
        }
        float milieu = milieu();
        enMajeste(canvas, objet, HAUT_ECRIT + EMBLEME);
        float y = HAUT_ECRIT + 2.0F * EMBLEME + 3.0F;
        List<FormattedCharSequence> nom = font.split(objet.getHoverName().copy().withStyle(Style.EMPTY), (int) largeur());
        for (FormattedCharSequence ligne : nom.subList(0, Math.min(2, nom.size()))) {
            canvas.text(ligne, milieu - font.width(ligne) / 2.0F, y, ENCRE, 1.0F);
            y += LIGNE;
        }
        EtatEveil etat = objet.get(EveilModule.ETAT.get());
        if (etat != null) {
            // Les places en losanges, pleins pour les éveils portés, creux pour ceux qui restent : pas de compte.
            int n = etat.places();
            float x = milieu - (n - 1) * PAS_DE_PLACE / 2.0F;
            for (int i = 0; i < n; i++) {
                losange(canvas, x + i * PAS_DE_PLACE, y + 3.0F, 2, RUBRIQUE, i < etat.occupees());
            }
            y += 8.0F;
            // Et le mot de son attachement, dessous : négligé, entretenu, chéri.
            int lien = Lien.de(objet);
            if (lien != Lien.AUCUN) {
                FormattedCharSequence mot = Component.translatable("eveil.fallow.lien." + Lien.cle(lien)).withStyle(ChatFormatting.ITALIC)
                        .getVisualOrderText();
                canvas.text(mot, milieu - font.width(mot) * PETIT / 2.0F, y, PETIT, lien == Lien.NEGLIGE ? PALE : SEPIA, 1.0F);
                y += PETITE_LIGNE;
            }
        }
        y += 3.0F;
        filet(canvas, y);
        y += 6.0F;
        // Ce qui s'écrit s'arrête au-dessus du chemin ; les éveils laissent toujours la place d'un haut fait sous leur intitulé.
        float limite = PIED - 8.0F;
        float pourLesFaits = LIGNE + 2.0F * PETITE_LIGNE;
        y = entete(canvas, Component.translatable("eveil.fallow.livre.eveils"), y);
        if (etat == null || etat.traces().isEmpty()) {
            canvas.text(Component.translatable("eveil.fallow.livre.aucun_eveil").getVisualOrderText(), corpsX(), y, PETIT, PALE, 1.0F);
            y += PETITE_LIGNE + 3.0F;
        } else {
            for (EtatEveil.Trace trace : etat.traces()) {
                Component nomTrace = nomEncre(trace.enchantement(), trace.niveau());
                Component detail = trace.eclat().isPresent()
                        ? Component.translatable("eveil.fallow.livre.trace.eclat", trace.jour(), trace.par(),
                                Component.translatable(trace.eclat().get()))
                        : Component.translatable("eveil.fallow.livre.trace", trace.jour(), trace.par());
                float haute = hauteurDe(nomTrace, corpsX(), 1.0F, 2) - 1.0F + hauteurDe(detail, corpsX(), PETIT, 3) + 2.0F;
                if (y + haute > limite - pourLesFaits) {
                    break;
                }
                y = paragraphe(canvas, nomTrace, corpsX(), y, 1.0F, RUBRIQUE, 2) - 1.0F;
                y = paragraphe(canvas, detail, corpsX(), y, PETIT, PALE, 3) + 2.0F;
            }
            y += 1.0F;
        }
        ListTag faits = TableEveil.liste(contenu.data(), TableEveil.FAITS);
        y = entete(canvas, Component.translatable("eveil.fallow.livre.faits"), y);
        if (faits.isEmpty()) {
            canvas.text(Component.translatable("eveil.fallow.livre.aucun_fait").getVisualOrderText(), corpsX(), y, PETIT, PALE, 1.0F);
        } else {
            for (int i = 0; i < faits.size(); i++) {
                CompoundTag fait = faits.getCompound(i);
                Component ligne = majuscule(Component.translatable(fait.getString(TableEveil.NOM), quantite(fait.getDouble(TableEveil.FOIS))));
                // Le premier toujours (sur une ligne s'il le faut), les suivants s'il reste la place.
                int auPlus = y + hauteurDe(ligne, corpsX() + 5.0F, PETIT, 2) <= limite ? 2 : 1;
                if (i > 0 && y + hauteurDe(ligne, corpsX() + 5.0F, PETIT, auPlus) > limite) {
                    break;
                }
                // Chaque haut fait marqué d'un point de rubrique, comme un article de compte.
                losange(canvas, corpsX() + 1.5F, y + 2.5F, 1, RUBRIQUE, true);
                y = paragraphe(canvas, ligne, corpsX() + 5.0F, y, PETIT, ENCRE, auPlus) + 1.5F;
            }
        }
        chemin(canvas, contenu, etat);
    }

    /**
     * Au pied de la page de gauche : où en est l'objet, en mots au milieu, et dessous son chemin vers le prochain éveil, une
     * rangée de neuf petits losanges que l'encre remplit ; prêt, les neuf sont rouges et deux traits de rubrique encadrent le mot.
     */
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
        Font font = Minecraft.getInstance().font;
        float milieu = milieu();
        boolean pret = avancee >= 1.0F;
        float taille = Math.min(PETIT, (largeur() - 20.0F) / Math.max(1.0F, font.width(mots)));
        float large = font.width(mots) * taille;
        float y = PIED - 4.0F;
        canvas.text(mots.getVisualOrderText(), milieu - large / 2.0F, y, taille, pret ? RUBRIQUE : SEPIA, 1.0F);
        if (pret) {
            // Prêt : le mot entre deux traits de rubrique, qui respirent.
            int a = Math.round(150.0F + 100.0F * (0.5F + 0.5F * Mth.sin(horloge * 3.0F)));
            float ty = y + 2.5F;
            canvas.rect(milieu - large / 2.0F - 11.0F, ty, 8.0F, 1.0F, OpenBook.MARK_LIFT, a << 24 | RUBRIQUE);
            canvas.rect(milieu + large / 2.0F + 3.0F, ty, 8.0F, 1.0F, OpenBook.MARK_LIFT, a << 24 | RUBRIQUE);
        }
        if (avancee >= 0.0F) {
            int pleins = Mth.floor(Mth.clamp(avancee, 0.0F, 1.0F) * PAS_DU_CHEMIN);
            float x = milieu - (PAS_DU_CHEMIN - 1) * 8.0F / 2.0F;
            x = Mth.floor(x) + 0.5F;
            for (int i = 0; i < PAS_DU_CHEMIN; i++) {
                losange(canvas, x + i * 8.0F, PIED + 8.0F, 1, i < pleins ? (pret ? RUBRIQUE : OR) : PALE_FILET, i < pleins);
                if (pret) {
                    // Prêt : chaque losange luit d'or, l'un après l'autre, comme une flamme qui court.
                    float onde = 0.5F + 0.5F * Mth.sin(battement * 3.0F - i * 0.7F);
                    canvas.brightRect(x + i * 8.0F - 0.5F, PIED + 7.5F, 1.0F, 1.0F, LUEUR_D_OR, (0.35F + 0.5F * onde) * canvas.ink(),
                            OpenBook.MARK_LIFT * 1.1F);
                }
            }
        }
    }

    /**
     * La page de droite sans éclat : la fourche. Les voies sont deux cartouches l'une sous l'autre ; une tige d'encre monte le
     * long de la marge, depuis le pied, et se partage vers chacune par un nœud en losange. La voie qu'on lit s'encre de rouge,
     * sa branche et la tige avec elle ; tenue, son cadre se remplit de rubrique, du coin haut gauche, dans le sens des aiguilles.
     */
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
            List<FormattedCharSequence> lignes = Minecraft.getInstance().font.split(Component.translatable(cle), (int) (largeur() - 8.0F));
            float y = HAUT / 2.0F - lignes.size() * LIGNE / 2.0F;
            for (FormattedCharSequence ligne : lignes) {
                float x = milieu() - Minecraft.getInstance().font.width(ligne) / 2.0F;
                canvas.text(ligne, x, y, PALE, 1.0F);
                y += LIGNE;
            }
            fleuron(canvas, y + 4.0F);
            return;
        }
        ListTag liste = TableEveil.liste(contenu.data(), TableEveil.VOIES);
        // Le titre seul : la fourche fait l'ornement de la page, et les voies ont besoin de toute sa hauteur.
        float y = titre(canvas, Component.translatable(liste.size() > 1 ? "eveil.fallow.livre.voies" : "eveil.fallow.livre.voie"), HAUT_ECRIT);
        y += 3.0F;
        Component manque = station == null ? null : manque(station, contenu);
        boolean sec = manque != null;
        Component piedTexte = sec ? manque : Component.translatable("eveil.fallow.livre.choisir");
        float bas = PIED + PETITE_LIGNE - lignesDuPied(piedTexte) * PETITE_LIGNE - 2.0F;
        // Sous les cartouches, un peu d'air au-dessus de la phrase du pied.
        float fond = bas - RACINE;
        int n = Math.min(2, liste.size());
        // Chaque voie est d'abord mesurée sur ses mots lus : son cartouche la couvre toute, et rien ne bouge pendant qu'on la déchiffre.
        // Trop long pour la page, une voie se resserre : d'abord sans ce qu'il portait avant, puis sans l'acte qui l'a éveillée,
        // puis son pouvoir en deux lignes ; son nom et son pouvoir restent toujours.
        List<List<Ligne>> textes = new ArrayList<>();
        float pris = 0.0F;
        for (int serre = 0; serre <= 3; serre++) {
            textes.clear();
            pris = 0.0F;
            for (int i = 0; i < n; i++) {
                List<Ligne> texte = texteDeVoie(liste.getCompound(i), contenu.revele()[i], serre);
                textes.add(texte);
                pris += hauteur(texte) + 2.0F * CARTOUCHE_DEDANS + (i > 0 ? ENTRE_VOIES : 0.0F);
            }
            if (y + pris <= fond) {
                break;
            }
        }
        // Le reste de la page se partage entre les cartouches, sans qu'un cartouche seul s'étire sur toute la page.
        float marge = Mth.clamp((fond - y - pris) / Math.max(1, n), 0.0F, 10.0F);
        float haut = y;
        float[] noeuds = new float[n];
        int lue = -1;
        for (int i = 0; i < n; i++) {
            List<Ligne> texte = textes.get(i);
            float hauteur = hauteur(texte) + 2.0F * CARTOUCHE_DEDANS + marge;
            if (courante) {
                voies[i][0] = haut;
                voies[i][1] = haut + hauteur + ENTRE_VOIES;
            }
            boolean vive = courante && i == survol;
            boolean coule = courante && i == coulante;
            if (vive) {
                lue = i;
            }
            float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
            float tenu = courante && i == tenue && !tenueEclat ? Math.min(1.0F, tenueT / TENUE) : 0.0F;
            cartouche(canvas, cartoucheX(), haut, cartoucheLarge(), hauteur, vive, tenu);
            float ly = haut + CARTOUCHE_DEDANS + marge / 2.0F;
            ecrireLignes(canvas, texte, ly, vive, courante ? 0.16F + 0.12F * Mth.sin(horloge * 2.2F + i * 1.7F) : 0.0F, LUEUR_DE_RUBRIQUE,
                    lettres);
            noeuds[i] = haut + hauteur / 2.0F;
            haut += hauteur + ENTRE_VOIES;
        }
        fourche(canvas, noeuds, haut - ENTRE_VOIES - 5.0F, lue, contenu.revele());
        pied(canvas, piedTexte, sec ? ROUGE : SEPIA);
    }

    /** La page de droite avec un éclat : la bête en majesté, son récit, la force qu'il donnerait en cartouche, ce que la table en dit. */
    private void pageEclat(OpenBook.Canvas canvas, Contenu contenu, boolean courante) {
        ItemStack stack = contenu.eclat();
        Eclat eclat = stack.get(EveilModule.ECLAT.get());
        if (eclat == null) {
            return;
        }
        Font font = Minecraft.getInstance().font;
        boolean ok = TableEveil.ECLAT_OK.equals(contenu.data().getString(TableEveil.ECLAT_ETAT));
        boolean vive = courante && pageSurvolee;
        boolean coule = courante && coulante >= 0;
        float revele = contenu.revele()[0];
        enMajeste(canvas, stack, HAUT_ECRIT + EMBLEME);
        float y = HAUT_ECRIT + 2.0F * EMBLEME + 3.0F;
        for (FormattedCharSequence ligne : font.split(stack.getHoverName().copy().withStyle(Style.EMPTY), (int) largeur())) {
            canvas.text(ligne, milieu() - font.width(ligne) / 2.0F, y, ENCRE, 1.0F);
            y += LIGNE;
        }
        filet(canvas, y + 2.0F);
        y += 8.0F;
        if (!eclat.recit().isEmpty()) {
            y = paragraphe(canvas, Component.translatable(eclat.recit()).copy().withStyle(s -> s.withItalic(true)), bordG, y, PETIT,
                    PALE, 4) + 4.0F;
        }
        // La force de l'éclat, en runes jusqu'à ce qu'on la lise, dans son cartouche.
        List<Ligne> force = new ArrayList<>();
        ajouter(force, nomEncre(eclat.enchantement(), eclat.niveau()), revele, 1.0F, RUBRIQUE, Style.EMPTY, 2, 0.0F);
        String pouvoir = "eveil.fallow.pouvoir." + eclat.enchantement().getNamespace() + "." + eclat.enchantement().getPath();
        if (I18n.exists(pouvoir)) {
            ajouter(force, Component.translatable(pouvoir), revele, PETIT, ENCRE, Style.EMPTY, 3, 1.0F);
        }
        float lettres = coule ? Mth.clamp(1.0F - (horloge - clicA) / COULEE, 0.0F, 1.0F) : 1.0F;
        float tenu = courante && tenueEclat && tenue >= 0 ? Math.min(1.0F, tenueT / TENUE) : 0.0F;
        float hauteur = hauteur(force) + 2.0F * CARTOUCHE_DEDANS;
        cartouche(canvas, cartoucheX(), y, cartoucheLarge(), hauteur, vive, tenu);
        ecrireLignes(canvas, force, y + CARTOUCHE_DEDANS, vive, courante && ok ? 0.16F + 0.12F * Mth.sin(horloge * 2.2F) : 0.0F,
                0xFF000000 | eclat.couleur(), lettres);
        fourche(canvas, new float[] {y + hauteur / 2.0F}, y + hauteur - 5.0F, vive ? 0 : -1, contenu.revele());
        y += hauteur + 5.0F;
        paragraphe(canvas, Component.translatable("eveil.fallow.livre.eclat.place"), bordG, y, PETIT, PALE, 3);
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

    // ---- le dessin ----

    /**
     * Un objet en majesté, au milieu de la page à la hauteur {@code cy} : un cercle d'or autour de lui, une lueur d'or derrière
     * qui respire, et autour du cercle une couronne de runes de la table, à l'encre pâle ; une lumière en fait lentement le tour,
     * et chaque rune s'allume d'or à son passage puis s'éteint. L'or est celui du vélin (l'encre, à la lumière de la pièce), ses
     * lueurs à pleine lumière : quand le monde s'en va autour de la table, le médaillon reste.
     */
    private static void enMajeste(OpenBook.Canvas canvas, ItemStack stack, float cy) {
        Font font = Minecraft.getInstance().font;
        float cx = Mth.floor(milieu()) + 0.5F;
        cy = Mth.floor(cy) + 0.5F;
        int r = Math.round(MEDAILLON);
        float souffle = 0.5F + 0.5F * Mth.sin(battement * 1.4F);
        float encre = canvas.ink();
        disque(canvas, cx, cy, r - 1.5F, LUEUR_D_OR, (0.07F + 0.05F * souffle) * encre);
        disque(canvas, cx, cy, r - 5.0F, LUEUR_D_OR, (0.06F + 0.05F * souffle) * encre);
        cercle(canvas, cx, cy, r, OR, 1, 0xFF);
        for (int[] point : anneau(r)) {
            canvas.brightRect(Mth.floor(cx) + point[0], Mth.floor(cy) + point[1], 1.0F, 1.0F, OR_CLAIR, 0.2F * encre,
                    OpenBook.MARK_LIFT * 1.1F);
        }
        cercle(canvas, cx, cy, COURONNE_BORD, OR, 2, 0xA0);
        // La couronne de runes : chacune droite, à sa place sur le tour ; la lumière qui tourne, et les runes qu'elle touche.
        float tour = battement * TOUR_DES_RUNES;
        for (int i = 0; i < COURONNE.length(); i++) {
            float angle = Mth.TWO_PI * i / COURONNE.length() - Mth.HALF_PI;
            FormattedCharSequence rune = Component.literal(String.valueOf(COURONNE.charAt(i)))
                    .withStyle(Style.EMPTY.withFont(Minecraft.ALT_FONT)).getVisualOrderText();
            float w = font.width(rune) * TAILLE_RUNE;
            float h = (font.lineHeight - 1) * TAILLE_RUNE;
            float x = Math.round(cx + Mth.cos(angle) * COURONNE_R - w / 2.0F);
            float y = Math.round(cy + Mth.sin(angle) * COURONNE_R - h / 2.0F) - (font.lineHeight * (1.0F - TAILLE_RUNE) * 0.5F);
            float ecart = Mth.wrapDegrees((angle - tour) * Mth.RAD_TO_DEG) / 360.0F;
            float allume = ecart <= 0.0F && ecart > -0.22F ? 1.0F + ecart / 0.22F : 0.0F;
            canvas.text(rune, x, y, TAILLE_RUNE, RUNES_DE_COURONNE, 0.9F * (1.0F - allume));
            if (allume > 0.02F) {
                // Allumée : un or soutenu, à pleine lumière, cerné de sa lueur ; elle s'éteint en revenant à l'encre pâle.
                canvas.brightText(rune, x, y, TAILLE_RUNE, OR_VIF, LUEUR_D_OR, allume, 0.0F);
            }
        }
        Level level = Minecraft.getInstance().level;
        if (level != null) {
            canvas.item(level, stack, cx - 8.0F, cy - 8.0F, 16.0F);
        }
    }

    /**
     * Les runes de la couronne, dans l'alphabet de la table (la police des runes de vanilla) : celles qui ont un corps, pas les
     * traits seuls ou les points ; et leur taille.
     */
    private static final String COURONNE = "abcdeghkmnos";
    private static final float TAILLE_RUNE = 0.75F;
    /** Le rayon où tournent les runes, et à quelle vitesse la lumière en fait le tour, radians par seconde. */
    private static final float COURONNE_R = 17.0F;
    /** Le cercle de points d'or qui ferme la couronne. */
    private static final int COURONNE_BORD = 22;
    private static final float TOUR_DES_RUNES = 0.7F;

    /** Les cercles déjà tracés, par rayon : les points d'un cercle d'une unité, dans l'ordre du tour, depuis la droite. */
    private static final Map<Integer, List<int[]>> ANNEAUX = new HashMap<>();

    /**
     * Les points d'un cercle de rayon {@code r} autour d'une unité (le cercle du point milieu : une unité d'épaisseur, sans
     * doublon), rangés dans l'ordre du tour.
     */
    private static List<int[]> anneau(int r) {
        return ANNEAUX.computeIfAbsent(r, rayon -> {
            java.util.Set<Long> vus = new java.util.HashSet<>();
            List<int[]> points = new ArrayList<>();
            int x = rayon;
            int y = 0;
            int err = 1 - rayon;
            while (x >= y) {
                for (int[] p : new int[][] {{x, y}, {y, x}, {-y, x}, {-x, y}, {-x, -y}, {-y, -x}, {y, -x}, {x, -y}}) {
                    if (vus.add((long) p[0] << 32 | p[1] & 0xFFFFFFFFL)) {
                        points.add(p);
                    }
                }
                y++;
                if (err < 0) {
                    err += 2 * y + 1;
                } else {
                    x--;
                    err += 2 * (y - x) + 1;
                }
            }
            points.sort(java.util.Comparator.comparingDouble(p -> Math.atan2(p[1], p[0])));
            return points;
        });
    }

    /** Un cercle d'or de rayon {@code r} autour de l'unité où tombe ({@code cx}, {@code cy}) ; {@code pas} 2 : un point sur deux. */
    private static void cercle(OpenBook.Canvas canvas, float cx, float cy, int r, int rgb, int pas, int alpha) {
        List<int[]> points = anneau(r);
        for (int i = 0; i < points.size(); i += pas) {
            int[] p = points.get(i);
            canvas.rect(Mth.floor(cx) + p[0], Mth.floor(cy) + p[1], 1.0F, 1.0F, OpenBook.MARK_LIFT, alpha << 24 | rgb);
        }
    }

    /** Un disque de lumière, à pleine lumière sous l'encre : la lueur derrière ce qui est en majesté. */
    private static void disque(OpenBook.Canvas canvas, float cx, float cy, float r, int rgb, float alpha) {
        float ox = Mth.floor(cx) + 0.5F;
        float oy = Mth.floor(cy) + 0.5F;
        for (int dy = -Mth.floor(r); dy <= Mth.floor(r); dy++) {
            float w = Mth.sqrt(r * r - dy * dy);
            float x0 = Math.round(ox - w);
            float x1 = Math.round(ox + w);
            canvas.brightRect(x0, Mth.floor(oy) + dy, x1 - x0, 1.0F, rgb, alpha, OpenBook.MARK_LIFT * 0.4F);
        }
    }

    /**
     * Des paillettes d'or qui montent lentement dans les marges de la page, s'allument et s'éteignent : la poussière qui vient au
     * livre, sur la page. Peu, et jamais sur ce qui est écrit au milieu.
     */
    private static void paillettes(OpenBook.Canvas canvas, int page) {
        float encre = canvas.ink();
        for (int i = 0; i < 5; i++) {
            long graine = page * 7919L + i * 104729L;
            float hasard = (float) ((graine * 2654435761L >>> 8 & 0xFFFF) / 65535.0);
            float hasard2 = (float) ((graine * 40503L + 977L >>> 4 & 0xFFFF) / 65535.0);
            float duree = 7.0F + 5.0F * hasard2;
            float t = (battement + hasard * duree) % duree / duree;
            // Dans la marge de la tranche ou du pied : jamais au milieu de l'écriture.
            boolean tranche = i % 2 == 0;
            float x = tranche ? (bordG < DEHORS ? bordD + 2.0F + hasard2 * (DEHORS - 5.0F) : 2.0F + hasard2 * (DEHORS - 5.0F))
                    : bordG + hasard2 * largeur();
            float y = tranche ? HAUT - 10.0F - t * (HAUT - 20.0F) : HAUT - 3.0F - t * 6.0F;
            float a = Mth.sin(t * Mth.PI) * 0.55F * encre;
            canvas.brightRect(Math.round(x), Math.round(y), 1.0F, 1.0F, LUEUR_D_OR, a, OpenBook.MARK_LIFT * 1.2F);
        }
    }

    /**
     * Un losange de pixels d'une unité, de rayon {@code r} (en unités), centré en ({@code cx}, {@code cy}) : plein, ou son seul
     * contour.
     */
    private static void losange(OpenBook.Canvas canvas, float cx, float cy, int r, int rgb, boolean plein) {
        int argb = 0xFF000000 | rgb;
        for (int dy = -r; dy <= r; dy++) {
            int w = r - Math.abs(dy);
            float y = cy + dy - 0.5F;
            if (plein || w == 0) {
                canvas.rect(cx - w - 0.5F, y, 2.0F * w + 1.0F, 1.0F, OpenBook.MARK_LIFT, argb);
            } else {
                canvas.rect(cx - w - 0.5F, y, 1.0F, 1.0F, OpenBook.MARK_LIFT, argb);
                canvas.rect(cx + w - 0.5F, y, 1.0F, 1.0F, OpenBook.MARK_LIFT, argb);
            }
        }
    }

    /**
     * Un cartouche : un trait d'une unité d'encre douce, et à chaque coin une équerre d'or posée par-dessus, deux unités de long,
     * avec un point d'or juste dedans ; sous le regard ({@code vive}) le trait passe à l'or et luit ; {@code tenu} de 0 à 1 : la
     * part du tour déjà repassée d'une lumière d'or, depuis le coin haut gauche.
     */
    private static void cartouche(OpenBook.Canvas canvas, float x, float y, float w, float h, boolean vive, float tenu) {
        float lift = OpenBook.MARK_LIFT;
        x = Math.round(x);
        y = Math.round(y);
        w = Math.round(w);
        h = Math.round(h);
        if (vive) {
            canvas.rect(x + 1.0F, y + 1.0F, w - 2.0F, h - 2.0F, lift * 0.5F, 0x48FFF6DC);
        }
        int trait = vive ? 0xFF000000 | OR : 0xB0000000 | SEPIA;
        canvas.rect(x + 3.0F, y, w - 6.0F, 1.0F, lift, trait);
        canvas.rect(x + 3.0F, y + h - 1.0F, w - 6.0F, 1.0F, lift, trait);
        canvas.rect(x, y + 3.0F, 1.0F, h - 6.0F, lift, trait);
        canvas.rect(x + w - 1.0F, y + 3.0F, 1.0F, h - 6.0F, lift, trait);
        int or = 0xFF000000 | OR;
        float encre = canvas.ink();
        if (vive) {
            // Lue, la voie s'encadre d'or qui luit : le trait passe à l'or et la lumière le suit.
            float a = (0.35F + 0.1F * Mth.sin(battement * 2.0F)) * encre;
            canvas.brightRect(x + 3.0F, y, w - 6.0F, 1.0F, OR_CLAIR, a, lift * 1.1F);
            canvas.brightRect(x + 3.0F, y + h - 1.0F, w - 6.0F, 1.0F, OR_CLAIR, a, lift * 1.1F);
            canvas.brightRect(x, y + 3.0F, 1.0F, h - 6.0F, OR_CLAIR, a, lift * 1.1F);
            canvas.brightRect(x + w - 1.0F, y + 3.0F, 1.0F, h - 6.0F, OR_CLAIR, a, lift * 1.1F);
        }
        for (int coin = 0; coin < 4; coin++) {
            boolean droite = coin % 2 == 1;
            boolean bas = coin >= 2;
            float cx = droite ? x + w - 1.0F : x;
            float cy = bas ? y + h - 1.0F : y;
            float sx = droite ? -1.0F : 1.0F;
            float sy = bas ? -1.0F : 1.0F;
            // L'équerre : le coin et deux unités de chaque côté ; un point d'or en diagonale, dedans.
            canvas.rect(cx, cy, 1.0F, 1.0F, lift * 1.05F, or);
            canvas.rect(droite ? cx - 2.0F : cx + 1.0F, cy, 2.0F, 1.0F, lift * 1.05F, or);
            canvas.rect(cx, bas ? cy - 2.0F : cy + 1.0F, 1.0F, 2.0F, lift * 1.05F, or);
            canvas.rect(cx + 2.0F * sx, cy + 2.0F * sy, 1.0F, 1.0F, lift * 1.05F, 0xC0000000 | OR);
            if (vive) {
                canvas.brightRect(cx, cy, 1.0F, 1.0F, LUEUR_D_OR, 0.6F * encre, lift * 1.1F);
                canvas.brightRect(cx + 2.0F * sx, cy + 2.0F * sy, 1.0F, 1.0F, LUEUR_D_OR, 0.45F * encre, lift * 1.1F);
            }
        }
        if (tenu <= 0.0F) {
            return;
        }
        // Le tour repassé de lumière d'or, côté par côté : haut, droite, bas, gauche.
        float reste = tenu * 2.0F * (w + h);
        float haut = Math.min(reste, w);
        canvas.brightRect(x, y, haut, 1.0F, LUEUR_D_OR, encre, lift * 1.2F);
        reste -= haut;
        float droite = Math.min(reste, h);
        canvas.brightRect(x + w - 1.0F, y, 1.0F, droite, LUEUR_D_OR, encre, lift * 1.2F);
        reste -= droite;
        float bas = Math.min(reste, w);
        canvas.brightRect(x + w - bas, y + h - 1.0F, bas, 1.0F, LUEUR_D_OR, encre, lift * 1.2F);
        reste -= bas;
        float gauche = Math.min(reste, h);
        canvas.brightRect(x, y + h - gauche, 1.0F, gauche, LUEUR_D_OR, encre, lift * 1.2F);
    }

    /**
     * La fourche : une tige d'une unité le long de la marge, de {@code pied} jusqu'à la plus haute des voies, une branche vers
     * chaque cartouche à la hauteur de son milieu ({@code noeuds}), un nœud en losange à chaque embranchement et une racine au
     * pied. La voie {@code lue} (ou aucune, -1) passe à l'or qui luit, et la tige depuis le pied jusqu'à elle ; une voie déjà
     * déchiffrée ({@code revele} à 1) garde son nœud plein.
     */
    private static void fourche(OpenBook.Canvas canvas, float[] noeuds, float pied, int lue, float[] revele) {
        if (noeuds.length == 0) {
            return;
        }
        float lift = OpenBook.MARK_LIFT;
        int doux = 0xC0000000 | SEPIA;
        int or = 0xFF000000 | OR;
        float encre = canvas.ink();
        float x = bordG + TIGE;
        // Every node on a whole unit, so that all of them are drawn alike; the stem stops at each node's outline, so a hollow
        // node stays hollow.
        for (int i = 0; i < noeuds.length; i++) {
            noeuds[i] = Math.round(noeuds[i]);
        }
        for (int i = 0; i < noeuds.length; i++) {
            float bas = i + 1 < noeuds.length ? noeuds[i + 1] - 2.5F : pied;
            float de = noeuds[i] + 2.5F;
            if (bas <= de) {
                continue;
            }
            canvas.rect(x - 0.5F, de, 1.0F, bas - de, lift, doux);
            if (lue >= 0 && i >= lue) {
                // Le chemin de la voie lue, du pied jusqu'à elle, passe à l'or qui luit.
                canvas.rect(x - 0.5F, de, 1.0F, bas - de, lift * 1.1F, or);
                canvas.brightRect(x - 0.5F, de, 1.0F, bas - de, OR_CLAIR, 0.35F * encre, lift * 1.2F);
            }
        }
        for (int i = 0; i < noeuds.length; i++) {
            float bx = x + 2.0F;
            float bw = cartoucheX() - x - 2.0F;
            canvas.rect(bx, noeuds[i] - 0.5F, bw, 1.0F, lift, i == lue ? or : doux);
            // Un nœud plein pour une voie déjà lue, creux pour celle qui reste à déchiffrer.
            losange(canvas, x, noeuds[i], 2, OR, i == lue || i < revele.length && revele[i] >= 1.0F);
            if (i == lue) {
                canvas.brightRect(bx, noeuds[i] - 0.5F, bw, 1.0F, OR_CLAIR, 0.35F * encre, lift * 1.2F);
                canvas.brightRect(x - 0.5F, noeuds[i] - 0.5F, 1.0F, 1.0F, LUEUR_D_OR, 0.8F * encre, lift * 1.2F);
            }
        }
        // La racine : un losange d'or serti d'un point de rubrique, comme les intitulés de la page d'en face.
        losange(canvas, x, pied + 2.5F, 2, OR, false);
        canvas.rect(x - 0.5F, pied + 2.0F, 1.0F, 1.0F, lift, 0xFF000000 | RUBRIQUE);
    }

    // ---- l'écriture ----

    /**
     * Un titre au milieu de la page, sur une ligne s'il y tient à peine plus petit, sinon coupé ; renvoie le bas de sa dernière
     * ligne.
     */
    private static float titre(OpenBook.Canvas canvas, Component texte, float y) {
        Font font = Minecraft.getInstance().font;
        float large = font.width(texte);
        float place = largeur();
        // À l'encre de rubrique, cerné d'une lueur d'or qui respire : le titre est doré, et luit quand la pièce s'assombrit.
        float lueur = 0.22F + 0.1F * Mth.sin(battement * 1.4F);
        if (large <= place / 0.75F) {
            float taille = Math.min(1.0F, place / large);
            FormattedCharSequence ligne = texte.getVisualOrderText();
            float x = milieu() - large * taille / 2.0F;
            canvas.brightText(ligne, x, y, taille, RUBRIQUE, LUEUR_DE_RUBRIQUE, lueur, 0.05F);
            canvas.text(ligne, x, y, taille, RUBRIQUE, 1.0F);
            return y + LIGNE;
        }
        for (FormattedCharSequence ligne : font.split(texte, (int) place)) {
            float x = milieu() - font.width(ligne) / 2.0F;
            canvas.brightText(ligne, x, y, 1.0F, RUBRIQUE, LUEUR_DE_RUBRIQUE, lueur, 0.05F);
            canvas.text(ligne, x, y, RUBRIQUE, 1.0F);
            y += LIGNE;
        }
        return y;
    }

    /** Un intitulé de partie : un petit losange creux, puis le mot à l'encre de rubrique. */
    private static float entete(OpenBook.Canvas canvas, Component texte, float y) {
        losange(canvas, bordG + 1.5F, y + 3.0F, 2, OR, false);
        canvas.rect(bordG + 1.0F, y + 2.5F, 1.0F, 1.0F, OpenBook.MARK_LIFT, 0xFF000000 | RUBRIQUE);
        canvas.text(texte.getVisualOrderText(), corpsX(), y, 0.85F, RUBRIQUE, 1.0F);
        return y + LIGNE;
    }

    /**
     * Les lignes d'une voie (ou de la force d'un éclat) : son nom respire d'une lueur faible ({@code souffle}) et s'allume sous le
     * regard ({@code vive}). Pas de fond : rien que l'encre et la lumière. Renvoie le bas de la dernière ligne.
     */
    private static float ecrireLignes(OpenBook.Canvas canvas, List<Ligne> texte, float y, boolean vive, float souffle, int lueur,
            float lettres) {
        for (Ligne ligne : texte) {
            y += ligne.avant();
            boolean nom = ligne.taille() == 1.0F && ligne.encre() == RUBRIQUE;
            if (nom) {
                canvas.brightText(ligne.texte(), voieX(), y, 1.0F, RUBRIQUE, lueur, vive ? 0.75F : souffle, 0.05F);
            }
            canvas.text(ligne.texte(), voieX(), y, ligne.taille(), vive && !nom ? sombre(ligne.encre()) : ligne.encre(),
                    nom ? lettres : 1.0F);
            y += ligne.taille() < 1.0F ? PETITE_LIGNE : LIGNE;
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
        return Minecraft.getInstance().font.split(texte, (int) (largeur() / PETIT)).size();
    }

    /** La phrase du pied de la page de droite, entière et centrée : elle monte d'autant de lignes qu'il lui en faut. */
    private static void pied(OpenBook.Canvas canvas, Component texte, int encre) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lignes = font.split(texte, (int) (largeur() / PETIT));
        float y = PIED + PETITE_LIGNE - lignes.size() * PETITE_LIGNE;
        for (FormattedCharSequence ligne : lignes) {
            canvas.text(ligne, milieu() - font.width(ligne) * PETIT / 2.0F, y, PETIT, encre, 1.0F);
            y += PETITE_LIGNE;
        }
    }

    /** Une ligne écrite sur une page : son texte, sa taille, son encre, et l'espace laissé au-dessus d'elle. */
    private record Ligne(FormattedCharSequence texte, float taille, int encre, float avant) {
    }

    /** Ce qu'une voie écrit : son nom, ce qu'elle portait avant, ce qu'elle fait, et l'acte qui l'a éveillée. */
    /** {@code serre} : de 0 (tout) à 3 (le nom et deux lignes du pouvoir), ce qu'une voie laisse de côté quand la place manque. */
    private static List<Ligne> texteDeVoie(CompoundTag voie, float revele, int serre) {
        List<Ligne> texte = new ArrayList<>();
        ResourceLocation id = ResourceLocation.tryParse(voie.getString(TableEveil.ENCHANTEMENT));
        int niveau = voie.getInt(TableEveil.NIVEAU);
        ajouter(texte, id == null ? Component.literal("?") : nomEncre(id, niveau), revele, 1.0F, RUBRIQUE, Style.EMPTY, 2, 0.0F);
        if (id != null && niveau > 1 && serre < 1) {
            ajouter(texte, Component.translatable("eveil.fallow.livre.avant", nomEncre(id, niveau - 1)), revele, PETIT, PALE,
                    Style.EMPTY, 1, 0.0F);
        }
        String pouvoir = id == null ? "" : "eveil.fallow.pouvoir." + id.getNamespace() + "." + id.getPath();
        if (I18n.exists(pouvoir)) {
            ajouter(texte, Component.translatable(pouvoir), revele, PETIT, ENCRE, Style.EMPTY, serre < 3 ? 3 : 2, 1.0F);
        }
        if (voie.contains(TableEveil.NE_DE) && serre < 2) {
            Component ne = Component.translatable("eveil.fallow.livre.ne_de",
                    Component.translatable(voie.getString(TableEveil.NE_DE), quantite(voie.getDouble(TableEveil.FOIS))));
            ajouter(texte, ne, revele, PETIT, SEPIA, Style.EMPTY.withItalic(true), 3, 2.0F);
        }
        return texte;
    }

    private static void ajouter(List<Ligne> texte, Component mots, float revele, float taille, int encre, Style style, int auPlus,
            float avant) {
        List<FormattedCharSequence> lignes = lignes(mots, revele, voieLarge() / taille, style);
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

    /** La hauteur que prendrait {@link #paragraphe}. */
    private static float hauteurDe(Component texte, float x, float taille, int auPlus) {
        int n = Minecraft.getInstance().font.split(texte, (int) ((bordD - x) / taille)).size();
        return Math.min(auPlus, n) * (taille < 1.0F ? PETITE_LIGNE : LIGNE);
    }

    private static float paragraphe(OpenBook.Canvas canvas, Component texte, float x, float y, float taille, int encre, int auPlus) {
        Font font = Minecraft.getInstance().font;
        List<FormattedCharSequence> lignes = font.split(texte, (int) ((bordD - x) / taille));
        float pas = taille < 1.0F ? PETITE_LIGNE : LIGNE;
        for (FormattedCharSequence ligne : lignes.subList(0, Math.min(auPlus, lignes.size()))) {
            canvas.text(ligne, x, y, taille, encre, 1.0F);
            y += pas;
        }
        return y;
    }

    /** Le filet des livres, court, un losange au milieu, comme entre deux parties d'un livre. */
    private static void filet(OpenBook.Canvas canvas, float y) {
        // Un filet d'or qui s'amincit vers ses bouts, un losange de rubrique serti d'or au milieu, un point d'or à chaque bout.
        float cx = Mth.floor(milieu());
        float ly = Math.round(y);
        float demi = Math.min(22.0F, largeur() / 2.0F - 4.0F);
        for (int i = 3; i <= demi; i++) {
            int a = i < demi * 0.6F ? 0xFF : i < demi * 0.85F ? 0xB0 : 0x70;
            canvas.rect(cx + i, ly, 1.0F, 1.0F, OpenBook.MARK_LIFT, a << 24 | OR);
            canvas.rect(cx - i, ly, 1.0F, 1.0F, OpenBook.MARK_LIFT, a << 24 | OR);
        }
        canvas.rect(cx + demi + 2.0F, ly, 1.0F, 1.0F, OpenBook.MARK_LIFT, 0xC0000000 | OR);
        canvas.rect(cx - demi - 2.0F, ly, 1.0F, 1.0F, OpenBook.MARK_LIFT, 0xC0000000 | OR);
        losange(canvas, cx + 0.5F, ly + 0.5F, 2, OR, false);
        canvas.rect(cx, ly, 1.0F, 1.0F, OpenBook.MARK_LIFT, 0xFF000000 | RUBRIQUE);
        // De loin en loin, un reflet court le long du filet, comme la lumière sur une dorure qu'on incline.
        float course = (battement % REFLET) / 1.4F;
        if (course < 1.0F) {
            float px = Math.round(cx - demi + course * 2.0F * demi);
            float force = Mth.sin(course * Mth.PI) * canvas.ink();
            for (int d = -2; d <= 2; d++) {
                if (Math.abs(px + d - cx) < 3.0F) {
                    continue;
                }
                canvas.brightRect(px + d, ly, 1.0F, 1.0F, LUEUR_D_OR, force * (1.0F - Math.abs(d) * 0.35F), OpenBook.MARK_LIFT * 1.2F);
            }
        }
    }

    /** Trois petits losanges sous une phrase seule. */
    private static void fleuron(OpenBook.Canvas canvas, float y) {
        for (int i = -1; i <= 1; i++) {
            losange(canvas, Mth.floor(milieu()) + 0.5F + i * 7.0F, y + 1.0F, 1, i == 0 ? RUBRIQUE : OR, true);
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
        cadrer(Math.floorMod(page, 2) == 1);
        List<FormattedCharSequence> lignes = runes.computeIfAbsent(page, p -> {
            Font font = Minecraft.getInstance().font;
            EnchantmentNames.getInstance().initSeed(p * 31L + 7L);
            List<FormattedCharSequence> faites = new ArrayList<>();
            for (int i = 0; i < 13; i++) {
                FormattedText mots = EnchantmentNames.getInstance().getRandomName(font, (int) largeur());
                faites.add(net.minecraft.locale.Language.getInstance().getVisualOrder(mots));
            }
            return faites;
        });
        if (runes.size() > 64) {
            runes.clear();
        }
        float y = HAUT_ECRIT;
        for (FormattedCharSequence ligne : lignes) {
            canvas.text(ligne, bordG, y, RUNES, 0.45F);
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
