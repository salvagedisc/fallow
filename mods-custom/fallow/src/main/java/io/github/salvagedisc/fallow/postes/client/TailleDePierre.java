package io.github.salvagedisc.fallow.postes.client;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.mojang.math.Axis;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

import net.stirdrem.overgeared.screen.RockKnappingMenu;

/**
 * La taille de pierre d'Overgeared sans fenêtre, dans l'esprit de Hands-On : une pierre dans chaque main, on garde le monde sous
 * les yeux et l'on taille celle qu'on tient, montrée en grand au milieu de l'écran, trois éclats sur trois, de la texture même que
 * le mod donne à cette pierre (un texel du bloc pour deux pixels). Un clic sur un éclat le détache : il saute, tourne et tombe, avec
 * le bruit de la pierre ; la pierre garde ses arêtes, claires en haut à gauche, sombres en bas à droite, là où elle est encore
 * entière. Ce que la taille donne flotte à côté ; un clic le range dans l'inventaire (il file vers la barre), et l'écran se
 * referme de lui-même, le geste fini. Le menu d'Overgeared reste le même : chaque
 * éclat passe par son bouton ({@code clickMenuButton}), comme sa fenêtre le fait par son paquet. Remplace sa fenêtre
 * ({@code postes.TailleDePierreMixin}) quand le réglage {@code postes.taille} est vrai. Ce n'est pas un écran de conteneur : Spatial
 * GUI, qui porte les fenêtres dans le monde, le laisse à sa place, devant les yeux.
 */
public final class TailleDePierre extends Screen implements MenuAccess<RockKnappingMenu> {
    /** Le côté d'un éclat à l'écran : seize texels de la texture, deux pixels chacun. */
    private static final int ECLAT = 32;
    /** La case du résultat dans le menu d'Overgeared (la barre, l'inventaire, la grille, puis lui). */
    private static final int RESULTAT = 45;
    private static final ResourceLocation PIERRE = ResourceLocation.withDefaultNamespace("textures/block/stone.png");

    private final RandomSource random = RandomSource.create();
    private final List<Morceau> morceaux = new ArrayList<>();
    private float temps;
    private float vuDepuis = -1.0F;
    /** Le résultat qui part vers l'inventaire : l'objet, et depuis quand. */
    private ItemStack parti = ItemStack.EMPTY;
    private float partiDepuis;
    private float partiX;
    private float partiY;
    /** Depuis quand la taille est finie (l'ouvrage rangé) : le geste fini, l'écran se referme de lui-même. */
    private float finie = -1.0F;

    private final RockKnappingMenu menu;

    public TailleDePierre(RockKnappingMenu menu, Inventory inventory, Component title) {
        super(title);
        this.menu = menu;
    }

    @Override
    public RockKnappingMenu getMenu() {
        return menu;
    }

    /** Un éclat détaché : un carré de deux texels de la pierre qui saute et tombe. */
    private static final class Morceau {
        float x;
        float y;
        float vx;
        float vy;
        float angle;
        float tour;
        float age;
        float u;
        float v;
    }

    private ResourceLocation texture() {
        ResourceLocation tex = menu.getUnchippedTexture();
        return tex == null ? PIERRE : tex;
    }

    /** Le bord gauche de la pierre à l'écran, un peu à gauche du milieu pour laisser la place de l'ouvrage. */
    private int gauche() {
        return width / 2 - ECLAT * 3 / 2 - 24;
    }

    /** Le bord haut de la pierre à l'écran, un pixel de souffle en plus. */
    private int haut() {
        return height / 2 - ECLAT * 3 / 2 + 12 + Math.round(Mth.sin(temps * 1.6F) * 1.0F);
    }

    private int eclatSous(double mx, double my) {
        int cx = Mth.floor((mx - gauche()) / ECLAT);
        int cy = Mth.floor((my - haut()) / ECLAT);
        return cx < 0 || cy < 0 || cx > 2 || cy > 2 ? -1 : cy * 3 + cx;
    }

    private boolean libre(int i) {
        return i >= 0 && i < 9 && !menu.isChipped(i) && !menu.isKnappingFinished() && !menu.isResultCollected();
    }

    /** Le résultat flotte à droite de la pierre. */
    private int resultatX() {
        return gauche() + ECLAT * 3 + 34;
    }

    private int resultatY() {
        return height / 2 - 4 + Math.round(Mth.sin(temps * 2.2F + 1.0F) * 1.5F);
    }

    private boolean surResultat(double mx, double my) {
        return Math.abs(mx - (resultatX() + 16)) <= 18 && Math.abs(my - (resultatY() + 16)) <= 18;
    }

