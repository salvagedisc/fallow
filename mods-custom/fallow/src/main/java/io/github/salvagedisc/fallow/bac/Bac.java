package io.github.salvagedisc.fallow.bac;

import dev.ryanhcode.sable.api.SubLevelAssemblyHelper;
import dev.ryanhcode.sable.api.physics.force.ForceGroups;
import dev.ryanhcode.sable.api.physics.mass.MassData;
import dev.ryanhcode.sable.api.sublevel.ServerSubLevelContainer;
import dev.ryanhcode.sable.api.sublevel.SubLevelContainer;
import dev.ryanhcode.sable.companion.math.BoundingBox3dc;
import dev.ryanhcode.sable.sublevel.ServerSubLevel;
import dev.ryanhcode.sable.sublevel.plot.LevelPlot;
import dev.ryanhcode.sable.sublevel.system.SubLevelPhysicsSystem;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;

/**
 * Le bac à traille sur les navires de Sable : prendre un radeau à la corde, le faire traverser, et la poussée à
 * chaque pas de la physique. Ce qui lie un radeau à sa corde est rangé dans les données du navire (clé
 * {@value #TAG}) : les deux pieux et le sens de la traversée.
 */
final class Bac {
    static final String TAG = "fallow_bac";
    /** Marque, gardée même lâché, d'un navire né radeau de bac : seul lui se reprend à la corde. */
    static final String RADEAU = "fallow_radeau";
    /** Un radeau de bac compte au plus tant de blocs. */
    static final int MAX_BLOCKS = 400;

    private Bac() {
    }

    // ---- La poulie ----

