package io.github.salvagedisc.fallow.prise;

import com.wdiscute.starcatcher.bobentity.FishingBobEntity;
import com.wdiscute.starcatcher.data.attachments.FishingBobAttachment;
import com.wdiscute.starcatcher.fish.FishProperties;
import com.wdiscute.starcatcher.registry.SCDataAttachments;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.ModIds;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.ServerTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;

/**
 * Module prise : la grosse prise de Starcatcher qui tire le bateau. Pendant le combat (le mini-jeu), si le pêcheur est
 * à bord d'un navire de Sable ou dans une barque, le poisson file et emmène le bateau avec lui, d'autant plus vite
 * qu'il est lourd et le bateau léger ({@link Prise}) ; le bouchon suit le poisson. Une grosse barque tient mieux.
 *
 * <p>Le navire de Sable est tiré par le serveur ({@link PriseSable}) ; une barque, que le client de celui qui la mène
 * fait avancer, est tirée chez ce client ({@link PriseTow}).
 */
public final class PriseModule {
    /** Le bouchon de Starcatcher en plein combat : la valeur de {@link FishingBobEntity#STATE} pour FISHING. */
    private static final int FIGHTING = 4;
    /** La barque de base de l'API de Fish 'N' Ships. */
    private static final String BASE_BOAT = "dev.hybridlabs.hapi.entity.base.vehicle.BaseBoatEntity";

    /** Une fuite en cours : le cap de départ, la graine de ses écarts, le tick où elle a commencé, l'annonce faite. */
    private static final class Run {
        double start;
        final double seed;
        final long since;
        /** La ligne au plus pour ce combat : la distance du bouchon au départ, plus le mou que le poisson peut prendre. */
        final double line;
        boolean told;

        Run(double start, double seed, long since, double line) {
            this.start = start;
            this.seed = seed;
            this.since = since;
            this.line = line;
        }
    }

    private static final Map<UUID, Run> RUNS = new HashMap<>();
    private static boolean sable;

    private PriseModule() {
    }