    @Override
    public void render(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
        float dt = minecraft == null ? 0.05F : minecraft.getTimer().getRealtimeDeltaTicks() / 20.0F;
        temps += dt;
        // Rien derrière : le monde, et les mains qui tiennent les pierres, restent sous les yeux.
        int x0 = gauche();
        int y0 = haut();
        ResourceLocation tex = texture();
        int sous = eclatSous(mouseX, mouseY);
        for (int i = 0; i < 9; i++) {
            if (menu.isChipped(i)) {
                continue;
            }
            eclat(gfx, tex, i, x0 + i % 3 * ECLAT, y0 + i / 3 * ECLAT, i == sous && libre(i));
        }
        morceaux(gfx, tex, dt);
        resultat(gfx, mouseX, mouseY, dt);
        if ((menu.isKnappingFinished() || menu.isResultCollected()) && parti.isEmpty() && morceaux.isEmpty()) {
            finie = finie < 0.0F ? 0.0F : finie + dt;
            if (finie > 0.35F) {
                onClose();
                return;
            }
        }
        if (sous >= 0 && libre(sous)) {
            gfx.renderTooltip(font, Component.translatable("postes.fallow.taille.eclat"), mouseX, mouseY);
        } else if (surResultat(mouseX, mouseY) && !menu.getSlot(RESULTAT).getItem().isEmpty()) {
            gfx.renderTooltip(font, menu.getSlot(RESULTAT).getItem(), mouseX, mouseY);
        }
    }

    /**
     * L'éclat {@code i}, ligne de texels par ligne de texels. Là où la pierre s'arrête (le bord, ou un éclat déjà parti), son arête
     * est éclairée en haut et à gauche, assombrie en bas et à droite ; et un coin où deux arêtes se rejoignent est abattu en trois
     * marches d'un texel, pour que la pierre ait l'air d'une pierre, et non d'un carré de bloc.
     */
    private void eclat(GuiGraphics gfx, ResourceLocation tex, int i, int x, int y, boolean vise) {
        int col = i % 3;
        int row = i / 3;
        boolean haut = row == 0 || menu.isChipped(i - 3);
        boolean bas = row == 2 || menu.isChipped(i + 3);
        boolean gauche = col == 0 || menu.isChipped(i - 1);
        boolean droite = col == 2 || menu.isChipped(i + 1);
        int texel = ECLAT / 16;
        for (int r = 0; r < 16; r++) {
            int g = Math.max(haut && gauche ? 3 - r : 0, bas && gauche ? r - 12 : 0);
            int d = Math.max(haut && droite ? 3 - r : 0, bas && droite ? r - 12 : 0);
            int a = x + g * texel;
            int b = x + (16 - d) * texel;
            int yy = y + r * texel;
            gfx.blit(tex, a, yy, b - a, texel, (float) g, (float) r, 16 - g - d, 1, 16, 16);
            boolean bordHaut = haut && (r == 0 || g > 0 || d > 0 && r < 4);
            boolean bordBas = bas && (r == 15 || r > 12 && (g > 0 || d > 0));
            if (bordHaut) {
                gfx.fill(a, yy, b, yy + texel, 0x40FFFFFF);
            } else if (bordBas) {
                gfx.fill(a, yy, b, yy + texel, 0x60000000);
            } else {
                if (gauche) {
                    gfx.fill(a, yy, a + texel, yy + texel, 0x40FFFFFF);
                }
                if (droite) {
                    gfx.fill(b - texel, yy, b, yy + texel, 0x60000000);
                }
            }
            if (vise) {
                gfx.fill(a, yy, b, yy + texel, 0x30FFFFFF);
            }
        }
    }

    private void morceaux(GuiGraphics gfx, ResourceLocation tex, float dt) {
        Iterator<Morceau> it = morceaux.iterator();
        while (it.hasNext()) {
            Morceau m = it.next();
            m.age += dt;
            if (m.age > 0.8F || m.y > height + 8) {
                it.remove();
                continue;
            }
            m.vy += 520.0F * dt;
            m.x += m.vx * dt;
            m.y += m.vy * dt;
            m.angle += m.tour * dt;
            gfx.pose().pushPose();
            gfx.pose().translate(m.x, m.y, 50.0F);
            gfx.pose().mulPose(Axis.ZP.rotationDegrees(m.angle));
            float fondu = m.age < 0.55F ? 1.0F : 1.0F - (m.age - 0.55F) / 0.25F;
            gfx.setColor(1.0F, 1.0F, 1.0F, Math.max(0.0F, fondu));
            gfx.blit(tex, -2, -2, 4, 4, m.u, m.v, 2, 2, 16, 16);
            gfx.setColor(1.0F, 1.0F, 1.0F, 1.0F);
            gfx.pose().popPose();
        }
    }

