package io.github.salvagedisc.fallow.postes.client;

import java.util.ArrayList;
import java.util.List;

import org.joml.Matrix4f;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.client.DiegesisClient;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.event.SceneEvent;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.MenuBinding;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;

import io.github.mortuusars.exposure.ExposureClient;
import io.github.mortuusars.exposure.client.image.modifier.ImageEffect;
import io.github.mortuusars.exposure.client.image.renderable.RenderableImage;
import io.github.mortuusars.exposure.client.render.image.RenderCoordinates;
import io.github.mortuusars.exposure.world.block.entity.LightroomBlockEntity;
import io.github.mortuusars.exposure.world.camera.frame.Frame;
import io.github.mortuusars.exposure.world.inventory.LightroomMenu;
import io.github.mortuusars.exposure.world.item.ChromaticSheetItem;
import io.github.mortuusars.exposure.world.item.DevelopedFilmItem;
import io.github.mortuusars.exposure.world.lightroom.PrintingMode;
import io.github.mortuusars.exposure.world.lightroom.PrintingProcess;

/**
 * Le labo photo d'Exposure en poste. Sur le plateau : la pellicule développée à gauche, le papier à droite, les quatre teintures en
 * rang au fond, le tirage debout au fond à droite. Entre elles, penché vers soi comme sous l'agrandisseur, le négatif de la vue
 * choisie, peint par Exposure lui-même. Un clic sur le négatif lance le tirage (le bouton de la fenêtre), la molette passe d'une vue à
 * l'autre, un clic droit change de procédé quand le labo en a deux. Ce qui manque pour tirer se dit en légende. La jauge du tirage
 * est sur la façade. Enregistré sur le client seul.
 */