    static void use(ServerLevel level, BlockPos pos, Player player, boolean reverse) {
        ServerSubLevel ship = shipAt(level, pos);
        if (ship == null) {
            attach(level, pos, player);
            return;
        }
        CompoundTag data = data(ship);
        if (data == null) {
            CompoundTag all = ship.getUserDataTag();
            if (all == null || !all.getBoolean(RADEAU)) {
                // Un navire bâti autrement (Create, Sable) n'a pas été jugé : ni sa taille, ni s'il flotte.
                say(player, Component.translatable("message.fallow.bac.pas_un_radeau"));
                return;
            }
            Vec3 center = center(ship);
            Cordes.Line line = Cordes.near(level, center.x, center.y, center.z);
            if (line == null) {
                say(player, Component.translatable("message.fallow.bac.pas_de_corde"));
                return;
            }
            store(ship, line, 0);
            say(player, Component.translatable("message.fallow.bac.pris"));
            return;
        }
        BlockPos a = BlockPos.of(data.getLong("a"));
        BlockPos b = BlockPos.of(data.getLong("b"));
        int direction = data.getInt("sens");
        if (reverse && direction != 0) {
            direction = -direction;
        } else if (direction != 0) {
            direction = 0;
        } else {
            // À l'arrêt, le bac part vers la rive la plus lointaine.
            Vec3 center = center(ship);
            Vec3 ka = Cordes.knot(a);
            Vec3 kb = Cordes.knot(b);
            direction = Traille.place(ka.x, ka.z, kb.x, kb.z, center.x, center.z).t() < 0.5 ? 1 : -1;
        }
        data.putInt("sens", direction);
        ship.setUserDataTag(withData(ship, data));
        if (direction == 0) {
            say(player, Component.translatable("message.fallow.bac.arret"));
        } else {
            Vec3 center = center(ship);
            BlockPos target = direction > 0 ? b : a;
            say(player, Component.translatable("message.fallow.bac.part",
                    Component.translatable("message.fallow.bac.cap." + heading(target.getX() + 0.5 - center.x, target.getZ() + 0.5 - center.z))));
            level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 0.8F, 0.6F);
        }
    }

    private static void say(Player player, Component message) {
        if (player != null) {
            player.displayClientMessage(message, true);
        }
    }

    /** Prend à la corde le radeau posé sur l'eau où se trouve la poulie : il devient un navire de Sable. */
    static ServerSubLevel attach(ServerLevel level, BlockPos pos, Player player) {
        Vec3 here = Vec3.atCenterOf(pos);
        Cordes.Line line = Cordes.near(level, here.x, here.y, here.z);
        if (line == null) {
            say(player, Component.translatable("message.fallow.bac.pas_de_corde"));
            return null;
        }
        SubLevelAssemblyHelper.GatherResult gathered = SubLevelAssemblyHelper.gatherConnectedBlocks(pos, level, MAX_BLOCKS,
                (from, fromState, to, toState, direction) -> isRaft(toState) && !isWaterPlant(level, to, toState));
        if (gathered.assemblyState() != SubLevelAssemblyHelper.GatherResult.State.SUCCESS || gathered.blocks().isEmpty()) {
            say(player, Component.translatable("message.fallow.bac.trop_grand", MAX_BLOCKS));
            return null;
        }
        boolean floating = gathered.blocks().stream().anyMatch(block -> !level.getFluidState(block.below()).isEmpty()
                && !gathered.blocks().contains(block.below()));
        if (!floating) {
            say(player, Component.translatable("message.fallow.bac.pas_sur_l_eau"));
            return null;
        }
        // Un radeau qui pose sur la rive ou sur le fond emporterait avec lui un ponton, un quai ou une maison.
        boolean grounded = gathered.blocks().stream().anyMatch(block -> {
            BlockPos below = block.below();
            if (gathered.blocks().contains(below) || !level.getFluidState(below).isEmpty()) {
                return false;
            }
            BlockState ground = level.getBlockState(below);
            return !ground.isAir() && !ground.canBeReplaced();
        });
        if (grounded) {
            say(player, Component.translatable("message.fallow.bac.a_terre"));
            return null;
        }
        ServerSubLevel ship;
        try {
            ship = SubLevelAssemblyHelper.assembleBlocks(level, pos, gathered.blocks(), gathered.boundingBox());
        } catch (RuntimeException e) {
            Fallow.LOGGER.warn("bac : le radeau n'a pas pu être pris à la corde", e);
            say(player, Component.translatable("message.fallow.bac.echec"));
            return null;
        }
        if (ship == null) {
            return null;
        }
        CompoundTag marque = ship.getUserDataTag() == null ? new CompoundTag() : ship.getUserDataTag().copy();
        marque.putBoolean(RADEAU, true);
        ship.setUserDataTag(marque);
        store(ship, line, 0);
        level.playSound(null, pos, SoundEvents.LEASH_KNOT_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F);
        say(player, Component.translatable("message.fallow.bac.pris"));
        return ship;
    }

    /** Ce qui compte comme radeau : ni l'eau, ni la rive (terre, sable, roche, plantes qu'on remplace), ni un pieu. */
    static boolean isRaft(BlockState state) {
        if (state.isAir() || state.getBlock() instanceof LiquidBlock || state.canBeReplaced()) {
            return false;
        }
        return !state.is(BlockTags.MINEABLE_WITH_SHOVEL) && !state.is(BlockTags.BASE_STONE_OVERWORLD)
                && !state.is(BacModule.PIEU.get());
    }

    /** Une plante d'eau sous le radeau (varech, herbes) : ni prise avec lui, ni prise pour le fond qui le porterait. */
    private static boolean isWaterPlant(ServerLevel level, BlockPos pos, BlockState state) {
        return !state.getFluidState().isEmpty() && state.getCollisionShape(level, pos).isEmpty();
    }

    // ---- Les données du navire ----

    private static CompoundTag data(ServerSubLevel ship) {
        CompoundTag all = ship.getUserDataTag();
        return all != null && all.contains(TAG) ? all.getCompound(TAG) : null;
    }

    private static CompoundTag withData(ServerSubLevel ship, CompoundTag data) {
        CompoundTag all = ship.getUserDataTag() == null ? new CompoundTag() : ship.getUserDataTag().copy();
        if (data == null) {
            all.remove(TAG);
        } else {
            all.put(TAG, data);
        }
        return all;
    }

    private static void store(ServerSubLevel ship, Cordes.Line line, int direction) {
        CompoundTag data = new CompoundTag();
        data.putLong("a", line.a().asLong());
        data.putLong("b", line.b().asLong());
        data.putInt("sens", direction);
        ship.setUserDataTag(withData(ship, data));
    }

    private static ServerSubLevel shipAt(ServerLevel level, BlockPos pos) {
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null || !container.inBounds(pos)) {
            return null;
        }
        LevelPlot plot = container.getPlot(new ChunkPos(pos));
        return plot != null && plot.getSubLevel() instanceof ServerSubLevel ship && !ship.isRemoved() ? ship : null;
    }

    private static Vec3 center(ServerSubLevel ship) {
        MassData mass = ship.getMassTracker();
        if (mass == null || mass.getCenterOfMass() == null) {
            BoundingBox3dc box = ship.boundingBox();
            return new Vec3((box.minX() + box.maxX()) * 0.5, (box.minY() + box.maxY()) * 0.5, (box.minZ() + box.maxZ()) * 0.5);
        }
        Vector3d com = ship.logicalPose().transformPosition(new Vector3d(mass.getCenterOfMass()), new Vector3d());
        return new Vec3(com.x, com.y, com.z);
    }

    /** Le cap, en huit directions, d'un déplacement (dx, dz) : nord, nord_est… (le nord est vers -z). */
    static String heading(double dx, double dz) {
        String[] names = {"sud", "sud_ouest", "ouest", "nord_ouest", "nord", "nord_est", "est", "sud_est"};
        double angle = Math.toDegrees(Math.atan2(-dx, dz));
        int index = Math.floorMod((int) Math.round(angle / 45.0), 8);
        return names[index];
    }

    // ---- La physique ----

    static void onPhysicsTick(SubLevelPhysicsSystem system, double dt) {
        ServerLevel level = system.getLevel();
        if (dt <= 0.0 || !FallowConfig.bacEnabled()) {
            return;
        }
        ServerSubLevelContainer container = SubLevelContainer.getContainer(level);
        if (container == null) {
            return;
        }
        for (ServerSubLevel ship : container.getAllSubLevels()) {
            if (ship.isRemoved()) {
                continue;
            }
            CompoundTag data = data(ship);
            if (data == null) {
                continue;
            }
            try {
                pull(level, system, ship, data, dt);
            } catch (RuntimeException e) {
                Fallow.LOGGER.debug("bac : poussée impossible sur {}", ship, e);
            }
        }
    }

    private static void pull(ServerLevel level, SubLevelPhysicsSystem system, ServerSubLevel ship, CompoundTag data, double dt) {
        BlockPos a = BlockPos.of(data.getLong("a"));
        BlockPos b = BlockPos.of(data.getLong("b"));
        if (!level.isLoaded(a) || !level.isLoaded(b)) {
            return;
        }
        // La corde se lit sur les pieux eux-mêmes : le registre des cordes ne se remplit qu'au tick qui suit le
        // chargement d'un chunk, et un radeau ne doit pas être lâché pendant ce tick-là.
        if (!(level.getBlockEntity(a) instanceof PieuBlockEntity post) || !b.equals(post.other()) || !post.tied()) {
            release(level, ship, "message.fallow.bac.coupee");
            return;
        }
        MassData mass = ship.getMassTracker();
        if (mass == null || mass.isInvalid() || mass.getMass() <= 0.0) {
            return;
        }
        Vec3 center = center(ship);
        Vec3 ka = Cordes.knot(a);
        Vec3 kb = Cordes.knot(b);
        Traille.Place place = Traille.place(ka.x, ka.z, kb.x, kb.z, center.x, center.z);
        if (place.offset() > Traille.SLIP) {
            release(level, ship, "message.fallow.bac.lache");
            return;
        }
        Vector3d velocity = system.getPhysicsHandle(ship).getLinearVelocity(new Vector3d());
        double[] hold = Traille.hold(place, velocity.x, velocity.z);
        double[] current = current(level, ship.boundingBox());
        double target = Traille.target(place, data.getInt("sens"), current[0], current[1]);
        double speed = velocity.x * place.ux() + velocity.z * place.uz();
        double along = Traille.along(speed, target);
        double ax = hold[0] + along * place.ux();
        double az = hold[1] + along * place.uz();
        double m = mass.getMass() * dt;
        Vector3d impulse = ship.logicalPose().transformNormalInverse(new Vector3d(ax * m, 0.0, az * m), new Vector3d());
        ship.getOrCreateQueuedForceGroup(ForceGroups.PROPULSION.get()).applyAndRecordPointForce(mass.getCenterOfMass(), impulse);
    }

    /** Le courant sous le radeau, en blocs par seconde, {@code [x, z]} : l'écoulement de l'eau à sa ligne de flottaison. */
    private static double[] current(ServerLevel level, BoundingBox3dc box) {
        double x = (box.minX() + box.maxX()) * 0.5;
        double z = (box.minZ() + box.maxZ()) * 0.5;
        for (int dy = 0; dy <= 2; dy++) {
            BlockPos pos = BlockPos.containing(x, box.minY() + dy, z);
            if (!level.isLoaded(pos)) {
                break;
            }
            FluidState fluid = level.getFluidState(pos);
            if (!fluid.isEmpty()) {
                Vec3 flow = fluid.getFlow(level, pos);
                return new double[] {flow.x * Traille.CURRENT_SPEED, flow.z * Traille.CURRENT_SPEED};
            }
        }
        return new double[] {0.0, 0.0};
    }

    private static void release(ServerLevel level, ServerSubLevel ship, String message) {
        ship.setUserDataTag(withData(ship, null));
        Vec3 center = center(ship);
        for (ServerPlayer player : level.players()) {
            if (player.distanceToSqr(center) < 32.0 * 32.0) {
                say(player, Component.translatable(message));
            }
        }
    }
}