    public static void start(IEventBus modBus) {
        sable = ModIds.isLoaded(ModIds.SABLE);
        if (sable) {
            PriseSable.start();
        }
        modBus.addListener(RegisterPayloadHandlersEvent.class, event -> event.registrar("1")
                .optional()
                .playToClient(PriseTow.TYPE, PriseTow.STREAM_CODEC,
                        (tow, context) -> context.enqueueWork(() -> io.github.salvagedisc.fallow.prise.client.PriseClient.receive(tow))));
        if (FMLEnvironment.dist == Dist.CLIENT) {
            io.github.salvagedisc.fallow.prise.client.PriseClient.start();
        }
        NeoForge.EVENT_BUS.addListener(ServerTickEvent.Post.class, event -> tick(event.getServer()));
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, event -> {
            RUNS.clear();
            if (sable) {
                PriseSable.clear();
            }
        });
        Fallow.LOGGER.info("prise : les grosses prises de Starcatcher tirent le bateau{}", sable ? " (barques et navires de Sable)" : " (barques)");
    }

    private static void tick(MinecraftServer server) {
        if (!FallowConfig.priseEnabled()) {
            RUNS.clear();
            if (sable) {
                // Coupé en cours de partie : un navire qu'un poisson tirait ne doit plus l'être.
                PriseSable.clear();
            }
            return;
        }
        long gameTime = server.overworld().getGameTime();
        if (sable) {
            PriseSable.tick(gameTime);
        }
        Map<UUID, Run> seen = new HashMap<>();
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            try {
                FishingBobEntity bob = fighting(player);
                if (bob != null) {
                    pull(player, bob, gameTime, seen);
                }
            } catch (RuntimeException e) {
                Fallow.LOGGER.debug("prise : traction impossible pour {}", player.getName().getString(), e);
            }
        }
        RUNS.keySet().retainAll(seen.keySet());
    }

    /** Le bouchon du joueur s'il est en plein combat avec un poisson, sinon {@code null}. */
    private static FishingBobEntity fighting(ServerPlayer player) {
        if (!player.hasData(SCDataAttachments.FISHING_BOB.get())) {
            return null;
        }
        FishingBobAttachment attachment = player.getData(SCDataAttachments.FISHING_BOB.get());
        if (attachment == null || attachment.isEmpty()) {
            return null;
        }
        UUID id = attachment.getUuid();
        if (id == null || !(player.serverLevel().getEntity(id) instanceof FishingBobEntity bob) || bob.isRemoved()) {
            return null;
        }
        if (bob.getEntityData().get(FishingBobEntity.STATE) != FIGHTING || bob.fpToFish == null) {
            return null;
        }
        return bob;
    }

    private static void pull(ServerPlayer player, FishingBobEntity bob, long gameTime, Map<UUID, Run> seen) {
        double kg = weightKg(bob.fpToFish);
        if (kg < Prise.MIN_KG) {
            return;
        }
        Vec3 boat = sable ? PriseSable.worldPosition(player) : player.position();
        Run run = RUNS.computeIfAbsent(bob.getUUID(), id -> new Run(
                Math.atan2(bob.getZ() - boat.z, bob.getX() - boat.x),
                (id.getLeastSignificantBits() & 0xFFFF) / 65536.0,
                gameTime,
                Math.min(Prise.REACH, Math.hypot(bob.getX() - boat.x, bob.getZ() - boat.z) + Prise.SLACK)));
        seen.put(bob.getUUID(), run);
        long ticks = gameTime - run.since;
        double heading = Prise.heading(run.start, ticks, run.seed);
        double dx = Math.cos(heading);
        double dz = Math.sin(heading);
        double surge = Prise.surge(ticks, run.seed);

        double speed = sable ? PriseSable.tow(player, dx, dz, kg, surge, gameTime) : -1.0;
        if (speed < 0.0) {
            Entity vehicle = player.getVehicle();
            if (!isBoat(vehicle) || vehicle.level().getFluidState(vehicle.blockPosition()).isEmpty()
                    && vehicle.level().getFluidState(vehicle.blockPosition().below()).isEmpty()) {
                return;
            }
            double width = vehicle.getBbWidth();
            speed = Prise.target(kg, Prise.KG_PER_AREA * width * width, surge);
            if (gameTime % 4 == 0 && vehicle.getControllingPassenger() instanceof ServerPlayer helmsman) {
                PacketDistributor.sendToPlayer(helmsman, new PriseTow((float) dx, (float) dz, (float) speed));
            }
        }
        if (speed <= 0.05) {
            return;
        }
        if (!run.told && speed > 0.4) {
            run.told = true;
            player.displayClientMessage(Component.translatable("message.fallow.prise.emporte"), true);
        }
        follow(bob, boat, run, dx, dz, speed);
        if (surge > 1.0 && gameTime % 5 == 0 && bob.level() instanceof ServerLevel level) {
            level.sendParticles(ParticleTypes.SPLASH, bob.getX(), bob.getY() + 0.2, bob.getZ(), 6, 0.4, 0.05, 0.4, 0.1);
        }
    }

    /**
     * Le bouchon suit le poisson, au pas visé du bateau, tant que la ligne n'est pas toute sortie : quelques blocs de mou
     * au-delà de la distance du départ, si bien qu'il attend le bateau quand celui-ci traîne ; devant la berge, le
     * poisson tourne.
     */
    private static void follow(FishingBobEntity bob, Vec3 boat, Run run, double dx, double dz, double speed) {
        double step = speed / 20.0;
        double nx = bob.getX() + dx * step;
        double nz = bob.getZ() + dz * step;
        double ox = nx - boat.x;
        double oz = nz - boat.z;
        if (ox * ox + oz * oz > run.line * run.line) {
            return;
        }
        BlockPos ahead = BlockPos.containing(nx + dx, bob.getY() - 0.2, nz + dz);
        if (bob.level().getFluidState(ahead).isEmpty() && bob.level().getFluidState(ahead.below()).isEmpty()) {
            run.start += Math.PI * (0.5 + run.seed * 0.5);
            return;
        }
        bob.setPos(nx, bob.getY(), nz);
    }

    /** Une barque : celles du jeu, et celles de Fish 'N' Ships (qui descendent de la barque de base de son API). */
    private static boolean isBoat(Entity vehicle) {
        if (vehicle == null) {
            return false;
        }
        if (vehicle instanceof Boat) {
            return true;
        }
        for (Class<?> type = vehicle.getClass(); type != null && type != Entity.class; type = type.getSuperclass()) {
            if (type.getName().equals(BASE_BOAT)) {
                return true;
            }
        }
        return false;
    }

    /** Poids moyen de l'espèce, en kilogrammes (Starcatcher le donne en grammes). */
    private static double weightKg(FishProperties fish) {
        return fish.sizeWeight() == null ? 0.0 : fish.sizeWeight().weightAverage() / 1000.0;
    }
}
