package io.github.salvagedisc.fallow.bord.client;

import io.github.salvagedisc.fallow.bord.BordModule;
import io.github.salvagedisc.fallow.bord.BordVent;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * Le côté client du bord : le vent contraire pousse ce que ce client fait avancer, le joueur à pied ou le véhicule
 * qu'il mène, d'après ce que le serveur envoie ({@link BordVent}). Le serveur ne peut pas le faire lui-même sans
 * effacer l'élan que le client a déjà donné.
 */
public final class BordClient {
    /** Un message du serveur vaut pour ce nombre de ticks ; il en envoie un à chaque tick dans la bande. */
    private static final int LIFETIME = 3;
    private static BordVent wind;
    private static int left;

    private BordClient() {
    }

    public static void start() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
    }

    public static void receive(BordVent payload) {
        wind = payload;
        left = LIFETIME;
    }

    private static void tick() {
        if (wind == null) {
            return;
        }
        if (--left < 0) {
            wind = null;
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.isPaused()) {
            return;
        }
        Entity moved = player.getVehicle() == null ? player : player.getRootVehicle();
        if (!moved.isControlledByLocalInstance()) {
            return;
        }
        Vec3 motion = moved.getDeltaMovement();
        double gain = BordModule.gain(motion.x, motion.z, wind.inX(), wind.inZ(), wind.strength(), wind.past());
        moved.setDeltaMovement(motion.add(wind.inX() * gain, 0.0, wind.inZ() * gain));
    }
}
