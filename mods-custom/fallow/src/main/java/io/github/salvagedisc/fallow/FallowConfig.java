package io.github.salvagedisc.fallow;

import java.util.List;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Configuration de Fallow : un booléen par module, plus quelques réglages.
 * {@code config/fallow-client.toml} pour les modules client, {@code config/fallow-common.toml} pour les autres,
 * {@code serverconfig/fallow-server.toml} (dans le monde, envoyé aux joueurs) pour ce que client et serveur partagent.
 * Les valeurs sont relues à chaque usage : couper un module en jeu prend effet tout de suite.
 */
public final class FallowConfig {
    public static final ModConfigSpec CLIENT_SPEC;
    public static final ModConfigSpec COMMON_SPEC;
    /**
     * Réglages que le client doit partager avec le serveur (vitesse de minage, enclume) : réglages de serveur,
     * envoyés aux joueurs à la connexion, dans {@code serverconfig/fallow-server.toml} du monde.
     */
    public static final ModConfigSpec SERVER_SPEC;

    private static final ModConfigSpec.BooleanValue CIEL_ENABLED;
    private static final ModConfigSpec.BooleanValue CIEL_REDRAW_MOON;
    private static final ModConfigSpec.BooleanValue CIEL_CLEAR_FOG_ON_BIOME_UPDATE;
    private static final ModConfigSpec.BooleanValue HORIZON_ENABLED;
    private static final ModConfigSpec.BooleanValue HORIZON_SKIP_HIDDEN_LODS;
    private static final ModConfigSpec.IntValue HORIZON_RING_MAX_DISTANCE;
    private static final ModConfigSpec.BooleanValue XP_ENABLED;
    private static final ModConfigSpec.BooleanValue XP_HIDE_BAR;
    private static final ModConfigSpec.BooleanValue XP_BLOCK_ENCHANT_MERGE;
    private static final ModConfigSpec.BooleanValue CLAIRIERE_ENABLED;
    private static final ModConfigSpec.IntValue CLAIRIERE_RING_TICKS;
    private static final ModConfigSpec.DoubleValue CIEL_CLEARING_FADE;
    private static final ModConfigSpec.DoubleValue POSTES_SOUND_VOLUME;
    private static final ModConfigSpec.BooleanValue CIEL_MIST_WITHOUT_SHADER;
    private static final ModConfigSpec.IntValue CIEL_MIST_DISTANCE;
    private static final ModConfigSpec.IntValue CIEL_CLEARING_DISTANCE;
    private static final ModConfigSpec.BooleanValue OUTILS_ENABLED;
    private static final ModConfigSpec.BooleanValue EVEIL_ENABLED;
    private static final ModConfigSpec.DoubleValue EVEIL_SEUIL;
    private static final ModConfigSpec.DoubleValue EVEIL_FACTEUR;
    private static final ModConfigSpec.DoubleValue EVEIL_RACCOMMODAGE;
    private static final ModConfigSpec.DoubleValue EVEIL_ALERTE;
    private static final ModConfigSpec.BooleanValue EVEIL_POSTE;
    private static final java.util.Map<String, ModConfigSpec.IntValue> EVEIL_PLACES = new java.util.LinkedHashMap<>();
    private static final ModConfigSpec.BooleanValue RENDU_ENABLED;
    private static final ModConfigSpec.BooleanValue VENT_ENABLED;
    private static final ModConfigSpec.BooleanValue VENT_MOTES;
    private static final ModConfigSpec.BooleanValue BORD_ENABLED;
    private static final ModConfigSpec.BooleanValue NAVIRES_WIND_ENABLED;
    private static final ModConfigSpec.DoubleValue NAVIRES_WIND_STRENGTH;
    private static final ModConfigSpec.BooleanValue NAVIRES_STORMS;
    private static final ModConfigSpec.BooleanValue NAVIRES_SNOW;
    private static final ModConfigSpec.IntValue NAVIRES_SNOW_MAX_LAYERS;
    private static final ModConfigSpec.BooleanValue NAVIRES_SAIL_WIND;
    private static final ModConfigSpec.BooleanValue BAC_ENABLED;
    private static final ModConfigSpec.BooleanValue PRISE_ENABLED;
    private static final ModConfigSpec.BooleanValue ZOO_ENABLED;
    private static final ModConfigSpec.IntValue ZOO_SPOT_DISTANCE;
    private static final ModConfigSpec.IntValue ZOO_OBSERVE_SECONDS;
    private static final ModConfigSpec.IntValue ZOO_SPYGLASS_DISTANCE;
    private static final ModConfigSpec.BooleanValue APPROCHE_ENABLED;
    private static final ModConfigSpec.DoubleValue APPROCHE_SCENT_RANGE;
    private static final ModConfigSpec.DoubleValue APPROCHE_SIGHT_RANGE;
    private static final ModConfigSpec.DoubleValue APPROCHE_HEARING_RANGE;
    private static final ModConfigSpec.BooleanValue APPROCHE_HINTS;
    private static final ModConfigSpec.BooleanValue REPOS_ENABLED;
    private static final ModConfigSpec.BooleanValue FEUILLAGE_ENABLED;
    private static final ModConfigSpec.IntValue REPOS_REST_SECONDS;
    private static final ModConfigSpec.DoubleValue REPOS_BASE_MINUTES;
    private static final ModConfigSpec.DoubleValue REPOS_MINUTES_PER_COMFORT;
    private static final ModConfigSpec.BooleanValue TANNERIE_ENABLED;
    private static final ModConfigSpec.BooleanValue HOTES_VISITES;
    private static final ModConfigSpec.IntValue HOTES_INTERVALLE;
    private static final ModConfigSpec.IntValue HOTES_PREMIERE;
    private static final ModConfigSpec.IntValue HOTES_REFUS;
    private static final ModConfigSpec.IntValue TANNERIE_STROKES;
    private static final ModConfigSpec.DoubleValue TANNERIE_TANNING_MINUTES;
    private static final ModConfigSpec.IntValue TANNERIE_DRYING_POINTS;
    private static final ModConfigSpec.DoubleValue TANNERIE_BARK_CHANCE;
    private static final ModConfigSpec.DoubleValue TANNERIE_FAT_CHANCE;
    private static final ModConfigSpec.BooleanValue POSTES_TANNERIE;
    private static final ModConfigSpec.BooleanValue POSTES_FORGE;
    private static final ModConfigSpec.BooleanValue POSTES_FOYER;
    private static final ModConfigSpec.BooleanValue PATINE_ENABLED;
    private static final ModConfigSpec.IntValue PATINE_MINUTES;
    private static final ModConfigSpec.BooleanValue CLIMAT_ENABLED;
    private static final ModConfigSpec.DoubleValue CLIMAT_SEASON_STRENGTH;
    private static final ModConfigSpec.DoubleValue CLIMAT_SNOW_COLD;
    private static final ModConfigSpec.DoubleValue CLIMAT_SNOW_WET;
    private static final ModConfigSpec.DoubleValue CLIMAT_SAND_THIRST;
    private static final ModConfigSpec.BooleanValue CLIMAT_STORM_VISUALS;
    private static final ModConfigSpec.BooleanValue ECRAN_ENABLED;
    private static final ModConfigSpec.BooleanValue INDICES_ENABLED;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> INDICES_IN_BAR;
    private static final ModConfigSpec.IntValue ECRAN_IDLE_SECONDS;
    private static final ModConfigSpec.BooleanValue ECRAN_SHOW_WHILE_MOVING;
    private static final ModConfigSpec.ConfigValue<List<? extends String>> ECRAN_LAYERS;

