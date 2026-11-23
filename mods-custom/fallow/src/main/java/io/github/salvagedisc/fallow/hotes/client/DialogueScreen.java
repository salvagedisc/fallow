package io.github.salvagedisc.fallow.hotes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import com.mojang.blaze3d.vertex.VertexSorting;
import net.minecraft.client.renderer.GameRenderer;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.hotes.Hote;
import io.github.salvagedisc.fallow.hotes.Peuple;
import io.github.salvagedisc.fallow.hotes.visite.Demande;
import io.github.salvagedisc.fallow.hotes.visite.Echanges;
import net.neoforged.neoforge.network.PacketDistributor;
import java.util.ArrayList;
import java.text.Normalizer;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dialogue avec un Hôte, à la façon des PNJ de Legendary Edition mais dans la manière des livres du pack : une feuille de
 * leur papier en bas de l'écran, écrite de leurs encres, l'interface du jeu masquée, l'Hôte vivant dans une gravure de son
 * milieu, la réplique qui s'écrit lettre par lettre avec un murmure, des réponses à pictogramme d'encre qu'on choisit au
 * clavier ou à la souris. La vue se tourne doucement vers
 * l'Hôte à l'ouverture. Rien n'est donné ni retiré : les réponses ne font que mener d'une réplique à l'autre.
 */
public class DialogueScreen extends Screen {
    private static final ResourceLocation ICONES = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/icones.png");
    private static final int PORTRAIT = 50;
    /** Bord du cadre peint ; l'intérieur fait 40 x 40. */
    private static final int CADRE = 5;
    /** Taille de l'Hôte dans le portrait, en pixels d'interface par bloc : deux pixels par texel, le buste et le masque. */
    private static final float ECHELLE = 32.0F;
    /** Le buste de trois quarts, le visage tourné vers la réplique. */
    private static final float TROIS_QUARTS = 25.0F;
    private static final float TRAIT_R = 0x21 / 255.0F;
    private static final float TRAIT_V = 0x1D / 255.0F;
    private static final float TRAIT_B = 0x19 / 255.0F;
    private static final int ECART = 8;
    private static final int MARGE = 10;
    private static final int LIGNE = 11;
    /** Une réponse qui montre un objet : l'objet à sa taille réelle (16 px), jamais réduit, pour garder ses pixels nets. */
    private static final int LIGNE_OBJET = 17;
    /** La place réservée : quatre lignes de réplique et quatre réponses. */
    private static final int LIGNES = 4;
    private static final int REPONSES = 4;
    private static final float LETTRES_PAR_SECONDE = 38.0F;
    /** Le regard se tourne vers l'Hôte en 150 ms, comme s'ouvrent les autres fenêtres du pack (docs/plan/interactions.md). */
    private static final long REGARD_MS = 150;
    private static final long FEUILLE_MS = 180;
    /** Où tombent les yeux de l'Hôte : à cette part de la demi-hauteur de l'écran au-dessus du centre. */
    /** Du sommet de l'Hôte au milieu de son masque, en blocs. */
    private static final double TETE = 0.25;
    /** De combien la vue se resserre sur l'Hôte, au plus fort (avant le réglage des effets de champ de vision). */
    private static final float SERRE = 0.12F;
    /** Les bandes du cinéma qui se referment en haut et en bas, en 450 ms, sur un douzième de la hauteur chacune. */
    private static final long BANDES_MS = 450;
    private static final float BANDES = 1.0F / 12.0F;
    /** Les lettres qui viennent de naître s'écrivent d'abord dans l'écriture ancienne (la police de la table d'enchantement). */
    private static final ResourceLocation ECRITURE_ANCIENNE = ResourceLocation.withDefaultNamespace("alt");
    private static final int ETRANGERES = 5;
    /** Les bandes : un brun-noir de suie, pas un noir pur. */
    private static final int BANDE = 0xF20A0806;

