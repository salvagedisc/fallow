package io.github.salvagedisc.fallow.eveil;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.enchantment.Enchantment;

/**
 * L'éclat d'une grande bête : pris sur Frostmaw, le Wroughtnaut, Umvuthi ou un dinosaure redoutable, il porte un
 * enchantement qu'on pose une fois sur un objet, à la table d'enchantement, l'objet en main principale et l'éclat
 * dans l'autre. Son contenu est le composant {@link Eclat}, donné par le butin de la bête.
 */
final class EclatItem extends Item {
    EclatItem(Properties properties) {
        super(properties);
    }

    @Override
    public Component getName(ItemStack stack) {
        Eclat eclat = stack.get(EveilModule.ECLAT.get());
        return eclat == null ? super.getName(stack)
                : Component.translatable("item.fallow.eclat_de_bete.de", Component.translatable(eclat.bete()));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        Eclat eclat = stack.get(EveilModule.ECLAT.get());
        if (eclat == null) {
            return;
        }
        if (context.registries() != null) {
            context.registries().lookup(Registries.ENCHANTMENT)
                    .flatMap(lookup -> lookup.get(ResourceKey.create(Registries.ENCHANTMENT, eclat.enchantement())))
                    .ifPresent(holder -> tooltip.add(Enchantment.getFullname(holder, eclat.niveau()).copy()
                            .withStyle(ChatFormatting.AQUA)));
        }
        if (!eclat.recit().isEmpty()) {
            tooltip.add(Component.translatable(eclat.recit()).withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        tooltip.add(Component.translatable("item.fallow.eclat_de_bete.usage").withStyle(ChatFormatting.GRAY));
    }
}