    /** La barre d'action et ce qui l'entoure : couches du jeu, air du scaphandre de Create, température du corps de Cold Sweat. La soif de Thirst est dessinée dans la couche de la faim. */
    private static final List<String> ECRAN_DEFAULT_LAYERS = List.of(
            "minecraft:hotbar", "minecraft:jump_meter", "minecraft:experience_bar", "minecraft:player_health",
            "minecraft:armor_level", "minecraft:food_level", "minecraft:vehicle_health", "minecraft:air_level",
            "minecraft:selected_item_name", "minecraft:experience_level",
            "create:remaining_air", "cold_sweat:body_temp", "cold_sweat:vague_temp");

    /** Les relevés qui restent au-dessus de la barre d'inventaire : début de clé de traduction. */
    private static final List<String> INDICES_DEFAULT_IN_BAR = List.of(
            "record.nowPlaying", "mount.onboard", "create.mount.onboard", "create.track.", "create.train.relocate",
            "create.weighted_ejector.targeting", "create.factory_panel.click", "create.contraption.controls.",
            "tooltip.spelunkery.", "message.supplementaries.altimeter", "message.supplementaries.compass",
            "message.supplementaries.speedometer", "message.supplementaries.cannon.maneuver", "hud.bettercombat.",
            "item.throw", "camera_type.", "info.eclipticseasons.calendar", "title.longwings.",
            "spawn.mob_perching.onboard");

