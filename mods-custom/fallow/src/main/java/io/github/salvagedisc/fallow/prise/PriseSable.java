package io.github.salvagedisc.fallow.prise;

import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.companion.SableCompanion;
import dev.ryanhcode.sable.platform.SableEventPlatform;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.salvagedisc.fallow.Fallow;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.Position;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * La prise qui tire un navire de Sable : le tick du serveur dit, navire par navire, où file le poisson et ce qu'il pèse ;
 * le pas de physique de Sable en fait une poussée au centre de masse. Chargée seulement si Sable est là.
 */
final class PriseSable {
    /** Une traction en cours : cap unitaire, poids du poisson, force du moment, et le tick de jeu où elle cesse. */
    private record Tow(double dx, double dz, double fishKg, double surge, long until) {
    }

    private static final Map<ServerSubLevel, Tow> TOWS = new ConcurrentHashMap<>();
    private static volatile long now;

    private PriseSable() {
    }

    static void start() {
        SableEventPlatform.INSTANCE.onPhysicsTick(PriseSable::onPhysicsTick);
    }

    /**
     * Si le joueur est à bord d'un navire de Sable, le poisson tire ce navire jusqu'au prochain ordre ; rend la vitesse
     * visée en blocs par seconde, ou -1 si le joueur n'est pas à bord d'un navire.
     */
    static double tow(ServerPlayer player, double dx, double dz, double fishKg, double surge, long gameTime) {
        now = gameTime;
        if (!(SableCompanion.INSTANCE.getTrackingOrVehicleSubLevel(player) instanceof ServerSubLevel ship) || ship.isRemoved()) {
            return -1.0;
        }
        MassData mass = ship.getMassTracker();
        if (mass == null || mass.isInvalid() || mass.getMass() <= 0.0) {
            return -1.0;
        }
        TOWS.put(ship, new Tow(dx, dz, fishKg, surge, gameTime + 2));
        return Prise.target(fishKg, mass.getMass() * Prise.KG_PER_MASS, surge);
    }

    /** Où le joueur se trouve dans le monde, même assis dans la parcelle d'un navire. */
    static Vec3 worldPosition(ServerPlayer player) {
        Vec3 projected = SableCompanion.INSTANCE.projectOutOfSubLevel(player.level(), (Position) player.position());
        return projected != null ? projected : player.position();
    }

    static void tick(long gameTime) {
        now = gameTime;
        TOWS.entrySet().removeIf(e -> e.getValue().until() < gameTime || e.getKey().isRemoved());
    }

    private static void onPhysicsTick(SubLevelPhysicsSystem system, double dt) {
        if (dt <= 0.0 || TOWS.isEmpty()) {
            return;
        }
        for (Map.Entry<ServerSubLevel, Tow> entry : TOWS.entrySet()) {
            ServerSubLevel ship = entry.getKey();
            Tow tow = entry.getValue();
            if (ship.isRemoved() || tow.until() < now || ship.getLevel() != system.getLevel()) {
                continue;
            }
            try {
                MassData mass = ship.getMassTracker();
                if (mass == null || mass.isInvalid() || mass.getMass() <= 0.0) {
                    continue;
                }
                double target = Prise.target(tow.fishKg(), mass.getMass() * Prise.KG_PER_MASS, tow.surge());
                Vector3d velocity = ship.latestLinearVelocity;
                double a = Prise.pull(velocity.x * tow.dx() + velocity.z * tow.dz(), target);
                if (a <= 0.0) {
                    continue;
                }
                double m = mass.getMass() * dt * a;
                Vector3d impulse = ship.logicalPose().transformNormalInverse(new Vector3d(tow.dx() * m, 0.0, tow.dz() * m), new Vector3d());
                ship.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).applyAndRecordPointForce(mass.getCenterOfMass(), impulse);
            } catch (RuntimeException e) {
                Fallow.LOGGER.debug("prise : traction impossible sur {}", ship, e);
            }
        }
    }
}
