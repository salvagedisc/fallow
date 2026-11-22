package io.github.salvagedisc.fallow.eveil.client;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.eveil.Eclat;
import io.github.salvagedisc.fallow.eveil.EtatEveil;
import io.github.salvagedisc.fallow.eveil.EveilModule;
import io.github.salvagedisc.fallow.eveil.Lien;
import io.github.salvagedisc.fallow.eveil.PartDeBete;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.FastColor;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterColorHandlersEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;

/**
 * L'infobulle d'un objet qui a commencé à vivre : ses éveils (quoi, quel jour, par qui, de quelle bête), où il en est
 * de son chemin vers le prochain, la place qui lui reste, et l'alerte quand il faiblit et risque d'oublier.
 */
public final class EveilClient {
    private EveilClient() {
    }

    public static void start(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(ItemTooltipEvent.class, EveilClient::onTooltip);
        if (io.github.salvagedisc.fallow.ModIds.isLoaded(io.github.salvagedisc.fallow.ModIds.DIEGESIS)) {
            TableClient.init();
        }
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> event.register((stack, layer) -> {
            Eclat eclat = stack.get(EveilModule.ECLAT.get());
            return layer == 0 ? FastColor.ARGB32.opaque(eclat == null ? 0xB98CFF : eclat.couleur()) : -1;
        }, EveilModule.ECLAT_DE_BETE.get()));
        // L'encre d'éveil est une fiole de vanilla : son liquide (la première couche) a la teinte du lapis et de l'améthyste.
        modBus.addListener(RegisterColorHandlersEvent.Item.class, event -> event.register(
                (stack, layer) -> layer == 0 ? FastColor.ARGB32.opaque(ENCRE) : -1, EveilModule.ENCRE_D_EVEIL.get()));
    }

    /** La teinte de l'encre d'éveil, dans la fiole et sur la table. */
    static final int ENCRE = 0x5B3BD9;

    private static void onTooltip(ItemTooltipEvent event) {
        EtatEveil etat = event.getItemStack().get(EveilModule.ETAT.get());
        if (etat == null) {
            return;
        }
        List<Component> tooltip = event.getToolTip();
        for (EtatEveil.Trace trace : etat.traces()) {
            Component nom = nom(trace);
            Component ligne = trace.eclat().isPresent()
                    ? Component.translatable("eveil.fallow.trace.eclat", nom, trace.jour(), trace.par(),
                            Component.translatable(trace.eclat().get()))
                    : Component.translatable("eveil.fallow.trace", nom, trace.jour(), trace.par());
            tooltip.add(ligne.copy().withStyle(ChatFormatting.DARK_AQUA));
        }
        ItemStack stack = event.getItemStack();
        if (etat.pret()) {
            tooltip.add(Component.translatable("eveil.fallow.tooltip.pret").withStyle(ChatFormatting.GOLD));
        } else if (etat.eteint()) {
            tooltip.add(Component.translatable(Lien.de(stack) >= Lien.ENTRETENU ? "eveil.fallow.lien.tooltip.tient"
                    : "eveil.fallow.tooltip.eteint").withStyle(ChatFormatting.DARK_GRAY));
        } else if (!etat.plein()) {
            tooltip.add(Component.translatable("eveil.fallow.tooltip.palier." + etat.palier())
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC));
        }
        if (!etat.traces().isEmpty() && !etat.eteint() && stack.isDamageableItem()
                && stack.getDamageValue() >= stack.getMaxDamage() * FallowConfig.eveilAlerte()) {
            tooltip.add(Component.translatable("eveil.fallow.tooltip.alerte",
                    nom(etat.traces().get(etat.traces().size() - 1)).getString())
                    .withStyle(ChatFormatting.RED));
        }
        tooltip.add(Component.translatable("eveil.fallow.tooltip.places", etat.occupees(), etat.places())
                .withStyle(ChatFormatting.GRAY));
        int lien = Lien.de(stack);
        if (lien != Lien.AUCUN) {
            tooltip.add(Component.translatable("eveil.fallow.lien.tooltip." + Lien.cle(lien)).withStyle(lien == Lien.NEGLIGE
                    ? ChatFormatting.DARK_GRAY : lien == Lien.CHERI ? ChatFormatting.LIGHT_PURPLE : ChatFormatting.GREEN));
        }
        PartDeBete.de(stack).ifPresent(part -> tooltip.add(Component.translatable("eveil.fallow.tooltip.part", part.getDescription())
                .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)));
    }

    private static Component nom(EtatEveil.Trace trace) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level != null) {
            var holder = minecraft.level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ResourceKey.create(Registries.ENCHANTMENT, trace.enchantement()));
            if (holder.isPresent()) {
                return net.minecraft.world.item.enchantment.Enchantment.getFullname(holder.get(), trace.niveau());
            }
        }
        return Component.literal(trace.enchantement().toString());
    }
}