    static {
        ModConfigSpec.Builder client = new ModConfigSpec.Builder();
        client.comment("Module ciel (client) : il relie ATMOSPHERICS et Enhanced Celestials.").push("ciel");
        CIEL_ENABLED = client
                .comment("Interrupteur du module. Faux : Fallow ne touche plus au ciel.")
                .define("enabled", true);
        CIEL_REDRAW_MOON = client
                .comment("Redessiner la lune d'Enhanced Celestials (texture, couleur, taille) à la place de la lune",
                        "vanilla que dessine ATMOSPHERICS. Demande Enhanced Celestials.")
                .define("redrawMoon", true);
        CIEL_CLEAR_FOG_ON_BIOME_UPDATE = client
                .comment("Vider le cache de brume d'ATMOSPHERICS dès que le serveur change des biomes",
                        "(/fillbiome, conversion en clairière), pour que la brume réagisse sans que le joueur bouge.")
                .define("clearFogOnBiomeUpdate", true);
        CIEL_CLEARING_FADE = client
                .comment("Quand une clairière s'ouvre ou se referme, la brume s'éclaircit ou s'épaissit en douceur :",
                        "secondes pour passer d'une brume pleine à un ciel dégagé, ou l'inverse (le fondu ralentit en fin",
                        "de course ; la brume visible part dans les 4 ou 5 premières). 0 : fondu normal d'ATMOSPHERICS, presque immédiat.")
                .defineInRange("clearingFadeSeconds", 10.0, 0.0, 30.0);
        CIEL_MIST_WITHOUT_SHADER = client
                .comment("Sans pack de shaders, la brume d'ATMOSPHERICS devient celle du pack : à l'air libre sur la Surface,",
                        "elle finit à mistDistance (hors désert et badlands) et s'ouvre dans les clairières jusqu'à",
                        "clearingDistance ; la nuit, les deux distances tombent à 55 %. Sans effet sous Brumaire, qui a sa brume.")
                .define("mistWithoutShader", true);
        CIEL_MIST_DISTANCE = client
                .comment("Fin de la brume de jour hors des clairières, sans pack de shaders, en blocs.")
                .defineInRange("mistDistance", 128, 32, 512);
        CIEL_CLEARING_DISTANCE = client
                .comment("Fin de la brume de jour dans une clairière, sans pack de shaders, en blocs.")
                .defineInRange("clearingDistance", 480, 128, 1024);
        client.pop();

        client.comment("Module horizon (client) : le paysage lointain de Distant Horizons suit la brume du terrain.").push("horizon");
        HORIZON_ENABLED = client
                .comment("Interrupteur du module. Vrai : dans la brume, les LOD restent cachés ; là où elle se lève,",
                        "le paysage lointain apparaît. Faux : Distant Horizons garde le brouillard de sa config",
                        "(celle du pack cache tous les LOD).")
                .define("enabled", true);
        HORIZON_SKIP_HIDDEN_LODS = client
                .comment("Ne pas dessiner du tout les LOD quand la brume les cache entièrement (gain de performance).")
                .define("skipHiddenLods", true);
        HORIZON_RING_MAX_DISTANCE = client
                .comment("Rayon maximal du cercle de brume, en blocs : même là où la brume se lève, le paysage lointain",
                        "s'efface toujours dans la brume puis le ciel à cette distance (ou avant, si la brume du terrain",
                        "finit plus tôt : 500 blocs de jour en clairière, 260 la nuit).")
                .defineInRange("ringMaxDistance", 512, 64, 4096);
        client.pop();

        client.comment("Module rendu (client) : sous Iris, il donne au pack de shaders Brumaire les clairières, le sol et le vent du pack.").push("rendu");
        RENDU_ENABLED = client
                .comment("Interrupteur du module. Vrai : la brume du pack de shaders se lève dans les clairières et ses plantes",
                        "ondulent au vent du pack. Faux : le pack de shaders garde une brume uniforme et son vent de repli.",
                        "Sans effet sans shader, ou avec un autre pack de shaders.")
                .define("enabled", true);
        client.pop();

        client.comment("Module vent (client) : le vent du pack, que suivent la fumée des feux de camp et les plantes du pack de shaders.").push("vent");
        VENT_ENABLED = client
                .comment("Interrupteur de la fumée des feux de camp. Vrai : elle suit le vent, les feux voisins font un seul",
                        "panache, le feu des âmes bleuit sa fumée. Faux : la fumée du jeu.")
                .define("enabled", true);
        VENT_MOTES = client
                .comment("Sans pack de shaders, de fins brins pâles filent avec le vent à l'air libre, pour qu'on lise d'où il",
                        "souffle (il porte l'odeur du joueur vers les bêtes). Sans effet sous Brumaire, dont les herbes ondulent.")
                .define("motes", true);
        client.pop();

        client.comment("Module ecran (client) : l'interface s'efface quand on ne fait rien.").push("ecran");
        ECRAN_ENABLED = client
                .comment("Interrupteur du module. Vrai : après quelques secondes sans geste, la barre d'action, la vie, la faim,",
                        "l'armure et les jauges voisines s'effacent en fondu ; elles reviennent dès qu'on change d'objet, frappe,",
                        "utilise, prend un coup ou qu'une valeur bouge, et restent quand la vie ou la faim sont basses et sous l'eau.",
                        "Faux : l'interface du jeu, toujours affichée.")
                .define("enabled", true);
        ECRAN_IDLE_SECONDS = client
                .comment("Secondes sans geste avant que l'interface s'efface.")
                .defineInRange("idleSeconds", 6, 1, 120);
        ECRAN_SHOW_WHILE_MOVING = client
                .comment("Marcher, nager ou sauter garde aussi l'interface. Faux : elle s'efface même en marche, comme au repos.")
                .define("showWhileMoving", false);
        ECRAN_LAYERS = client
                .comment("Couches de l'interface qui s'effacent (noms NeoForge des couches). Le viseur, le chat, les titres,",
                        "les effets et les messages ne sont pas dans la liste et restent toujours visibles.")
                .defineListAllowEmpty("layers", ECRAN_DEFAULT_LAYERS, () -> "minecraft:hotbar",
                        o -> o instanceof String text && ResourceLocation.tryParse(text) != null);
        client.pop();

        client.comment("Module indices (client) : les messages de la barre d'action en marge.").push("indices");
        INDICES_ENABLED = client
                .comment("Interrupteur du module. Vrai : les messages qui s'affichaient au-dessus de la barre d'inventaire",
                        "(indices d'outils, approche, repos, tannerie, et ceux des autres mods) deviennent des notes à gauche de",
                        "l'écran, qui glissent puis s'estompent. Faux : la barre d'action du jeu.")
                .define("enabled", true);
        INDICES_IN_BAR = client
                .comment("Débuts de clés de traduction des relevés qui restent au-dessus de la barre d'inventaire, à côté de",
                        "l'objet ou de la monture dont ils parlent (altimètre, vitesse, disque qui joue, rail qu'on pose...).",
                        "S'y ajoutent les textes sans clé (souvent des nombres) et ceux qui reviennent trois fois en deux secondes.")
                .defineListAllowEmpty("inBar", INDICES_DEFAULT_IN_BAR, () -> "record.nowPlaying", o -> o instanceof String);
        client.pop();

        client.comment("Module climat (client) : ce qu'on voit des tempêtes.").push("climat");
        CLIMAT_STORM_VISUALS = client
                .comment("Vrai : dans une tempête de sable, la poussière vole avec le vent du pack et la brume se ferme et prend la",
                        "couleur du sable ; dans une tempête de neige, la neige est soufflée et la brume blanchit. Faux : rien de plus",
                        "que la neige du jeu (le froid et la soif restent).")
                .define("stormVisuals", true);
        client.pop();

        client.comment("Module postes (client) : les postes de la tannerie, de la forge et du foyer.").push("postes");
        POSTES_SOUND_VOLUME = client
                .comment("Volume des bruits des postes (gestes, arrivées, refus, carillons), de 0 (muets) à 1. Les bruits du bloc",
                        "lui-même, que les autres joueurs entendent aussi, n'en dépendent pas.")
                .defineInRange("soundVolume", 1.0, 0.0, 1.0);
        client.pop();

        CLIENT_SPEC = client.build();

        ModConfigSpec.Builder server = new ModConfigSpec.Builder();
        server.comment("Module xp : Fallow n'a plus d'expérience. Les orbes et les gains sont coupés par",
                "kubejs/server_scripts/sans_xp.js ; ce module règle l'enclume (la barre se règle dans fallow-common.toml).").push("xp");
        XP_ENABLED = server
                .comment("Interrupteur du module. Vrai : l'enclume ne demande plus de niveaux ; son coût devient une usure",
                        "(à 40, « Trop usé ! »). Faux : l'enclume vanilla, qui demande des niveaux.")
                .define("enabled", true);
        XP_BLOCK_ENCHANT_MERGE = server
                .comment("L'enclume refuse un second objet qui porte des enchantements (livre enchanté, objet enchanté) :",
                        "un enchantement ne vient que de l'éveil de l'objet (module eveil). Faux : l'enclume fusionne comme en vanilla.")
                .define("blockEnchantMerge", true);
        server.pop();

        server.comment("Module outils : à mains nues, on ramasse mais on ne coupe pas. Le bois, brut ou travaillé (tag fallow:outil_requis),",
                "et les blocs qui ne lâcheraient rien sans le bon outil (roches, minerais) ne se cassent plus sans lui.").push("outils");
        OUTILS_ENABLED = server
                .comment("Interrupteur du module. Faux : on casse de nouveau tout à la main, comme en vanilla.")
                .define("enabled", true);
        server.pop();

        server.comment("Module eveil : l'enchantement est un haut fait. Un objet compte ce qu'il a vécu (data/*/eveil/*.json),",
                "s'éveille passé un seuil et révèle à la table d'enchantement deux enchantements tirés de ses actes ; les grandes",
                "bêtes laissent un éclat ; à bout de durabilité, l'objet oublie son dernier éveil. Rien ne sort enchanté d'un butin",
                "ni d'un échange.").push("eveil");
        EVEIL_ENABLED = server
                .comment("Interrupteur du module. Faux : plus d'éveil ni d'éclat, les butins et les échanges gardent leurs",
                        "enchantements, la table d'enchantement redevient celle du jeu, rien n'est retiré des inventaires.")
                .define("enabled", true);
        EVEIL_SEUIL = server
                .comment("Points d'actes pour le premier éveil (une pierre creusée vaut 1, un minerai 3, un zombie vaincu 15).",
                        "2 000 : une pioche de fer bien entretenue s'éveille vers la fin de sa vie, une de diamant bien avant.")
                .defineInRange("seuil", 2000.0, 1.0, 1.0E7);
        EVEIL_FACTEUR = server
                .comment("Chaque éveil suivant demande ce facteur de plus que le précédent (3 : 2 000, 6 000, 18 000).")
                .defineInRange("facteur", 3.0, 1.0, 100.0);
        EVEIL_RACCOMMODAGE = server
                .comment("Raccommodage (sans expérience dans Fallow) : points de durabilité rendus par point d'acte, par niveau.",
                        "0,2 : une pioche Raccommodage I regagne un point tous les cinq blocs creusés.")
                .defineInRange("raccommodage", 0.2, 0.0, 10.0);
        EVEIL_ALERTE = server
                .comment("Part de la durabilité usée à partir de laquelle un objet éveillé prévient qu'il risque d'oublier.")
                .defineInRange("alerte", 0.9, 0.5, 1.0);
        EVEIL_POSTE = server
                .comment("La table d'enchantement s'ouvre en poste (avec Diegesis) : la caméra se penche sur la table, l'objet s'y pose,",
                        "le livre ouvert raconte sa vie et montre ses deux voies. Faux, ou accroupi : la fenêtre de choix d'avant.")
                .define("poste", true);
        server.comment("Nombre d'éveils qu'un objet peut porter, selon sa qualité de forge d'Overgeared (none : sans qualité).")
                .push("places");
        for (String[] q : new String[][] {{"none", "1"}, {"poor", "1"}, {"well", "1"}, {"expert", "2"}, {"perfect", "2"}, {"master", "3"}}) {
            EVEIL_PLACES.put(q[0], server.defineInRange(q[0], Integer.parseInt(q[1]), 0, 16));
        }
        server.pop();
        server.pop();
        SERVER_SPEC = server.build();

        ModConfigSpec.Builder common = new ModConfigSpec.Builder();
        common.comment("Module xp : la barre d'expérience (le reste du module est dans fallow-server.toml).").push("xp");
        XP_HIDE_BAR = common
                .comment("Cacher la barre d'expérience et le chiffre du niveau (client). La jauge de saut à cheval reste.")
                .define("hideBar", true);
        common.pop();

        common.comment("Modules clairiere et dissipateur : poser un feu de veille (rayon 16), une lanterne de clairière (32)",
                "ou faire tourner un dissipateur (64) lève la brume autour ; le retirer la rend. Le biome ne change pas :",
                "climat, saisons, cultures et faune restent ceux du lieu ; seuls les monstres n'y apparaissent plus.").push("clairiere");
        CLAIRIERE_ENABLED = common
                .comment("Interrupteur des clairières (serveur). Faux : les blocs restent, mais ne lèvent plus la brume.")
                .define("enabled", true);
        CLAIRIERE_RING_TICKS = common
                .comment("La clairière s'ouvre (et se referme) en vague, un anneau de 4 blocs tous les tant de ticks.",
                        "6 : une lanterne (32 blocs) en 2,7 secondes, un dissipateur (64) en 5. 1 : presque d'un coup.")
                .defineInRange("ringTicks", 6, 1, 40);
        common.pop();

        common.comment("Module bord : le monde est un disque. Près du cercle de data/fallow/monde/bord.json (centre et rayon, écrit",
                "par scripts/terrain/disc.py), un vent contraire repousse vers les terres le joueur, sa monture, son bateau et",
                "les navires de Sable, jusqu'à ne plus laisser avancer.").push("bord");
        BORD_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : plus de bord, on peut naviguer sans fin sur l'océan.")
                .define("enabled", true);
        common.pop();

        common.comment("Module navires : le vent de Fallow (celui de la fumée et des shaders) pousse les navires de Sable à ciel",
                "ouvert, selon la face qu'ils offrent au vent ; l'orage les secoue et la neige les alourdit. Le vent contraire du bord",
                "dépend du module bord. Le réglage sailWind (voilier de Fish 'N' Ships) vaut même sans Sable.").push("navires");
        NAVIRES_WIND_ENABLED = common
                .comment("Le vent pousse les navires (serveur). Faux : seuls leurs moteurs, hélices et voiles de Create les font avancer.")
                .define("wind", true);
        NAVIRES_WIND_STRENGTH = common
                .comment("Force du vent sur les navires. 1 : un dirigeable sans moteur dérive à peu près à la vitesse du vent ;",
                        "0,5 : moitié moins ; 2 : le double.")
                .defineInRange("windStrength", 1.0, 0.0, 4.0);
        NAVIRES_STORMS = common
                .comment("Sous la pluie et l'orage, des rafales frappent le haut des navires à ciel ouvert : ils dérivent par à-coups",
                        "et gîtent, plus fort en altitude (serveur).")
                .define("storms", true);
        NAVIRES_SNOW = common
                .comment("Quand il neige, de vraies couches de neige se posent sur ce que le navire a de plus haut et l'alourdissent ;",
                        "elles fondent à l'abri ou quand il fait doux (serveur).")
                .define("snow", true);
        NAVIRES_SNOW_MAX_LAYERS = common
                .comment("Couches de neige au plus par case de pont (8 : un bloc entier).")
                .defineInRange("snowMaxLayers", 4, 1, 8);
        NAVIRES_SAIL_WIND = common
                .comment("Le voilier de Fish 'N' Ships avance selon le vent de Fallow et son allure : rien face au vent, le plus",
                        "vent de travers, plus vite par grand vent. Faux : la voile pousse toujours pareil, comme sans Fallow.")
                .define("sailWind", true);
        common.pop();

        common.comment("Module bac : une corde tendue entre deux pieux au-dessus d'un fleuve, une poulie sur un radeau, et le",
                "radeau (devenu navire de Sable) traverse le long de la corde, porté par le courant pris de biais.").push("bac");
        BAC_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : les blocs restent, mais les cordes ne tiennent plus les radeaux.")
                .define("enabled", true);
        common.pop();

        common.comment("Module prise : pendant le combat de Starcatcher, une grosse prise tire le bateau du pêcheur (barque, ou",
                "navire de Sable), d'autant plus qu'elle est lourde et le bateau léger.").push("prise");
        PRISE_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : le bateau reste où il est pendant le combat, comme sans Fallow.")
                .define("enabled", true);
        common.pop();

        common.comment("Module zoo : les quatre tampons du guide d'Archie's Dinosaurs pour toutes les bêtes du monde (Apercevoir,",
                "Photographier, Apprivoiser ou héberger, Maîtriser), sans jamais tuer. Les tampons restent enregistrés quand le module est coupé.").push("zoo");
        ZOO_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : plus aucun tampon n'est donné.")
                .define("enabled", true);
        ZOO_SPOT_DISTANCE = common
                .comment("Distance en blocs à laquelle une bête en vue (champ de vision, sans obstacle) vaut le tampon Apercevoir.",
                        "25 : la règle des dinosaures d'Archie.")
                .defineInRange("spotDistance", 25, 4, 64);
        ZOO_OBSERVE_SECONDS = common
                .comment("Secondes d'observation (une bête de l'espèce en vue à moins de 16 blocs) demandées par le tampon Maîtriser,",
                        "en plus des autres tampons de l'espèce.")
                .defineInRange("observeSeconds", 60, 0, 600);
        ZOO_SPYGLASS_DISTANCE = common
                .comment("Distance en blocs à laquelle une bête vue dans la longue-vue (au centre de l'image) vaut le tampon Apercevoir.")
                .defineInRange("spyglassDistance", 64, 4, 128);
        common.pop();

        common.comment("Module approche : les bêtes farouches (tag fallow:farouches) sentent le joueur quand le vent porte son",
                "odeur vers elles, l'entendent quand il court et le voient quand il se tient debout à découvert ; elles lèvent",
                "la tête, puis fuient. Approcher contre le vent et accroupi permet de s'en approcher.").push("approche");
        APPROCHE_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : les bêtes ne se soucient plus du vent ni de la posture.")
                .define("enabled", true);
        APPROCHE_SCENT_RANGE = common
                .comment("Portée de l'odeur en blocs, sous le vent, par vent moyen. Le vent fort la porte jusqu'à 30 % plus loin,",
                        "la pluie l'abrège ; hors du cône de 70° autour du vent, aucune odeur.")
                .defineInRange("scentRange", 32.0, 0.0, 96.0);
        APPROCHE_SIGHT_RANGE = common
                .comment("Distance en blocs à laquelle une bête voit un joueur debout, sans obstacle. Accroupi : 45 % ;",
                        "en courant : 130 % ; dans l'ombre ou la nuit : 60 %.")
                .defineInRange("sightRange", 14.0, 0.0, 64.0);
        APPROCHE_HEARING_RANGE = common
                .comment("Distance en blocs à laquelle une bête entend un joueur qui marche. En courant : le double ; accroupi : 30 %.")
                .defineInRange("hearingRange", 6.0, 0.0, 32.0);
        APPROCHE_HINTS = common
                .comment("Vrai : quand une bête fuit, un mot dans la barre d'action dit ce qui a trahi le joueur (au plus une",
                        "fois toutes les 90 secondes).")
                .define("hints", true);
        common.pop();

        common.comment("Module repos : immobile ou assis près d'un feu allumé (5 blocs), sous un toit, le joueur se repose et",
                "gagne l'effet « Reposé » : un demi-cœur toutes les 6 secondes et un quart de faim en moins. L'effet dure",
                "d'autant plus que la maison est variée (familles de confort, tags fallow:confort/*, à 8 blocs).").push("repos");
        REPOS_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : plus de repos ; un effet en cours va à son terme sans réduire la faim.")
                .define("enabled", true);
        REPOS_REST_SECONDS = common
                .comment("Secondes de repos au foyer avant de gagner l'effet.")
                .defineInRange("restSeconds", 20, 0, 300);
        REPOS_BASE_MINUTES = common
                .comment("Durée de l'effet sans aucun confort, en minutes.")
                .defineInRange("baseMinutes", 2.0, 0.5, 60.0);
        REPOS_MINUTES_PER_COMFORT = common
                .comment("Minutes ajoutées par famille de confort trouvée (lit, siège, table, tapis, rangement, livres, lumière,",
                        "décoration, cuisine). Avec les 9 : 2 + 9 × 1,5 = 15,5 minutes.")
                .defineInRange("minutesPerComfort", 1.5, 0.0, 10.0);
        common.pop();

        common.comment("Module feuillage (avec Passable Foliage) : une chute dans un feuillage garde la moitié des dégâts",
                "qu'elle ferait au sol.").push("feuillage");
        FEUILLAGE_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : la chute dans les feuilles suit le seul réglage de Passable",
                        "Foliage, que le pack règle à aucun dégât (fallDamageMultiplier dans passablefoliage-common.yaml).")
                .define("enabled", true);
        common.pop();

