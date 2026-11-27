package io.github.salvagedisc.fallow.postes.client;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.MenuBinding;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;

import net.stirdrem.overgeared.config.ServerConfig;

/**
 * Les tables d'Overgeared en postes, comme les tables de Hands-On Vanilla : la table d'empennage (le bloc de vanilla, qu'Overgeared
 * fait travailler) et la table à dessin des plans. Le poste tient la fenêtre d'Overgeared (son menu ; côté serveur rien ne change)
 * et en pose les cases sur le plateau. Enregistrés sur le client seul.
 */
public final class PostesTables {
    static final ResourceLocation EMPENNAGE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "empennage");
    static final ResourceLocation PLANS = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "plans");

    static final String POINTE = "pointe";
    static final String HAMPE = "hampe";
    static final String PLUME = "plume";
    static final String FIOLE = "fiole";
    static final String PAPIER = "papier";
    static final String PLAN = "plan";
    static final String RESULTAT = "resultat";

    private static final ResourceLocation MENU_EMPENNAGE = og("fletching_station");
    private static final ResourceLocation MENU_PLANS = og("blueprint_workbench");

    private PostesTables() {
    }

    private static ResourceLocation og(String path) {
        return ResourceLocation.fromNamespaceAndPath("overgeared", path);
    }

    static void register() {
        // La table d'empennage est ouverte avec la position du bloc en plus ({@link MenusAvecDonnees}), la table à dessin en menu
        // simple. Diegesis veut un type de menu en main : celui d'un coffre d'une rangée ; le vrai est accepté en plus.
        Diegesis.station(EMPENNAGE)
                .block(() -> Blocks.FLETCHING_TABLE)
                .orientation(StationType.Orientation.PLAYER)
                .reach(4.5)
                .schema(StationSchema.builder().slots(POINTE, 1).slots(HAMPE, 1).slots(PLUME, 1).slots(FIOLE, 1).slots(RESULTAT, 1)
                        .build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_EMPENNAGE).slots(POINTE, 0, 1).slots(HAMPE, 1, 1)
                        .slots(PLUME, 2, 1).slots(FIOLE, 3, 1).slots(RESULTAT, 4, 1).build())
                .openWhen(PostesTables::ouvreEmpennage)
                .register();
        Diegesis.station(PLANS)
                .block(() -> BuiltInRegistries.BLOCK.get(og("drafting_table")))
                .orientation(StationType.Orientation.BLOCK)
                .reach(4.5)
                .schema(StationSchema.builder().slots(PAPIER, 1).slots(PLAN, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU_PLANS).slots(PAPIER, 0, 1).slots(PLAN, 1, 1).build())
                .openWhen(PostesTables::ouvre)
                .register();
        DiegesisClient.registerBehavior(EMPENNAGE, view -> new PosteDeMod(List.of(PosteDeMod.Case.of(POINTE, POINTE, 0),
                PosteDeMod.Case.of(HAMPE, HAMPE, 0), PosteDeMod.Case.of(PLUME, PLUME, 0), PosteDeMod.Case.of(FIOLE, FIOLE, 0),
                PosteDeMod.Case.of(RESULTAT, RESULTAT, 0)), 5, null, 0, 0));
        DiegesisClient.registerBehavior(PLANS, view -> new PostePlans());
    }

    /** Accroupi, la table garde son usage (poser un bloc contre elle). */
    private static boolean ouvre(Player player, BlockPos pos, BlockState state) {
        return FallowConfig.postesTables() && !player.isSecondaryUseActive();
    }

    private static boolean ouvreEmpennage(Player player, BlockPos pos, BlockState state) {
        return empennage(player);
    }

    /**
     * Le poste d'empennage s'ouvre : allumé, pas accroupi, et la table d'Overgeared en service (sans elle, le serveur n'ouvre rien).
     * Lu aussi par {@code EmpennageMixin}.
     */
    public static boolean empennage(Player player) {
        if (!FallowConfig.postesTables() || player.isSecondaryUseActive()) {
            return false;
        }
        try {
            return ServerConfig.ENABLE_FLETCHING_RECIPES.get();
        } catch (IllegalStateException e) {
            return true;
        }
    }

    /** Pris au démarrage du client, après Diegesis : voir {@link #apresDiegesis}. */
    static void ecouter() {
        NeoForge.EVENT_BUS.addListener(EventPriority.LOWEST, PlayerInteractEvent.RightClickBlock.class, PostesTables::apresDiegesis);
    }

    /**
     * La table d'empennage de vanilla ne fait rien d'un clic : une fois le poste armé par Diegesis, le clic irait encore à l'objet
     * tenu (l'arc se bander, la perle partir) puis à la main gauche (la torche se poser contre la table). Comme Overgeared le fait
     * dans sa fenêtre, le clic s'arrête là, réussi ; le paquet du clic part quand même au serveur, qui ouvre le menu.
     */
    private static void apresDiegesis(PlayerInteractEvent.RightClickBlock event) {
        if (event.isCanceled() || event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof LocalPlayer player)
                || !event.getLevel().getBlockState(event.getPos()).is(Blocks.FLETCHING_TABLE) || !empennage(player)) {
            return;
        }
        if (player.isUsingItem()) {
            player.stopUsingItem();
        }
        event.setCancellationResult(InteractionResult.SUCCESS);
        event.setCanceled(true);
    }
}