final class PostesLabo {
    static final ResourceLocation LABO = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "labo");

    static final String PELLICULE = "pellicule";
    static final String PAPIER = "papier";
    static final String TEINTURE = "teinture";
    static final String RESULTAT = "resultat";
    static final String NEGATIF = "negatif";

    /** Le menu du labo : NeoForge l'ouvre avec la position du bloc en plus ({@link MenusAvecDonnees}). */
    static final ResourceLocation MENU = ResourceLocation.fromNamespaceAndPath("exposure", "lightroom");

    private PostesLabo() {
    }

    static void register() {
        Diegesis.station(LABO)
                .block(() -> BuiltInRegistries.BLOCK.get(ResourceLocation.fromNamespaceAndPath("exposure", "lightroom")))
                .orientation(StationType.Orientation.BLOCK)
                .reach(4.5)
                .schema(StationSchema.builder().slots(PELLICULE, 1).slots(PAPIER, 1).slots(TEINTURE, 4).slots(RESULTAT, 1).build())
                .menu(MenuBinding.of(MenuType.GENERIC_9x1).alsoAccepts(MENU).slots(PELLICULE, 0, 1).slots(PAPIER, 1, 1)
                        .slots(TEINTURE, 2, 4).slots(RESULTAT, 6, 1).build())
                .openWhen(PostesLabo::ouvre)
                .register();
        DiegesisClient.registerBehavior(LABO, view -> new Labo());
    }

    private static boolean ouvre(Player player, BlockPos pos, BlockState state) {
        return FallowConfig.postesLabo() && !player.isSecondaryUseActive();
    }

    private static List<PosteDeMod.Case> cases() {
        List<PosteDeMod.Case> cases = new ArrayList<>();
        cases.add(PosteDeMod.Case.of(PELLICULE, PELLICULE, 0));
        cases.add(PosteDeMod.Case.of(PAPIER, PAPIER, 0));
        for (int i = 0; i < 4; i++) {
            cases.add(PosteDeMod.Case.of(TEINTURE + "_" + i, TEINTURE, i));
        }
        cases.add(PosteDeMod.Case.of(RESULTAT, RESULTAT, 0));
        return cases;
    }

    /** Où en est le tirage, de 0 à 1 ; négatif quand rien ne se tire. */
    private static double tirage(AbstractContainerMenu menu) {
        if (menu instanceof LightroomMenu labo) {
            int fait = labo.getData().get(LightroomBlockEntity.CONTAINER_DATA_PROGRESS_ID);
            int temps = labo.getData().get(LightroomBlockEntity.CONTAINER_DATA_PRINT_TIME_ID);
            return fait > 0 && temps > 0 ? fait / (double) temps : -1.0;
        }
        return -1.0;
    }

    private static final class Labo extends PosteDeMod {
        private static final ResourceLocation BLANC = ResourceLocation.withDefaultNamespace("textures/misc/white.png");
        /** Le côté du négatif peint, en pixels de la scène. */
        private static final float COTE = 4.8F;

        Labo() {
            super(cases(), 7, PostesLabo::tirage, 0x6A1010, 0xE04030);
        }

        @Override
        public void onSceneReady(StationView view) {
            super.onSceneReady(view);
            view.node(NEGATIF).ifPresent(node -> node.setPainter((pose, buffers, light) -> peindre(view, pose, buffers, light)));
        }

        private LightroomMenu menu(StationView view) {
            return view.menu() != null && view.menu().handle() instanceof LightroomMenu labo ? labo : null;
        }

        private void peindre(StationView view, PoseStack pose, MultiBufferSource buffers, int light) {
            LightroomMenu labo = menu(view);
            if (labo == null || !(labo.getSlot(0).getItem().getItem() instanceof DevelopedFilmItem film)) {
                return;
            }
            Frame frame = labo.getFrameByIndex(labo.getSelectedFrame());
            if (frame == null) {
                return;
            }
            // La bande de la pellicule, de sa couleur, un peu plus large que la vue.
            var couleur = film.getType().getFilmColor();
            int teinte = (int) (couleur.r() * 110) << 16 | (int) (couleur.g() * 110) << 8 | (int) (couleur.b() * 110);
            VertexConsumer out = buffers.getBuffer(RenderType.entityCutoutNoCull(BLANC));
            quad(pose, out, -COTE / 2.0F - 0.6F, -COTE / 2.0F - 1.0F, COTE + 1.2F, COTE + 2.0F, teinte, light, 0.0F);
            RenderableImage image = ExposureClient.renderedExposures().getOrCreate(frame).modifyWith(ImageEffect.NEGATIVE_FILM);
            pose.pushPose();
            // Le moteur d'image d'Exposure dessine comme à l'écran : x vers la droite, y vers le bas. Vu de devant, x grand est à
            // gauche et y monte : on retourne les deux.
            pose.translate(0.0F, 0.0F, -0.02F);
            pose.scale(-1.0F, -1.0F, 1.0F);
            ExposureClient.imageRenderer().render(image, pose, buffers, new RenderCoordinates(-COTE / 2.0F, -COTE / 2.0F, COTE, COTE),
                    light, film.getType().getImageColor());
            pose.popPose();
        }

        private static void quad(PoseStack pose, VertexConsumer out, float x, float y, float w, float h, int rgb, int light, float z) {
            Matrix4f m = pose.last().pose();
            int r = rgb >> 16 & 0xFF;
            int g = rgb >> 8 & 0xFF;
            int b = rgb & 0xFF;
            float[][] coins = { { -x, y }, { -(x + w), y }, { -(x + w), y + h }, { -x, y + h } };
            for (float[] c : coins) {
                out.addVertex(m, c[0], c[1], z).setColor(r, g, b, 255).setUv(0.0F, 0.0F).setOverlay(OverlayTexture.NO_OVERLAY)
                        .setLight(light).setNormal(pose.last(), 0.0F, 0.0F, -1.0F);
            }
        }

        @Override
        public void tick(StationView view) {
            super.tick(view);
            LightroomMenu labo = menu(view);
            view.node(NEGATIF).ifPresent(node -> node.setTooltip(labo == null || labo.getExposedFrames().isEmpty() ? null
                    : Component.translatable(labo.canChangeProcess() ? "postes.fallow.labo.vue_procede" : "postes.fallow.labo.vue",
                            labo.getSelectedFrame() + 1, labo.getExposedFrames().size(),
                            Component.translatable("postes.fallow.labo.procede." + labo.getBlockEntity().getActualPrintingMode()
                                    .getSerializedName()))));
        }

        @Override
        public void onEvent(StationView view, SceneEvent event) {
            if (event instanceof SceneEvent.Click click && NEGATIF.equals(click.node())) {
                clic(view, click.button());
            }
        }

        @Override
        public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
            if (NEGATIF.equals(node)) {
                clic(view, button);
                return true;
            }
            return super.onItemClick(view, node, stack, button, shift);
        }

        @Override
        public boolean onScroll(StationView view, String node, double amount) {
            LightroomMenu labo = menu(view);
            if (!NEGATIF.equals(node) || labo == null || labo.getExposedFrames().isEmpty() || labo.isPrinting()) {
                return false;
            }
            int vers = amount > 0 ? LightroomMenu.PREVIOUS_FRAME_BUTTON_ID : LightroomMenu.NEXT_FRAME_BUTTON_ID;
            int choisie = labo.getSelectedFrame() + (amount > 0 ? -1 : 1);
            if (choisie >= 0 && choisie < labo.getExposedFrames().size()) {
                view.menu().pressButton(vers);
                Son.joue(view, SoundEvents.BOOK_PAGE_TURN, NEGATIF, 0.4F, 1.3F);
            }
            return true;
        }

        private void clic(StationView view, int button) {
            LightroomMenu labo = menu(view);
            if (labo == null) {
                return;
            }
            if (button == 1) {
                if (labo.canChangeProcess() && !labo.isPrinting()) {
                    view.menu().pressButton(LightroomMenu.TOGGLE_PROCESS_BUTTON_ID);
                    Son.joue(view, SoundEvents.UI_BUTTON_CLICK.value(), NEGATIF, 0.3F, 1.2F);
                }
                return;
            }
            LightroomBlockEntity labor = labo.getBlockEntity();
            if (labo.isPrinting()) {
                return;
            }
            if (!labor.canPrint()) {
                view.node(NEGATIF).ifPresent(n -> n.shake(0.5F, 0.25F));
                view.showCaption(Component.translatable("postes.fallow.labo." + manque(labo, labor)), 2.5F);
                return;
            }
            view.menu().pressButton(LightroomMenu.PRINT_BUTTON_ID);
        }

        /** Ce qui manque pour tirer, dans l'ordre où Exposure le cherche. */
        private static String manque(LightroomMenu labo, LightroomBlockEntity labor) {
            if (labo.getExposedFrames().isEmpty()) {
                return "pellicule";
            }
            ItemStack papier = labo.getSlot(1).getItem();
            if (papier.isEmpty()) {
                return "papier";
            }
            if (!labor.hasSufficientLightLevel()) {
                return "lumiere";
            }
            Frame frame = labo.getFrameByIndex(labo.getSelectedFrame());
            if (frame == null) {
                return "pellicule";
            }
            // Le procédé, comme Exposure le choisit : la vue seule en tirage simple, la couche où en est la feuille en chromatique.
            int couche = papier.getItem() instanceof ChromaticSheetItem feuille ? feuille.getLayers(papier).size() : 0;
            PrintingProcess procede = labor.getActualPrintingMode() == PrintingMode.CHROMATIC ? PrintingProcess.fromChromaticStep(couche)
                    : PrintingProcess.fromExposureType(frame.type());
            if (!labor.canOutputToResultSlot(frame, procede)) {
                return "tirage";
            }
            for (DyeColor teinte : procede.getRequiredDyes()) {
                if (labor.getItem(LightroomBlockEntity.DYE_SLOTS.get(teinte)).isEmpty()) {
                    return "teintures";
                }
            }
            return "papier_procede";
        }
    }
}
