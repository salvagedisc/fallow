package io.github.salvagedisc.fallow.navires;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.force.QueuedForceGroup;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.bord.BordModule;
import io.github.salvagedisc.fallow.vent.WindField;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import org.joml.Vector3d;

/**
 * Module navires : le vent de Fallow sur les navires de Sable (bateaux, dirigeables de Create Aeronautics), et le
 * vent contraire du bord du disque qui les repousse vers les terres.
 *
 * <p>À chaque pas de la physique de Sable, chaque navire à ciel ouvert reçoit le vent de fond de {@link WindField},
 * le même que celui que voient la fumée, les shaders et les bêtes : une force selon la face qu'il offre au vent
 * (hors de l'eau) et le vent relatif. Près du bord du disque ({@link BordModule}), un vent contraire le pousse vers
 * le centre, de plus en plus fort, et freine ce qui file encore vers le large. Ces forces passent au centre de masse.
 * Sous la pluie et l'orage, des rafales ({@link Gros#gust}) frappent en plus le haut du navire, qui dérive par à-coups
 * et gîte. La neige qui se dépose sur les ponts est l'affaire de {@link Neige}.
 */
public final class NaviresModule {
    private NaviresModule() {
    }

    public static void start(IEventBus modBus) {
        SableEventPlatform.INSTANCE.onPhysicsTick(NaviresModule::onPhysicsTick);
        NeoForge.EVENT_BUS.addListener(LevelTickEvent.Post.class, event -> {
            if (event.getLevel() instanceof ServerLevel level) {
                Neige.tick(level);
            }
        });
        Fallow.LOGGER.info("navires : le vent de Fallow et le bord du disque poussent les navires de Sable");
    }

    private static void onPhysicsTick(SubLevelPhysicsSystem system, double dt) {
        ServerLevel level = system.getLevel();
        if (level.dimension() != Level.OVERWORLD || dt <= 0.0) {
            return;
        }
        boolean wind = FallowConfig.naviresWindEnabled();
        BordModule.Edge edge = FallowConfig.bordEnabled() ? BordModule.edge() : null;
        if (!wind && edge == null) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        double multiplier = FallowConfig.naviresWindStrength();
        boolean storms = FallowConfig.naviresStorms();
        float rain = level.getRainLevel(1.0F);
        float thunder = level.getThunderLevel(1.0F);
        long gameTime = level.getGameTime();
        for (ServerSubLevel ship : container.getAllSubLevels()) {
            if (ship.isRemoved()) {
                continue;
            }
            try {
                push(level, ship, dt, wind ? multiplier : 0.0, storms, rain, thunder, gameTime, edge);
            } catch (RuntimeException e) {
                Fallow.LOGGER.debug("navires : poussée impossible sur {}", ship, e);
            }
        }
    }

    private static void push(ServerLevel level, ServerSubLevel ship, double dt, double multiplier, boolean storms, float rain,
            float thunder, long gameTime, BordModule.Edge edge) {
        BoundingBox3dc box = ship.boundingBox();
        double sizeX = box.maxX() - box.minX();
        double sizeY = box.maxY() - box.minY();
        double sizeZ = box.maxZ() - box.minZ();
        if (Math.max(sizeX, Math.max(sizeY, sizeZ)) < ShipForces.MIN_SIZE) {
            return;
        }
        MassData mass = ship.getMassTracker();
        if (mass == null || mass.isInvalid() || mass.getMass() <= 0.0) {
            return;
        }
        double centerX = (box.minX() + box.maxX()) * 0.5;
        double centerZ = (box.minZ() + box.maxZ()) * 0.5;
        Vector3d velocity = ship.latestLinearVelocity;
        double forceX = 0.0;
        double forceZ = 0.0;
        double gustX = 0.0;
        double gustZ = 0.0;

        if (multiplier > 0.0) {
            BlockPos above = BlockPos.containing(centerX, box.maxY() + 1.0, centerZ);
            if (level.isLoaded(above) && level.canSeeSky(above)) {
                double centerY = (box.minY() + box.maxY()) * 0.5;
                double[] w = WindField.velocity(gameTime, rain, thunder, centerY);
                double height = box.maxY() - Math.max(box.minY(), level.getSeaLevel() - 1.0);
                double area = ShipForces.area(sizeX, sizeZ, height, w[0], w[1]);
                double[] f = ShipForces.wind(area, w[0], w[1], velocity.x, velocity.z, multiplier);
                forceX += f[0];
                forceZ += f[1];
                if (storms) {
                    // Les rafales d'orage poussent le haut du navire : il dérive par à-coups et gîte.
                    double[] g = Gros.gust(gameTime, ship.getUniqueId().getMostSignificantBits(), rain, thunder, centerY,
                            WindField.angle(gameTime, thunder));
                    double gustArea = ShipForces.area(sizeX, sizeZ, height, g[0], g[1]);
                    double k = ShipForces.WIND_COEFFICIENT * multiplier * gustArea;
                    gustX = k * g[0];
                    gustZ = k * g[1];
                }
            }
        }

        if (edge != null) {
            double[] in = edge.inward(centerX, centerZ);
            double outward = -(velocity.x * in[0] + velocity.z * in[1]);
            double a = ShipForces.edge(edge.past(centerX, centerZ), outward) * mass.getMass();
            forceX += in[0] * a;
            forceZ += in[1] * a;
        }

        QueuedForceGroup group = ship.getOrCreateQueuedForceGroup(ForceGroups.DRAG.get());
        // Sable prend des impulsions dans le repère du navire, au point donné.
        if (forceX != 0.0 || forceZ != 0.0) {
            Vector3d impulse = ship.logicalPose().transformNormalInverse(new Vector3d(forceX * dt, 0.0, forceZ * dt), new Vector3d());
            group.applyAndRecordPointForce(mass.getCenterOfMass(), impulse);
        }
        if (gustX != 0.0 || gustZ != 0.0) {
            Vector3d impulse = ship.logicalPose().transformNormalInverse(new Vector3d(gustX * dt, 0.0, gustZ * dt), new Vector3d());
            Vector3d point = new Vector3d(mass.getCenterOfMass()).add(0.0, sizeY * ShipForces.GUST_LEVER, 0.0);
            group.applyAndRecordPointForce(point, impulse);
        }
    }
}
