package io.github.salvagedisc.fallow.postes.client;

import java.util.List;
import java.util.function.ToDoubleFunction;

import org.joml.Vector3f;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;

import com.pockyl.diegesis.api.client.DiegesisParticles;
import com.pockyl.diegesis.api.client.StationBehavior;
import com.pockyl.diegesis.api.client.StationView;
import com.pockyl.diegesis.api.state.SlotRef;

/**
 * La conduite commune des postes faits des blocs d'autres mods (les fours d'Overgeared, ses tables, celles de Cold Sweat) : le
 * poste tient la fenêtre du mod (son menu ; côté serveur rien ne change) et en pose les cases sur le bloc.
 *
 * <ul>
 * <li>L'inventaire grise ce qu'aucune case du poste ne prend (les cases du menu le disent elles-mêmes).</li>
 * <li>Ce qui arrive par Maj + clic vole de sa case de la barre jusqu'à sa place ({@link Arrivee}) ; ce qu'on lâche du curseur,
 * on l'a vu partir de la main.</li>
 * <li>Un tintement quand une pièce se pose, un glissement quand elle part, comme à l'enclume de Hands-On Vanilla.</li>
 * <li>Si le poste a un {@code avancement}, sa jauge ({@link Jauge}) montre où en est l'ouvrage, sur le nœud du même nom.</li>
 * <li>Si le bloc brûle (son état {@code lit}), des flammes et un peu de fumée montent du nœud {@code feu}, et le feu crépite.</li>
 * <li>Un souffle ({@link #avecSouffle}) : la vapeur de la chaudière, le froid de la glacière, qui monte ou tombe du nœud
 * {@code souffle} tant que le poste travaille.</li>
 * </ul>
 */
class PosteDeMod implements StationBehavior {
    /** Une case du poste : son nœud dans la scène et sa place dans l'état du poste. */
    record Case(String noeud, SlotRef ref) {
        static Case of(String noeud, String groupe, int rang) {
            return new Case(noeud, SlotRef.of(groupe, rang));
        }
    }

    private final List<Case> cases;
    /** Combien de cases du menu, depuis la première, sont celles du poste (le reste est l'inventaire du joueur). */
    private final int casesDuMenu;
    private final ToDoubleFunction<AbstractContainerMenu> avancement;
    private final Jauge jauge;
    private final int[] comptes;
    private boolean connu;
    private int porte;
    private float flammes;
    private float fumee;
    protected final RandomSource random = RandomSource.create();
    /** La dernière part lue pour la jauge, de 0 à 1, ou négative. */
    protected double part = -1.0;
    /** Vrai si le bloc du poste brûle, lu à la dernière image. */
    protected boolean feu;
    private Souffle souffle;
    private float souffles;

    /** Un souffle de particules : sa couleur (ARGB), sa vitesse verticale (pixels par seconde) et quand il part. */
    record Souffle(int couleur, float vitesse, java.util.function.Predicate<PosteDeMod> quand) {
    }

    /**
     * {@code avancement} : la part de l'ouvrage faite, de 0 à 1, lue dans le menu, ou une valeur négative quand rien ne se fait ;
     * {@code null} si le poste n'a pas de jauge. {@code debut} et {@code fin} : les couleurs de la jauge.
     */
    PosteDeMod(List<Case> cases, int casesDuMenu, ToDoubleFunction<AbstractContainerMenu> avancement, int debut, int fin) {
        this.cases = cases;
        this.casesDuMenu = casesDuMenu;
        this.avancement = avancement;
        this.jauge = avancement == null ? null : new Jauge(debut, fin);
        this.comptes = new int[cases.size()];
    }

    /** Ajoute un souffle, qui part du nœud {@code souffle} quand {@code quand} est vrai. */
    PosteDeMod avecSouffle(int couleur, float vitesse, java.util.function.Predicate<PosteDeMod> quand) {
        this.souffle = new Souffle(couleur, vitesse, quand);
        return this;
    }

    @Override
    public void onSceneReady(StationView view) {
        if (jauge != null) {
            view.node("avancement").ifPresent(node -> node.setPainter((pose, buffers, light) -> jauge.paint(pose, buffers)));
        }
    }

    @Override
    public void onOpen(StationView view) {
        connu = false;
        view.setInventoryFilter(stack -> prend(view, stack));
    }

