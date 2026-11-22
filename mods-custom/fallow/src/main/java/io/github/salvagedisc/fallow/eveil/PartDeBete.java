package io.github.salvagedisc.fallow.eveil;

import java.util.Map;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * Les armures d'Archie's Dinosaurs sont faites d'une bête, pas d'un métal : pour s'éveiller, en plus de l'encre, elles demandent
 * une part de leur créature (une corne, une côte, une griffe…), que le joueur porte sur lui et que la table prend.
 */
public final class PartDeBete {
    private static final String ARCHIE = "ec_ad";
    private static final Map<String, String> PARTS = Map.of(
            "obsidiceratops", "obsidian_horn",
            "chorusoatl", "chorus_fossil",
            "coralosaur", "coral_rib",
            "therimoss", "mossy_claw",
            "sculksaur", "sculk_toxin",
            "lushsaur", "lush_core");

    private PartDeBete() {
    }

    /** La part de bête que l'objet demande pour s'éveiller, s'il en demande une. */
    public static Optional<Item> de(ItemStack objet) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(objet.getItem());
        if (!ARCHIE.equals(id.getNamespace())) {
            return Optional.empty();
        }
        int coupe = id.getPath().indexOf('_');
        String part = coupe < 0 ? null : PARTS.get(id.getPath().substring(0, coupe));
        return part == null ? Optional.empty()
                : BuiltInRegistries.ITEM.getOptional(ResourceLocation.fromNamespaceAndPath(ARCHIE, part));
    }

    /** Le joueur a-t-il sur lui ce que l'objet demande ? (Le mode créatif n'en a pas besoin.) */
    public static boolean porte(Player player, ItemStack objet) {
        Optional<Item> part = de(objet);
        return part.isEmpty() || player.getAbilities().instabuild || player.getInventory().contains(new ItemStack(part.get()));
    }

    /** La table prend la part ; {@code false} si le joueur ne l'a pas. */
    static boolean prendre(Player player, ItemStack objet) {
        Optional<Item> part = de(objet);
        if (part.isEmpty() || player.getAbilities().instabuild) {
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (inventory.getItem(i).is(part.get())) {
                inventory.getItem(i).shrink(1);
                return true;
            }
        }
        return false;
    }
}
