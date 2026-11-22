package io.github.salvagedisc.fallow.postes.client;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.scene.Visual;

import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.tannerie.ChevaletBlock;
import io.github.salvagedisc.fallow.tannerie.TannerieModule;

/**
 * Le chevalet au poste, sur le modèle de la planche à découper de Hands-On Delight : le couteau qu'on tient n'est plus au
 * curseur, il est posé sur la peau, à l'endroit visé ; un clic donne un coup qui glisse vers soi, soulève des copeaux et fait
 * trembler la peau. La peau brute pâlit coup après coup jusqu'à la peau raclée. Au-dessus flotte, pâle, ce qu'il faut poser
 * ensuite : une peau brute, puis un couteau.
 */
final class ChevaletAllure implements Allure {
    /** Durée d'un coup : la lame part vers soi, puis revient. */
    private static final float COUP = 0.32F;
    private static final int CHAIR = 0xFFC07A62;

    private final RandomSource random = RandomSource.create();
    private float horloge;
    /** Temps depuis le dernier coup ; au-delà de {@link #COUP} le couteau suit le curseur. */
    private float coup = 9.0F;
    private float vise = 8.0F;
    private Item tenu;
    private Item fantome;
    private boolean peau;
    private int raclage;
    private final Envol envol = new Envol();
    /** Le couteau d'un autre joueur qui racle ici, vu par celui qui regarde. */
    private Item autre;
    /** Vrai pendant un coup donné par un autre joueur. */
    private boolean coupAutre;
    /** Les coups cliqués pendant qu'un autre se donnait, joués à la suite. */
    private int enAttente;
    private final Arrivee.Volee volee = new Arrivee.Volee();
    /** Faux jusqu'au premier tick du poste : une peau déjà tendue n'arrive pas. */
    private boolean connu;

    @Override
    public void onOpen(StationView view) {
        tenu = null;
        fantome = null;
        connu = false;
    }

    /** Le dessus de la peau pliée sur la poutre, en pixels. */
    private static final float DESSUS = 12.0F;

    private static boolean couteau(ItemStack stack) {
        return stack.is(TannerieModule.COUTEAUX);
    }

