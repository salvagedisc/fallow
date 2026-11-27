package io.github.salvagedisc.fallow.postes.client;

import java.util.List;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.neoforged.neoforge.network.PacketDistributor;

import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.scene.Visual;

import io.github.salvagedisc.fallow.Fallow;

import net.stirdrem.overgeared.item.ToolType;
import net.stirdrem.overgeared.item.ToolTypeRegistry;
import net.stirdrem.overgeared.networking.packet.SelectToolTypeC2SPacket;

/**
 * La table à dessin d'Overgeared en poste. Sur la feuille quadrillée du plateau : le plan vierge à gauche, le plan tracé à droite,
 * comme dans la fenêtre d'Overgeared ; au fond, en rang, une tête d'outil d'acier pour chaque sorte d'outil qu'un plan peut dire.
 * Un clic sur une tête trace son plan sur la feuille vierge (le choix et le bouton de la fenêtre en un geste), avec le bruit de la
 * plume du cartographe. Sans feuille vierge, la tête tremble et la légende dit ce qui manque.
 */
final class PostePlans extends PosteDeMod {
    /** Autant de nœuds {@code modele_N} dans la scène. */
    private static final int MODELES = 8;
    /** Le rang des têtes : son milieu, sa profondeur, sa hauteur (pixels de la scène), l'écart le plus grand entre deux têtes. */
    private static final float MILIEU = 8.0F;
    private static final float FOND = 13.0F;
    private static final float ECART = 2.4F;
    private static final float LARGEUR = 13.0F;

    private List<ToolType> sortes = List.of();
    /** Où chaque tête a été posée, par rapport à son nœud. */
    private final float[][] places = new float[MODELES][];

    PostePlans() {
        super(List.of(Case.of(PostesTables.PAPIER, PostesTables.PAPIER, 0), Case.of(PostesTables.PLAN, PostesTables.PLAN, 0)), 2, null,
                0, 0);
    }

    @Override
    public void onSceneReady(StationView view) {
        super.onSceneReady(view);
        sortes = ToolTypeRegistry.getRegisteredTypes();
        if (sortes.size() > MODELES) {
            Fallow.LOGGER.warn("Table à dessin : {} sortes d'outils, le poste n'en montre que {}", sortes.size(), MODELES);
        }
        int n = Math.min(MODELES, sortes.size());
        float ecart = n <= 1 ? 0.0F : Math.min(ECART, LARGEUR / (n - 1));
        for (int i = 0; i < MODELES; i++) {
            int rang = i;
            view.node("modele_" + i).ifPresent(node -> {
                if (rang >= n) {
                    node.setVisible(false);
                    return;
                }
                ToolType sorte = sortes.get(rang);
                // Vu de devant, x grand est à gauche : la première sorte à gauche, comme on lit.
                float x = MILIEU + ecart * (n - 1) / 2.0F - ecart * rang;
                var base = node.definition().transform().pos();
                places[rang] = new float[] { x - base.x(), 0.0F, FOND - base.z() };
                node.moveTo(places[rang][0], places[rang][1], places[rang][2], 0.0F);
                node.setVisual(Visual.ofItem(tete(sorte.getId())));
                node.setTooltip(sorte.getDisplayName());
                node.setVisible(true);
            });
        }
    }

    /** La tête d'outil qui montre une sorte d'outil : d'acier, sinon de fer, sinon le plan lui-même. */
    static Item tete(String sorte) {
        for (String nom : new String[] { "steel_" + sorte + "_head", "steel_" + sorte + "_blade", "iron_" + sorte + "_head",
                "iron_" + sorte + "_blade" }) {
            Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("overgeared", nom));
            if (item != Items.AIR) {
                return item;
            }
        }
        return BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath("overgeared", "blueprint"));
    }

    @Override
    public void onEvent(StationView view, SceneEvent event) {
        if (event instanceof SceneEvent.Click click && click.node().startsWith("modele_")) {
            trace(view, click.node());
        }
    }

    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        if (node.startsWith("modele_")) {
            trace(view, node);
            return true;
        }
        return super.onItemClick(view, node, stack, button, shift);
    }

    private void trace(StationView view, String node) {
        int rang;
        try {
            rang = Integer.parseInt(node.substring("modele_".length()));
        } catch (NumberFormatException e) {
            return;
        }
        if (rang >= sortes.size() || view.menu() == null) {
            return;
        }
        boolean vierge = !view.state().getItem(SlotRef.of(PostesTables.PAPIER, 0)).isEmpty();
        boolean libre = view.state().getItem(SlotRef.of(PostesTables.PLAN, 0)).isEmpty();
        if (!vierge || !libre) {
            view.node(node).ifPresent(n -> n.shake(0.6F, 0.25F));
            view.showCaption(Component.translatable(vierge ? "postes.fallow.plans.prenez" : "postes.fallow.plans.vierge"), 2.0F);
            return;
        }
        PacketDistributor.sendToServer(new SelectToolTypeC2SPacket(sortes.get(rang).getId(), view.menu().handle().containerId));
        Son.joue(view, SoundEvents.UI_CARTOGRAPHY_TABLE_TAKE_RESULT, PostesTables.PLAN, 0.6F, 1.0F);
        float[] place = places[rang];
        if (place != null) {
            view.node(node).ifPresent(n -> n.hopTo(place[0], place[1], place[2], 1.2F, 0.25F));
        }
    }
}
