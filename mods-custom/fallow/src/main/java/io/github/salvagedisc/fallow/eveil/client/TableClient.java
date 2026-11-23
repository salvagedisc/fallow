package io.github.salvagedisc.fallow.eveil.client;

import java.util.List;
import java.util.stream.IntStream;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.RandomSource;
import net.minecraft.world.phys.BlockHitResult;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.ParticleStyle;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.eveil.TableEveil;

/**
 * Le poste de la table d'enchantement sur le client (seulement avec Diegesis) : sa conduite ({@link TableBehavior}), le corps de
 * la table, les runes qui coulent du livre dans l'objet, et ce que la table dit en légende de la scène.
 */
final class TableClient {
    /**
     * Les lettres de l'alphabet de la table de vanilla ({@code sga_*}) qui font les runes, comme chez Hands-On Vanilla : celles qui
     * coulent vers l'objet (droites, lumineuses), celles qui s'envolent (elles flottent et se dissipent).
     */
    private static final String LETTRES = "aeklqrvz";
    private static final List<ResourceLocation> GLYPHES = IntStream.range(0, LETTRES.length())
            .mapToObj(i -> ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "glyphe_" + i)).toList();
    private static final List<ResourceLocation> GLYPHES_VOLANTS = IntStream.range(0, LETTRES.length())
            .mapToObj(i -> ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "glyphe_volant_" + i)).toList();

    /** L'encre qui monte de l'encrier vers les runes qu'on lit ; les étincelles d'un éveil accompli. */
    static final ResourceLocation VOLUTE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "volute_d_encre");
    static final ResourceLocation ETINCELLE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "etincelle_d_eveil");
    /** La poussière que la table aspire hors de la brume. */
    static final ResourceLocation POUSSIERE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "poussiere_de_brume");

    private static final ResourceLocation VOILE = ResourceLocation.withDefaultNamespace("textures/misc/vignette.png");

    private TableClient() {
    }

    static void init() {
        DiegesisClient.registerBehavior(TableEveil.ID, view -> new TableBehavior());
        DiegesisClient.registerModel(ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "diegesis/table_d_eveil_corps"));
        for (int i = 0; i < LETTRES.length(); i++) {
            ResourceLocation lettre = ResourceLocation.withDefaultNamespace("sga_" + LETTRES.charAt(i));
            DiegesisClient.registerParticle(GLYPHES.get(i), ParticleStyle.builder(lettre).color(0xFFEDE6FF).size(1.1F, 1.5F)
                    .life(0.5F, 0.5F).drag(0.0F).emissive().grow(0.6F).build());
            DiegesisClient.registerParticle(GLYPHES_VOLANTS.get(i), ParticleStyle.builder(lettre).color(0xFFB98EFF).size(0.8F, 1.2F)
                    .life(1.0F, 1.6F).gravity(-2.0F).drag(1.4F).wobble(0.5F).emissive().grow(0.4F).fadeIn(0.1F).build());
        }
        DiegesisClient.registerParticle(VOLUTE, ParticleStyle.builder(ResourceLocation.withDefaultNamespace("generic_2"))
                .color(0xFF6A48E8).size(0.45F, 0.8F).life(0.7F, 1.0F).drag(0.5F).wobble(0.6F).emissive().fadeIn(0.15F).build());
        DiegesisClient.registerParticle(ETINCELLE, ParticleStyle.builder(ResourceLocation.withDefaultNamespace("glitter_4"))
                .color(0xFFD9CCFF).size(0.25F, 0.5F).life(0.9F, 1.6F).gravity(-0.4F).drag(1.2F).wobble(0.3F).emissive().build());
        DiegesisClient.registerParticle(POUSSIERE, ParticleStyle.builder(ResourceLocation.withDefaultNamespace("generic_0"))
                .color(0xFFB4A6D8).size(0.3F, 0.55F).life(2.0F, 2.3F).drag(0.0F).wobble(0.2F).emissive().fadeIn(0.4F).build());
        Isolement.inscrire();
        NeoForge.EVENT_BUS.addListener(InputEvent.InteractionKeyMappingTriggered.class, event -> {
            if (event.isUseItem() && Minecraft.getInstance().hitResult instanceof BlockHitResult hit) {
                TableBehavior.usage(hit.getBlockPos());
            }
        });
        NeoForge.EVENT_BUS.addListener(ClientChatReceivedEvent.System.class, TableClient::enLegende);
        NeoForge.EVENT_BUS.addListener(ScreenEvent.Render.Pre.class, TableClient::assombrir);
    }

    static ResourceLocation glyphe(RandomSource random) {
        return GLYPHES.get(random.nextInt(GLYPHES.size()));
    }

    static ResourceLocation glypheVolant(RandomSource random) {
        return GLYPHES_VOLANTS.get(random.nextInt(GLYPHES_VOLANTS.size()));
    }

    /**
     * Autour de la table, la pièce s'assombrit : le voile de vanilla (celui des bords de l'écran) posé sur le monde avant l'écran du
     * poste, de plus en plus fort ; la barre et le livre restent clairs.
     */
    private static void assombrir(ScreenEvent.Render.Pre event) {
        float k = TableBehavior.obscurite();
        if (k <= 0.01F && TableBehavior.eclair() <= 0.0F || !DiegesisClient.isStationScreen(event.getScreen())
                || DiegesisClient.focusedStation().filter(view -> TableEveil.ID.equals(view.type().id())).isEmpty()) {
            return;
        }
        GuiGraphics graphics = event.getGuiGraphics();
        int large = graphics.guiWidth();
        int haut = graphics.guiHeight();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GlStateManager.SourceFactor.ZERO, GlStateManager.DestFactor.ONE_MINUS_SRC_COLOR);
        // Deux passes du voile : la seconde mord plus loin vers le milieu quand la table s'éveille.
        // Le vert s'éteint plus vite que le rouge et le bleu : l'ombre tire sur le violet.
        graphics.setColor(Math.min(1.0F, k * 1.15F), Math.min(1.0F, k * 1.4F), Math.min(1.0F, k * 1.0F), 1.0F);
        graphics.blit(VOILE, 0, 0, -90, 0.0F, 0.0F, large, haut, large, haut);
        float second = Math.max(0.0F, k - 0.5F) * 1.6F;
        if (second > 0.0F) {
            graphics.setColor(second * 0.95F, second * 1.1F, second * 0.85F, 1.0F);
            graphics.blit(VOILE, 0, 0, -90, 0.0F, 0.0F, large, haut, large, haut);
        }
        graphics.setColor(1.0F, 1.0F, 1.0F, 1.0F);
        RenderSystem.defaultBlendFunc();
        float eclair = TableBehavior.eclair();
        if (eclair > 0.0F) {
            // L'éveil s'accomplit : un éclair blanc, bleuté, qui s'éteint vite.
            graphics.fill(0, 0, large, haut, Math.round(200.0F * eclair * eclair) << 24 | 0xEEE8FF);
        }
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
    }

    /** Au poste la barre d'action est cachée : ce que la table dit (l'éveil, ce qui ne prend pas) passe en légende de la scène. */
    private static void enLegende(ClientChatReceivedEvent.System event) {
        if (event.isOverlay()) {
            DiegesisClient.focusedStation().filter(view -> TableEveil.ID.equals(view.type().id())).ifPresent(view -> {
                view.showCaption(event.getMessage(), 3.0F);
                event.setCanceled(true);
            });
        }
    }
}
