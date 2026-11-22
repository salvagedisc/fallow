package io.github.salvagedisc.fallow.eveil;

import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.serialization.MapCodec;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.commands.Commands;
import net.minecraft.core.RegistryAccess;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.Event;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModList;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.loot.IGlobalLootModifier;
import net.neoforged.neoforge.event.AddReloadListenerEvent;
import net.neoforged.neoforge.event.GrindstoneEvent;
import net.neoforged.neoforge.event.RegisterCommandsEvent;
import net.neoforged.neoforge.event.entity.EntityLeaveLevelEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingDropsEvent;
import net.neoforged.neoforge.event.entity.living.LivingShieldBlockEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;
import net.neoforged.neoforge.event.entity.player.PlayerDestroyItemEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.entity.player.PlayerInteractEvent;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.event.village.VillagerTradesEvent;
import net.neoforged.neoforge.event.village.WandererTradesEvent;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
import net.minecraft.world.entity.npc.VillagerTrades;

/**
 * Module éveil : l'enchantement est un haut fait (docs/plan/eveil.md). Un objet compte ce qu'il a vécu, s'éveille
 * passé un seuil et révèle à la table d'enchantement deux enchantements tirés de ses actes ; les grandes bêtes
 * laissent un éclat qui porte un enchantement fort ; à bout de durabilité, l'objet oublie son dernier éveil. Rien ne
 * sort enchanté d'un butin ni d'un échange, et la table n'a plus d'autre usage.
 */
public final class EveilModule {
    public static final DeferredRegister.DataComponents COMPONENTS =
            DeferredRegister.createDataComponents(Registries.DATA_COMPONENT_TYPE, Fallow.MOD_ID);
    public static final Supplier<DataComponentType<EtatEveil>> ETAT = COMPONENTS.registerComponentType("eveil",
            builder -> builder.persistent(EtatEveil.CODEC).networkSynchronized(EtatEveil.STREAM_CODEC));
    /** Le mot de l'attachement de l'objet ({@link Lien}) : -1 négligé, 1 entretenu, 2 chéri ; absent entre les deux. */
    public static final Supplier<DataComponentType<Integer>> LIEN = COMPONENTS.registerComponentType("lien",
            builder -> builder.persistent(com.mojang.serialization.Codec.intRange(-1, 2))
                    .networkSynchronized(net.minecraft.network.codec.ByteBufCodecs.VAR_INT));
    public static final Supplier<DataComponentType<Eclat>> ECLAT = COMPONENTS.registerComponentType("eclat",
            builder -> builder.persistent(Eclat.CODEC).networkSynchronized(Eclat.STREAM_CODEC));

    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(Fallow.MOD_ID);
    public static final DeferredItem<Item> ECLAT_DE_BETE = ITEMS.registerItem("eclat_de_bete", EclatItem::new,
            new Item.Properties().stacksTo(1).rarity(Rarity.EPIC).fireResistant()
                    .component(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true));
    /** L'encre d'éveil : sans elle la table ne peut écrire aucune voie ; une fiole par éveil ({@link #prendreEncre}). */
    public static final DeferredItem<Item> ENCRE_D_EVEIL = ITEMS.registerSimpleItem("encre_d_eveil",
            new Item.Properties().stacksTo(16).rarity(Rarity.UNCOMMON));

    public static final DeferredRegister<MapCodec<? extends IGlobalLootModifier>> LOOT_MODIFIERS =
            DeferredRegister.create(NeoForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Fallow.MOD_ID);

    static {
        LOOT_MODIFIERS.register("sans_enchantement", () -> SansEnchantement.CODEC);
        LOOT_MODIFIERS.register("eclat_de_bete", () -> EclatDeBete.CODEC);
    }

    /** Distance plafond comptée par seconde : au-delà, c'est un voyage (monture, téléportation), pas une marche. */
    private static final double PAS_MAX = 12.0;