        common.comment("Module tannerie : la peau brute se racle au couteau sur le chevalet, trempe dans la cuve à tan (eau et",
                "écorce), puis sèche sur le séchoir avant de donner du cuir. Le séchage dépend du ciel, de la pluie, du froid et",
                "d'un feu proche (tag fallow:foyers).").push("tannerie");
        TANNERIE_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : plus d'écorce à la hache ; les blocs restent mais ne travaillent plus.")
                .define("enabled", true);
        TANNERIE_STROKES = common
                .comment("Coups de couteau pour racler une peau sur le chevalet.")
                .defineInRange("strokes", 6, 1, 15);
        TANNERIE_TANNING_MINUTES = common
                .comment("Minutes de trempage dans la cuve à tan, pour toutes les peaux de la cuve à la fois.")
                .defineInRange("tanningMinutes", 5.0, 0.5, 120.0);
        TANNERIE_DRYING_POINTS = common
                .comment("Points de séchage pour qu'un cuir soit sec. Le séchoir en gagne toutes les 20 secondes : 2 au soleil,",
                        "1 à l'ombre ou la nuit, 2 de plus près d'un feu, 1 de moins par temps de neige ; la pluie le mouille (−1).",
                        "Avec 15 : 2 min 40 au soleil, 5 minutes sous un toit, 1 min 40 sous un toit près d'un feu.")
                .defineInRange("dryingPoints", 15, 1, 15);
        TANNERIE_BARK_CHANCE = common
                .comment("Chance qu'une bûche écorcée à la hache donne un morceau d'écorce (celle de Farmer's Delight).")
                .defineInRange("barkChance", 0.5, 0.0, 1.0);
        TANNERIE_FAT_CHANCE = common
                .comment("Chance qu'une peau raclée laisse un morceau de gras (celui de Naturalist).")
                .defineInRange("fatChance", 0.33, 0.0, 1.0);
        common.pop();

