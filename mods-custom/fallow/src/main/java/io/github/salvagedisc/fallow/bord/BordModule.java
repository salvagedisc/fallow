package io.github.salvagedisc.fallow.bord;

import com.google.gson.JsonObject;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import java.io.Reader;
import java.util.Optional;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Module bord : le monde est un disque, et le vent du large en garde le bord. Le fichier de données
 * {@code data/fallow/monde/bord.json} (centre et rayon, écrit par {@code scripts/terrain/disc.py} avec la forme
 * du continent) donne un cercle dans l'Overworld. À l'approche, un message prévient ; dans les derniers blocs, un
 * vent contraire se lève et repousse vers les terres le joueur, sa monture ou son bateau, de plus en plus fort, jusqu'à
 * ne plus laisser avancer. Les navires de Sable sont repoussés de même par le module navires ({@link #push}).
 * Seul ce qui passerait malgré tout (un vol très rapide) est ramené en deçà, loin derrière le bord.
 *
 * <p>Le bord est rond, ce que la bordure du jeu ne sait pas faire, et il ne se voit pas : en mer, la brume cache
 * déjà le large. Sans le fichier, le module ne fait rien. Les spectateurs passent. Un joueur à bord d'un navire de
 * Sable n'est pas poussé lui-même : c'est son navire qui l'est (voir {@link Ships}).
 */
public final class BordModule {
    private static final ResourceLocation FILE = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "monde/bord.json");
    /** Largeur de la bande, en blocs, où le joueur est prévenu avant le bord. */
    private static final double WARNING = 64.0;
    /** Le joueur est ramené à cette distance en deçà du bord. */
    private static final double STEP_BACK = 2.0;
    /** Largeur de la bande, en blocs, où souffle le vent contraire avant le bord (et l'échelle de sa montée au-delà). */
    public static final double PUSH_BAND = 24.0;
    /** Au-delà du bord, ce qui est encore là après tant de blocs (un vol très rapide) est ramené en deçà. */
    private static final double SAFETY = 48.0;
    /** Vitesse ajoutée vers les terres à chaque tick au bord même, en blocs par tick (le double à 8 blocs au-delà). */
    private static final double PLAYER_PUSH = 0.03;
    /** Un rappel toutes les cinq secondes dans la bande d'approche. */
    private static final int WARNING_INTERVAL = 100;

    private static volatile Edge edge;

    /** Le bord du monde : un cercle dans l'Overworld. */
    public record Edge(double centerX, double centerZ, double radius) {
        /** Le point où ramener ce qui est sorti, ou {@code null} si la position est dans le cercle. */
        public Vec3 inside(double x, double y, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            double distance = Math.sqrt(dx * dx + dz * dz);
            if (distance <= radius) {
                return null;
            }
            double scale = Math.max(0.0, radius - STEP_BACK) / distance;
            return new Vec3(centerX + dx * scale, y, centerZ + dz * scale);
        }

        /** Distance au-delà du bord, en blocs (négative en deçà). */
        public double past(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            return Math.sqrt(dx * dx + dz * dz) - radius;
        }

        /** Direction des terres depuis (x, z), dans le plan : {@code [x, z]} unitaire (vers le centre). */
        public double[] inward(double x, double z) {
            double dx = centerX - x;
            double dz = centerZ - z;
            double length = Math.sqrt(dx * dx + dz * dz);
            return length < 1.0e-6 ? new double[] {0.0, 0.0} : new double[] {dx / length, dz / length};
        }

        boolean isNear(double x, double z) {
            double dx = x - centerX;
            double dz = z - centerZ;
            double near = radius - WARNING;
            return near <= 0.0 || dx * dx + dz * dz >= near * near;
        }
    }

    /**
     * Force du vent contraire, sans unité, à {@code past} blocs au-delà du bord, sur une bande de {@code band} blocs :
     * nulle avant la bande, elle monte doucement (au carré) jusqu'à 1 au bord, puis plus vite au-delà, jusqu'à 4.
     */
    public static double push(double past, double band) {
        if (past <= -band) {
            return 0.0;
        }
        if (past <= 0.0) {
            double s = (past + band) / band;
            return s * s;
        }
        return Math.min(4.0, 1.0 + 3.0 * past / band);
    }

    private BordModule() {
    }

    public static void start(IEventBus modBus) {
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class,
                event -> event.addListener((ResourceManagerReloadListener) BordModule::load));
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, BordModule::onPlayerTick);
        Fallow.LOGGER.info("bord : le bord du monde suit data/fallow/monde/bord.json");
    }

    /** Le bord en vigueur, ou {@code null} s'il n'y en a pas. */
    public static Edge edge() {
        return edge;
    }

    private static void load(ResourceManager manager) {
        Optional<Resource> resource = manager.getResource(FILE);
        if (resource.isEmpty()) {
            edge = null;
            Fallow.LOGGER.info("bord : pas de fichier {}, le monde n'a pas de bord", FILE);
            return;
        }
        try (Reader reader = resource.get().openAsReader()) {
            JsonObject json = GsonHelper.parse(reader);
            Edge loaded = new Edge(GsonHelper.getAsDouble(json, "center_x", 0.0), GsonHelper.getAsDouble(json, "center_z", 0.0),
                    GsonHelper.getAsDouble(json, "radius"));
            if (loaded.radius() <= 0.0) {
                throw new IllegalArgumentException("radius doit être positif");
            }
            edge = loaded;
            Fallow.LOGGER.info("bord : cercle de rayon {} autour de {}, {}", loaded.radius(), loaded.centerX(), loaded.centerZ());
        } catch (Exception e) {
            edge = null;
            Fallow.LOGGER.error("bord : {} illisible, le monde n'a pas de bord", FILE, e);
        }
    }

    private static void onPlayerTick(PlayerTickEvent.Post event) {
        Edge current = edge;
        if (current == null || !(event.getEntity() instanceof ServerPlayer player) || player.isSpectator()
                || player.level().dimension() != Level.OVERWORLD) {
            return;
        }
        // Les parcelles de Sable sont loin hors du disque : un joueur bien à l'intérieur est à terre, et la
        // question à Sable n'est posée que près du bord ou à bord d'un navire.
        if (!FallowConfig.bordEnabled() || !current.isNear(player.getX(), player.getZ())) {
            return;
        }
        Vec3 position = Ships.worldPosition(player);
        if (!current.isNear(position.x, position.z)) {
            return;
        }
        double past = current.past(position.x, position.z);
        double strength = push(past, PUSH_BAND);
        if (Ships.isAboard(player)) {
            // C'est le navire que le vent repousse (module navires) : le joueur à bord est seulement prévenu.
            if (strength > 0.0 && player.tickCount % WARNING_INTERVAL == 0) {
                player.displayClientMessage(Component.translatable("message.fallow.bord.navire"), true);
            } else if (strength <= 0.0 && player.tickCount % WARNING_INTERVAL == 0) {
                player.displayClientMessage(Component.translatable("message.fallow.bord.proche"), true);
            }
            return;
        }
        Entity root = player.getRootVehicle();
        if (past > SAFETY) {
            bringBack(current, player, root, position);
            return;
        }
        if (strength <= 0.0) {
            if (player.tickCount % WARNING_INTERVAL == 0) {
                player.displayClientMessage(Component.translatable("message.fallow.bord.proche"), true);
            }
            return;
        }
        double[] in = current.inward(position.x, position.z);
        Vec3 motion = root.getDeltaMovement();
        // Ce qui va encore vers le large perd une part de son élan au-delà du bord : on n'y avance plus.
        double outward = -(motion.x * in[0] + motion.z * in[1]);
        double brake = outward > 0.0 && past > 0.0 ? outward * Math.min(0.5, 0.15 * strength) : 0.0;
        double gain = PLAYER_PUSH * strength + brake;
        root.setDeltaMovement(motion.add(in[0] * gain, 0.0, in[1] * gain));
        if (root == player) {
            // Le mouvement du joueur est calculé par son client : il faut lui envoyer, comme un recul.
            player.hurtMarked = true;
        } else {
            // Un bateau ou une monture est dirigé par le client de son pilote.
            for (Entity rider : root.getIndirectPassengers()) {
                if (rider instanceof ServerPlayer riding) {
                    riding.connection.send(new ClientboundSetEntityMotionPacket(root));
                }
            }
        }
        if (player.tickCount % WARNING_INTERVAL == 0) {
            player.displayClientMessage(Component.translatable("message.fallow.bord.vent"), true);
        }
    }

    /** Ramène en deçà du bord ce qui est passé malgré le vent. */
    private static void bringBack(Edge current, ServerPlayer player, Entity root, Vec3 position) {
        Vec3 back = current.inside(position.x, position.y, position.z);
        if (back == null) {
            return;
        }
        if (root == player) {
            player.connection.teleport(back.x, back.y, back.z, player.getYRot(), player.getXRot());
        } else {
            // Un bateau ou une monture est dirigé par le client : il faut lui dire que le véhicule a reculé et s'est arrêté.
            Vec3 rootBack = current.inside(root.getX(), root.getY(), root.getZ());
            if (rootBack == null) {
                rootBack = new Vec3(back.x, root.getY(), back.z);
            }
            root.moveTo(rootBack.x, rootBack.y, rootBack.z, root.getYRot(), root.getXRot());
            root.setDeltaMovement(Vec3.ZERO);
            for (Entity rider : root.getIndirectPassengers()) {
                if (rider instanceof ServerPlayer riding) {
                    riding.connection.send(new ClientboundMoveVehiclePacket(root));
                    riding.connection.send(new ClientboundSetEntityMotionPacket(root));
                }
            }
        }
        player.displayClientMessage(Component.translatable("message.fallow.bord.limite"), true);
    }
}