    @Override
    public void frame(StationView view, float dt) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof ChevaletBlock)) {
            return;
        }
        horloge += dt;
        coup += dt;
        if (enAttente > 0 && coup >= COUP * 0.6F) {
            enAttente--;
            coupAutre = false;
            if (state.getValue(ChevaletBlock.PEAU)) {
                coup(view);
            } else {
                enAttente = 0;
            }
        }
        envol.frame(view, dt);
        volee.frame(view, dt);
        boolean shown = view.isBlockHidden();
        boolean hide = state.getValue(ChevaletBlock.PEAU);
        float done = Math.min(1.0F, state.getValue(ChevaletBlock.RACLAGE) / (float) Math.max(1, Math.min(15, FallowConfig.tannerieStrokes())));
        view.node("chevalet").ifPresent(node -> node.setVisible(shown));
        view.node("peau").ifPresent(node -> node.setVisible(shown && hide));
        view.node("raclee").ifPresent(node -> {
            node.setVisible(shown && hide && done > 0.0F);
            node.setTint((int) (done * 0.85F * 255.0F) << 24 | 0xFFFFFF);
        });

        LocalPlayer player = Minecraft.getInstance().player;
        ItemStack carried = player == null ? ItemStack.EMPTY : player.inventoryMenu.getCarried();
        boolean mien = shown && view.isFocused() && hide && couteau(carried);
        view.setCarriedHidden(mien);
        // Celui qui regarde voit le couteau de l'autre le temps de son coup.
        Item lame = shown && hide && coupAutre && coup < COUP ? autre : mien ? carried.getItem() : null;
        boolean knife = lame != null;
        view.node("couteau").ifPresent(node -> {
            node.setVisible(knife);
            if (!knife) {
                return;
            }
            if (lame != tenu) {
                tenu = lame;
                node.setVisual(Visual.ofItem(tenu));
            }
            if (coup >= COUP) {
                view.cursorOn(DESSUS).ifPresent(point -> vise = Mth.clamp(point.z(), 8.5F, 11.5F));
            }
            // Un coup : la lame appuie et file de 4 pixels vers soi, puis revient se poser.
            float t = Math.min(1.0F, coup / COUP);
            float stroke = t < 0.45F ? -4.0F * Mth.sin(t / 0.45F * Mth.HALF_PI) : -4.0F * (1.0F - (t - 0.45F) / 0.55F);
            float press = t < 0.45F ? -0.5F * Mth.sin(t / 0.45F * Mth.PI) : 0.0F;
            float rest = 0.15F * Mth.sin(horloge * 2.0F);
            float z = vise + stroke;
            node.moveTo(0.0F, press + rest, z - 8.0F, 0.0F);
            node.rotateTo(t < 0.45F ? 12.0F * Mth.sin(t / 0.45F * Mth.PI) : 0.0F, 0.0F, 0.0F, 0.0F);
        });

        // La poutre vide montre, pâle et battant doucement, la peau à y tendre ; la peau posée attend le couteau.
        boolean inUse = shown && view.isFocused();
        boolean wantsHide = inUse && !hide && !carried.is(TannerieModule.PEAU_BRUTE.get());
        view.node("fantome").ifPresent(node -> {
            node.setVisible(wantsHide);
            if (wantsHide) {
                node.setTint((int) (110.0F + 45.0F * Mth.sin(horloge * 2.6F)) << 24 | 0xFFFFFF);
            }
        });
        Item wanted = inUse && hide && !couteau(carried) ? couteauAttendu() : null;
        view.node("fantome_couteau").ifPresent(node -> {
            node.setVisible(wanted != null);
            if (wanted != fantome) {
                fantome = wanted;
                if (wanted != null) {
                    node.setVisual(Visual.ofItem(wanted).withTint(0x70FFFFFF).asTranslucent());
                }
            }
            if (wanted != null) {
                node.moveTo(0.0F, 0.5F * Mth.sin(horloge * 2.2F), 0.0F, 0.0F);
            }
        });
    }

    /** Le premier couteau à écharner connu, montré pâle quand la peau l'attend. */
    private static Item couteauAttendu() {
        return TannerieBehavior.premier(TannerieModule.COUTEAUX);
    }

    @Override
    public void geste(StationView view, ItemStack stack) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof ChevaletBlock) || !state.getValue(ChevaletBlock.PEAU) || !couteau(stack)) {
            return;
        }
        if (coup < COUP * 0.6F) {
            // Cliqué plus vite que la lame : le coup attend son tour, comme les coups de marteau de Hands-On Vanilla.
            enAttente = Math.min(8, enAttente + 1);
            return;
        }
        coupAutre = false;
        coup(view);
    }

    @Override
    public void gesteAutre(StationView view, ItemStack stack) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof ChevaletBlock) || !state.getValue(ChevaletBlock.PEAU) || !couteau(stack) || coup < COUP * 0.6F) {
            return;
        }
        // Le coup d'un autre : son couteau à lui, au milieu de la peau ; son bruit vient du bloc, son bras n'est pas le nôtre.
        autre = stack.getItem();
        if (!view.isFocused()) {
            vise = 8.0F;
        }
        coupAutre = true;
        coup(view);
    }

    /** Un coup de couteau : la lame part, les copeaux volent, la peau tremble. */
    private void coup(StationView view) {
        coup = 0.0F;
        // Les copeaux partent de la lame, vers soi et un peu en l'air ; la peau tremble sous le coup.
        view.spawnParticles(DiegesisParticles.DUST, new Vector3f(8.0F, DESSUS + 0.2F, vise - 1.0F), new Vector3f(0.0F, 6.0F, -10.0F), 1.5F, 12, CHAIR);
        view.spawnParticles(DiegesisParticles.DUST, new Vector3f(8.0F, DESSUS + 0.2F, vise - 2.5F), new Vector3f(0.0F, 3.0F, -6.0F), 1.0F, 6, 0xFFE8D6C0);
        view.node("peau").ifPresent(node -> node.shake(0.25F, 0.15F));
        view.node("raclee").ifPresent(node -> node.shake(0.25F, 0.15F));
        if (coupAutre) {
            return;
        }
        // Le même raclement que le bloc, que le serveur fait entendre aux autres et pas à nous.
        Son.joue(view, SoundEvents.BRUSH_GENERIC, "couteau", 0.45F, 0.8F + random.nextFloat() * 0.3F);
        LocalPlayer player = Minecraft.getInstance().player;
        if (player != null && view.isFocused()) {
            player.swing(net.minecraft.world.InteractionHand.MAIN_HAND, false);
        }
    }

    @Override
    public void tick(StationView view) {
        BlockState state = view.level().getBlockState(view.pos());
        if (!(state.getBlock() instanceof ChevaletBlock)) {
            return;
        }
        boolean hide = state.getValue(ChevaletBlock.PEAU);
        if (peau && !hide && view.isBlockHidden()) {
            // La peau est finie (ou reprise) : elle quitte le chevalet dans un éclat et vole jusqu'à la barre.
            view.spawnParticles(DiegesisParticles.GLINT, new Vector3f(8.0F, DESSUS + 0.3F, 8.0F), new Vector3f(0.0F, 4.0F, 0.0F), 4.0F, 8, 0xFFFFF0D0);
            envol.part(view, raclage > 0 ? TannerieModule.PEAU_RACLEE.get() : TannerieModule.PEAU_BRUTE.get(), new Vector3f(8.0F, DESSUS + 0.5F, 8.0F));
        }
        if (connu && !peau && hide && view.isBlockHidden()) {
            volee.arrive(view, TannerieModule.PEAU_BRUTE.get(), new Vector3f(8.0F, DESSUS + 0.5F, 8.0F));
        }
        connu = true;
        peau = hide;
        raclage = hide ? state.getValue(ChevaletBlock.RACLAGE) : 0;
    }
}