        common.comment("Module postes : les blocs de Fallow travaillés à la main, sur place, comme ceux de Hands-On Delight et",
                "Hands-On Vanilla (bibliothèque Diegesis) : la caméra vient au bloc, on y pose ce qu'on prend dans l'inventaire",
                "et le livre de recettes est à côté. Accroupi, le bloc garde son usage d'avant.").push("postes");
        POSTES_TANNERIE = common
                .comment("Le chevalet, la cuve à tan et le séchoir s'ouvrent en postes. Faux : on y travaille au clic, comme avant.")
                .define("tannerie", true);
        POSTES_FORGE = common
                .comment("Les enclumes de forge d'Overgeared s'ouvrent en postes : les pièces, le marteau et le plan se posent sur",
                        "l'enclume, le livre de forge à côté ; on frappe toujours accroupi, le marteau en main. Faux : la fenêtre d'Overgeared.",
                        "Lu par le jeu de chaque joueur (son propre fichier), pas imposé par le serveur.")
                .define("forge", true);
        POSTES_FOYER = common
                .comment("Le foyer de Cold Sweat s'ouvre en poste : le combustible se pose devant la grille, ses deux jauges (chaud,",
                        "froid) sur la façade. Un combustible en main se jette toujours au foyer d'un clic. Faux : la fenêtre de Cold Sweat.",
                        "Lu par le jeu de chaque joueur (son propre fichier), pas imposé par le serveur.")
                .define("foyer", true);
        common.pop();

