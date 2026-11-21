package io.github.salvagedisc.fallow.postes.client;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.StationView;

import io.github.salvagedisc.fallow.tannerie.SechoirBlock;
import io.github.salvagedisc.fallow.tannerie.TannerieModule;

/**
 * Le séchoir au poste : le cuir tendu sèche à vue. Mouillé, il est sombre et goutte par le bas ; il pâlit à mesure que le
 * séchage avance, se resserre un peu dans son cadre, et fume quand un feu ou le soleil le chauffe. Quand il est sec, un éclat et
 * le bruit du cuir le disent ; survolé, il donne où en est le séchage et pourquoi. Le cadre vide montre, pâle et battant
 * doucement, le cuir à tendre.
 */
final class SechoirAllure implements Allure {
    private static final int EAU = 0xC06E8FB8;
    private static final int VAPEUR = 0x60E8E8E8;

    private final RandomSource random = RandomSource.create();
    private float horloge;
    private float goutte;
    private float fumee;
    private float lueur;
    /** Le séchage montré, qui rejoint doucement celui du bloc (il avance par paliers de 20 secondes). */
    private float montre = -1.0F;
    private SechoirBlock.Etat avant;
    private final Envol envol = new Envol();
    private final Arrivee.Volee volee = new Arrivee.Volee();
    /** Le séchage, du cuir mouillé au cuir sec. */
    private final Jauge jauge = new Jauge(0x6E4A2E, 0xE0B070);

    @Override
    public void onSceneReady(StationView view) {
        view.node("jauge").ifPresent(node -> {
            node.setPainter((pose, buffers, light) -> jauge.paint(pose, buffers));
            node.setOnTop(true);
        });
    }

    @Override
    public void onOpen(StationView view) {
        montre = -1.0F;
        avant = null;
    }