    private static final Map<UUID, Vec3> DERNIERES_POSITIONS = new HashMap<>();
    /** La dernière usure vue de chaque objet éveillé : une baisse est une réparation, un haut fait. */
    private static final Map<UUID, Integer> USURES = new HashMap<>();
    /** Les objets qui ont déjà prévenu qu'ils faiblissent (jusqu'à leur réparation). */
    private static final java.util.Set<UUID> AVERTIS = new java.util.HashSet<>();
    /** Les objets descendus dans le rouge depuis leur dernière réparation (l'attachement n'en pâtit qu'une fois). */
    private static final java.util.Set<UUID> ROUGES = new java.util.HashSet<>();
    /** Depuis combien de secondes un objet est dans d'autres mains que celles de son maître. */
    private static final Map<UUID, Integer> PRETS = new HashMap<>();
    /** Les objets tombés à la mort d'un joueur, et ce joueur : les reprendre est un geste d'attachement. */
    private static final Map<UUID, UUID> MORTS = new HashMap<>();
    private static final java.util.Set<UUID> REPRIS = new java.util.HashSet<>();
    /** Diegesis est là : la table d'enchantement s'ouvre en poste ({@link TableEveil}). */
    static final boolean DIEGESIS = io.github.salvagedisc.fallow.ModIds.isLoaded(io.github.salvagedisc.fallow.ModIds.DIEGESIS);
    private static final EquipmentSlot[] ARMURE = {EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
    private static volatile MinecraftServer server;

    private EveilModule() {
    }

    public static void start(IEventBus modBus) {
        COMPONENTS.register(modBus);
        ITEMS.register(modBus);
        modBus.addListener(net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent.class, event -> {
            if (event.getTabKey() == net.minecraft.world.item.CreativeModeTabs.INGREDIENTS) {
                event.accept(ENCRE_D_EVEIL);
            }
        });
        LOOT_MODIFIERS.register(modBus);

        NeoForge.EVENT_BUS.addListener(ServerStartingEvent.class, e -> server = e.getServer());
        NeoForge.EVENT_BUS.addListener(ServerStoppedEvent.class, e -> {
            server = null;
            DERNIERES_POSITIONS.clear();
            USURES.clear();
            AVERTIS.clear();
            ROUGES.clear();
            PRETS.clear();
            MORTS.clear();
            REPRIS.clear();
        });
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.tick.ServerTickEvent.Post.class, EveilModule::horsDesMains);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.ItemEntityPickupEvent.Post.class, e -> {
            EtatEveil etat = e.getOriginalStack().get(ETAT.get());
            if (etat != null && e.getPlayer().getUUID().equals(MORTS.remove(etat.id()))) {
                REPRIS.add(etat.id());
            }
        });
        NeoForge.EVENT_BUS.addListener(AddReloadListenerEvent.class, e -> e.addListener(new HautsFaits()));
        NeoForge.EVENT_BUS.addListener(PlayerEvent.PlayerLoggedOutEvent.class,
                e -> DERNIERES_POSITIONS.remove(e.getEntity().getUUID()));