        common.comment("Module patine : les armures portées se salissent (marche, terre, combat) et se lavent dans l'eau ; la pluie",
                "les rince lentement. La saleté reste sur l'objet.").push("patine");
        PATINE_ENABLED = common
                .comment("Interrupteur du module. Faux : les armures ne se salissent plus et s'affichent propres.")
                .define("enabled", true);
        PATINE_MINUTES = common
                .comment("Minutes d'activité (marche, sans boue ni combat) pour qu'une cuirasse soit couverte de boue.",
                        "Les bottes vont plus vite, le casque plus lentement.")
                .defineInRange("minutesToFull", 40, 5, 600);
        common.pop();

        common.comment("Module climat : le climat pèse sur le corps (Cold Sweat). La saison d'Ecliptic Seasons réchauffe ou refroidit",
                "chaque biome comme Ecliptic le dit pour ses cultures et sa neige : l'hiver mord, l'été écrase.").push("climat");
        CLIMAT_ENABLED = common
                .comment("Interrupteur du module (serveur). Faux : la saison ne change plus la température ressentie.")
                .define("enabled", true);
        CLIMAT_SEASON_STRENGTH = common
                .comment("Force de la saison : l'écart de température d'Ecliptic (en unités de biome, -0,45 au cœur de l'hiver,",
                        "+0,25 au cœur de l'été dans un biome tempéré) multiplié par ce nombre, puis compté en unités de Cold Sweat",
                        "(1 = 25 °C). 1,5 : l'hiver retire environ 15 à 17 °C, l'été ajoute environ 9 °C. 0 : aucune saison.")
                .defineInRange("seasonStrength", 1.5, 0.0, 4.0);
        CLIMAT_SNOW_COLD = common
                .comment("Froid de la neige qui tombe sur soi, à découvert, en unités de Cold Sweat (1 = 25 °C). 0,12 : environ 3 °C",
                        "sous une neige simple ; deux fois et demie plus (environ 7,5 °C) sous une tempête de neige (neige et orage).")
                .defineInRange("snowCold", 0.12, 0.0, 1.0);
        CLIMAT_SNOW_WET = common
                .comment("Froid de la neige posée sur les vêtements quand on en est couvert (elle se pose en une minute et demie",
                        "de neige, une demi-minute de tempête, et fond à l'abri). 0,2 : environ 5 °C.")
                .defineInRange("snowWet", 0.2, 0.0, 1.0);
        CLIMAT_SAND_THIRST = common
                .comment("Fatigue de soif (Thirst Was Reclaimed) ajoutée chaque seconde à découvert dans une tempête de sable sous",
                        "l'orage, 60 % sans orage. 4 de fatigue font une goutte : 0,15, une goutte toutes les 27 secondes en plus de la",
                        "soif ordinaire. 0 : le sable n'assoiffe pas.")
                .defineInRange("sandThirst", 0.15, 0.0, 4.0);
        common.pop();
        common.comment("Module hotes : les six peuples masqués. Un signe au bord d'une clairière annonce une visite pour le",
                "lendemain soir ; l'émissaire attend à la lisière avec une natte d'offrande jusqu'à l'aube, et répond aux dons",
                "par un contre-don. Le lien avec chaque peuple se tient par clairière.").push("hotes");
        HOTES_VISITES = common
                .comment("Vrai : les peuples viennent d'eux-mêmes aux clairières habitées. Faux : seulement par commande.")
                .define("visites", true);
        HOTES_INTERVALLE = common
                .comment("Jours de jeu entre deux visites d'un même peuple à une clairière, pour un peuple encore inconnu. Un jour de",
                        "moins à chaque palier du lien et pendant la saison du peuple (jamais moins de deux).")
                .defineInRange("intervalle", 5, 2, 30);
        HOTES_PREMIERE = common
                .comment("Jours passés dans une clairière avant le premier signe d'un peuple.")
                .defineInRange("premiereVisite", 2, 0, 30);
        HOTES_REFUS = common
                .comment("Jours entre deux refus polis (« Ne rien laisser ») sans vexer le peuple : environ une saison d'Ecliptic.")
                .defineInRange("refusPoli", 24, 1, 120);
        common.pop();

