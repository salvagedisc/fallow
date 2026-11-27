package io.github.salvagedisc.fallow.postes.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.MenuBinding;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;

import com.momosoftworks.coldsweat.common.blockentity.BoilerBlockEntity;
import com.momosoftworks.coldsweat.common.blockentity.IceboxBlockEntity;
import com.momosoftworks.coldsweat.common.container.BoilerContainer;
import com.momosoftworks.coldsweat.common.container.IceboxContainer;

/**
 * Les postes de Cold Sweat autres que le foyer : la table de couture, la chaudière et la glacière. Chacun tient la fenêtre de Cold
 * Sweat (son menu ; côté serveur rien ne change) et en pose les cases sur le bloc :
 *
 * <ul>
 * <li>à la table de couture, la pièce à garnir à plat sur le coussin, la garniture (ou les cisailles) à côté, la pièce garnie
 * debout au fond ;</li>
 * <li>à la chaudière et à la glacière, les neuf outres sur le dessus, trois rangs de trois, le combustible devant, au sol, et la
 * jauge du combustible sur la façade. La chaudière fume de vapeur quand elle brûle ; la glacière, ouverte, laisse monter son froid
 * tant qu'elle a de la glace.</li>
 * </ul>
 * Enregistrés sur le client seul.
 */
final class PostesColdSweat {
    static final ResourceLocation COUTURE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "couture");
    static final ResourceLocation CHAUDIERE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "chaudiere");
    static final ResourceLocation GLACIERE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "glaciere");

    static final String PIECE = "piece";
    static final String GARNITURE = "garniture";
    static final String RESULTAT = "resultat";
    static final String COMBUSTIBLE = "combustible";
    static final String OUTRE = "outre";

    /** Les menus de Cold Sweat : NeoForge les ouvre avec la position du bloc en plus ({@link MenusAvecDonnees}). */
    static final ResourceLocation MENU_COUTURE = cs("sewing_table");
    static final ResourceLocation MENU_CHAUDIERE = cs("boiler");
    static final ResourceLocation MENU_GLACIERE = cs("ice_box");
    /** La cheminée de Cold Sweat : en main, elle se pose sur la chaudière ou la glacière. */
    private static final ResourceLocation CHEMINEE = cs("smokestack");

    /** La jauge de la chaudière va du rouge sombre à l'orange ; celle de la glacière, du bleu profond au bleu de glace. */
    private static final int BRAISE = 0x8A2A10;
    private static final int FLAMME = 0xFFB040;
    private static final int FROID = 0x2A4A8A;
    private static final int GLACE = 0xA8E0FF;

    private PostesColdSweat() {
    }

    private static ResourceLocation cs(String path) {
        return ResourceLocation.fromNamespaceAndPath("cold_sweat", path);
    }

    static void register() {
        // Par nom, comme le foyer : Diegesis veut un type de menu en main, celui d'un coffre d'une rangée, qu'aucun de ces blocs
        // n'ouvre jamais ; le vrai est accepté en plus.
        Diegesis.station(COUTURE)
                .block(() -> BuiltInRegistries.BLOCK.get(cs("sewing_table")))
                .orientation(StationType.Orientation.PLAYER)
                .reach(4.5)
                .schema(StationSchema.builder().slots(PIECE, 1).slots(GARNITURE, 1).slots(RESULTAT, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_COUTURE).slots(PIECE, 0, 1).slots(GARNITURE, 1, 1)
                        .slots(RESULTAT, 2, 1).build())
                .openWhen(PostesColdSweat::ouvreTable)
                .register();
        for (ResourceLocation id : new ResourceLocation[] { CHAUDIERE, GLACIERE }) {
            boolean chaudiere = id.equals(CHAUDIERE);
            Diegesis.station(id)
                    .block(() -> BuiltInRegistries.BLOCK.get(cs(chaudiere ? "boiler" : "icebox")))
                    .orientation(StationType.Orientation.BLOCK)
                    .reach(4.5)
                    .schema(StationSchema.builder().slots(COMBUSTIBLE, 1).slots(OUTRE, 9).build())
                    .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(chaudiere ? MENU_CHAUDIERE : MENU_GLACIERE)
                            .slots(COMBUSTIBLE, 0, 1).slots(OUTRE, 1, 9).build())
                    .openWhen(PostesColdSweat::ouvreCaisse)
                    .register();
        }
        DiegesisClient.registerBehavior(COUTURE, view -> new PosteDeMod(List.of(PosteDeMod.Case.of(PIECE, PIECE, 0),
                PosteDeMod.Case.of(GARNITURE, GARNITURE, 0), PosteDeMod.Case.of(RESULTAT, RESULTAT, 0)), 3, null, 0, 0));
        DiegesisClient.registerBehavior(CHAUDIERE, view -> new PosteDeMod(cases(), 10, PostesColdSweat::combustible, BRAISE, FLAMME)
                .avecSouffle(0x90F0F0F0, 2.5F, poste -> poste.feu));
        DiegesisClient.registerBehavior(GLACIERE, view -> new PosteDeMod(cases(), 10, PostesColdSweat::combustible, FROID, GLACE)
                .avecSouffle(0x70D8F0FF, 0.8F, poste -> poste.part > 0.0));
    }

    private static List<PosteDeMod.Case> cases() {
        List<PosteDeMod.Case> cases = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            cases.add(PosteDeMod.Case.of(OUTRE + "_" + i, OUTRE, i));
        }
        cases.add(PosteDeMod.Case.of(COMBUSTIBLE, COMBUSTIBLE, 0));
        return cases;
    }

    /** Le combustible de la chaudière ou de la glacière, de 0 à 1 ; la jauge reste montrée, vide quand il n'y en a plus. */
    private static double combustible(AbstractContainerMenu menu) {
        if (menu instanceof BoilerContainer chaudiere && chaudiere.te.getMaxFuel() > 0) {
            return Math.min(1.0, chaudiere.getFuel() / (double) chaudiere.te.getMaxFuel());
        }
        if (menu instanceof IceboxContainer glaciere && glaciere.te.getMaxFuel() > 0) {
            return Math.min(1.0, glaciere.getFuel() / (double) glaciere.te.getMaxFuel());
        }
        return -1.0;
    }

    /** Accroupi, la table garde son usage (poser un bloc contre elle). */
    private static boolean ouvreTable(Player player, BlockPos pos, BlockState state) {
        return FallowConfig.postesTables() && !player.isSecondaryUseActive();
    }

    /**
     * Accroupi, ou un combustible en main qui tient encore dans le bloc (Cold Sweat l'y jette d'un clic), ou une cheminée en main
     * (elle se pose dessus), le bloc garde ses gestes de Cold Sweat. Une glacière sous un bloc plein ne s'ouvre pas, comme un coffre.
     */
    private static boolean ouvreCaisse(Player player, BlockPos pos, BlockState state) {
        if (!FallowConfig.postesChaudieres() || player.isSecondaryUseActive()) {
            return false;
        }
        ItemStack main = player.getMainHandItem();
        if (BuiltInRegistries.ITEM.getKey(main.getItem()).equals(CHEMINEE)) {
            return false;
        }
        if (player.level().getBlockEntity(pos) instanceof IceboxBlockEntity && ChestBlock.isChestBlockedAt(player.level(), pos)) {
            return false;
        }
        if (main.isEmpty()) {
            return true;
        }
        BlockEntity be = player.level().getBlockEntity(pos);
        if (be instanceof BoilerBlockEntity chaudiere) {
            int fuel = chaudiere.getItemFuel(main);
            return fuel == 0 || chaudiere.getFuel() + fuel * 0.75 >= chaudiere.getMaxFuel();
        }
        if (be instanceof IceboxBlockEntity glaciere) {
            int fuel = glaciere.getItemFuel(main);
            return fuel == 0 || glaciere.getFuel() + fuel * 0.75 >= glaciere.getMaxFuel();
        }
        return true;
    }
}
