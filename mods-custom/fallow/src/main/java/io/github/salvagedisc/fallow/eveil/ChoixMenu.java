package io.github.salvagedisc.fallow.eveil;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Le choix de l'éveil, à la table : une rangée de coffre où l'objet est au milieu et ses deux enchantements possibles
 * de part et d'autre. Tout se passe sur le serveur (le client montre une rangée de coffre ordinaire) ; rien ne se
 * prend ni ne se dépose, un clic sur un livre choisit.
 */
final class ChoixMenu extends ChestMenu {
    private static final int[] PLACES = {2, 6};
    private static final int OBJET = 4;

    private final List<Eveil.Option> options;
    private final UUID objet;
    private final BlockPos table;

    ChoixMenu(int id, Inventory inventory, ItemStack stack, List<Eveil.Option> options, BlockPos table) {
        super(MenuType.GENERIC_9x1, id, inventory, vitrine(stack, options), 1);
        this.options = options;
        this.objet = stack.get(EveilModule.ETAT.get()).id();
        this.table = table.immutable();
    }

    private static SimpleContainer vitrine(ItemStack stack, List<Eveil.Option> options) {
        SimpleContainer container = new SimpleContainer(9);
        container.setItem(OBJET, stack.copy());
        for (int i = 0; i < options.size(); i++) {
            Eveil.Option option = options.get(i);
            ItemStack livre = new ItemStack(Items.ENCHANTED_BOOK);
            ItemEnchantments.Mutable stored = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
            stored.set(option.enchantement(), option.niveau());
            livre.set(DataComponents.STORED_ENCHANTMENTS, stored.toImmutable().withTooltip(false));
            livre.set(DataComponents.CUSTOM_NAME, Enchantment.getFullname(option.enchantement(), option.niveau())
                    .copy().withStyle(s -> s.withItalic(false).withColor(ChatFormatting.AQUA)));
            livre.set(DataComponents.LORE, new ItemLore(List.of(Component.translatable("eveil.fallow.choix.cliquer")
                    .withStyle(s -> s.withItalic(false).withColor(ChatFormatting.GRAY)))));
            container.setItem(PLACES[i], livre);
        }
        return container;
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        for (int i = 0; i < options.size(); i++) {
            if (slotId == PLACES[i] && player instanceof ServerPlayer serverPlayer) {
                ItemStack stack = player.getMainHandItem();
                EtatEveil etat = stack.get(EveilModule.ETAT.get());
                if (etat != null && etat.id().equals(objet) && etat.pret()) {
                    if (EveilModule.prendreEncre(serverPlayer)) {
                        Eveil.appliquer(serverPlayer, stack, options.get(i), Optional.empty(), table);
                    } else {
                        serverPlayer.displayClientMessage(net.minecraft.network.chat.Component.translatable("eveil.fallow.table.sans_encre"), true);
                    }
                }
                serverPlayer.closeContainer();
                return;
            }
        }
        // Rien d'autre ne bouge : on renvoie l'état au client, qui a pu croire prendre quelque chose.
        broadcastFullState();
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean stillValid(Player player) {
        EtatEveil etat = player.getMainHandItem().get(EveilModule.ETAT.get());
        return etat != null && etat.id().equals(objet) && player.distanceToSqr(table.getCenter()) <= 64.0;
    }

    @Override
    public void removed(Player player) {
        // La vitrine n'appartient à personne : rien n'est rendu au joueur.
        getContainer().clearContent();
        super.removed(player);
    }
}