        NeoForge.EVENT_BUS.addListener(BlockEvent.BreakEvent.class, EveilModule::onBreak);
        NeoForge.EVENT_BUS.addListener(LivingDeathEvent.class, EveilModule::onDeath);
        NeoForge.EVENT_BUS.addListener(LivingDamageEvent.Post.class, EveilModule::onDamage);
        NeoForge.EVENT_BUS.addListener(LivingShieldBlockEvent.class, EveilModule::onShield);
        NeoForge.EVENT_BUS.addListener(ItemFishedEvent.class, EveilModule::onFished);
        NeoForge.EVENT_BUS.addListener(PlayerTickEvent.Post.class, EveilModule::onTick);
        NeoForge.EVENT_BUS.addListener(EventPriority.HIGH, PlayerInteractEvent.RightClickBlock.class, EveilModule::onTable);
        NeoForge.EVENT_BUS.addListener(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open.class, EveilModule::jamaisVanilla);
        NeoForge.EVENT_BUS.addListener(VillagerTradesEvent.class,
                e -> e.getTrades().values().forEach(EveilModule::sansEnchantement));
        NeoForge.EVENT_BUS.addListener(WandererTradesEvent.class, e -> {
            sansEnchantement(e.getGenericTrades());
            sansEnchantement(e.getRareTrades());
        });
        NeoForge.EVENT_BUS.addListener(LivingDropsEvent.class, EveilModule::onDrops);
        NeoForge.EVENT_BUS.addListener(GrindstoneEvent.OnPlaceItem.class, EveilModule::onGrindstone);
        NeoForge.EVENT_BUS.addListener(PlayerDestroyItemEvent.class, e -> oublier(e.getOriginal()));
        NeoForge.EVENT_BUS.addListener(ItemExpireEvent.class, e -> oublier(e.getEntity().getItem()));
        NeoForge.EVENT_BUS.addListener(EntityLeaveLevelEvent.class, e -> {
            if (e.getEntity() instanceof ItemEntity item && e.getEntity().getRemovalReason() == Entity.RemovalReason.KILLED) {
                oublier(item.getItem());
            }
        });
        NeoForge.EVENT_BUS.addListener(RegisterCommandsEvent.class, EveilModule::onRegisterCommands);
        if (ModList.get().isLoaded("treechop")) {
            ecouterTreeChop();
        }
        if (DIEGESIS) {
            TableEveil.register();
        }
        if (FMLEnvironment.dist.isClient()) {
            io.github.salvagedisc.fallow.eveil.client.EveilClient.start(modBus);
        }
        Fallow.LOGGER.info("eveil : l'enchantement vient des hauts faits et des grandes bêtes");
    }

    /** Les registres du serveur en cours (le client ne s'en sert pas : ses infobulles lisent les siens). */
    static RegistryAccess registres() {
        MinecraftServer current = server;
        return current == null ? null : current.registryAccess();
    }

    // ---- les actes ----

    private static void onBreak(BlockEvent.BreakEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player) {
            BlockState state = event.getState();
            HautsFaits.Contexte moment = new HautsFaits.Contexte(player, null, null, player.getBlockStateOn());
            Eveil.acte(player, player.getMainHandItem(), HautsFaits.Type.CASSER,
                    entree -> entree.bloc(state) && entree.si(moment), 1.0);
        }
    }

    private static void onDeath(LivingDeathEvent event) {
        if (!(event.getSource().getEntity() instanceof ServerPlayer player)) {
            return;
        }
        ItemStack arme = event.getSource().getWeaponItem();
        if (arme == null || arme.isEmpty()) {
            arme = player.getMainHandItem();
        }
        LivingEntity bete = event.getEntity();
        double poids = Math.min(10.0, bete.getMaxHealth() / 20.0);
        HautsFaits.Contexte moment = new HautsFaits.Contexte(player, bete, event.getSource(), null);
        Eveil.acte(player, Eveil.reel(player, arme), HautsFaits.Type.VAINCRE,
                entree -> entree.creature(bete.getType()) && entree.si(moment), poids);
    }

    private static void onDamage(LivingDamageEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || event.getNewDamage() <= 0) {
            return;
        }
        LivingEntity attaquant = event.getSource().getEntity() instanceof LivingEntity living ? living : null;
        HautsFaits.Contexte moment = new HautsFaits.Contexte(player, attaquant, event.getSource(), null);
        for (EquipmentSlot slot : ARMURE) {
            Eveil.acte(player, player.getItemBySlot(slot), HautsFaits.Type.ENCAISSER,
                    entree -> entree.degats(event.getSource()) && entree.si(moment), event.getNewDamage());
        }
    }

    private static void onShield(LivingShieldBlockEvent event) {
        if (event.getEntity() instanceof ServerPlayer player && event.getBlocked() && event.getBlockedDamage() > 0) {
            ItemStack bouclier = player.getUseItem();
            LivingEntity attaquant = event.getDamageSource().getEntity() instanceof LivingEntity living ? living : null;
            HautsFaits.Contexte moment = new HautsFaits.Contexte(player, attaquant, event.getDamageSource(), null);
            Eveil.acte(player, bouclier, HautsFaits.Type.BLOQUER, entree -> entree.si(moment), event.getBlockedDamage());
        }
    }

    private static void onFished(ItemFishedEvent event) {
        if (event.getEntity() instanceof ServerPlayer player) {
            ItemStack canne = player.getMainHandItem();
            if (HautsFaits.pour(canne).stream().noneMatch(e -> e.type() == HautsFaits.Type.PECHER)) {
                canne = player.getOffhandItem();
            }
            HautsFaits.Contexte moment = HautsFaits.Contexte.de(player);
            Eveil.acte(player, canne, HautsFaits.Type.PECHER, entree -> entree.si(moment), 1.0);
        }
    }

    private static void onTick(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.tickCount % 20 != 0 || player.isSpectator()
                || !FallowConfig.eveilEnabled()) {
            return;
        }
        inventaire(player);
        Vec3 here = player.position();
        Vec3 before = DERNIERES_POSITIONS.put(player.getUUID(), here);
        HautsFaits.Contexte moment = new HautsFaits.Contexte(player, null, null, player.getBlockStateOn());
        if (player.isUnderWater()) {
            for (EquipmentSlot slot : ARMURE) {
                Eveil.acte(player, player.getItemBySlot(slot), HautsFaits.Type.SOUS_L_EAU, entree -> entree.si(moment), 1.0);
            }
        }
        if (before == null || player.isPassenger() || player.isFallFlying()) {
            return;
        }
        double pas = Math.hypot(here.x - before.x, here.z - before.z);
        if (pas <= 0.1 || pas > PAS_MAX) {
            return;
        }
        String mode = player.isInWater() ? "nage" : player.onGround() ? "marche" : "";
        if (!mode.isEmpty()) {
            for (EquipmentSlot slot : ARMURE) {
                Eveil.acte(player, player.getItemBySlot(slot), HautsFaits.Type.PARCOURIR,
                        entree -> entree.mode(mode) && entree.si(moment), pas);
            }
        }
    }

    /**
     * Une fois par seconde, tout ce que le joueur porte : seul l'éveil enchante (le reste s'efface, hors mode créatif) ;
     * une copie reçoit son propre identifiant ; une réparation compte ; un objet qui faiblit prévient, à bout il oublie.
     */
    private static void inventaire(ServerPlayer player) {
        Inventory inventaire = player.getInventory();
        boolean createur = player.isCreative();
        java.util.Set<UUID> vus = new java.util.HashSet<>();
        for (int i = 0; i < inventaire.getContainerSize(); i++) {
            ItemStack stack = inventaire.getItem(i);
            if (stack.isEmpty()) {
                continue;
            }
            if (!createur && stack.is(Items.ENCHANTED_BOOK)) {
                inventaire.setItem(i, SansEnchantement.nu(stack));
                continue;
            }
            if (!createur) {
                Eveil.purifier(stack);
            }
            EtatEveil etat = stack.get(ETAT.get());
            if (etat == null && stack.isDamaged() && !HautsFaits.pour(stack).isEmpty()) {
                etat = Eveil.etat(stack);
            }
            if (etat == null) {
                continue;
            }
            if (!vus.add(etat.id())) {
                EtatEveil copie = etat.avecId(UUID.randomUUID());
                Actes.get(player.server).copier(etat.id(), copie.id());
                stack.set(ETAT.get(), copie);
                etat = copie;
            }
            attachement(player, stack, etat);
            if (stack.isDamageableItem()) {
                int usure = stack.getDamageValue();
                Integer avant = USURES.put(etat.id(), usure);
                if (avant != null && usure < avant) {
                    AVERTIS.remove(etat.id());
                    ROUGES.remove(etat.id());
                    HautsFaits.Contexte moment = HautsFaits.Contexte.de(player);
                    Eveil.acte(player, stack, HautsFaits.Type.REPARER, entree -> entree.si(moment), avant - usure);
                    // Réparé à temps, c'est du soin : avant la mi-usure beaucoup, avant le rouge un peu, après rien.
                    double soin = avant < stack.getMaxDamage() * 0.5 ? Lien.REPARE_TOT
                            : avant < stack.getMaxDamage() * FallowConfig.eveilAlerte() ? Lien.REPARE : 0.0;
                    Lien.ajouter(player.server, stack, etat, soin, player);
                } else if (usure >= stack.getMaxDamage() * FallowConfig.eveilAlerte() && ROUGES.add(etat.id())) {
                    Lien.ajouter(player.server, stack, etat, Lien.ROUGE, player);
                }
            }
            Eveil.usure(player, stack);
            if (Eveil.alerte(player, stack, AVERTIS.contains(etat.id()))) {
                AVERTIS.add(etat.id());
            }
        }
    }

    /**
     * Ce que le porteur fait à l'objet, chaque seconde : le nommer (une fois), le reprendre après une mort, le garder ou le
     * prêter ; ses places suivent son métal et son attachement.
     */
    private static void attachement(ServerPlayer player, ItemStack stack, EtatEveil etat) {
        Actes actes = Actes.get(player.server);
        UUID id = etat.id();
        if (stack.has(DataComponents.CUSTOM_NAME) && actes.note(id, Lien.NOMME) == 0.0) {
            actes.noter(id, Lien.NOMME, 1.0);
            Lien.ajouter(player.server, stack, etat, Lien.NOMME_POINTS, player);
        }
        if (REPRIS.remove(id)) {
            Lien.ajouter(player.server, stack, etat, Lien.REPRIS, player);
        }
        // Le maître est le premier à l'avoir porté (une empreinte de son identifiant, qui tient dans une note).
        double moi = player.getUUID().hashCode() | 1;
        double maitre = actes.note(id, Lien.MAITRE);
        if (maitre == 0.0) {
            actes.noter(id, Lien.MAITRE, moi);
        } else if (maitre != moi) {
            int secondes = PRETS.merge(id, 1, Integer::sum);
            if (secondes % 600 == 0) {
                Lien.ajouter(player.server, stack, etat, Lien.PRETE, player);
            }
        } else {
            PRETS.remove(id);
        }
        Lien.places(stack);
    }

    /**
     * Toutes les trente secondes, les objets hors des mains : ceux qui passent la nuit chez soi (dans un cadre, sur un support
     * d'armure) gagnent en attachement, une fois par nuit ; ceux qui traînent au sol en perdent, et plus encore dans le feu.
     */
    private static void horsDesMains(net.neoforged.neoforge.event.tick.ServerTickEvent.Post event) {
        MinecraftServer current = event.getServer();
        if (!FallowConfig.eveilEnabled() || current.getTickCount() % 600 != 0) {
            return;
        }
        Actes actes = Actes.get(current);
        for (ServerLevel level : current.getAllLevels()) {
            boolean nuit = level.isNight();
            double jour = level.getDayTime() / 24000L + 1.0;
            for (Entity entity : level.getAllEntities()) {
                if (entity instanceof ItemEntity item) {
                    ItemStack stack = item.getItem();
                    EtatEveil etat = stack.get(ETAT.get());
                    if (etat != null) {
                        double perte = item.isOnFire() || item.isInLava() ? Lien.FEU : item.getAge() > 1200 ? Lien.AU_SOL : 0.0;
                        Lien.ajouter(current, stack, etat, perte, null);
                    }
                } else if (nuit && (entity instanceof net.minecraft.world.entity.decoration.ItemFrame
                        || entity instanceof net.minecraft.world.entity.decoration.ArmorStand)) {
                    List<ItemStack> poses = new ArrayList<>();
                    if (entity instanceof net.minecraft.world.entity.decoration.ItemFrame frame) {
                        poses.add(frame.getItem());
                    } else {
                        ((LivingEntity) entity).getAllSlots().forEach(poses::add);
                    }
                    for (ItemStack stack : poses) {
                        EtatEveil etat = stack.get(ETAT.get());
                        if (etat != null && actes.note(etat.id(), Lien.NUIT) < jour) {
                            actes.noter(etat.id(), Lien.NUIT, jour);
                            Lien.ajouter(current, stack, etat, Lien.NUIT_CHEZ_SOI, null);
                        }
                    }
                }
            }
        }
    }

    /** Une réparation par Raccommodage n'est pas un haut fait : son usure est notée tout de suite. */
    static void vu(UUID id, int usure) {
        USURES.put(id, usure);
    }

    /** L'objet a disparu pour de bon : ses actes avec lui. */
    private static void oublier(ItemStack stack) {
        EtatEveil etat = stack.get(ETAT.get());
        MinecraftServer current = server;
        if (etat != null && current != null) {
            Actes.get(current).oublier(etat.id());
            USURES.remove(etat.id());
            AVERTIS.remove(etat.id());
        }
    }

    /** L'équipement d'une créature tombe sans enchantement, sauf un objet éveillé qu'elle avait ramassé. */
    private static void onDrops(LivingDropsEvent event) {
        if (!FallowConfig.eveilEnabled()) {
            return;
        }
        if (event.getEntity() instanceof Player player) {
            // Ce qui tombe à la mort d'un joueur : le reprendre lui-même sera un geste d'attachement.
            for (ItemEntity drop : event.getDrops()) {
                EtatEveil etat = drop.getItem().get(ETAT.get());
                if (etat != null) {
                    MORTS.put(etat.id(), player.getUUID());
                }
            }
            return;
        }
        for (ItemEntity drop : event.getDrops()) {
            ItemStack stack = drop.getItem();
            if (stack.is(ECLAT_DE_BETE.get())) {
                eclatTombe(event, drop);
                continue;
            }
            if (stack.has(ETAT.get())) {
                continue;
            }
            ItemStack nu = SansEnchantement.nu(stack);
            if (nu != stack) {
                drop.setItem(nu);
            }
        }
    }

    /**
     * Un éclat qui tombe se voit : il luit à travers les murs, ne disparaît jamais, une colonne de lumière monte, un
     * son cristallin et une note le disent au joueur qui a vaincu la bête.
     */
    private static void eclatTombe(LivingDropsEvent event, ItemEntity drop) {
        drop.setGlowingTag(true);
        drop.setUnlimitedLifetime();
        drop.setInvulnerable(true);
        if (event.getEntity().level() instanceof ServerLevel level) {
            Vec3 p = drop.position();
            level.sendParticles(net.minecraft.core.particles.ParticleTypes.END_ROD, p.x, p.y + 1.0, p.z, 40, 0.15, 1.2, 0.15, 0.02);
            level.playSound(null, drop.blockPosition(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.5F, 0.6F);
        }
        if (event.getSource().getEntity() instanceof ServerPlayer player) {
            player.displayClientMessage(Component.translatable("eveil.fallow.message.eclat_tombe",
                    event.getEntity().getDisplayName()), true);
        }
    }

    /** La meule n'efface pas un éveil : un objet éveillé n'y donne rien. Les autres enchantements n'existent plus. */
    private static void onGrindstone(GrindstoneEvent.OnPlaceItem event) {
        if (!FallowConfig.eveilEnabled()) {
            return;
        }
        ItemStack haut = event.getTopItem();
        ItemStack bas = event.getBottomItem();
        ItemStack seul = haut.isEmpty() ? bas : bas.isEmpty() ? haut : ItemStack.EMPTY;
        EtatEveil seulEtat = seul.get(ETAT.get());
        if (seulEtat != null && !seulEtat.traces().isEmpty()) {
            // Seul sur la meule, un objet éveillé y perd son usure d'enclume (« Trop usé ! ») et garde ses éveils.
            if (seul.getOrDefault(DataComponents.REPAIR_COST, 0) > 0) {
                ItemStack poli = seul.copyWithCount(1);
                poli.remove(DataComponents.REPAIR_COST);
                event.setOutput(poli);
                event.setXp(0);
            } else {
                event.setCanceled(true);
            }
            return;
        }
        for (ItemStack stack : new ItemStack[] {haut, bas}) {
            EtatEveil etat = stack.get(ETAT.get());
            if (etat != null && !etat.traces().isEmpty() && stack.isEnchanted()) {
                event.setCanceled(true);
                return;
            }
        }
    }

    /** TreeChop abat les arbres coup par coup sans casser de bloc : chaque coup compte comme un bloc cassé. */
    @SuppressWarnings("unchecked")
    private static void ecouterTreeChop() {
        try {
            Class<? extends Event> finish = (Class<? extends Event>) Class.forName("ht.treechop.api.ChopEvent$FinishChopEvent");
            Method getPlayer = finish.getMethod("getPlayer");
            Method getState = finish.getMethod("getChoppedBlockState");
            Method getChops = finish.getMethod("getNumChops");
            NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, false, (Class<Event>) finish, event -> {
                try {
                    if (getPlayer.invoke(event) instanceof ServerPlayer player) {
                        BlockState state = (BlockState) getState.invoke(event);
                        int chops = (Integer) getChops.invoke(event);
                        Eveil.acte(player, player.getMainHandItem(), HautsFaits.Type.CASSER, entree -> entree.bloc(state),
                                Math.max(1, chops));
                    }
                } catch (ReflectiveOperationException e) {
                    Fallow.LOGGER.debug("eveil : coup de TreeChop illisible", e);
                }
            });
        } catch (ReflectiveOperationException | LinkageError e) {
            Fallow.LOGGER.warn("eveil : TreeChop présent mais son événement n'a pas été trouvé ; les coups ne compteront pas", e);
        }
    }

    // ---- la table ----

    private static void onTable(PlayerInteractEvent.RightClickBlock event) {
        if (!FallowConfig.eveilEnabled() || !event.getLevel().getBlockState(event.getPos()).is(Blocks.ENCHANTING_TABLE)) {
            return;
        }
        Player joueur = event.getEntity();
        boolean poste = DIEGESIS && FallowConfig.eveilPoste() && !joueur.isSecondaryUseActive();
        if (poste && TableEveil.ouvre(joueur, event.getPos(), event.getLevel().getBlockState(event.getPos()))) {
            // Le poste de Diegesis s'ouvre (TableEveil), par son propre écouteur du clic ; s'il ne s'ouvre pas, jamaisVanilla
            // ferme la porte à la table de vanilla.
            return;
        }
        event.setCanceled(true);
        event.setCancellationResult(InteractionResult.SUCCESS);
        if (event.getHand() != InteractionHand.MAIN_HAND || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (poste) {
            // La table dort : rien de ce que le joueur porte n'est prêt. Un souffle à peine, et elle se tait.
            dire(player, "eveil.fallow.table.muette");
            player.level().playSound(null, event.getPos(), SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 0.25F, 0.5F);
            return;
        }
        ItemStack objet = player.getMainHandItem();
        ItemStack autre = player.getOffhandItem();
        if (!aDeLEncre(player) && (TableEveil.attendu(objet) || TableEveil.eclat(autre))) {
            // Sans encre d'éveil, la table ne peut rien écrire.
            dire(player, "eveil.fallow.table.sans_encre");
            return;
        }
        if (autre.is(ECLAT_DE_BETE.get()) && autre.has(ECLAT.get())) {
            poserEclat(player, objet, autre, event);
            return;
        }
        if (objet.is(ECLAT_DE_BETE.get())) {
            dire(player, "eveil.fallow.table.eclat_main");
            return;
        }
        if (objet.isEmpty()) {
            dire(player, "eveil.fallow.table.vide");
            return;
        }
        EtatEveil etat = objet.get(ETAT.get());
        if (etat != null) {
            etat = Eveil.reconcilier(objet, etat);
        }
        if (etat == null || (!etat.pret() && !etat.plein())) {
            dire(player, HautsFaits.pour(objet).isEmpty() ? "eveil.fallow.table.rien" : "eveil.fallow.table.pas_encore");
            return;
        }
        if (!etat.pret()) {
            dire(player, "eveil.fallow.table.plein");
            return;
        }
        List<Eveil.Option> options = Eveil.options((ServerLevel) player.level(), objet, etat);
        if (options.isEmpty()) {
            objet.set(ETAT.get(), etat.avecPret(false));
            objet.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            dire(player, "eveil.fallow.table.rien_a_apprendre");
            return;
        }
        player.openMenu(new SimpleMenuProvider(
                (id, inventory, p) -> new ChoixMenu(id, inventory, objet, options, event.getPos()),
                Component.translatable("eveil.fallow.choix.titre", objet.getHoverName())));
    }

    /**
     * La fenêtre d'enchantement de vanilla ne reste jamais ouverte : elle passerait outre l'éveil. La table ouvre notre poste
     * ({@code PosteDeTableMixin} fait qu'il est trouvé avant celui, coupé, de Hands-On Vanilla) ou la fenêtre de choix ; si une
     * fenêtre de vanilla s'ouvre quand même, d'où qu'elle vienne, elle se referme aussitôt.
     */
    private static void jamaisVanilla(net.neoforged.neoforge.event.entity.player.PlayerContainerEvent.Open event) {
        if (FallowConfig.eveilEnabled() && event.getContainer() instanceof net.minecraft.world.inventory.EnchantmentMenu
                && event.getEntity() instanceof ServerPlayer player) {
            player.server.execute(() -> {
                if (player.containerMenu instanceof net.minecraft.world.inventory.EnchantmentMenu) {
                    player.closeContainer();
                }
            });
        }
    }

    private static void poserEclat(ServerPlayer player, ItemStack objet, ItemStack eclatStack,
            PlayerInteractEvent.RightClickBlock event) {
        Eclat eclat = eclatStack.get(ECLAT.get());
        if (objet.isEmpty()) {
            dire(player, "eveil.fallow.table.vide");
            return;
        }
        Optional<net.minecraft.core.Holder.Reference<net.minecraft.world.item.enchantment.Enchantment>> holder =
                player.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                        .getHolder(ResourceKey.create(Registries.ENCHANTMENT, eclat.enchantement()));
        if (holder.isEmpty() || !objet.supportsEnchantment(holder.get())) {
            dire(player, "eveil.fallow.eclat.ne_prend_pas");
            return;
        }
        Eveil.Option option = Eveil.option(objet, holder.get(), eclat.niveau());
        if (option == null) {
            dire(player, "eveil.fallow.eclat.deja");
            return;
        }
        EtatEveil etat = Eveil.reconcilier(objet, Eveil.etat(objet));
        if (etat.plein()) {
            dire(player, "eveil.fallow.table.plein");
            return;
        }
        if (!PartDeBete.porte(player, objet)) {
            player.displayClientMessage(Component.translatable("eveil.fallow.table.sans_part",
                    PartDeBete.de(objet).map(Item::getDescription).orElse(Component.empty())), true);
            return;
        }
        if (!prendreEncre(player)) {
            dire(player, "eveil.fallow.table.sans_encre");
            return;
        }
        PartDeBete.prendre(player, objet);
        Eveil.appliquer(player, objet, option, Optional.of(eclat.bete()), event.getPos());
        eclatStack.shrink(1);
    }

    static boolean encre(ItemStack stack) {
        return stack.is(ENCRE_D_EVEIL.get());
    }

    /** Le joueur porte au moins une fiole d'encre d'éveil (la fenêtre d'avant, accroupi, la prend dans l'inventaire). */
    static boolean aDeLEncre(Player player) {
        return player.getAbilities().instabuild || player.getInventory().items.stream().anyMatch(EveilModule::encre)
                || encre(player.getOffhandItem());
    }

    /** Une fiole d'encre d'éveil prise dans l'inventaire ; faux s'il n'y en a pas (le mode créatif n'en use pas). */
    static boolean prendreEncre(Player player) {
        if (player.getAbilities().instabuild) {
            return true;
        }
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (encre(inventory.getItem(i))) {
                inventory.getItem(i).shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static void dire(Player player, String key) {
        player.displayClientMessage(Component.translatable(key), true);
    }

    // ---- les échanges ----

    /** Un marchand ne vend plus rien d'enchanté : l'offre est retirée (une offre nulle n'est pas proposée). */
    private static void sansEnchantement(List<VillagerTrades.ItemListing> listings) {
        listings.replaceAll(listing -> listing instanceof SansEnchantementListing ? listing
                : new SansEnchantementListing(listing));
    }

    private record SansEnchantementListing(VillagerTrades.ItemListing listing) implements VillagerTrades.ItemListing {
        @Override
        public MerchantOffer getOffer(net.minecraft.world.entity.Entity trader, net.minecraft.util.RandomSource random) {
            MerchantOffer offer = listing.getOffer(trader, random);
            if (offer == null || !FallowConfig.eveilEnabled()) {
                return offer;
            }
            ItemStack result = offer.getResult();
            return result.isEnchanted() || result.has(DataComponents.STORED_ENCHANTMENTS) ? null : offer;
        }
    }

    // ---- commandes d'essai ----

    /**
     * {@code /fallow eveil voir} : les points de l'objet en main, par acte, et le seuil du prochain éveil.
     * {@code /fallow eveil points <n>} : ajoute {@code n} points au premier acte (hors réparation) de l'objet en main (opérateurs, pour les essais).
     */
    private static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal(Fallow.MOD_ID)
                .requires(source -> source.hasPermission(2))
                .then(Commands.literal("eveil")
                        .then(Commands.literal("voir").executes(context -> {
                            ServerPlayer player = context.getSource().getPlayerOrException();
                            ItemStack stack = player.getMainHandItem();
                            EtatEveil etat = stack.get(ETAT.get());
                            StringBuilder text = new StringBuilder(etat == null ? "Aucun acte compté"
                                    : "Éveils " + etat.occupees() + "/" + etat.places() + (etat.pret() ? ", prêt" : "")
                                            + (etat.eteint() ? ", éteint" : "") + ", qualité " + Eveil.qualite(stack)
                                            + ", attachement " + String.format("%.1f", Actes.get(context.getSource().getServer())
                                                    .note(etat.id(), Lien.POINTS)) + " (" + Lien.cle(Lien.de(stack)) + ")");
                            Eveil.detail(context.getSource().getServer(), stack).forEach((k, v) ->
                                    text.append("\n").append(k).append(" : ").append(String.format("%.1f", v)));
                            context.getSource().sendSuccess(() -> Component.literal(text.toString()), false);
                            return 1;
                        }))
                        .then(Commands.literal("points")
                                .then(Commands.argument("n", DoubleArgumentType.doubleArg(0))
                                        .executes(context -> {
                                            ServerPlayer player = context.getSource().getPlayerOrException();
                                            double n = DoubleArgumentType.getDouble(context, "n");
                                            ItemStack stack = player.getMainHandItem();
                                            List<HautsFaits.Entree> entrees = HautsFaits.pour(stack);
                                            if (entrees.isEmpty()) {
                                                context.getSource().sendFailure(Component.literal("Cet objet n'a pas de hauts faits"));
                                                return 0;
                                            }
                                            HautsFaits.Entree premiere = entrees.stream()
                                                    .filter(e -> e.type() != HautsFaits.Type.REPARER).findFirst()
                                                    .orElse(entrees.get(0));
                                            Eveil.acte(player, stack, premiere.type(), entree -> entree == premiere,
                                                    n / Math.max(premiere.points(), 1e-6));
                                            context.getSource().sendSuccess(() -> Component.literal(
                                                    n + " points ajoutés à " + premiere.cle()), true);
                                            return 1;
                                        })))));
    }
}
