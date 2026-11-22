package io.github.salvagedisc.fallow.eveil;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import com.pockyl.diegesis.api.Diegesis;
import com.pockyl.diegesis.api.state.SlotRef;
import com.pockyl.diegesis.api.state.SlotRules;
import com.pockyl.diegesis.api.state.StationSchema;
import com.pockyl.diegesis.api.station.StationContext;
import com.pockyl.diegesis.api.station.StationLogic;
import com.pockyl.diegesis.api.station.StationType;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;

/**
 * La table d'enchantement en poste (bibliothèque Diegesis), comme celles de Hands-On Vanilla : la caméra se penche sur la
 * table, l'objet s'y pose (et l'éclat d'une grande bête à côté), le livre vient de la table et s'ouvre. La page de gauche
 * raconte l'objet (ses éveils, ses plus grands hauts faits, où il en est), celle de droite montre les deux voies que ses actes
 * ouvrent, ou ce que l'éclat lui donnerait ; un clic sur une voie l'éveille. Tout ce que les pages disent est calculé ici, sur
 * le serveur qui tient les actes ({@link Actes}), et part au client dans les données du poste ; le client ne fait que l'écrire.
 * Accroupi, ou le poste coupé ({@link FallowConfig#eveilPoste}), la table ouvre la fenêtre de choix d'avant ({@link ChoixMenu}).
 */
