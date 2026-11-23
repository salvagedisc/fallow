package io.github.salvagedisc.fallow.fiches;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.salvagedisc.fallow.Fallow;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;

/**
 * Les fenêtres en fiche et leur caisse. {@code assets/fallow/fiches.json}, écrit par {@code scripts/fiches/fiches.py} dans les
 * ressources du pack à côté des textures qu'il redessine (sans elles, aucune fenêtre n'est touchée), donne,
 * pour chaque classe d'écran, où poser la caisse dans la fenêtre : son coin haut gauche, la caisse faisant 176 de large et
 * 113 de haut (plus 18 par rangée de coffre), la place exacte de l'inventaire d'une fenêtre de coffre (le premier rang de
 * l'inventaire 31 sous le haut de la caisse, sous ses rangées).
 *
 * <p>La caisse est la fenêtre de coffre vanilla réduite à l'inventaire : sa bande de titre (le haut de
 * {@code generic_54.png}, 17 de haut) et son bas (l'inventaire, 96 de haut, à partir de 126), comme la fenêtre du coffre les
 * assemble. Le pack de ressources actif la dessine donc comme ses coffres. La fiche la recouvre en partie ; le nom
 * « Inventaire » n'est pas écrit, et le titre de la fenêtre est à l'encre brune des livres.
 */
public final class Fiches {
    /**
     * Où poser la caisse dans la fenêtre ; {@code rangees} : ses rangées de coffre au-dessus de l'inventaire (une fenêtre qui
     * est un coffre sous un autre nom, la caisse de Quark) ; {@code fiche} : la fenêtre est une fiche, son nom
     * « Inventaire » tu et son titre à l'encre (sinon elle garde les titres d'un coffre) ; {@code titre} : son titre est écrit
     * (non quand il passerait sur le dessin du poste, le tableau d'affichage).
     */
    public record Caisse(int x, int y, int rangees, boolean fiche, boolean titre) {
    }

