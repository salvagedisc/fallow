package io.github.salvagedisc.fallow.gabarits.client;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.github.salvagedisc.fallow.Fallow;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.ResourceLocation;

/**
 * Une texture de mod à refaire d'après un gabarit du jeu.
 *
 * @param target la texture du mod (son dessin d'origine donne la couleur)
 * @param template la texture du jeu dont on prend le dessin, telle que le pack actif la dessine
 * @param ref la même texture dans le jeu sans pack : l'écart entre elle et la texture du mod donne la teinte ;
 *        {@code null} : le gabarit est repris tel quel (le pack dessine déjà ce métal, le cuivre par exemple)
 * @param others les mêmes objets dans d'autres matières (or, diamant) : ce qui ne change pas entre eux (le manche)
 *        n'est pas du métal et garde sa couleur
 * @param handles les couleurs du bâton sont aussi gardées (manches)
 * @param lightness part de l'écart de clarté reportée (1 : toute)
 * @param chroma part de la couleur reportée
 * @param gate recouvrement minimal entre la forme de la texture du mod et celle du gabarit sans pack (0 : aucun) ;
 *        en dessous, l'objet a son propre dessin et on n'y touche pas
 * @param gateRef la texture du jeu qui sert à ce contrôle, si ce n'est pas {@code ref}
 * @param extras les pixels du mod là où le gabarit est vide sont gardés (cimier d'un casque, pièces en plus)
 */
record Regle(ResourceLocation target, ResourceLocation template, ResourceLocation ref, List<ResourceLocation> others,
        boolean handles, double lightness, double chroma, double gate, ResourceLocation gateRef, boolean extras) {

    static final String PATH = "/assets/" + Fallow.MOD_ID + "/gabarits/regles.json";

    static List<Regle> load() {
        List<Regle> regles = new ArrayList<>();
        try (InputStream in = Regle.class.getResourceAsStream(PATH)) {
            if (in == null) {
                Fallow.LOGGER.warn("gabarits : {} introuvable", PATH);
                return regles;
            }
            try (Reader reader = new InputStreamReader(in, StandardCharsets.UTF_8)) {
                for (JsonElement element : JsonParser.parseReader(reader).getAsJsonArray()) {
                    JsonObject o = element.getAsJsonObject();
                    List<ResourceLocation> others = new ArrayList<>();
                    if (o.has("others")) {
                        JsonArray array = o.getAsJsonArray("others");
                        for (JsonElement other : array) {
                            others.add(ResourceLocation.parse(other.getAsString()));
                        }
                    }
                    regles.add(new Regle(ResourceLocation.parse(o.get("target").getAsString()),
                            ResourceLocation.parse(o.get("template").getAsString()), location(o, "ref"), List.copyOf(others),
                            o.has("handles") && o.get("handles").getAsBoolean(), number(o, "kl", 1.0), number(o, "kc", 0.9),
                            number(o, "gate", 0.0), location(o, "gateref"), o.has("extras") && o.get("extras").getAsBoolean()));
                }
            }
        } catch (IOException | RuntimeException e) {
            Fallow.LOGGER.warn("gabarits : {} illisible", PATH, e);
        }
        return regles;
    }

    private static ResourceLocation location(JsonObject o, String key) {
        return o.has(key) && !o.get(key).isJsonNull() ? ResourceLocation.parse(o.get(key).getAsString()) : null;
    }

    private static double number(JsonObject o, String key, double fallback) {
        return o.has(key) ? o.get(key).getAsDouble() : fallback;
    }
}