public final class TableEveil {
    public static final ResourceLocation ID = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "table_d_eveil");

    /** Les deux places de la table : l'objet, et l'éclat d'une grande bête. */
    public static final String OBJET = "objet";
    public static final String ECLAT = "eclat";
    public static final SlotRef OBJET_REF = SlotRef.of(OBJET, 0);
    public static final SlotRef ECLAT_REF = SlotRef.of(ECLAT, 0);
    /** L'encrier : l'encre d'éveil, sans laquelle la table n'écrit aucune voie ; une fiole par éveil. */
    public static final String ENCRE = "encre";
    public static final SlotRef ENCRE_REF = SlotRef.of(ENCRE, 0);

    /** {@code choisir} : l'éveil de la voie {@link #VOIE} (0 ou 1) de la page de droite. */
    public static final String CHOISIR = "choisir";
    public static final String VOIE = "voie";
    /** {@code eclat} : l'éclat posé sur la table entre dans l'objet. */
    public static final String VERSER = "eclat";
    /** {@code poser} : la pile de la case {@link #DEPUIS} de l'inventaire va à sa place sur la table. */
    public static final String POSER = "poser";
    public static final String DEPUIS = "depuis";

    /**
     * L'effet joué pour tous ceux qui voient la table quand un objet s'éveille : {@link #ENCHANTEMENT} (l'id), {@link #NIVEAU},
     * {@link #PAR_ECLAT} (venu d'un éclat) et {@link #ACTEUR} (qui l'a fait, qui l'a déjà vu chez lui).
     */
    public static final ResourceLocation EVEIL = ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "eveil");
    public static final String ENCHANTEMENT = "enchantement";
    public static final String NIVEAU = "niveau";
    public static final String PAR_ECLAT = "par_eclat";
    public static final String ACTEUR = "acteur";

    // Les données des pages (StationState.data()).
    /** Ce que dit la page de droite : un des {@code ETAT_*}. */
    public static final String ETAT = "etat";
    public static final String ETAT_VIDE = "vide";
    public static final String ETAT_RIEN = "rien";
    public static final String ETAT_PAS_ENCORE = "pas_encore";
    public static final String ETAT_PRET = "pret";
    public static final String ETAT_PLEIN = "plein";
    public static final String ETAT_RIEN_A_APPRENDRE = "rien_a_apprendre";
    /** Avec un éclat posé : ce qu'il ferait ({@code ECLAT_*}). */
    public static final String ECLAT_ETAT = "eclat_etat";
    public static final String ECLAT_OK = "ok";
    public static final String ECLAT_SANS_OBJET = "sans_objet";
    public static final String ECLAT_NE_PREND_PAS = "ne_prend_pas";
    public static final String ECLAT_DEJA = "deja";
    public static final String ECLAT_PLEIN = "plein";
    /** Le niveau que l'éclat donnerait, et celui que l'objet porte déjà (0 : nouveau). */
    public static final String ECLAT_NIVEAU = "eclat_niveau";
    /** Le chemin vers le prochain éveil, de 0 à 1. */
    public static final String AVANCEE = "avancee";
    /**
     * Les voies : {@link #ENCHANTEMENT}, {@link #NIVEAU}, {@link #NE_DE} (la clé du nom de l'acte qui l'a le plus fait naître) et
     * {@link #FOIS} (combien de fois cet acte).
     */
    public static final String VOIES = "voies";
    public static final String NE_DE = "ne_de";
    /** Les plus grands hauts faits de l'objet : {@link #NOM} (la clé de son nom) et {@link #FOIS}. */
    public static final String FAITS = "faits";
    public static final String NOM = "nom";
    public static final String FOIS = "fois";
    /** Change à chaque nouvelle page à écrire : le client tourne la page quand il change. */
    public static final String CLE = "cle";

    private TableEveil() {
    }

    /** Ce qui se pose à la place de l'objet : ce qui a pu vivre (s'use, s'enchante, ou a déjà compté). Les deux côtés le lisent. */
    public static boolean objet(ItemStack stack) {
        return !stack.isEmpty() && !stack.is(EveilModule.ECLAT_DE_BETE.get())
                && (stack.has(EveilModule.ETAT.get()) || stack.isDamageableItem() || stack.isEnchantable());
    }

    public static boolean eclat(ItemStack stack) {
        return stack.is(EveilModule.ECLAT_DE_BETE.get()) && stack.has(EveilModule.ECLAT.get());
    }

    public static boolean encre(ItemStack stack) {
        return EveilModule.encre(stack);
    }

    static void register() {
        Diegesis.station(ID)
                .blocks(Blocks.ENCHANTING_TABLE)
                .hidesBlock()
                .schema(StationSchema.builder()
                        .slots(OBJET, 1, SlotRules.DEFAULT.maxStack(1).accepts(TableEveil::objet))
                        .slots(ECLAT, 1, SlotRules.DEFAULT.maxStack(1).accepts(TableEveil::eclat))
                        .slots(ENCRE, 1, SlotRules.DEFAULT.maxStack(16).accepts(TableEveil::encre))
                        .build())
                .logic(Table::new)
                .orientation(StationType.Orientation.PLAYER_FREE)
                .exclusive()
                .openWhen(TableEveil::ouvre)
                .register();
    }

    /** Accroupi, la table garde la fenêtre de choix d'avant ; le poste coupé aussi. */
    static boolean ouvre(Player player, BlockPos pos, BlockState state) {
        return FallowConfig.eveilEnabled() && FallowConfig.eveilPoste() && !player.isSecondaryUseActive() && eveille(player);
    }

    /**
     * La table dort : elle ne s'éveille que pour qui porte un objet prêt à s'éveiller ou l'éclat d'une grande bête. Le reste du
     * temps, un clic droit ne l'ouvre pas ({@link EveilModule}, « La table reste muette »).
     */
    public static boolean eveille(Player player) {
        net.minecraft.world.entity.player.Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            if (attendu(inventory.getItem(i))) {
                return true;
            }
        }
        return false;
    }

    /** Ce qui fait s'éveiller la table : un objet prêt, ou un éclat. */
    public static boolean attendu(ItemStack stack) {
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        return eclat(stack) || etat != null && etat.pret();
    }

    /** Le travail de la table sur le serveur : ce qu'elle dit de l'objet posé, et l'éveil. */
    private static final class Table implements StationLogic {
        private int cle;
        /** L'objet qui vient de n'avoir plus rien à apprendre : il cesse d'attendre, la page continue de le dire. */
        private java.util.UUID muet;
        /** Ce que les pages disaient de la table, pour voir ce qui a changé. */
        private ItemStack vuObjet = ItemStack.EMPTY;
        private ItemStack vuEclat = ItemStack.EMPTY;

        Table(StationContext ctx) {
        }

        @Override
        public void onOpen(StationContext ctx, Player player) {
            ecrire(ctx);
        }

        @Override
        public boolean mayPlace(StationContext ctx, Player player, SlotRef ref, ItemStack stack) {
            return OBJET.equals(ref.group()) ? objet(stack) : ENCRE.equals(ref.group()) ? encre(stack) : eclat(stack);
        }

        @Override
        public void onSlotChanged(StationContext ctx, SlotRef ref) {
            ecrire(ctx);
        }

        @Override
        public void onCustomAction(StationContext ctx, Player player, String action, CompoundTag data) {
            if (!(player instanceof ServerPlayer serverPlayer) || !FallowConfig.eveilEnabled()) {
                return;
            }
            switch (action) {
                case CHOISIR -> choisir(ctx, serverPlayer, data.getInt(VOIE));
                case VERSER -> verser(ctx, serverPlayer);
                case POSER -> poser(ctx, serverPlayer, data.getInt(DEPUIS));
                default -> {
                }
            }
        }

        private void choisir(StationContext ctx, ServerPlayer player, int voie) {
            ItemStack stack = ctx.state().getItem(OBJET_REF).copy();
            EtatEveil etat = stack.get(EveilModule.ETAT.get());
            if (etat == null || !(ctx.level() instanceof ServerLevel level)) {
                return;
            }
            etat = Eveil.reconcilier(stack, etat);
            if (!etat.pret()) {
                return;
            }
            List<Eveil.Option> options = Eveil.options(level, stack, etat);
            if (voie < 0 || voie >= options.size()) {
                return;
            }
            Eveil.Option option = options.get(voie);
            // Une armure d'Archie demande aussi une part de sa bête ; le livre dit ce qui manque.
            if (!PartDeBete.porte(player, stack) || !encrer(ctx, player)) {
                return;
            }
            PartDeBete.prendre(player, stack);
            Eveil.appliquer(player, stack, option, Optional.empty(), ctx.pos());
            ctx.state().setItem(OBJET_REF, stack);
            joue(ctx, player, option, false);
            ecrire(ctx);
        }

        private void verser(StationContext ctx, ServerPlayer player) {
            ItemStack eclatStack = ctx.state().getItem(ECLAT_REF).copy();
            ItemStack stack = ctx.state().getItem(OBJET_REF).copy();
            Eclat eclat = eclatStack.get(EveilModule.ECLAT.get());
            if (eclat == null || stack.isEmpty()) {
                return;
            }
            Eveil.Option option = optionEclat(ctx, stack, eclat);
            if (option == null || Eveil.reconcilier(stack, Eveil.etat(stack)).plein() || !PartDeBete.porte(player, stack)
                    || !encrer(ctx, player)) {
                return;
            }
            PartDeBete.prendre(player, stack);
            Eveil.appliquer(player, stack, option, Optional.of(eclat.bete()), ctx.pos());
            eclatStack.shrink(1);
            ctx.state().setItem(OBJET_REF, stack);
            ctx.state().setItem(ECLAT_REF, eclatStack);
            joue(ctx, player, option, true);
            ecrire(ctx);
        }

        /** Une fiole de l'encrier part dans les runes de la voie ; sans encre, rien ne s'écrit (le mode créatif n'en use pas). */
        private static boolean encrer(StationContext ctx, ServerPlayer player) {
            if (player.getAbilities().instabuild) {
                return true;
            }
            ItemStack encre = ctx.state().getItem(ENCRE_REF).copy();
            if (encre.isEmpty()) {
                // Le livre le dit déjà (« L'encrier est vide »), en rouge au pied de la page.
                return false;
            }
            encre.shrink(1);
            ctx.state().setItem(ENCRE_REF, encre);
            return true;
        }

        /** Du curseur de l'inventaire (Maj + clic) ou de la main en arrivant : la pile va à sa place, ce qui y était revient. */
        private void poser(StationContext ctx, ServerPlayer player, int depuis) {
            Inventory inventory = player.getInventory();
            if (depuis < 0 || depuis >= inventory.getContainerSize()) {
                return;
            }
            ItemStack stack = inventory.getItem(depuis);
            if (encre(stack)) {
                // L'encre se verse toute dans l'encrier, jusqu'à ce qu'il soit plein.
                ItemStack dedans = ctx.state().getItem(ENCRE_REF);
                int place = 16 - dedans.getCount();
                if (place > 0) {
                    ItemStack verse = stack.split(place);
                    verse.grow(dedans.getCount());
                    inventory.setChanged();
                    ctx.state().setItem(ENCRE_REF, verse);
                }
                return;
            }
            SlotRef ref = objet(stack) ? OBJET_REF : eclat(stack) ? ECLAT_REF : null;
            if (ref == null) {
                return;
            }
            ItemStack avant = ctx.state().getItem(ref);
            ItemStack pose = stack.split(1);
            inventory.setChanged();
            ctx.state().setItem(ref, pose);
            if (!avant.isEmpty() && !inventory.add(avant)) {
                player.drop(avant, false);
            }
            ecrire(ctx);
        }

        /** Ce qui a changé sur la table sans passer par un geste du poste (posé au curseur, repris) : les pages suivent. */
        @Override
        public void tick(StationContext ctx) {
            if (!ItemStack.matches(ctx.state().getItem(OBJET_REF), vuObjet) || !ItemStack.matches(ctx.state().getItem(ECLAT_REF), vuEclat)) {
                ecrire(ctx);
            }
        }

        private static void joue(StationContext ctx, ServerPlayer player, Eveil.Option option, boolean eclat) {
            CompoundTag effet = new CompoundTag();
            effet.putString(ENCHANTEMENT, option.enchantement().unwrapKey().orElseThrow().location().toString());
            effet.putInt(NIVEAU, option.niveau());
            effet.putBoolean(PAR_ECLAT, eclat);
            effet.putUUID(ACTEUR, player.getUUID());
            ctx.effect(EVEIL, OBJET, effet);
        }

        private static Eveil.Option optionEclat(StationContext ctx, ItemStack stack, Eclat eclat) {
            Optional<Holder.Reference<Enchantment>> holder = ctx.level().registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ResourceKey.create(Registries.ENCHANTMENT, eclat.enchantement()));
            return holder.isEmpty() || !stack.supportsEnchantment(holder.get()) ? null : Eveil.option(stack, holder.get(), eclat.niveau());
        }

        /** Les pages : ce que la table dit de l'objet posé et de l'éclat, pour le livre du client. */
        private void ecrire(StationContext ctx) {
            if (!(ctx.level() instanceof ServerLevel level)) {
                return;
            }
            CompoundTag data = new CompoundTag();
            ItemStack stack = ctx.state().getItem(OBJET_REF);
            ItemStack eclatStack = ctx.state().getItem(ECLAT_REF);
            Actes actes = Actes.get(level.getServer());
            EtatEveil etat = stack.get(EveilModule.ETAT.get());
            if (etat != null) {
                EtatEveil reconcilie = Eveil.reconcilier(stack.copy(), etat);
                etat = reconcilie;
            }
            String dit;
            if (stack.isEmpty()) {
                dit = ETAT_VIDE;
            } else if (HautsFaits.pour(stack).isEmpty() && etat == null) {
                dit = ETAT_RIEN;
            } else if (etat != null && etat.pret()) {
                List<Eveil.Option> options = Eveil.options(level, stack, etat);
                if (options.isEmpty()) {
                    // Comme à la fenêtre d'avant : il n'y a plus rien à apprendre, l'objet cesse d'attendre.
                    ItemStack eteint = stack.copy();
                    eteint.set(EveilModule.ETAT.get(), etat.avecPret(false));
                    eteint.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
                    muet = etat.id();
                    ctx.state().setItem(OBJET_REF, eteint);
                    dit = ETAT_RIEN_A_APPRENDRE;
                } else {
                    dit = ETAT_PRET;
                    ListTag voies = new ListTag();
                    for (Eveil.Option option : options) {
                        CompoundTag voie = new CompoundTag();
                        ResourceLocation id = option.enchantement().unwrapKey().orElseThrow().location();
                        voie.putString(ENCHANTEMENT, id.toString());
                        voie.putInt(NIVEAU, option.niveau());
                        HautsFaits.Entree ne = origine(actes, stack, etat, id);
                        if (ne != null) {
                            voie.putString(NE_DE, ne.nom());
                            voie.putDouble(FOIS, fois(actes, etat, ne));
                        }
                        voies.add(voie);
                    }
                    data.put(VOIES, voies);
                }
            } else if (etat != null && etat.plein()) {
                dit = ETAT_PLEIN;
            } else if (etat != null && etat.id().equals(muet)) {
                dit = ETAT_RIEN_A_APPRENDRE;
            } else {
                dit = ETAT_PAS_ENCORE;
                data.putFloat(AVANCEE, etat == null ? 0.0F : (float) Eveil.avancee(actes, etat));
            }
            data.putString(ETAT, dit);
            if (etat != null) {
                data.put(FAITS, faits(actes, stack, etat));
            }
            Eclat eclat = eclatStack.get(EveilModule.ECLAT.get());
            if (eclat != null) {
                String eclatDit;
                if (stack.isEmpty()) {
                    eclatDit = ECLAT_SANS_OBJET;
                } else {
                    Optional<Holder.Reference<Enchantment>> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                            .getHolder(ResourceKey.create(Registries.ENCHANTMENT, eclat.enchantement()));
                    if (holder.isEmpty() || !stack.supportsEnchantment(holder.get())) {
                        eclatDit = ECLAT_NE_PREND_PAS;
                    } else if (Eveil.option(stack, holder.get(), eclat.niveau()) == null) {
                        eclatDit = ECLAT_DEJA;
                    } else if (etat != null && etat.plein()) {
                        eclatDit = ECLAT_PLEIN;
                    } else {
                        eclatDit = ECLAT_OK;
                    }
                }
                data.putString(ECLAT_ETAT, eclatDit);
            }
            CompoundTag avant = ctx.state().data().copy();
            avant.remove(CLE);
            if (!avant.equals(data)) {
                cle++;
            }
            data.putInt(CLE, cle);
            ctx.state().setData(data);
            vuObjet = ctx.state().getItem(OBJET_REF).copy();
            vuEclat = ctx.state().getItem(ECLAT_REF).copy();
        }

        /** L'acte qui a le plus fait naître cet enchantement chez cet objet, pour « né de … » ; null si aucun n'a de nom. */
        private static HautsFaits.Entree origine(Actes actes, ItemStack stack, EtatEveil etat, ResourceLocation enchantement) {
            HautsFaits.Entree meilleure = null;
            double plus = 0.0;
            for (HautsFaits.Entree entree : HautsFaits.pour(stack)) {
                Double poids = entree.enchantements().get(enchantement);
                double part = poids == null ? 0.0 : poids * actes.points(etat.id(), entree.cle());
                if (part > plus && !entree.nom().isEmpty()) {
                    plus = part;
                    meilleure = entree;
                }
            }
            return meilleure;
        }

        /** Combien de fois l'acte a eu lieu ; ceux comptés avant que la table sache compter sont estimés d'après leurs points. */
        private static double fois(Actes actes, EtatEveil etat, HautsFaits.Entree entree) {
            double fois = actes.fois(etat.id(), entree.cle());
            return fois > 0 ? fois : actes.points(etat.id(), entree.cle()) / Math.max(entree.points(), 1e-6);
        }

        /** Les trois plus grands hauts faits de l'objet, par les points qu'ils lui ont valus, avec combien de fois. */
        private static ListTag faits(Actes actes, ItemStack stack, EtatEveil etat) {
            List<Map.Entry<HautsFaits.Entree, Double>> tries = new ArrayList<>();
            for (HautsFaits.Entree entree : HautsFaits.pour(stack)) {
                double points = actes.points(etat.id(), entree.cle());
                if (points > 0 && !entree.nom().isEmpty()) {
                    tries.add(Map.entry(entree, points));
                }
            }
            tries.sort(Comparator.comparingDouble((Map.Entry<HautsFaits.Entree, Double> e) -> e.getValue()).reversed());
            ListTag faits = new ListTag();
            for (Map.Entry<HautsFaits.Entree, Double> e : tries.subList(0, Math.min(3, tries.size()))) {
                CompoundTag fait = new CompoundTag();
                fait.putString(NOM, e.getKey().nom());
                fait.putDouble(FOIS, fois(actes, etat, e.getKey()));
                faits.add(fait);
            }
            return faits;
        }
    }

    /** Lit une liste de données des pages (côté client aussi). */
    public static ListTag liste(CompoundTag data, String cle) {
        return data.getList(cle, Tag.TAG_COMPOUND);
    }
}
