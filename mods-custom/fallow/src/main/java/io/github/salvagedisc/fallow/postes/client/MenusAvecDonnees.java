package io.github.salvagedisc.fallow.postes.client;

import java.util.Set;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundOpenScreenPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.payload.AdvancedOpenScreenPayload;

/**
 * Overgeared (ses enclumes, ses fours, sa table d'empennage), Cold Sweat (son foyer, sa chaudière, sa glacière, sa table de couture)
 * et Exposure (son labo) ouvrent leurs menus par le chemin de NeoForge (un menu avec des données en plus, la position du bloc), que
 * Diegesis n'écoute pas : il n'attend que le paquet vanilla d'ouverture. Pour ces menus-là seulement, le paquet de NeoForge est
 * rejoué en paquet vanilla ; ses données attendent ici le temps que le menu soit créé, par Diegesis s'il le prend pour son poste,
 * par l'écran vanilla sinon (mixins {@code postes.MenuOuvertureMixin} et {@code postes.MenuCreationMixin}).
 */
public final class MenusAvecDonnees {
    private static AdvancedOpenScreenPayload enCours;
    private static ConnectionType connexion;

    private MenusAvecDonnees() {
    }

    /** Vrai si l'ouverture a été rejouée en paquet vanilla : NeoForge ne doit plus rien en faire. */
    public static boolean rejouer(AdvancedOpenScreenPayload payload, ConnectionType type) {
        Minecraft minecraft = Minecraft.getInstance();
        if (!pourUnPoste(payload.menuType()) || !minecraft.isSameThread() || minecraft.getConnection() == null) {
            return false;
        }
        enCours = payload;
        connexion = type;
        try {
            minecraft.getConnection().handleOpenScreen(new ClientboundOpenScreenPacket(payload.windowId(), payload.menuType(),
                    payload.name()));
        } finally {
            enCours = null;
            connexion = null;
        }
        return true;
    }

    /** Les données du menu en train d'ouvrir, lues à neuf à chaque création ; null pour tout autre menu. */
    public static RegistryFriendlyByteBuf donnees(MenuType<?> type) {
        Minecraft minecraft = Minecraft.getInstance();
        if (enCours == null || enCours.menuType() != type || minecraft.player == null) {
            return null;
        }
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(enCours.additionalData()), minecraft.player.registryAccess(),
                connexion);
    }

    /**
     * Les menus des postes ouverts par NeoForge : fours et table d'empennage d'Overgeared, foyer, chaudière, glacière et table de
     * couture de Cold Sweat, labo d'Exposure (les enclumes d'Overgeared, par leur nom). Écrits ici par leur nom : les classes des postes lisent celles des
     * mods, qui peuvent manquer.
     */
    private static final Set<ResourceLocation> MENUS = Set.of(id("overgeared", "alloy_smelter_menu"),
            id("overgeared", "nether_alloy_smelter_menu"), id("overgeared", "casting_smelter_menu"), id("overgeared", "fletching_station"),
            id("cold_sweat", "hearth"),
            id("cold_sweat", "boiler"), id("cold_sweat", "ice_box"), id("cold_sweat", "sewing_table"), id("exposure", "lightroom"));

    private static ResourceLocation id(String namespace, String path) {
        return ResourceLocation.fromNamespaceAndPath(namespace, path);
    }

    private static boolean pourUnPoste(MenuType<?> type) {
        ResourceLocation id = BuiltInRegistries.MENU.getKey(type);
        return id != null && (id.getNamespace().equals("overgeared") && id.getPath().endsWith("smithing_anvil_menu") || MENUS.contains(id));
    }
}