    /**
     * Le feuillet : une feuille du papier des livres (scripts/hotes/feuillet.py), son ombre sur le monde comprise. Elle se
     * pose un pixel au-dessus et à gauche du panneau, trois en dessous et à droite.
     */
    private static final ResourceLocation FEUILLET = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "hote/feuillet");
    /** Les encres des livres : brun foncé pour ce qui est écrit, brun doux, brun pâle ; le rouge de rubrique. Jamais de noir. */
    private static final int ENCRE = 0xFF3B2A1A;
    private static final int ENCRE_DOUCE = 0xFF8A6B45;
    private static final int ENCRE_PALE = 0xFFB9A27C;
    private static final int RUBRIQUE = 0xFFA3322A;
    /** La réponse choisie : le papier un ton plus foncé, comme une ligne qu'on suit du doigt. */
    private static final int SOULIGNE = 0x80E2CFA2;
    /** L'objet d'une réponse fermée s'efface sous un voile du papier. */
    private static final int VOILE = 0xA8F6EDD4;
    private static final int NOM = ENCRE;
    private static final int TEXTE = ENCRE;
    private static final int REPONSE = ENCRE_DOUCE;
    private static final int CHOISIE = ENCRE;
    /** Une réponse qu'on ne peut pas prendre : ce qui manque, ce qui est déjà posé. */
    private static final int FERMEE = 0xFFA89070;

    /** Les icônes des réponses, dans l'ordre de l'atlas (11 px de côté) : le baluchon, la
     * question, partir, revenir, déjà demandée, le linge vide (ne rien laisser). */
    private enum Icone { CADEAU, QUESTION, PARTIR, RETOUR, LUE, RIEN }

    /** Une réponse ; {@code ouverte} faux : grisée (il manque ce qu'il faut, ou c'est déjà fait). */
    private record Reponse(Component texte, Icone icone, ItemStack objet, boolean ouverte, Runnable action) {
        Reponse(String cle, Icone icone, Runnable action) {
            this(Component.translatable(cle), icone, ItemStack.EMPTY, true, action);
        }
    }

    private final Hote hote;
    private final RandomSource hasard = RandomSource.create();
    private Echanges.Echange echange;
    /** Les questions déjà posées : leur réponse reste marquée, comme une ligne déjà lue. */
    private final java.util.Set<String> posees = new java.util.HashSet<>();
    private boolean refuse;
    private String replique;
    private List<Reponse> reponses = List.of();
    private int choisie;
    private long debutTexte;
    private int murmures;
    private boolean interfaceCachee;
    private boolean cachait;
    private long ouverture;
    private float lacetDepart;
    private float tangageDepart;

    private int panneauX;
    private int panneauY;
    private int panneauL;
    private int panneauH;
    private int reponsesY;

    public DialogueScreen(Hote hote, Echanges.Echange echange) {
        super(Component.translatable("entity.fallow.hote"));
        this.hote = hote;
        this.echange = echange;
        accueil();
    }

    public Hote hote() {
        return hote;
    }

    /** Le serveur a répondu (un don posé, un refus entendu) : les souhaits remplis se mettent à jour. */
    public void maj(Echanges.Echange echange) {
        this.echange = echange;
    }

    /** Les particules qui flottent dans le portrait : spores, poussière de redstone, éclats de prismarite, bulles, pollen, âmes du sculk. */
    private static int particule(Peuple peuple) {
        return switch (peuple) {
            case ECORCE -> 0xE6EDA0;
            case BRAISE -> 0xFF8A3A;
            case GIVRE -> 0xEEE6FF;
            case FONDS -> 0xC8F0FF;
            case SABLES -> 0xF4E6A0;
            case SILENCE -> 0x29DFEB;
        };
    }

    /** La couleur du titre de l'émissaire, prise dans l'armure de sa bête et assez claire pour le panneau sombre. */
    private static int titre(Peuple peuple) {
        // L'encre du Peuple sur le papier : assez sombre pour se lire, la couleur de son milieu.
        return switch (peuple) {
            case ECORCE -> 0xFF4E6E2E;
            case BRAISE -> 0xFFA3322A;
            case GIVRE -> 0xFF6A4C96;
            case FONDS -> 0xFF22686E;
            case SABLES -> 0xFF86641E;
            case SILENCE -> 0xFF2C5A66;
        };
    }

    // --- Les répliques et leurs réponses ---

    private String cle(String suffixe) {
        return "screen.fallow.hote." + hote.peuple().id() + "." + suffixe;
    }

    private static String rep(String suffixe) {
        return "screen.fallow.hote.rep." + suffixe;
    }

    /** Une réplique et ses variantes (« clé.2 », « clé.3 »… dans les fichiers de langue), l'une tirée au hasard. */
    private String variante(String cleReplique) {
        List<String> cles = new ArrayList<>(List.of(cleReplique));
        for (int i = 2; I18n.exists(cleReplique + "." + i); i++) {
            cles.add(cleReplique + "." + i);
        }
        return I18n.get(cles.get(hasard.nextInt(cles.size())));
    }

    private void dire(String cleReplique, List<Reponse> suite) {
        replique = variante(cleReplique);
        reponses = suite;
        choisie = 0;
        debutTexte = System.currentTimeMillis();
        murmures = 0;
    }

    private boolean resteASouhaiter() {
        for (int i = 0; i < echange.demandes().size(); i++) {
            if (!echange.donne(i)) {
                return true;
            }
        }
        return false;
    }

    private void accueil() {
        accueil(cle("accueil"));
    }

    private void accueil(String cleReplique) {
        List<Reponse> suite = new ArrayList<>();
        if (echange.enVisite() && resteASouhaiter()) {
            suite.add(new Reponse(rep("donner"), Icone.CADEAU, this::don));
        }
        suite.add(new Reponse(rep("demander"), Icone.QUESTION, () -> demande(cle("demande"))));
        if (echange.enVisite() && echange.refusPossible() && !refuse && echange.donnes() == 0) {
            suite.add(new Reponse(rep("refuser"), Icone.RIEN, this::refuser));
        }
        suite.add(new Reponse(rep("partir"), Icone.PARTIR, this::onClose));
        dire(cleReplique, suite);
    }

    private void don() {
        List<Reponse> suite = new ArrayList<>();
        var player = Minecraft.getInstance().player;
        for (int i = 0; i < echange.demandes().size(); i++) {
            Demande demande = echange.demandes().get(i);
            ItemStack pile = demande.pile();
            // Un souhait large (toute laine, toute viande cuite) se lit par le nom du tag, pas par celui de son icône.
            Component nom = demande.tag().<Component>map(t -> Component.translatableWithFallback(
                    "tag.item." + t.location().getNamespace() + "." + t.location().getPath().replace('/', '.'),
                    pile.getHoverName().getString())).orElse(pile.getHoverName());
            // Le nombre s'écrit à l'encre après le nom (« Charbon ×6 ») : le chiffre blanc du jeu ne se lit pas sur le papier.
            if (demande.nombre() > 1) {
                nom = Component.translatable(rep("nombre"), nom, demande.nombre());
            }
            int index = i;
            if (echange.donne(i)) {
                suite.add(new Reponse(Component.translatable(rep("pose"), nom), null, pile, false, () -> { }));
            } else if (player != null && Echanges.compte(player, demande) >= demande.nombre()) {
                suite.add(new Reponse(nom, null, pile, true, () -> donner(index)));
            } else {
                suite.add(new Reponse(Component.translatable(rep("manque"), nom), null, pile, false, () -> { }));
            }
        }
        suite.add(new Reponse(rep("retour"), Icone.RETOUR, () -> accueil(cle("encore"))));
        dire(cle("don"), suite);
    }

    private void donner(int index) {
        PacketDistributor.sendToServer(new Echanges.Donner(hote.getId(), index));
        // Le serveur confirme et corrige au besoin ; la réponse de l'émissaire, elle, vient tout de suite.
        echange = new Echanges.Echange(echange.entite(), echange.demandes(), echange.donnes() | (1 << index),
                echange.refusPossible(), echange.enVisite(), false);
        List<Reponse> suite = new ArrayList<>();
        if (resteASouhaiter()) {
            suite.add(new Reponse(rep("donner_encore"), Icone.CADEAU, this::don));
        }
        suite.add(new Reponse(rep("partir"), Icone.PARTIR, this::onClose));
        dire(cle(resteASouhaiter() ? "merci" : "comble"), suite);
    }

    private void refuser() {
        PacketDistributor.sendToServer(new Echanges.Refuser(hote.getId()));
        refuse = true;
        dire(cle("refus"), List.of(new Reponse(rep("partir"), Icone.PARTIR, this::onClose)));
    }

    private void demande(String cleReplique) {
        List<Reponse> suite = new ArrayList<>();
        for (String sujet : new String[] {"bete", "saison", "lieu"}) {
            Component texte = Component.translatable(rep(sujet));
            boolean posee = posees.contains(sujet);
            suite.add(new Reponse(texte, posee ? Icone.LUE : Icone.QUESTION, ItemStack.EMPTY, true, () -> {
                posees.add(sujet);
                demande(cle("question." + sujet));
            }));
        }
        suite.add(new Reponse(rep("retour"), Icone.RETOUR, () -> accueil(cle("encore"))));
        dire(cleReplique, suite);
    }

    // --- Ouverture et fermeture : interface masquée, regard tourné vers l'Hôte. ---

    @Override
    protected void init() {
        Minecraft mc = Minecraft.getInstance();
        if (!interfaceCachee) {
            interfaceCachee = true;
            cachait = mc.options.hideGui;
            mc.options.hideGui = true;
            ouverture = System.currentTimeMillis();
            // Un son grave et lointain, comme une note tenue dans la brume : on entre dans le temps de l'Hôte.
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.AMETHYST_BLOCK_RESONATE, ton(hote.peuple()), 0.55F));
            mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.SOUL_ESCAPE.value(), 0.5F, 0.35F));
            if (mc.player != null) {
                lacetDepart = mc.player.getYRot();
                tangageDepart = mc.player.getXRot();
            }
        }
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = cachait;
        // Le tampon du portrait ne sert qu'à cette fenêtre : il est rendu à la carte graphique en la fermant.
        if (tampon != null) {
            tampon.destroyBuffers();
            tampon = null;
        }
        super.removed();
    }

    /**
     * Des lettres dans l'écriture de la table d'enchantement. Cette police ne connaît que les lettres latines sans accent : on
     * ôte les accents, et ce qui n'est pas une lettre (espace, ponctuation) reste dans l'écriture ordinaire, à l'encre douce.
     */
    private static Component ancienne(String texte) {
        MutableComponent tout = Component.empty().withColor(ENCRE_DOUCE & 0xFFFFFF);
        String nu = Normalizer.normalize(texte, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
        for (int i = 0; i < nu.length(); i++) {
            char c = nu.charAt(i);
            boolean lettre = c < 128 && Character.isLetter(c);
            tout.append(Component.literal(String.valueOf(c)).withStyle(style -> lettre ? style.withFont(ECRITURE_ANCIENNE) : style));
        }
        return tout;
    }

    private void tournerLeRegard(float partialTick) {
        var player = Minecraft.getInstance().player;
        if (player == null) {
            return;
        }
        float t = Mth.clamp((System.currentTimeMillis() - ouverture) / (float) REGARD_MS, 0.0F, 1.0F);
        if (t >= 1.0F && player.getYRot() == player.yRotO) {
            return;
        }
        float lisse = t * t * (3.0F - 2.0F * t);
        Vec3 oeil = player.getEyePosition(partialTick);
        // Cadrer le masque dans la fenêtre que laissent la bande du haut et la feuille : on vise le milieu de sa tête, puis on
        // baisse le regard d'autant qu'il faut pour que ce point tombe au milieu de cette fenêtre (le champ de vision, une fois
        // resserré par la rencontre, compris). De près, la tête déborde la fenêtre des deux côtés et les yeux restent au centre.
        Vec3 cible = hote.getPosition(partialTick).add(0.0, hote.getBbHeight() - TETE, 0.0).subtract(oeil);
        float lacet = (float) (Mth.atan2(cible.z, cible.x) * Mth.RAD_TO_DEG) - 90.0F;
        var options = Minecraft.getInstance().options;
        double demiChamp = Math.toRadians(options.fov().get() * (1.0 - SERRE * options.fovEffectScale().get()) / 2.0);
        double haut = height * BANDES;
        double bas = panneauH > 0 ? height - panneauH - 10 : height;
        double ecart = (height / 2.0 - (haut + bas) / 2.0) / (height / 2.0);
        float baisse = (float) Math.toDegrees(Math.atan(ecart * Math.tan(demiChamp)));
        float tangage = Mth.clamp((float) -(Mth.atan2(cible.y, cible.horizontalDistance()) * Mth.RAD_TO_DEG) + baisse, -90.0F, 90.0F);
        float y = lacetDepart + Mth.wrapDegrees(lacet - lacetDepart) * lisse;
        float x = tangageDepart + (tangage - tangageDepart) * lisse;
        player.setYRot(y);
        player.setXRot(x);
        player.yRotO = y;
        player.xRotO = x;
        player.setYHeadRot(y);
    }

    // --- Rendu ---

    /** La réplique coupée en lignes, sans laisser un mot seul sur la dernière : on resserre un peu la colonne s'il le faut. */
    private List<String> lignes(int largeur) {
        List<String> out = decoupe(largeur);
        for (int essai = 1; essai <= 6 && out.size() > 1 && !out.get(out.size() - 1).trim().contains(" "); essai++) {
            List<String> serre = decoupe(largeur - essai * 10);
            if (serre.size() == out.size() && serre.get(serre.size() - 1).trim().contains(" ")) {
                return serre;
            }
        }
        return out;
    }

    private List<String> decoupe(int largeur) {
        List<String> out = new ArrayList<>();
        for (FormattedText ligne : font.getSplitter().splitLines(replique, largeur, Style.EMPTY)) {
            out.add(ligne.getString());
        }
        return out;
    }

    private int lettresVisibles() {
        return (int) ((System.currentTimeMillis() - debutTexte) * LETTRES_PAR_SECONDE / 1000.0F);
    }

    private boolean ecrit() {
        return lettresVisibles() >= replique.length();
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Pas de voile sur le monde : on parle à l'Hôte, on le voit.
    }

    /**
     * Le portrait : le fond peint de l'émissaire, un peu de pollen qui flotte, l'Hôte lui-même rendu vivant (Fresh Animations
     * l'anime comme dans le monde), puis le cadre commun à tous les Peuples par-dessus. L'Hôte est coupé net au bord intérieur du cadre.
     */
    private void portrait(GuiGraphics graphics, int px, int py) {
        String peuple = hote.peuple().id();
        // Seule la peinture (l'intérieur) : autour, c'est le papier du feuillet, où le cadre est imprimé.
        graphics.blit(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/portrait_" + peuple + ".png"),
                px + CADRE, py + CADRE, CADRE, CADRE, PORTRAIT - 2 * CADRE, PORTRAIT - 2 * CADRE, PORTRAIT, PORTRAIT);
        int ix = px + CADRE;
        int iy = py + CADRE;
        int cote = PORTRAIT - 2 * CADRE;
        float t = (System.currentTimeMillis() % 600_000L) / 1000.0F;
        for (int i = 0; i < 4; i++) {
            float phase = i * 1.7F;
            int gx = ix + 4 + Math.floorMod((int) (i * 11 + Mth.sin(t * 0.35F + phase) * 3.0F), cote - 8);
            int gy = iy + 3 + Math.floorMod((int) (i * 9 - t * (1.2F + i * 0.25F)), cote - 12);
            int alpha = (int) (110 + 110 * Mth.sin(t * 1.3F + phase));
            graphics.fill(gx, gy, gx + 1, gy + 1, (Math.max(0, alpha) << 24) | particule(hote.peuple()));
        }

        hoteSurLaGrille(graphics, ix, iy, cote);

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        RenderSystem.enableBlend();
        graphics.blit(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/portrait_cadre.png"),
                px, py, 0, 0, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
        RenderSystem.disableBlend();
        graphics.pose().popPose();
    }

    // --- La rencontre : le monde mis à part autour de l'Hôte ---

    /**
     * La brume de chaque Peuple, qui se referme autour de l'Hôte et de soi pendant qu'on se parle : sombre et teintée de son
     * milieu, l'Hôte net à deux ou trois blocs, la lisière qui s'y perd vers onze, le ciel noyé dedans. Le monde baisse la voix
     * (un quart de son volume), le sien aussi : seuls la note de la rencontre et l'écriture gardent le leur. Elle vient en 1,2 s et repart en 1,6 s quand on se quitte.
     */
    private static io.github.salvagedisc.fallow.eveil.client.Isolement.Scene scene(Peuple peuple) {
        float[] c = switch (peuple) {
            case ECORCE -> new float[] {0.10F, 0.14F, 0.10F};
            case BRAISE -> new float[] {0.17F, 0.08F, 0.05F};
            case GIVRE -> new float[] {0.12F, 0.11F, 0.20F};
            case FONDS -> new float[] {0.05F, 0.13F, 0.16F};
            case SABLES -> new float[] {0.12F, 0.14F, 0.08F};
            case SILENCE -> new float[] {0.04F, 0.07F, 0.08F};
        };
        return new io.github.salvagedisc.fallow.eveil.client.Isolement.Scene(c[0], c[1], c[2], 5.0F, 18.0F, 2.5F, 11.0F,
                0.25F, 3.5, 1.2F, 1.6F, SERRE);
    }

    /** Ce qui flotte autour de l'Hôte pendant qu'il parle : spores, cendre, flocons, lueurs des fonds, pollen, âmes du sculk. */
    private static net.minecraft.core.particles.ParticleOptions flottant(Peuple peuple) {
        return switch (peuple) {
            case ECORCE -> net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR;
            case BRAISE -> net.minecraft.core.particles.ParticleTypes.ASH;
            case GIVRE -> net.minecraft.core.particles.ParticleTypes.SNOWFLAKE;
            case FONDS -> net.minecraft.core.particles.ParticleTypes.GLOW;
            case SABLES -> net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR;
            case SILENCE -> net.minecraft.core.particles.ParticleTypes.SCULK_SOUL;
        };
    }

    private long dernierFlottant;

    /**
     * À chaque image : la brume du Peuple tient le monde à part et la vue s'approche lentement de l'Hôte ; quelques poussières
     * de son milieu flottent autour de lui ; deux bandes sombres se referment en haut et en bas de l'écran, comme au cinéma.
     */
    private void rencontre(GuiGraphics graphics) {
        io.github.salvagedisc.fallow.eveil.client.Isolement.viser(scene(hote.peuple()), 1.0F, hote.position(), 1.0F);

        long maintenant = System.currentTimeMillis();
        var level = Minecraft.getInstance().level;
        // Les spores flottent une demi-minute : moins souvent, pour qu'elles ne s'amassent pas.
        long cadence = flottant(hote.peuple()) == net.minecraft.core.particles.ParticleTypes.SPORE_BLOSSOM_AIR ? 900L : 220L;
        if (level != null && maintenant - dernierFlottant > cadence) {
            dernierFlottant = maintenant;
            double a = hasard.nextDouble() * Math.PI * 2.0;
            double r = 0.6 + hasard.nextDouble() * 1.6;
            Vec3 lieu = new Vec3(hote.getX() + Math.cos(a) * r, hote.getY() + 0.3 + hasard.nextDouble() * 2.0,
                    hote.getZ() + Math.sin(a) * r);
            var camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
            // Pas sous le nez : une poussière à moins de deux blocs de l'œil ne serait plus qu'une tache floue.
            if (lieu.distanceToSqr(camera) > 4.0) {
                level.addParticle(flottant(hote.peuple()), lieu.x, lieu.y, lieu.z, 0.0, hasard.nextDouble() * 0.01, 0.0);
            }
        }

        float t = Mth.clamp((maintenant - ouverture) / (float) BANDES_MS, 0.0F, 1.0F);
        float e = 1.0F - (1.0F - t) * (1.0F - t) * (1.0F - t);
        int bande = Math.round(height * BANDES * e);
        if (bande > 0) {
            graphics.fill(0, 0, width, bande, BANDE);
            graphics.fill(0, height - bande, width, height, BANDE);
        }
    }

    /** La note de chaque Peuple, du plus grave (le Silence) au plus clair (les Chantres). */
    private static float ton(Peuple peuple) {
        return switch (peuple) {
            case SILENCE -> 0.5F;
            case BRAISE -> 0.56F;
            case FONDS -> 0.63F;
            case ECORCE -> 0.71F;
            case SABLES -> 0.79F;
            case GIVRE -> 0.89F;
        };
    }

    /** Le tampon où l'Hôte est rendu à un pixel par pixel d'interface, puis agrandi sans lissage. */
    private static TextureTarget tampon;

    /**
     * L'Hôte, rendu dans un petit tampon de {@code cote} pixels (un par pixel d'interface) puis posé agrandi au plus proche :
     * ses pixels tombent sur la grille de l'interface, comme ceux du cadre et des icônes, au lieu d'être plus fins qu'eux.
     * À deux pixels par texel, le masque et le buste se lisent comme une image peinte, de trois quarts.
     */
    private void hoteSurLaGrille(GuiGraphics graphics, int ix, int iy, int cote) {
        Minecraft mc = Minecraft.getInstance();
        graphics.flush();
        if (tampon == null || tampon.width != cote) {
            if (tampon != null) {
                tampon.destroyBuffers();
            }
            tampon = new TextureTarget(cote, cote, true, Minecraft.ON_OSX);
            tampon.setFilterMode(GL11.GL_NEAREST);
        }
        tampon.setClearColor(0.0F, 0.0F, 0.0F, 0.0F);
        tampon.clear(Minecraft.ON_OSX);
        tampon.bindWrite(true);
        Matrix4f projection = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting tri = RenderSystem.getVertexSorting();
        RenderSystem.setProjectionMatrix(new Matrix4f().setOrtho(0.0F, cote, cote, 0.0F, 1000.0F, 21000.0F), VertexSorting.ORTHOGRAPHIC_Z);

        float corps = hote.yBodyRot;
        float corpsAvant = hote.yBodyRotO;
        float lacet = hote.getYRot();
        float lacetAvant = hote.yRotO;
        float tangage = hote.getXRot();
        float tangageAvant = hote.xRotO;
        float tete = hote.yHeadRot;
        float teteAvant = hote.yHeadRotO;
        float face = 180.0F - TROIS_QUARTS;
        hote.yBodyRot = face;
        hote.yBodyRotO = face;
        hote.setYRot(face);
        hote.yRotO = face;
        hote.setXRot(0.0F);
        hote.xRotO = 0.0F;
        hote.yHeadRot = face;
        hote.yHeadRotO = face;
        GuiGraphics dedans = new GuiGraphics(mc, mc.renderBuffers().bufferSource());
        // Les pieds sur un pixel entier : les rangées de texels tombent juste. Le haut des créneaux du masque à trois
        // pixels du bord, le bas du cadre coupe le torse au-dessus de la ceinture.
        float piedsY = Math.round(3.0F + 2.05F * ECHELLE);
        float centreX = cote / 2.0F;
        // Le trait sombre autour de la silhouette : quatre copies teintées, décalées d'un pixel et poussées derrière.
        RenderSystem.setShaderColor(TRAIT_R, TRAIT_V, TRAIT_B, 1.0F);
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            dedans.pose().pushPose();
            dedans.pose().translate(0.0F, 0.0F, -40.0F);
            InventoryScreen.renderEntityInInventory(dedans, centreX + d[0], piedsY + d[1], ECHELLE, new Vector3f(),
                    new Quaternionf().rotateZ((float) Math.PI), null, hote);
            dedans.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        InventoryScreen.renderEntityInInventory(dedans, centreX, piedsY, ECHELLE, new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI), null, hote);
        dedans.flush();
        hote.yBodyRot = corps;
        hote.yBodyRotO = corpsAvant;
        hote.setYRot(lacet);
        hote.yRotO = lacetAvant;
        hote.setXRot(tangage);
        hote.xRotO = tangageAvant;
        hote.yHeadRot = tete;
        hote.yHeadRotO = teteAvant;

        RenderSystem.setProjectionMatrix(projection, tri);
        mc.getMainRenderTarget().bindWrite(true);

        // Le tampon posé dans le portrait (sa première rangée est en bas : les coordonnées de texture se retournent).
        RenderSystem.setShaderTexture(0, tampon.getColorTextureId());
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        Matrix4f m = graphics.pose().last().pose();
        BufferBuilder quad = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        quad.addVertex(m, ix, iy, 0.0F).setUv(0.0F, 1.0F);
        quad.addVertex(m, ix, iy + cote, 0.0F).setUv(0.0F, 0.0F);
        quad.addVertex(m, ix + cote, iy + cote, 0.0F).setUv(1.0F, 0.0F);
        quad.addVertex(m, ix + cote, iy, 0.0F).setUv(1.0F, 1.0F);
        BufferUploader.drawWithShader(quad.buildOrThrow());
        RenderSystem.disableBlend();
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        if (hote.isRemoved()) {
            // L'Hôte est reparti (touché, ou rappelé) : la conversation s'arrête avec lui.
            onClose();
            return;
        }
        tournerLeRegard(partialTick);

        panneauL = Math.min(360, width - 2 * 8);
        int largeurTexte = panneauL - 2 * MARGE - PORTRAIT - ECART;
        List<String> lignes = lignes(largeurTexte);
        // Une hauteur fixe (quatre lignes de réplique, quatre réponses) : le panneau ne saute pas d'une réplique à l'autre.
        int hauteurTexte = LIGNE + 6 + Math.max(LIGNES, lignes.size()) * LIGNE + 8 + Math.max(REPONSES, reponses.size()) * ligneReponse() - 2;
        panneauH = MARGE + Math.max(hauteurTexte, PORTRAIT) + MARGE;
        panneauX = (width - panneauL) / 2;
        // La feuille monte de quelques pixels en s'ouvrant, dans le temps des autres fenêtres du pack.
        float montee = Mth.clamp((System.currentTimeMillis() - ouverture) / (float) FEUILLE_MS, 0.0F, 1.0F);
        panneauY = height - panneauH - 10 + Math.round((1.0F - montee) * (1.0F - montee) * 14.0F);

        rencontre(graphics);

        // Le panneau et son liseré ; le portrait, avec son cadre, en haut à gauche dans le panneau.
        RenderSystem.enableBlend();
        graphics.blitSprite(FEUILLET, panneauX - 1, panneauY - 1, panneauL + 4, panneauH + 4);
        RenderSystem.disableBlend();
        portrait(graphics, panneauX + MARGE, panneauY + MARGE);

        // À droite du portrait : nom du peuple, titre, réplique, réponses.
        int x = panneauX + MARGE + PORTRAIT + ECART;
        int y = panneauY + MARGE;
        // L'émissaire attitré du Peuple, par son nom, un tiret, puis son Peuple : « Nom – Peuple ».
        Component nom = Component.translatable(cle("nom"));
        String tiret = " – ";
        graphics.drawString(font, nom, x, y, NOM, false);
        graphics.drawString(font, tiret, x + font.width(nom), y, REPONSE, false);
        graphics.drawString(font, Component.translatable(cle("titre")), x + font.width(nom) + font.width(tiret), y,
                titre(hote.peuple()), false);
        y += LIGNE + 2;
        filet(graphics, x, panneauX + panneauL - MARGE, y);
        y += 4;

        int reste = lettresVisibles();
        // Les dernières lettres écrites viennent d'ailleurs : elles naissent dans l'écriture de la table d'enchantement, puis
        // se fixent en français, comme si la parole de l'Hôte se traduisait en arrivant.
        int etrangeres = ecrit() ? 0 : ETRANGERES;
        for (String ligne : lignes) {
            int n = Math.max(0, Math.min(ligne.length(), reste));
            int fixe = Math.max(0, Math.min(n, reste - etrangeres));
            graphics.drawString(font, Component.literal(ligne.substring(0, fixe)).append(ancienne(ligne.substring(fixe, n))), x, y,
                    TEXTE, false);
            reste -= ligne.length() + 1;
            y += LIGNE;
        }
        murmurer();

        y += 8;
        reponsesY = y;
        if (ecrit()) {
            int ligne = ligneReponse();
            for (int i = 0; i < reponses.size(); i++) {
                int ry = y + i * ligne;
                if (mouseX >= x && mouseX < panneauX + panneauL - MARGE && mouseY >= ry - 2 && mouseY < ry + ligne - 2) {
                    choisie = i;
                }
                Reponse r = reponses.get(i);
                boolean active = i == choisie;
                if (active) {
                    // La réponse choisie : le papier un ton plus foncé, et dans la marge le losange rouge des livres.
                    graphics.fill(x - 3, ry - 2, panneauX + panneauL - MARGE + 3, ry + ligne - 3, SOULIGNE);
                    losange(graphics, x - 8, ry - 2 + (ligne - 1) / 2, r.ouverte() ? RUBRIQUE : ENCRE_PALE);
                }
                if (r.objet().isEmpty()) {
                    // Les autres icônes restent en retrait, comme une encre plus légère ; l'icône choisie à pleine encre.
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, active ? 1.0F : 0.6F);
                    icone(graphics, r.icone(), x + 1, ry + 1);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                    RenderSystem.disableBlend();
                } else {
                    graphics.renderItem(r.objet(), x - 2, ry - 2);
                    if (!r.ouverte()) {
                        // L'objet d'une réponse fermée s'éteint avec son texte.
                        graphics.pose().pushPose();
                        graphics.pose().translate(0.0F, 0.0F, 300.0F);
                        graphics.fill(x - 2, ry - 2, x + 14, ry + 14, VOILE);
                        graphics.pose().popPose();
                    }
                }
                // Le texte centré dans la bande de la réponse, quelle que soit sa hauteur.
                int texteY = ry - 2 + (ligne - 1 - 7) / 2;
                int couleur = !r.ouverte() ? FERMEE : active ? CHOISIE : REPONSE;
                graphics.drawString(font, r.texte(), x + 19, texteY, couleur, false);
            }
        }
    }

    /** Toutes les réponses ont la hauteur d'un objet (16 px et un d'écart), qu'elles en montrent un ou une icône. */
    private int ligneReponse() {
        return LIGNE_OBJET;
    }

    /** Le filet sous le nom, comme sous le nom d'une recette : un trait d'encre pâle qui s'efface aux bouts, un losange rouge au milieu. */
    private static void filet(GuiGraphics graphics, int x0, int x1, int y) {
        int milieu = (x0 + x1) / 2;
        graphics.fill(x0 + 3, y, milieu - 4, y + 1, ENCRE_PALE);
        graphics.fill(milieu + 5, y, x1 - 3, y + 1, ENCRE_PALE);
        graphics.fill(x0, y, x0 + 3, y + 1, (ENCRE_PALE & 0xFFFFFF) | 0x60000000);
        graphics.fill(x1 - 3, y, x1, y + 1, (ENCRE_PALE & 0xFFFFFF) | 0x60000000);
        losange(graphics, milieu, y, RUBRIQUE);
    }

    /** Un petit losange de 5 px centré en ({@code cx}, {@code cy}). */
    private static void losange(GuiGraphics graphics, int cx, int cy, int couleur) {
        graphics.fill(cx, cy - 2, cx + 1, cy + 3, couleur);
        graphics.fill(cx - 1, cy - 1, cx + 2, cy + 2, couleur);
        graphics.fill(cx - 2, cy, cx + 3, cy + 1, couleur);
    }

    private static void icone(GuiGraphics graphics, Icone icone, int x, int y) {
        if (icone != null) {
            graphics.blit(ICONES, x, y, icone.ordinal() * 11, 0, 11, 11, Icone.values().length * 11, 11);
        }
    }

    /** Un murmure de bois toutes les deux lettres écrites, à hauteur changeante. */
    private void murmurer() {
        int visibles = Math.min(lettresVisibles(), replique.length());
        while (murmures * 2 + 2 <= visibles) {
            murmures++;
            char c = replique.charAt(Math.min(replique.length() - 1, murmures * 2));
            if (Character.isLetter(c)) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(
                        SoundEvents.NOTE_BLOCK_XYLOPHONE.value(), 0.5F + hasard.nextFloat() * 0.25F, 0.12F));
            }
        }
    }

    // --- Commandes ---

    private void confirmer() {
        if (!ecrit()) {
            debutTexte = 0;
            return;
        }
        if (choisie >= 0 && choisie < reponses.size()) {
            Reponse r = reponses.get(choisie);
            if (!r.ouverte()) {
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.WOODEN_BUTTON_CLICK_OFF, 0.7F, 0.3F));
                return;
            }
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.25F));
            r.action().run();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= panneauX && mouseX < panneauX + panneauL && mouseY >= panneauY && mouseY < panneauY + panneauH) {
            if (!ecrit()) {
                confirmer();
            } else {
                // La réponse sous la souris au moment du clic, pas celle de la dernière image : un clic rapide après
                // un geste de la souris ne doit pas choisir la réponse d'avant.
                int i = (int) Math.floor((mouseY - reponsesY + 2) / ligneReponse());
                if (mouseY >= reponsesY - 2 && i >= 0 && i < reponses.size()) {
                    choisie = i;
                    confirmer();
                }
            }
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean keyPressed(int key, int scan, int modifiers) {
        switch (key) {
            case InputConstants.KEY_UP, InputConstants.KEY_W -> {
                choisie = Math.floorMod(choisie - 1, Math.max(1, reponses.size()));
                return true;
            }
            case InputConstants.KEY_DOWN, InputConstants.KEY_S -> {
                choisie = Math.floorMod(choisie + 1, Math.max(1, reponses.size()));
                return true;
            }
            case InputConstants.KEY_RETURN, InputConstants.KEY_NUMPADENTER, InputConstants.KEY_SPACE, InputConstants.KEY_E -> {
                confirmer();
                return true;
            }
            default -> {
                return super.keyPressed(key, scan, modifiers);
            }
        }
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
