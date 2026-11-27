package io.github.salvagedisc.fallow.postes.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.MenuBinding;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;

import net.stirdrem.overgeared.screen.AlloySmelterMenu;
import net.stirdrem.overgeared.screen.CastingSmelterMenu;
import net.stirdrem.overgeared.screen.NetherAlloySmelterMenu;

/**
 * Les fours d'Overgeared en postes, comme les fourneaux de Hands-On Vanilla : le poste tient la fenêtre d'Overgeared (son menu,
 * côté serveur rien ne change) et en pose les cases sur le four. Les métaux se posent à plat sur le dessus, comme dans le creuset
 * qu'il chauffe ; l'ouvrage se tient debout à côté d'eux ; le combustible devant le foyer, au sol. La jauge de cuisson est sur la
 * façade, entre les bouches et le foyer. Enregistrés sur le client seul.
 */
final class PostesFours {
    static final ResourceLocation ALLIAGE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "four_alliage");
    static final ResourceLocation ALLIAGE_NETHER = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "four_alliage_nether");
    static final ResourceLocation COULEE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "four_coulee");

    static final String ENTREE = "entree";
    static final String MOULE = "moule";
    static final String COMBUSTIBLE = "combustible";
    static final String RESULTAT = "resultat";

    /** Les menus d'Overgeared : NeoForge les ouvre avec la position du four en plus ({@link MenusAvecDonnees}). */
    static final ResourceLocation MENU_ALLIAGE = og("alloy_smelter_menu");
    static final ResourceLocation MENU_ALLIAGE_NETHER = og("nether_alloy_smelter_menu");
    static final ResourceLocation MENU_COULEE = og("casting_smelter_menu");

    /** Le rouge sombre puis l'orange du métal qui chauffe. */
    private static final int CHAUFFE = 0x8A2A10;
    private static final int FONDU = 0xFFB040;

    private PostesFours() {
    }

    private static ResourceLocation og(String path) {
        return ResourceLocation.fromNamespaceAndPath("overgeared", path);
    }

    static void register() {
        // Par nom : ni les blocs ni les menus d'Overgeared ne sont enregistrés quand le poste l'est. Diegesis veut un type de
        // menu en main : celui d'un coffre d'une rangée, qu'un four n'ouvre jamais ; le vrai est accepté en plus.
        Diegesis.station(ALLIAGE)
                .block(() -> BuiltInRegistries.BLOCK.get(og("alloy_furnace")))
                .orientation(StationType.Orientation.BLOCK_OPPOSITE)
                .reach(4.5)
                .schema(StationSchema.builder().slots(ENTREE, 4).slots(COMBUSTIBLE, 1).slots(RESULTAT, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_ALLIAGE).slots(ENTREE, 0, 4).slots(COMBUSTIBLE, 4, 1)
                        .slots(RESULTAT, 5, 1).build())
                .openWhen(PostesFours::ouvre)
                .register();
        Diegesis.station(ALLIAGE_NETHER)
                .block(() -> BuiltInRegistries.BLOCK.get(og("nether_alloy_furnace")))
                .orientation(StationType.Orientation.BLOCK_OPPOSITE)
                .reach(4.5)
                .schema(StationSchema.builder().slots(ENTREE, 9).slots(COMBUSTIBLE, 1).slots(RESULTAT, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_ALLIAGE_NETHER).slots(ENTREE, 0, 9).slots(COMBUSTIBLE, 9, 1)
                        .slots(RESULTAT, 10, 1).build())
                .openWhen(PostesFours::ouvre)
                .register();
        Diegesis.station(COULEE)
                .block(() -> BuiltInRegistries.BLOCK.get(og("casting_furnace")))
                .orientation(StationType.Orientation.BLOCK_OPPOSITE)
                .reach(4.5)
                .schema(StationSchema.builder().slots(ENTREE, 1).slots(COMBUSTIBLE, 1).slots(RESULTAT, 1).slots(MOULE, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_COULEE).slots(ENTREE, 0, 1).slots(COMBUSTIBLE, 1, 1)
                        .slots(RESULTAT, 2, 1).slots(MOULE, 3, 1).build())
                .openWhen(PostesFours::ouvre)
                .register();
        DiegesisClient.registerBehavior(ALLIAGE, view -> new PosteDeMod(cases(4, false), 6, PostesFours::cuisson, CHAUFFE, FONDU));
        DiegesisClient.registerBehavior(ALLIAGE_NETHER, view -> new PosteDeMod(cases(9, false), 11, PostesFours::cuisson, CHAUFFE, FONDU));
        DiegesisClient.registerBehavior(COULEE, view -> new PosteDeMod(cases(1, true), 4, PostesFours::cuisson, CHAUFFE, FONDU));
    }

    private static List<PosteDeMod.Case> cases(int entrees, boolean moule) {
        List<PosteDeMod.Case> cases = new ArrayList<>();
        for (int i = 0; i < entrees; i++) {
            cases.add(PosteDeMod.Case.of(entrees == 1 ? ENTREE : ENTREE + "_" + i, ENTREE, i));
        }
        if (moule) {
            cases.add(PosteDeMod.Case.of(MOULE, MOULE, 0));
        }
        cases.add(PosteDeMod.Case.of(COMBUSTIBLE, COMBUSTIBLE, 0));
        cases.add(PosteDeMod.Case.of(RESULTAT, RESULTAT, 0));
        return cases;
    }

    /** Où en est la cuisson, de 0 à 1 ; négatif quand rien ne cuit (la jauge se cache). */
    private static double cuisson(AbstractContainerMenu menu) {
        int pixels;
        if (menu instanceof AlloySmelterMenu alliage) {
            pixels = alliage.getCookProgress();
        } else if (menu instanceof NetherAlloySmelterMenu nether) {
            pixels = nether.getCookProgress();
        } else if (menu instanceof CastingSmelterMenu coulee) {
            pixels = coulee.getCookProgress();
        } else {
            return -1.0;
        }
        return pixels <= 0 ? -1.0 : pixels / 24.0;
    }

    /** Accroupi, le four garde son usage (poser un bloc contre lui). */
    private static boolean ouvre(Player player, BlockPos pos, BlockState state) {
        return FallowConfig.postesFours() && !player.isSecondaryUseActive();
    }
}