    private void resultat(GuiGraphics gfx, int mouseX, int mouseY, float dt) {
        ItemStack fait = menu.getSlot(RESULTAT).getItem();
        if (fait.isEmpty()) {
            vuDepuis = -1.0F;
        } else {
            vuDepuis = vuDepuis < 0.0F ? 0.0F : vuDepuis + dt;
            float s = 2.0F * Flottant.sortie(vuDepuis / 0.3F);
            int x = resultatX();
            int y = resultatY();
            // Une ombre douce sous l'objet, puis l'objet, deux fois sa taille.
            gfx.fill(x + 6, y + 36, x + 26, y + 38, 0x30000000);
            gfx.pose().pushPose();
            gfx.pose().translate(x + 16, y + 16, 100.0F);
            gfx.pose().scale(s, s, 1.0F);
            gfx.renderItem(fait, -8, -8);
            gfx.pose().popPose();
            if (surResultat(mouseX, mouseY)) {
                gfx.fill(x - 2, y - 2, x + 34, y + 34, 0x20FFFFFF);
            }
        }
        if (!parti.isEmpty()) {
            partiDepuis += dt;
            float t = partiDepuis / 0.35F;
            if (t >= 1.0F) {
                parti = ItemStack.EMPTY;
                return;
            }
            // Il part vers la barre, en bas au milieu, et rapetisse en chemin.
            float x = Mth.lerp(t * t, partiX, width / 2.0F);
            float y = Mth.lerp(t * t, partiY, height - 12.0F);
            float s = Mth.lerp(t, 2.0F, 0.8F);
            gfx.pose().pushPose();
            gfx.pose().translate(x, y, 100.0F);
            gfx.pose().scale(s, s, 1.0F);
            gfx.renderItem(parti, -8, -8);
            gfx.pose().popPose();
        }
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int i = eclatSous(mouseX, mouseY);
        if (button == 0 && libre(i)) {
            taille(i);
            return true;
        }
        ItemStack fait = menu.getSlot(RESULTAT).getItem();
        if (button == 0 && !fait.isEmpty() && surResultat(mouseX, mouseY)) {
            ItemStack copie = fait.copy();
            if (minecraft != null && minecraft.gameMode != null && minecraft.player != null) {
                minecraft.gameMode.handleInventoryMouseClick(menu.containerId, RESULTAT, 0, ClickType.QUICK_MOVE, minecraft.player);
            }
            // L'inventaire plein, l'ouvrage reste là (Overgeared le donne ou le pose à terre quand l'écran se ferme).
            if (menu.getSlot(RESULTAT).getItem().isEmpty()) {
                parti = copie;
                partiDepuis = 0.0F;
                partiX = resultatX() + 16;
                partiY = resultatY() + 16;
                jouer(SoundEvents.ITEM_PICKUP, 0.4F, 1.2F);
            }
            return true;
        }
        return true;
    }

    private void taille(int i) {
        menu.setChip(i);
        if (minecraft != null && minecraft.gameMode != null) {
            minecraft.gameMode.handleInventoryButtonClick(menu.containerId, i);
        }
        SoundEvent son = menu.getSound();
        jouer(son == null ? SoundEvents.STONE_BREAK : son, 0.7F, 0.9F + random.nextFloat() * 0.25F);
        int x = gauche() + i % 3 * ECLAT + ECLAT / 2;
        int y = haut() + i / 3 * ECLAT + ECLAT / 2;
        for (int k = 0; k < 7; k++) {
            Morceau m = new Morceau();
            m.x = x + (random.nextFloat() - 0.5F) * ECLAT * 0.7F;
            m.y = y + (random.nextFloat() - 0.5F) * ECLAT * 0.7F;
            float cote = i % 3 - 1;
            m.vx = (cote + (random.nextFloat() - 0.5F) * 1.6F) * 90.0F;
            m.vy = -90.0F - random.nextFloat() * 110.0F;
            m.tour = (random.nextFloat() - 0.5F) * 720.0F;
            m.u = random.nextInt(8) * 2;
            m.v = random.nextInt(8) * 2;
            morceaux.add(m);
        }
    }

    private void jouer(SoundEvent son, float volume, float pitch) {
        if (minecraft != null) {
            minecraft.getSoundManager().play(SimpleSoundInstance.forUI(son, pitch, volume));
        }
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        return true;
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return true;
    }

    /** La touche de l'inventaire referme, comme une fenêtre. */
    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (minecraft != null && minecraft.options.keyInventory.matches(keyCode, scanCode)) {
            onClose();
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public void tick() {
        if (minecraft != null && minecraft.player != null && (!minecraft.player.isAlive() || minecraft.player.isRemoved())) {
            minecraft.player.closeContainer();
        }
    }

    @Override
    public void onClose() {
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.closeContainer();
        }
        super.onClose();
    }

    @Override
    public void renderBackground(GuiGraphics gfx, int mouseX, int mouseY, float partialTick) {
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