    private static final ResourceLocation TABLE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "fiches.json");
    private static final ResourceLocation COFFRE = ResourceLocation.withDefaultNamespace("textures/gui/container/generic_54.png");
    /** L'encre des livres, pour le titre (le gris foncé des fenêtres vanilla, 0x404040, devient cette encre). */
    public static final int ENCRE = 0x3B2A1A;
    /** L'encre douce des livres, pour un bouton éteint ou un texte d'aide. */
    private static final int ENCRE_DOUCE = 0x8A6B45;
    /** Les boutons et champs de texte vanilla d'une fenêtre en fiche : cartes et puits du papier (scripts/fiches/fiches.py). */
    private static final Map<ResourceLocation, ResourceLocation> CARTES = Map.of(
            ResourceLocation.withDefaultNamespace("widget/button"), carte("button"),
            ResourceLocation.withDefaultNamespace("widget/button_highlighted"), carte("button_highlighted"),
            ResourceLocation.withDefaultNamespace("widget/button_disabled"), carte("button_disabled"),
            ResourceLocation.withDefaultNamespace("widget/text_field"), carte("text_field"),
            ResourceLocation.withDefaultNamespace("widget/text_field_highlighted"), carte("text_field_highlighted"));

    private static volatile Map<String, Caisse> ecrans = Map.of();
    /** Pendant que la fenêtre en fiche écrit ses titres : le nom à taire (« Inventaire »), sinon {@code null}. */
    private static Component tu;
    private static Component tuAussi;
    /** Une fenêtre en fiche se dessine, et où elle est (x, y, largeur, hauteur). */
    private static boolean feuille;
    private static int gauche;
    private static int haut;
    private static int largeur;
    private static int hauteur;
    /** Un bouton ou un champ de texte vanilla s'y dessine. */
    private static boolean widget;

    private Fiches() {
    }

    static void reload(ResourceManager manager) {
        Map<String, Caisse> lu = new HashMap<>();
        manager.getResource(TABLE).ifPresent(resource -> {
            try (Reader reader = resource.openAsReader()) {
                JsonObject screens = JsonParser.parseReader(reader).getAsJsonObject().getAsJsonObject("screens");
                screens.entrySet().forEach(e -> {
                    JsonObject o = e.getValue().getAsJsonObject();
                    lu.put(e.getKey(), new Caisse(o.get("x").getAsInt(), o.get("y").getAsInt(),
                            o.has("rows") ? o.get("rows").getAsInt() : 0, !o.has("fiche") || o.get("fiche").getAsBoolean(),
                            !o.has("title") || o.get("title").getAsBoolean()));
                });
            } catch (Exception e) {
                Fallow.LOGGER.warn("fiches : {} illisible ({})", TABLE, e.toString());
            }
        });
        ecrans = Map.copyOf(lu);
    }

    /** La caisse de cet écran, ou {@code null} si ce n'est pas une fenêtre en fiche. */
    public static Caisse de(Screen screen) {
        return ecrans.get(screen.getClass().getName());
    }

    /** Dessine la caisse de l'inventaire, la fenêtre étant en (left, top). */
    public static void caisse(GuiGraphics g, Caisse c, int left, int top) {
        int x = left + c.x();
        int y = top + c.y();
        int haut = 17 + 18 * c.rangees();
        g.blit(COFFRE, x, y, 0, 0, 176, haut);
        g.blit(COFFRE, x, y + haut, 0, 126, 176, 96);
    }

    private static ResourceLocation carte(String nom) {
        return ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "fiche/" + nom);
    }

    /**
     * Début ou fin des titres d'une fenêtre en fiche ({@code inventaire} : le nom à taire ; {@code titre} : le titre de la
     * fenêtre, à taire aussi, ou {@code null}).
     */
    public static void titres(boolean debut, Component inventaire, Component titre) {
        tu = debut ? inventaire : null;
        tuAussi = debut ? titre : null;
    }

    /** Le nom « Inventaire » d'une fenêtre en fiche, ou un titre tu, qui n'est pas écrit. */
    public static boolean tait(Component texte) {
        return texte != null && (texte == tu || texte == tuAussi);
    }

    /** Début ou fin du dessin d'une fenêtre en fiche, la fenêtre étant en (x, y), de largeur w et de hauteur h. */
    public static void feuille(boolean debut, int x, int y, int w, int h) {
        feuille = debut;
        gauche = x;
        haut = y;
        largeur = w;
        hauteur = h;
        widget = false;
    }

    /**
     * Début ou fin du dessin d'un bouton ou d'un champ de texte vanilla en (x, y, w, h) : une carte de papier s'il est dans
     * la fenêtre en fiche (pas la recherche d'EMI au pied de l'écran, ni ce qu'un autre mod pose à côté).
     */
    public static void widget(boolean debut, int x, int y, int w, int h) {
        int cx = x + w / 2;
        int cy = y + h / 2;
        widget = debut && feuille && cx >= gauche && cx < gauche + largeur && cy >= haut && cy < haut + hauteur;
    }

    /** Le sprite d'un bouton ou d'un champ de texte vanilla : dans une fenêtre en fiche, sa carte de papier. */
    public static ResourceLocation sprite(ResourceLocation sprite) {
        return widget ? CARTES.getOrDefault(sprite, sprite) : sprite;
    }

    /**
     * La couleur d'un texte dans une fenêtre en fiche : ses gris foncés (le 0x404040 des titres vanilla, ce qu'un mod écrit
     * sur son fond) deviennent l'encre, ses gris moyens (un compte qui manque, une aide) l'encre douce ; dans un bouton ou un
     * champ de texte vanilla, le blanc et les gris clairs l'encre, les gris moyens (éteint, aide) l'encre douce. Le blanc
     * des nombres d'objets reste blanc ; les infobulles sont dessinées après la fenêtre.
     */
    public static int couleur(int color) {
        if (!feuille) {
            return color;
        }
        int rgb = color & 0xFFFFFF;
        int r = rgb >> 16 & 0xFF;
        if (r != (rgb >> 8 & 0xFF) || r != (rgb & 0xFF)) {
            return color;
        }
        int a = color & 0xFF000000;
        if (widget) {
            return r >= 0xC0 ? a | ENCRE : r >= 0x60 ? a | ENCRE_DOUCE : color;
        }
        if (r >= 0x30 && r <= 0x50) {
            return a | ENCRE;
        }
        return r >= 0x80 && r <= 0xB0 ? a | ENCRE_DOUCE : color;
    }

    /** La couleur d'un aplat : le curseur gris clair d'un champ de texte vanilla, dans une fenêtre en fiche, à l'encre. */
    public static int curseur(int color) {
        return widget ? couleur(color) : color;
    }

    /** L'ombre d'un texte : l'encre d'un bouton ou d'un champ de texte, sur le papier, n'en a pas. */
    public static boolean ombre(boolean shadow) {
        return shadow && !widget;
    }
}
