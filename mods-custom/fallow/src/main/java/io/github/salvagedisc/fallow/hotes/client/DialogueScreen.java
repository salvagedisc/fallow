package io.github.salvagedisc.fallow.hotes.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.blaze3d.systems.RenderSystem;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.hotes.Hote;
import io.github.salvagedisc.fallow.hotes.Peuple;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Dialogue avec un Hôte (pilote), à la façon des PNJ de Legendary Edition mais dessiné à nous : un panneau sombre en
 * bas de l'écran, l'interface du jeu masquée, l'Hôte vivant dans un cadre peint du peuple, la réplique qui s'écrit lettre par
 * lettre avec un murmure, des réponses à icône qu'on choisit au clavier ou à la souris. La vue se tourne doucement vers
 * l'Hôte à l'ouverture. Rien n'est donné ni retiré : les réponses ne font que mener d'une réplique à l'autre.
 */
public class DialogueScreen extends Screen {
    private static final ResourceLocation ICONES = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/icones.png");
    private static final int PORTRAIT = 50;
    /** Bord du cadre peint ; l'intérieur fait 40 x 40. */
    private static final int CADRE = 5;
    /** Taille de l'Hôte dans le portrait, en pixels d'interface par bloc : le buste tient dans le cadre avec 4 pixels de marge. */
    private static final float ECHELLE = 26.0F;
    /** Le buste de trois quarts, le visage tourné vers la réplique. */
    private static final float TROIS_QUARTS = 25.0F;
    private static final float TRAIT_R = 0x21 / 255.0F;
    private static final float TRAIT_V = 0x1D / 255.0F;
    private static final float TRAIT_B = 0x19 / 255.0F;
    private static final int ECART = 8;
    private static final int MARGE = 10;
    private static final int LIGNE = 11;
    private static final int LIGNE_REPONSE = 14;
    /** Une réponse qui montre un objet : l'objet à sa taille réelle (16 px), jamais réduit, pour garder ses pixels nets. */
    private static final int LIGNE_OBJET = 17;
    private static final float LETTRES_PAR_SECONDE = 38.0F;
    /** Le regard se tourne vers l'Hôte en 150 ms, comme s'ouvrent les autres fenêtres du pack (docs/plan/interactions.md). */
    private static final long REGARD_MS = 150;

    private static final int FOND = 0xD0101513;
    private static final int BORD = 0x50C9D3C4;
    private static final int NOM = 0xFFF2EEE4;
    private static final int TEXTE = 0xFFE0DDD4;
    private static final int REPONSE = 0xFFA9B3A6;
    private static final int CHOISIE = 0xFFF0CF78;

    /** Icônes de l'atlas, 11 px de côté. */
    private enum Icone { CADEAU, QUESTION, PARTIR, RETOUR }

    private record Reponse(String cle, Icone icone, ItemStack objet, Runnable action) {
    }

    private final Hote hote;
    private final RandomSource hasard = RandomSource.create();
    private final List<ItemStack> souhaits;
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

    public DialogueScreen(Hote hote) {
        super(Component.translatable("entity.fallow.hote"));
        this.hote = hote;
        this.souhaits = souhaits(hote.peuple());
        accueil();
    }

    /** Ce que chaque peuple aime qu'on lui laisse (pilote : trois objets fixes par peuple). */
    private static List<ItemStack> souhaits(Peuple peuple) {
        return switch (peuple) {
            // Les Discrets : des baies qui éclairent, du miel, de quoi faire repousser ce qu'on a mangé.
            case ECORCE -> List.of(new ItemStack(Items.GLOW_BERRIES), new ItemStack(Items.HONEY_BOTTLE), new ItemStack(Items.BONE_MEAL));
            // Les Attiseurs : ce qui brûle longtemps, ce qui garde la chaleur, ce qui nourrit celui qui veille.
            case BRAISE -> List.of(new ItemStack(Items.CHARCOAL), new ItemStack(Items.MAGMA_BLOCK), new ItemStack(Items.COOKED_MUTTON));
            // Les Chantres : ce qui tinte, ce qui fleurit en hauteur, ce qui aide à planer.
            case GIVRE -> List.of(new ItemStack(Items.AMETHYST_SHARD), new ItemStack(Items.CHORUS_FRUIT), new ItemStack(Items.FEATHER));
            // Les Souffleurs : ce qui nage, ce qui flotte, ce qui éclaire sous l'eau.
            case FONDS -> List.of(new ItemStack(Items.COD), new ItemStack(Items.KELP), new ItemStack(Items.SEA_PICKLE));
            // Les Guetteurs : un don qui se compte en tas (melon, bambou, cacao).
            case SABLES -> List.of(new ItemStack(Items.MELON_SLICE), new ItemStack(Items.BAMBOO), new ItemStack(Items.COCOA_BEANS));
            // Les Taiseux : ce qui étouffe les pas, ce qui garde un écho, ce qui éclaire sans bruit.
            case SILENCE -> List.of(new ItemStack(Items.GRAY_WOOL), new ItemStack(Items.ECHO_SHARD), new ItemStack(Items.GRAY_CANDLE));
        };
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
        return switch (peuple) {
            case ECORCE -> 0xFF9CBF6E;
            case BRAISE -> 0xFFE8735A;
            case GIVRE -> 0xFFD58BE0;
            case FONDS -> 0xFF7FC4A8;
            case SABLES -> 0xFFD9C36A;
            case SILENCE -> 0xFF5FCFD3;
        };
    }