    /** Vrai si une case du poste prend cet objet. */
    private boolean prend(StationView view, ItemStack stack) {
        if (view.menu() == null) {
            return true;
        }
        AbstractContainerMenu menu = view.menu().handle();
        for (int i = 0; i < Math.min(casesDuMenu, menu.slots.size()); i++) {
            Slot slot = menu.slots.get(i);
            if (slot.mayPlace(stack)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void tick(StationView view) {
        if (!view.isFocused() || view.menu() == null) {
            return;
        }
        LocalPlayer joueur = Minecraft.getInstance().player;
        int tenu = joueur == null ? 0 : joueur.containerMenu.getCarried().getCount();
        boolean deLaMain = tenu < porte;
        porte = tenu;
        int pose = 0;
        int pris = 0;
        for (int i = 0; i < cases.size(); i++) {
            ItemStack stack = view.state().getItem(cases.get(i).ref());
            int now = stack.getCount();
            if (connu && now > comptes[i]) {
                if (!deLaMain && !Arrivee.duCurseur(view)) {
                    Arrivee.caseDepuisLaBarre(view, cases.get(i).noeud(), stack.getItem(), pose);
                } else {
                    Son.joue(view, SoundEvents.ITEM_FRAME_ADD_ITEM, cases.get(i).noeud(), 0.35F, 1.0F);
                }
                pose++;
            } else if (connu && now < comptes[i]) {
                pris++;
            }
            comptes[i] = now;
        }
        if (pris > 0) {
            Son.joue(view, SoundEvents.ITEM_FRAME_REMOVE_ITEM, cases.get(0).noeud(), 0.3F, 1.0F);
        }
        connu = true;
        if (brule(view) && random.nextFloat() < 0.06F) {
            Son.joue(view, SoundEvents.FURNACE_FIRE_CRACKLE, "feu", 0.4F, 0.9F + random.nextFloat() * 0.2F);
        }
    }

    @Override
    public void frame(StationView view, float deltaSeconds) {
        if (avancement != null) {
            part = view.menu() == null ? -1.0 : avancement.applyAsDouble(view.menu().handle());
            jauge.frame((float) part, deltaSeconds);
        }
        feu = brule(view);
        souffle(view, deltaSeconds);
        Son.boucle(view, "feu", SoundEvents.FIRE_AMBIENT, view.isFocused() && feu ? 0.25F : 0.0F, 0.9F);
        if (!view.isFocused() || !feu || view.node("feu").isEmpty()) {
            return;
        }
        Vector3f at = new Vector3f(view.node("feu").get().definition().transform().pos());
        flammes += deltaSeconds * 4.0F;
        while (flammes >= 1.0F) {
            flammes -= 1.0F;
            view.spawnParticles(DiegesisParticles.FLAME, new Vector3f(at.x - 3.0F + random.nextFloat() * 6.0F, at.y + random.nextFloat(), at.z),
                    new Vector3f(0.0F, 2.5F, -0.3F), 0.3F, 1, 0xFFFFB050);
        }
        fumee += deltaSeconds * 0.6F;
        while (fumee >= 1.0F) {
            fumee -= 1.0F;
            view.spawnParticles(DiegesisParticles.SMOKE, new Vector3f(at.x - 2.0F + random.nextFloat() * 4.0F, at.y + 4.0F, at.z - 0.2F),
                    new Vector3f(0.0F, 2.5F, -0.6F), 0.5F, 1, 0x60404040);
        }
    }

    private void souffle(StationView view, float dt) {
        if (souffle == null || !view.isFocused() || !souffle.quand().test(this) || view.node("souffle").isEmpty()) {
            return;
        }
        Vector3f at = new Vector3f(view.node("souffle").get().definition().transform().pos());
        souffles += dt * 2.5F;
        while (souffles >= 1.0F) {
            souffles -= 1.0F;
            view.spawnParticles(DiegesisParticles.STEAM, new Vector3f(at.x - 4.0F + random.nextFloat() * 8.0F, at.y, at.z - 4.0F
                    + random.nextFloat() * 8.0F), new Vector3f(0.0F, souffle.vitesse(), 0.0F), 0.4F, 1, souffle.couleur());
        }
    }

    /** Vrai si le bloc du poste brûle. */
    static boolean brule(StationView view) {
        BlockState state = view.level().getBlockState(view.pos());
        return state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT);
    }

    @Override
    public boolean onItemClick(StationView view, String node, ItemStack stack, int button, boolean shift) {
        Arrivee.parLeCurseur(view);
        return false;
    }

    @Override
    public boolean onInventoryShiftClick(StationView view, int slot, ItemStack stack) {
        Arrivee.depuis(view, slot);
        return false;
    }
}
