package io.github.salvagedisc.fallow.prise.client;

import io.github.salvagedisc.fallow.prise.Prise;
import io.github.salvagedisc.fallow.prise.PriseTow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Le côté client de la prise : une barque (vanilla ou de Fish 'N' Ships) avance selon le client de celui qui la mène,
 * alors c'est ici que le poisson la tire, d'après ce que le serveur envoie ({@link PriseTow}).
 */
public final class PriseClient {
    /** Un ordre du serveur vaut pour ce nombre de ticks ; il en renvoie un tous les quatre. */
    private static final int LIFETIME = 8;
    private static PriseTow tow;
    private static int left;

    private PriseClient() {
    }

    public static void start() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    public static void receive(PriseTow payload) {
        tow = payload;
        left = LIFETIME;
    }

    private static void tick() {
        if (tow == null) {
            return;
        }
        if (--left < 0) {
            tow = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        Entity boat = player.getVehicle();
        if (boat == null || !boat.isControlledByLocalInstance()) {
            return;
        }
        Vec3 v = boat.getDeltaMovement();
        // Vitesses en blocs par tick : ×20 pour des blocs par seconde, et l'accélération d'un tick vaut a / 20 / 20.
        double along = (v.x * tow.dx() + v.z * tow.dz()) * 20.0;
        double a = Prise.pull(along, tow.speed()) / 400.0;
        if (a > 0.0) {
            boat.setDeltaMovement(v.add(tow.dx() * a, 0.0, tow.dz() * a));
        }
    }
}
