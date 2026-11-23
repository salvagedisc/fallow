package io.github.salvagedisc.fallow;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;

/**
 * Les modules de Fallow. Un module ne démarre que si tous les mods qu'il relie sont présents ;
 * sa classe n'est jamais chargée sinon, et ses mixins sont écartés par {@link io.github.salvagedisc.fallow.mixin.FallowMixinPlugin}.
 * L'interrupteur de chaque module est un booléen de {@link FallowConfig}, lu à chaque usage.
 */
public enum FallowModule {
    /** La lune d'Enhanced Celestials et la brume sous ATMOSPHERICS (client). La lune demande aussi Enhanced Celestials. */
    CIEL("ciel", Dist.CLIENT, bus -> io.github.salvagedisc.fallow.ciel.CielModule.start(bus), ModIds.ATMOSPHERICS),
    /** Plus d'expérience : l'enclume sans niveaux, la barre cachée (client et serveur). Aucun mod requis. */
    XP("xp", null, bus -> io.github.salvagedisc.fallow.xp.XpModule.start(bus)),
    /** Le paysage lointain de Distant Horizons suit la brume : caché dans la brume, visible là où elle se lève (client et serveur). */
    HORIZON("horizon", null, bus -> io.github.salvagedisc.fallow.horizon.HorizonModule.start(bus), ModIds.DISTANT_HORIZONS),
    /** La brume se lève autour du feu de veille et de la lanterne de clairière, sans changer le biome (client et serveur). Aucun mod requis. */
    CLAIRIERE("clairiere", null, bus -> io.github.salvagedisc.fallow.clairiere.ClairiereModule.start(bus)),
    /** Le dissipateur, troisième palier des clairières : un bloc cinétique de Create (client et serveur). */
    DISSIPATEUR("dissipateur", null, bus -> io.github.salvagedisc.fallow.dissipateur.DissipateurModule.start(bus), ModIds.CREATE),
    /** À mains nues, on ramasse mais on ne coupe pas : le bois et la roche demandent le bon outil (client et serveur). Aucun mod requis. */
    OUTILS("outils", null, bus -> io.github.salvagedisc.fallow.outils.OutilsModule.start(bus)),
    /** Les monuments d'Ancient Remnants posés seulement sur un terrain qui s'y prête : type de structure
     *  {@code fallow:terrain_checked} (client et serveur). Aucun mod requis. */
    MONUMENTS("monuments", null, bus -> io.github.salvagedisc.fallow.monuments.MonumentsModule.start(bus)),
    /** Le bord du monde : un cercle donné par {@code data/fallow/monde/bord.json}, où un vent contraire repousse vers les
     *  terres (client et serveur ; tout se passe sur le serveur). Aucun mod requis. */
    BORD("bord", null, bus -> io.github.salvagedisc.fallow.bord.BordModule.start(bus)),
    /** Les navires de Sable poussés par le vent du pack, et repoussés par celui du bord (client et serveur ; tout se passe
     *  sur le serveur). Demande Sable. */
    NAVIRES("navires", null, bus -> io.github.salvagedisc.fallow.navires.NaviresModule.start(bus), ModIds.SABLE),
    /** Le bac à traille : pieux, corde et poulie, et le radeau qui traverse le long de la corde (client et serveur :
     *  blocs enregistrés des deux côtés, la corde dessinée au client). Demande Sable. */
    BAC("bac", null, bus -> io.github.salvagedisc.fallow.bac.BacModule.start(bus), ModIds.SABLE),
    /** La grosse prise de Starcatcher qui tire le bateau pendant le combat : barques, et navires de Sable si Sable est là
     *  (client et serveur : le client de celui qui mène une barque la fait avancer). */
    PRISE("prise", null, bus -> io.github.salvagedisc.fallow.prise.PriseModule.start(bus), ModIds.STARCATCHER),
    /** L'interface qui s'efface quand on ne fait rien (client). Aucun mod requis. */
    ECRAN("ecran", Dist.CLIENT, bus -> io.github.salvagedisc.fallow.ecran.EcranModule.start(bus)),
    /** Les messages de la barre d'action en notes discrètes, en marge à gauche (client). Aucun mod requis. */
    INDICES("indices", Dist.CLIENT, bus -> io.github.salvagedisc.fallow.indices.IndicesModule.start(bus)),
    /** Le vent du pack : direction, force, rafales, et la fumée des feux de camp qui le suit (client). Aucun mod requis. */
    VENT("vent", Dist.CLIENT, bus -> io.github.salvagedisc.fallow.vent.VentModule.start(bus)),
    /** Les clairières et le vent du pack donnés au pack de shaders de Fallow, sous Iris (client). */
    RENDU("rendu", Dist.CLIENT, bus -> io.github.salvagedisc.fallow.rendu.RenduModule.start(bus), ModIds.IRIS),
    /** Le zoo : les quatre tampons du guide d'Archie's Dinosaurs pour toutes les bêtes (client et serveur). Le suivi
     *  ne demande aucun mod ; les pages du guide demandent Archie's Dinosaurs, la photo demande Exposure. */
    ZOO("zoo", null, bus -> io.github.salvagedisc.fallow.zoo.ZooModule.start(bus)),
    /** La patine : les armures se salissent quand on les porte et se lavent dans l'eau (client et serveur). Aucun mod requis. */
    PATINE("patine", null, bus -> io.github.salvagedisc.fallow.patine.PatineModule.start(bus)),
    /** L'acier noir, palier entre l'acier d'Overgeared et le diamant : objets, armure et palier d'outil (client et
     *  serveur). Aucun mod requis pour l'enregistrement ; ses recettes et ses tags sont des données du pack. */
    ACIER_NOIR("acier_noir", null, bus -> io.github.salvagedisc.fallow.acier.AcierNoirModule.start(bus)),
    /** Le minage : une pioche bien forgée d'Overgeared rend plus de minerai (serveur). Aucun mod requis ; sans Overgeared, rien ne change. */
    MINAGE("minage", null, bus -> io.github.salvagedisc.fallow.minage.MinageModule.start(bus)),
    /** Les filons : de vrais gisements liés au biome, avec des affleurements en surface (serveur ; le type est
     *  enregistré des deux côtés). Aucun mod requis ; les échantillons de surface viennent de Surface Samples, nommés par les données. */
    FILONS("filons", null, bus -> io.github.salvagedisc.fallow.filons.FilonsModule.start(bus)),
    /** Le climat qui pèse : la saison d'Ecliptic Seasons donnée à la température de Cold Sweat (serveur ; le monde
     *  intégré du client compte comme serveur). */
    CLIMAT("climat", null, bus -> io.github.salvagedisc.fallow.climat.ClimatModule.start(bus), ModIds.COLD_SWEAT, ModIds.ECLIPTIC_SEASONS),
    /** L'approche : les bêtes farouches sentent le joueur quand le vent porte son odeur, et fuient (serveur ; le monde
     *  intégré du client compte comme serveur). Aucun mod requis ; le vent est celui du module vent, recalculé. */
    APPROCHE("approche", null, bus -> io.github.salvagedisc.fallow.approche.ApprocheModule.start(bus)),
    /** Le repos au foyer : l'effet « Reposé » près d'un feu, sous un toit, selon le confort de la maison (client et
     *  serveur : l'effet est enregistré des deux côtés). Aucun mod requis. */
    REPOS("repos", null, bus -> io.github.salvagedisc.fallow.repos.ReposModule.start(bus)),
    /** La tannerie : chevalet, cuve à tan et séchoir, de la peau brute au cuir (client et serveur : blocs et objets
     *  enregistrés des deux côtés). Aucun mod requis ; l'écorce est celle de Farmer's Delight, le gras celui de Naturalist. */
    TANNERIE("tannerie", null, bus -> io.github.salvagedisc.fallow.tannerie.TannerieModule.start(bus)),
    /** Le glanage : secouer des feuilles main vide fait parfois tomber un bâton (client et serveur : le geste est prédit
     *  côté client). Aucun mod requis ; les cailloux trouvés en creusant sont du butin réglé par le pack. */
    GLANAGE("glanage", null, bus -> io.github.salvagedisc.fallow.glanage.GlanageModule.start(bus)),
    /** Le feuillage : tomber dans les feuilles que Passable Foliage laisse traverser garde la moitié des dégâts de la
     *  chute (serveur). Demande Passable Foliage. */
    FEUILLAGE("feuillage", null, bus -> io.github.salvagedisc.fallow.feuillage.FeuillageModule.start(bus), ModIds.PASSABLE_FOLIAGE),
    /** Les hôtes (pilote) : un peuple de la brume au modèle du joueur, vêtu de l'armure de sa bête (client et serveur). */
    HOTES("hotes", null, bus -> io.github.salvagedisc.fallow.hotes.HotesModule.start(bus)),
    /** Les postes : les blocs de la tannerie travaillés à la main sur place, caméra au bloc et livre de recettes à côté, sur
     *  le modèle de Hands-On Delight et Hands-On Vanilla (client et serveur). Demande Diegesis. */
    /** L'éveil : l'enchantement vient des hauts faits de l'objet et des éclats des grandes bêtes ; rien ne sort enchanté
     *  d'un butin ni d'un échange (client et serveur : composants et éclat enregistrés des deux côtés). Aucun mod requis ;
     *  la qualité de forge d'Overgeared et TreeChop sont lus s'ils sont là. */
    EVEIL("eveil", null, bus -> io.github.salvagedisc.fallow.eveil.EveilModule.start(bus)),
    POSTES("postes", null, bus -> io.github.salvagedisc.fallow.postes.PostesModule.start(bus), ModIds.DIEGESIS);

    private final String id;
    private final Dist side;
    private final List<String> requiredMods;
    private final Consumer<IEventBus> starter;

    FallowModule(String id, Dist side, Consumer<IEventBus> starter, String... requiredMods) {
        this.id = id;
        this.side = side;
        this.requiredMods = List.of(requiredMods);
        this.starter = starter;
    }

    public String id() {
        return id;
    }

    public List<String> requiredMods() {
        return requiredMods;
    }

    /** Le module tourne-t-il de ce côté ? ({@code null} : client et serveur.) */
    public boolean runsOn(Dist dist) {
        return side == null || side == dist;
    }

    public boolean isAvailable() {
        return requiredMods.stream().allMatch(ModIds::isLoaded);
    }

    public List<String> missingMods() {
        return requiredMods.stream().filter(m -> !ModIds.isLoaded(m)).toList();
    }

    void start(IEventBus modBus) {
        starter.accept(modBus);
    }

    @Override
    public String toString() {
        return id + Arrays.toString(requiredMods.toArray());
    }
}