    @Override
    public void frame(StationView view, float dt) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof SechoirBlock)) {
            return;
        }
        horloge += dt;
        envol.frame(view, dt);
        volee.frame(view, dt);
        boolean shown = view.isBlockHidden();
        SechoirBlock.Etat etat = state.getValue(SechoirBlock.ETAT);
        float vise = switch (etat) {
            case VIDE -> 0.0F;
            case HUMIDE -> Math.min(1.0F, state.getValue(SechoirBlock.SECHAGE) / (float) SechoirBlock.needed());
            case SEC -> 1.0F;
        };
        montre = montre < 0.0F || etat == SechoirBlock.Etat.VIDE ? vise : montre + (vise - montre) * Math.min(1.0F, dt * 1.2F);
        float sec = montre;

        view.node("cadre").ifPresent(node -> node.setVisible(shown));
        jauge.frame(shown && etat == SechoirBlock.Etat.HUMIDE ? sec : -1.0F, dt);
        boolean tendu = shown && etat != SechoirBlock.Etat.VIDE;
        // En séchant, le cuir se resserre : 4 % de moins quand il est sec.
        float serre = 1.0F - 0.04F * sec;
        view.node("cuir").ifPresent(node -> {
            node.setVisible(tendu);
            node.scaleTo(serre, 0.0F);
        });
        // L'infobulle va sur la surface, qui prend le survol (le cuir ne le prend pas).
        view.node("surface").ifPresent(node -> node.setTooltip(tendu ? lecture(view, etat, sec) : null));
        view.node("sec").ifPresent(node -> {
            node.setVisible(tendu && sec > 0.0F);
            node.setTint((int) (Mth.clamp(sec, 0.0F, 1.0F) * 255.0F) << 24 | 0xFFFFFF);
            node.scaleTo(serre, 0.0F);
        });
        // Sec, le cuir est à prendre : il luit par moments, et s'entoure sous le curseur à main vide, comme ce qui sort d'un
        // poste de Hands-On.
        boolean pret = shown && view.isFocused() && etat == SechoirBlock.Etat.SEC;
        LocalPlayer joueur = Minecraft.getInstance().player;
        boolean mainVide = joueur != null && joueur.containerMenu.getCarried().isEmpty();
        boolean survole = pret && mainVide && view.node("surface").map(node -> node.isHovered()).orElse(false);
        view.node("cuir").ifPresent(node -> node.setOutlined(survole));
        view.node("sec").ifPresent(node -> node.setOutlined(survole));
        if (pret) {
            lueur += dt;
            if (lueur >= 1.8F) {
                lueur = 0.0F;
                float haut = 4.0F + random.nextFloat() * 9.0F;
                view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(4.0F + random.nextFloat() * 8.0F, haut, devant(haut) - 0.6F),
                        new Vector3f(0.0F, 0.5F, -1.0F), 0.5F, 2, 0xFFFFF0D0);
            }
        }

        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack carried = player == null ? ItemStack.EMPTY : player.inventoryMenu.getCarried();
        boolean wanted = shown && view.isFocused() && etat == SechoirBlock.Etat.VIDE && !carried.is(TannerieModule.CUIR_MOUILLE.get());
        view.node("fantome").ifPresent(node -> {
            node.setVisible(wanted);
            if (wanted) {
                node.setTint((int) (70.0F + 40.0F * Mth.sin(horloge * 2.6F)) << 24 | 0xFFFFFF);
            }
        });

        if (!shown || etat != SechoirBlock.Etat.HUMIDE) {
            return;
        }
        // Le cuir mouillé goutte par le bas, moins à mesure qu'il sèche.
        goutte += dt * (2.2F * (1.0F - sec) + 0.2F);
        while (goutte >= 1.0F) {
            goutte -= 1.0F;
            float x = 3.0F + random.nextFloat() * 10.0F;
            view.spawnParticles(DiegesisParticles.DROPLET, new Vector3f(x, 3.2F, devant(3.2F) + 0.2F), new Vector3f(0.0F, -2.0F, 0.0F), 0.2F, 1, EAU);
        }
        // La chaleur le fait fumer : un feu tout près, ou le plein soleil.
        String cause = cause(view);
        float chaleur = "feu".equals(cause) ? 1.6F : "soleil".equals(cause) ? 0.6F : 0.0F;
        fumee += dt * chaleur * (1.0F - 0.6F * sec);
        while (fumee >= 1.0F) {
            fumee -= 1.0F;
            float x = 3.0F + random.nextFloat() * 10.0F;
            float y = 5.0F + random.nextFloat() * 8.0F;
            view.spawnParticles(DiegesisParticles.STEAM, new Vector3f(x, y, devant(y) - 0.5F), new Vector3f(0.0F, 3.0F, -0.6F), 0.5F, 1, VAPEUR);
        }
    }

    /** La face avant du cuir à la hauteur {@code y} (pixels) : le cadre penche vers l'arrière de 22,5°. */
    private static float devant(float y) {
        return 3.8F + 0.414F * y;
    }

    private Component lecture(StationView view, SechoirBlock.Etat etat, float sec) {
        if (etat == SechoirBlock.Etat.SEC) {
            return Component.translatable("postes.fallow.sechoir.sec");
        }
        return Component.translatable("tannerie.fallow.sechoir.seche", Math.round(sec * 100.0F),
                Component.translatable("tannerie.fallow.sechoir.cause." + cause(view)));
    }

    /** Ce qui sèche le cuir (un feu tout près, le soleil, l'air), relu à chaque tick : la recherche du feu coûte. */
    private String causeLue;

    private String cause(StationView view) {
        if (causeLue == null) {
            causeLue = SechoirBlock.cause(view.level(), view.pos());
        }
        return causeLue;
    }

    @Override
    public void geste(StationView view, ItemStack stack) {
        BlockState state = view.level().getBlockState(view.pos());
        if (state.getBlock() instanceof SechoirBlock && state.getValue(SechoirBlock.ETAT) == SechoirBlock.Etat.VIDE
                && stack.is(TannerieModule.CUIR_MOUILLE.get())) {
            // Le cuir claque en se tendant : il éclabousse un peu.
            view.spawnParticles(DiegesisParticles.SPLASH, new Vector3f(8.0F, 8.5F, devant(8.5F) - 0.5F), new Vector3f(0.0F, 1.0F, -3.0F), 4.0F, 8, EAU);
        }
    }

    @Override
    public void tick(StationView view) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof SechoirBlock)) {
            return;
        }
        SechoirBlock.Etat etat = state.getValue(SechoirBlock.ETAT);
        causeLue = SechoirBlock.cause(view.level(), view.pos());
        if (avant != null && avant != etat && view.isBlockHidden()) {
            switch (etat) {
                case HUMIDE -> {
                    volee.arrive(view, TannerieModule.CUIR_MOUILLE.get(), new Vector3f(8.0F, 8.5F, devant(8.5F) - 0.5F));
                    view.node("cuir").ifPresent(node -> node.shake(0.4F, 0.25F));
                }
                case SEC -> {
                    view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(8.0F, 8.5F, devant(8.5F) - 0.5F), new Vector3f(0.0F, 1.5F, -1.0F), 5.0F, 10, 0xFFFFF0D0);
                    Son.joue(view, SoundEvents.ARMOR_EQUIP_LEATHER.value(), "cuir", 0.6F, 1.3F);
                }
                case VIDE -> {
                    view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(8.0F, 8.5F, devant(8.5F) - 0.5F), new Vector3f(0.0F, 3.0F, -2.0F), 4.0F, 6, 0xFFFFF0D0);
                    envol.part(view, avant == SechoirBlock.Etat.SEC ? net.minecraft.world.item.Items.LEATHER : TannerieModule.CUIR_MOUILLE.get(),
                            new Vector3f(8.0F, 7.0F, devant(7.0F) - 1.0F));
                }
            }
        }
        avant = etat;
    }
}