        COMMON_SPEC = common.build();
    }

    private FallowConfig() {
    }

    public static boolean cielEnabled() {
        return read(CLIENT_SPEC, CIEL_ENABLED);
    }

    public static boolean cielRedrawMoon() {
        return cielEnabled() && read(CLIENT_SPEC, CIEL_REDRAW_MOON);
    }

    public static boolean cielClearFogOnBiomeUpdate() {
        return cielEnabled() && read(CLIENT_SPEC, CIEL_CLEAR_FOG_ON_BIOME_UPDATE);
    }

    /** Constante de temps du fondu de brume après un changement de biomes, en secondes ; 0 si coupé. */
    public static float cielClearingFadeSeconds() {
        return cielEnabled() ? (float) read(CLIENT_SPEC, CIEL_CLEARING_FADE) : 0.0F;
    }

    public static boolean cielMistWithoutShader() {
        return cielEnabled() && read(CLIENT_SPEC, CIEL_MIST_WITHOUT_SHADER);
    }

    public static float cielMistDistance() {
        return read(CLIENT_SPEC, CIEL_MIST_DISTANCE);
    }

    public static float cielClearingDistance() {
        return read(CLIENT_SPEC, CIEL_CLEARING_DISTANCE);
    }

    public static boolean horizonEnabled() {
        return read(CLIENT_SPEC, HORIZON_ENABLED);
    }

    public static boolean horizonSkipHiddenLods() {
        return horizonEnabled() && read(CLIENT_SPEC, HORIZON_SKIP_HIDDEN_LODS);
    }

    public static int horizonRingMaxDistance() {
        return read(CLIENT_SPEC, HORIZON_RING_MAX_DISTANCE);
    }


    public static boolean ventEnabled() {
        return read(CLIENT_SPEC, VENT_ENABLED);
    }

    public static boolean ventMotes() {
        return read(CLIENT_SPEC, VENT_MOTES);
    }

    public static boolean renduEnabled() {
        return read(CLIENT_SPEC, RENDU_ENABLED);
    }

    public static boolean xpEnabled() {
        return read(SERVER_SPEC, XP_ENABLED);
    }

    public static boolean xpHideBar() {
        return xpEnabled() && read(COMMON_SPEC, XP_HIDE_BAR);
    }

    public static boolean eveilEnabled() {
        return read(SERVER_SPEC, EVEIL_ENABLED);
    }

    public static double eveilSeuil() {
        return SERVER_SPEC.isLoaded() ? EVEIL_SEUIL.get() : EVEIL_SEUIL.getDefault();
    }

    public static double eveilFacteur() {
        return SERVER_SPEC.isLoaded() ? EVEIL_FACTEUR.get() : EVEIL_FACTEUR.getDefault();
    }

    public static double eveilRaccommodage() {
        return SERVER_SPEC.isLoaded() ? EVEIL_RACCOMMODAGE.get() : EVEIL_RACCOMMODAGE.getDefault();
    }

    public static boolean eveilPoste() {
        return read(SERVER_SPEC, EVEIL_POSTE);
    }

    public static double eveilAlerte() {
        return SERVER_SPEC.isLoaded() ? EVEIL_ALERTE.get() : EVEIL_ALERTE.getDefault();
    }

    /** Nombre d'éveils pour une qualité de forge ({@code none} si l'objet n'en a pas ou si elle est inconnue). */
    public static int eveilPlaces(String qualite) {
        ModConfigSpec.IntValue value = EVEIL_PLACES.getOrDefault(qualite, EVEIL_PLACES.get("none"));
        return SERVER_SPEC.isLoaded() ? value.get() : value.getDefault();
    }

    public static boolean xpBlockEnchantMerge() {
        return xpEnabled() && read(SERVER_SPEC, XP_BLOCK_ENCHANT_MERGE);
    }

    public static boolean clairiereEnabled() {
        return read(COMMON_SPEC, CLAIRIERE_ENABLED);
    }

    public static int clairiereRingTicks() {
        return read(COMMON_SPEC, CLAIRIERE_RING_TICKS);
    }

    public static boolean outilsEnabled() {
        return read(SERVER_SPEC, OUTILS_ENABLED);
    }

    public static boolean bordEnabled() {
        return read(COMMON_SPEC, BORD_ENABLED);
    }

    public static boolean naviresWindEnabled() {
        return read(COMMON_SPEC, NAVIRES_WIND_ENABLED);
    }

    public static double naviresWindStrength() {
        return read(COMMON_SPEC, NAVIRES_WIND_STRENGTH);
    }

    public static boolean naviresStorms() {
        return read(COMMON_SPEC, NAVIRES_STORMS);
    }

    public static boolean naviresSnow() {
        return read(COMMON_SPEC, NAVIRES_SNOW);
    }

    public static int naviresSnowMaxLayers() {
        return read(COMMON_SPEC, NAVIRES_SNOW_MAX_LAYERS);
    }

    public static boolean bacEnabled() {
        return read(COMMON_SPEC, BAC_ENABLED);
    }

    public static boolean priseEnabled() {
        return read(COMMON_SPEC, PRISE_ENABLED);
    }

    public static boolean naviresSailWind() {
        return read(COMMON_SPEC, NAVIRES_SAIL_WIND);
    }

    public static boolean zooEnabled() {
        return read(COMMON_SPEC, ZOO_ENABLED);
    }

    public static int zooSpotDistance() {
        return read(COMMON_SPEC, ZOO_SPOT_DISTANCE);
    }

    public static boolean patineEnabled() {
        return read(COMMON_SPEC, PATINE_ENABLED);
    }

    public static int patineMinutesToFull() {
        return read(COMMON_SPEC, PATINE_MINUTES);
    }

    public static int zooObserveSeconds() {
        return read(COMMON_SPEC, ZOO_OBSERVE_SECONDS);
    }

    public static int zooSpyglassDistance() {
        return read(COMMON_SPEC, ZOO_SPYGLASS_DISTANCE);
    }

    public static boolean approcheEnabled() {
        return read(COMMON_SPEC, APPROCHE_ENABLED);
    }

    public static double approcheScentRange() {
        return read(COMMON_SPEC, APPROCHE_SCENT_RANGE);
    }

    public static double approcheSightRange() {
        return read(COMMON_SPEC, APPROCHE_SIGHT_RANGE);
    }

    public static double approcheHearingRange() {
        return read(COMMON_SPEC, APPROCHE_HEARING_RANGE);
    }

    public static boolean approcheHints() {
        return read(COMMON_SPEC, APPROCHE_HINTS);
    }

    public static boolean feuillageEnabled() {
        return read(COMMON_SPEC, FEUILLAGE_ENABLED);
    }

    public static boolean reposEnabled() {
        return read(COMMON_SPEC, REPOS_ENABLED);
    }

    public static int reposRestSeconds() {
        return read(COMMON_SPEC, REPOS_REST_SECONDS);
    }

    public static double reposBaseMinutes() {
        return read(COMMON_SPEC, REPOS_BASE_MINUTES);
    }

    public static double reposMinutesPerComfort() {
        return read(COMMON_SPEC, REPOS_MINUTES_PER_COMFORT);
    }

    public static boolean hotesVisites() {
        return read(COMMON_SPEC, HOTES_VISITES);
    }

    public static int hotesIntervalle() {
        return read(COMMON_SPEC, HOTES_INTERVALLE);
    }

    public static int hotesPremiereVisite() {
        return read(COMMON_SPEC, HOTES_PREMIERE);
    }

    public static int hotesRefusPoli() {
        return read(COMMON_SPEC, HOTES_REFUS);
    }

    public static boolean tannerieEnabled() {
        return read(COMMON_SPEC, TANNERIE_ENABLED);
    }

    public static boolean postesTannerie() {
        return read(COMMON_SPEC, POSTES_TANNERIE);
    }

    public static boolean postesForge() {
        return read(COMMON_SPEC, POSTES_FORGE);
    }

    public static boolean postesFoyer() {
        return read(COMMON_SPEC, POSTES_FOYER);
    }

    public static int tannerieStrokes() {
        return read(COMMON_SPEC, TANNERIE_STROKES);
    }

    public static double tannerieTanningMinutes() {
        return read(COMMON_SPEC, TANNERIE_TANNING_MINUTES);
    }

    public static int tannerieDryingPoints() {
        return read(COMMON_SPEC, TANNERIE_DRYING_POINTS);
    }

    public static double tannerieBarkChance() {
        return read(COMMON_SPEC, TANNERIE_BARK_CHANCE);
    }

    public static double tannerieFatChance() {
        return read(COMMON_SPEC, TANNERIE_FAT_CHANCE);
    }

    public static boolean climatEnabled() {
        return read(COMMON_SPEC, CLIMAT_ENABLED);
    }

    public static double climatSeasonStrength() {
        return climatEnabled() ? read(COMMON_SPEC, CLIMAT_SEASON_STRENGTH) : 0.0;
    }

    public static double climatSnowCold() {
        return read(COMMON_SPEC, CLIMAT_SNOW_COLD);
    }

    public static double climatSnowWet() {
        return read(COMMON_SPEC, CLIMAT_SNOW_WET);
    }

    public static double climatSandThirst() {
        return read(COMMON_SPEC, CLIMAT_SAND_THIRST);
    }

    public static boolean climatStormVisuals() {
        return read(CLIENT_SPEC, CLIMAT_STORM_VISUALS);
    }

    public static boolean indicesEnabled() {
        return read(CLIENT_SPEC, INDICES_ENABLED);
    }

    public static List<? extends String> indicesInBar() {
        return CLIENT_SPEC.isLoaded() ? INDICES_IN_BAR.get() : INDICES_DEFAULT_IN_BAR;
    }

    public static boolean ecranEnabled() {
        return read(CLIENT_SPEC, ECRAN_ENABLED);
    }

    public static int ecranIdleSeconds() {
        return read(CLIENT_SPEC, ECRAN_IDLE_SECONDS);
    }

    public static boolean ecranShowWhileMoving() {
        return read(CLIENT_SPEC, ECRAN_SHOW_WHILE_MOVING);
    }

    public static List<? extends String> ecranLayers() {
        return CLIENT_SPEC.isLoaded() ? ECRAN_LAYERS.get() : ECRAN_DEFAULT_LAYERS;
    }

    /** Lit une valeur sans planter si la config n'est pas encore chargée. */
    public static float postesSoundVolume() {
        return (float) read(CLIENT_SPEC, POSTES_SOUND_VOLUME);
    }

    static boolean read(ModConfigSpec spec, ModConfigSpec.BooleanValue value) {
        return spec.isLoaded() ? value.get() : value.getDefault();
    }

    static int read(ModConfigSpec spec, ModConfigSpec.IntValue value) {
        return spec.isLoaded() ? value.get() : value.getDefault();
    }

    static double read(ModConfigSpec spec, ModConfigSpec.DoubleValue value) {
        return spec.isLoaded() ? value.get() : value.getDefault();
    }
}