    // --- Les répliques et leurs réponses (pilote : écrites ici ; à terme lues en JSON par peuple). ---

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

    private void accueil() {
        dire(cle("accueil"), List.of(
                new Reponse(rep("donner"), Icone.CADEAU, ItemStack.EMPTY, this::don),
                new Reponse(rep("demander"), Icone.QUESTION, ItemStack.EMPTY, () -> demande(cle("demande"))),
                new Reponse(rep("refuser"), Icone.PARTIR, ItemStack.EMPTY,
                        () -> dire(cle("refus"), List.of(new Reponse(rep("partir"), Icone.PARTIR, ItemStack.EMPTY, this::onClose))))));
    }

    private void don() {
        List<Reponse> suite = new ArrayList<>();
        for (ItemStack souhait : souhaits) {
            suite.add(new Reponse(souhait.getDescriptionId(), null, souhait,
                    () -> dire(cle("merci"), List.of(new Reponse(rep("partir"), Icone.PARTIR, ItemStack.EMPTY, this::onClose)))));
        }
        suite.add(new Reponse(rep("retour"), Icone.RETOUR, ItemStack.EMPTY, this::accueil));
        dire(cle("don"), suite);
    }

    private void demande(String cleReplique) {
        List<Reponse> suite = new ArrayList<>();
        for (String sujet : new String[] {"bete", "saison", "lieu"}) {
            suite.add(new Reponse(rep(sujet), Icone.QUESTION, ItemStack.EMPTY, () -> demande(cle("question." + sujet))));
        }
        suite.add(new Reponse(rep("retour"), Icone.RETOUR, ItemStack.EMPTY, this::accueil));
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
            if (mc.player != null) {
                lacetDepart = mc.player.getYRot();
                tangageDepart = mc.player.getXRot();
            }
        }
    }

    @Override
    public void removed() {
        Minecraft.getInstance().options.hideGui = cachait;
        super.removed();
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
        // Viser bas (les genoux) : le buste et le masque restent au-dessus du panneau.
        Vec3 cible = hote.getPosition(partialTick).add(0.0, 0.5, 0.0).subtract(oeil);
        float lacet = (float) (Mth.atan2(cible.z, cible.x) * Mth.RAD_TO_DEG) - 90.0F;
        float tangage = (float) -(Mth.atan2(cible.y, cible.horizontalDistance()) * Mth.RAD_TO_DEG);
        float y = lacetDepart + Mth.wrapDegrees(lacet - lacetDepart) * lisse;
        float x = tangageDepart + (tangage - tangageDepart) * lisse;
        player.setYRot(y);
        player.setXRot(x);
        player.yRotO = y;
        player.xRotO = x;
        player.setYHeadRot(y);
    }

    // --- Rendu ---

    private List<String> lignes(int largeur) {
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
        graphics.blit(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/portrait_" + peuple + ".png"),
                px, py, 0, 0, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
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

        graphics.enableScissor(ix, iy, ix + cote, iy + cote);
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
        // Le haut des créneaux du masque (2,05 blocs) à 7 pixels du bord : le bas du cadre coupe le torse au-dessus de sa
        // jointure avec les jambes, qui s'ouvre quand Fresh Animations le fait respirer.
        float piedsY = iy + 7.0F + 2.05F * ECHELLE;
        // Le masque avance vers la droite à trois quarts : un demi-pixel de décalage centre la silhouette.
        float centreX = ix + cote / 2.0F + 0.5F;
        // Le trait sombre autour de la silhouette, comme sur les portraits peints : quatre copies teintées, décalées d'un
        // pixel et poussées derrière, puis l'Hôte lui-même par-dessus.
        RenderSystem.setShaderColor(TRAIT_R, TRAIT_V, TRAIT_B, 1.0F);
        for (int[] d : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            graphics.pose().pushPose();
            graphics.pose().translate(0.0F, 0.0F, -40.0F);
            InventoryScreen.renderEntityInInventory(graphics, centreX + d[0], piedsY + d[1], ECHELLE, new Vector3f(),
                    new Quaternionf().rotateZ((float) Math.PI), null, hote);
            graphics.pose().popPose();
        }
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        InventoryScreen.renderEntityInInventory(graphics, centreX, piedsY, ECHELLE, new Vector3f(),
                new Quaternionf().rotateZ((float) Math.PI), null, hote);
        hote.yBodyRot = corps;
        hote.yBodyRotO = corpsAvant;
        hote.setYRot(lacet);
        hote.yRotO = lacetAvant;
        hote.setXRot(tangage);
        hote.xRotO = tangageAvant;
        hote.yHeadRot = tete;
        hote.yHeadRotO = teteAvant;
        graphics.disableScissor();

        graphics.pose().pushPose();
        graphics.pose().translate(0.0F, 0.0F, 200.0F);
        graphics.blit(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "textures/gui/hote/portrait_cadre.png"),
                px, py, 0, 0, PORTRAIT, PORTRAIT, PORTRAIT, PORTRAIT);
        graphics.pose().popPose();
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
        int hauteurTexte = LIGNE + 6 + lignes.size() * LIGNE + 8 + reponses.size() * ligneReponse() - 2;
        panneauH = MARGE + Math.max(hauteurTexte, PORTRAIT) + MARGE;
        panneauX = (width - panneauL) / 2;
        panneauY = height - panneauH - 10;

        // Panneau sans bordure ; le portrait, avec son cadre, en haut à gauche dans le panneau.
        graphics.fill(panneauX, panneauY, panneauX + panneauL, panneauY + panneauH, FOND);
        portrait(graphics, panneauX + MARGE, panneauY + MARGE);

        // À droite du portrait : nom du peuple, titre, réplique, réponses.
        int x = panneauX + MARGE + PORTRAIT + ECART;
        int y = panneauY + MARGE;
        // L'émissaire attitré du Peuple, par son nom, un tiret, puis son Peuple : « Nom – Peuple ».
        Component nom = Component.translatable(cle("nom"));
        String tiret = " – ";
        graphics.drawString(font, nom, x, y, NOM, true);
        graphics.drawString(font, tiret, x + font.width(nom), y, REPONSE, true);
        graphics.drawString(font, Component.translatable(cle("titre")), x + font.width(nom) + font.width(tiret), y,
                titre(hote.peuple()), true);
        y += LIGNE + 2;
        graphics.fill(x, y, panneauX + panneauL - MARGE, y + 1, BORD);
        y += 4;

        int reste = lettresVisibles();
        for (String ligne : lignes) {
            String part = ligne.substring(0, Math.max(0, Math.min(ligne.length(), reste)));
            graphics.drawString(font, part, x, y, TEXTE, true);
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
                    // La réponse choisie : une bande claire et, à sa gauche, un trait de la couleur du Peuple.
                    graphics.fill(x - 3, ry - 2, panneauX + panneauL - MARGE + 3, ry + ligne - 3, 0x22F0CF78);
                    graphics.fill(x - 5, ry - 2, x - 4, ry + ligne - 3, titre(hote.peuple()));
                }
                if (r.objet().isEmpty()) {
                    // Les autres icônes restent en retrait, l'icône choisie à pleine lumière.
                    float lumiere = active ? 1.0F : 0.62F;
                    RenderSystem.setShaderColor(lumiere, lumiere, lumiere, 1.0F);
                    icone(graphics, r.icone(), x, ry - 1);
                    RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
                } else {
                    graphics.renderItem(r.objet(), x - 2, ry - 2);
                }
                // Le texte centré dans la bande de la réponse, quelle que soit sa hauteur.
                int texteY = ry - 2 + (ligne - 1 - 7) / 2;
                graphics.drawString(font, Component.translatable(r.cle()), x + 16, texteY, active ? CHOISIE : REPONSE, true);
            }
        }
    }

    private int ligneReponse() {
        for (Reponse r : reponses) {
            if (!r.objet().isEmpty()) {
                return LIGNE_OBJET;
            }
        }
        return LIGNE_REPONSE;
    }

    private static void icone(GuiGraphics graphics, Icone icone, int x, int y) {
        if (icone != null) {
            graphics.blit(ICONES, x, y, icone.ordinal() * 11, 0, 11, 11, 44, 11);
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
            Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK.value(), 1.2F, 0.25F));
            reponses.get(choisie).action().run();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && mouseX >= panneauX && mouseX < panneauX + panneauL && mouseY >= panneauY && mouseY < panneauY + panneauH) {
            if (!ecrit() || mouseY >= reponsesY - 2) {
                confirmer();
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
