package io.github.salvagedisc.fallow.hotes;

import io.github.salvagedisc.fallow.ModIds;
import java.util.EnumSet;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * Un Hôte : une silhouette au modèle du joueur, animée par Fresh Animations comme les joueurs, vêtue de l'armure
 * de sa bête gardienne (plastron, jambières et bottes) et de la parure de son peuple. Il s'avance vers le joueur le plus proche, s'arrête à distance, le regarde, et se dissout
 * dans la brume si on le frappe. Un clic ouvre la fenêtre d'échange (pilote, aucune logique de don).
 */
public class Hote extends PathfinderMob {
    private static final EntityDataAccessor<String> PEUPLE = SynchedEntityData.defineId(Hote.class, EntityDataSerializers.STRING);

    /** Distance à laquelle l'Hôte s'arrête devant le joueur. */
    static final double DISTANCE_D_ARRET = 4.0;

    public Hote(EntityType<? extends Hote> type, Level level) {
        super(type, level);
        setPersistenceRequired();
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            setDropChance(slot, 0.0F);
        }
    }

    public static AttributeSupplier.Builder attributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25)
                .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(PEUPLE, Peuple.ECORCE.id());
    }

    @Override
    protected void registerGoals() {
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(1, new SAvancerGoal(this));
        goalSelector.addGoal(2, new LookAtPlayerGoal(this, Player.class, 16.0F, 1.0F));
        goalSelector.addGoal(3, new RandomLookAroundGoal(this));
    }

    public Peuple peuple() {
        return Peuple.parse(entityData.get(PEUPLE)).orElse(Peuple.ECORCE);
    }

    /** Change de peuple et revêt l'armure de sa bête, si Archie's Dinosaurs est là. */
    public void setPeuple(Peuple peuple) {
        entityData.set(PEUPLE, peuple.id());
        // La tête porte le masque du peuple (un objet de Fallow, toujours là) ; pas de casque.
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(HotesModule.MASQUES.get(peuple).get()));
        if (!ModIds.isLoaded(ModIds.ARCHIES_DINOSAURS)) {
            return;
        }
        // Plastron, jambières et bottes de la bête.
        setItemSlot(EquipmentSlot.CHEST, piece(peuple, "chestplate"));
        setItemSlot(EquipmentSlot.LEGS, piece(peuple, "leggings"));
        setItemSlot(EquipmentSlot.FEET, piece(peuple, "boots"));
    }

    /** Appelé par {@code /summon} aussi : l'Hôte arrive toujours masqué, même sans {@link #setPeuple} explicite. */
    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty, net.minecraft.world.entity.MobSpawnType reason,
            net.minecraft.world.entity.SpawnGroupData data) {
        net.minecraft.world.entity.SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, data);
        if (getItemBySlot(EquipmentSlot.HEAD).isEmpty()) {
            setPeuple(peuple());
        }
        return result;
    }

    private static ItemStack piece(Peuple peuple, String slot) {
        Item item = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(ModIds.ARCHIES_DINOSAURS, peuple.bete() + "_" + slot));
        return new ItemStack(item);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag tag) {
        super.addAdditionalSaveData(tag);
        tag.putString("peuple", entityData.get(PEUPLE));
    }

    @Override
    public void readAdditionalSaveData(CompoundTag tag) {
        super.readAdditionalSaveData(tag);
        if (tag.contains("peuple")) {
            entityData.set(PEUPLE, peuple(tag.getString("peuple")));
        }
    }

    private static String peuple(String id) {
        return Peuple.parse(id).orElse(Peuple.ECORCE).id();
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /**
     * Frappé, il ne se bat pas : il se dissout dans la brume. Seul un coup (d'un joueur ou d'une créature) ou un
     * {@code /kill} le chasse ; le feu, la noyade, une chute ou un cactus ne le touchent pas, sans quoi l'émissaire
     * attitré d'un Peuple pourrait disparaître sans que personne n'y soit pour rien.
     */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source)) {
            return false;
        }
        if (source.getEntity() == null && !source.is(DamageTypeTags.BYPASSES_INVULNERABILITY)) {
            return false;
        }
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.CLOUD, getX(), getY() + 1.0, getZ(), 40, 0.35, 0.9, 0.35, 0.01);
            level().playSound(null, blockPosition(), SoundEvents.SOUL_ESCAPE.value(), SoundSource.NEUTRAL, 1.0F, 0.6F);
            discard();
        }
        return false;
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.PASS;
        }
        getNavigation().stop();
        if (level().isClientSide) {
            io.github.salvagedisc.fallow.hotes.client.HotesClient.ouvrirEchange(this);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    /** S'avance au pas vers le joueur le plus proche, jusqu'à quelques blocs de lui. */
    static final class SAvancerGoal extends Goal {
        private final Hote hote;
        private Player cible;

        SAvancerGoal(Hote hote) {
            this.hote = hote;
            setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            cible = hote.level().getNearestPlayer(hote, 24.0);
            return cible != null && !cible.isSpectator() && hote.distanceTo(cible) > DISTANCE_D_ARRET + 0.5;
        }

        @Override
        public boolean canContinueToUse() {
            return cible != null && cible.isAlive() && hote.distanceTo(cible) > DISTANCE_D_ARRET;
        }

        private int prochainChemin;

        @Override
        public void start() {
            prochainChemin = 0;
        }

        @Override
        public void tick() {
            // Le sélecteur ne fait tourner ce tick qu'un tick sur deux, selon la parité de l'identifiant : un compteur
            // à soi, jamais tickCount, sinon le chemin n'est jamais recalculé.
            if (--prochainChemin <= 0) {
                prochainChemin = adjustedTickDelay(10);
                hote.getNavigation().moveTo(cible, 0.5);
            }
        }

        @Override
        public void stop() {
            hote.getNavigation().stop();
            cible = null;
        }
    }
}
