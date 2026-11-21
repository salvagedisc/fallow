package io.github.salvagedisc.fallow.eveil;

import io.github.salvagedisc.fallow.FallowConfig;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.ItemEnchantments;

/**
 * Le cœur des hauts faits (docs/plan/eveil.md) : compter les actes d'un objet, le dire prêt quand il a assez vécu,
 * proposer deux enchantements tirés de ses actes, poser l'éveil choisi ou l'éclat d'une grande bête, et oublier le
 * dernier éveil quand l'objet arrive à bout.
 */
public final class Eveil {
    private static final ResourceLocation QUALITE = ResourceLocation.fromNamespaceAndPath("overgeared", "forging_quality");
    private static final ResourceLocation MENDING = ResourceLocation.withDefaultNamespace("mending");

    private Eveil() {
    }

    /** Un choix proposé à la table : l'enchantement et le niveau qu'il prendrait. */
    record Option(Holder<Enchantment> enchantement, int niveau) {
    }

    // ---- les actes ----

    /** L'objet vient d'accomplir {@code quantite} unités d'actes de ce type ; {@code filtre} choisit les entrées concernées. */
    static void acte(ServerPlayer player, ItemStack stack, HautsFaits.Type type, Predicate<HautsFaits.Entree> filtre,
            double quantite) {
        if (!FallowConfig.eveilEnabled() || stack.isEmpty() || quantite <= 0 || player.isSpectator()) {
            return;
        }
        List<HautsFaits.Entree> entrees = new ArrayList<>();
        for (HautsFaits.Entree entree : HautsFaits.pour(stack)) {
            if (entree.type() == type && filtre.test(entree)) {
                entrees.add(entree);
            }
        }
        if (entrees.isEmpty()) {
            return;
        }
        EtatEveil etat = etat(stack);
        Actes actes = Actes.get(player.server);
        double gagnes = 0;
        for (HautsFaits.Entree entree : entrees) {
            actes.ajouter(etat.id(), entree.cle(), entree.points() * quantite);
            gagnes += entree.points() * quantite;
        }
        if (type != HautsFaits.Type.REPARER) {
            raccommoder(player, stack, etat, gagnes);
        }
        verifier(player, stack, etat, actes);
    }

    /**
     * L'objet réellement porté par le joueur : une flèche ou un carreau tient une copie de l'arc qui l'a tiré, on
     * retrouve l'original par son identifiant ; un trident lancé tient le vrai, qui reviendra.
     */
    static ItemStack reel(ServerPlayer player, ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return ItemStack.EMPTY;
        }
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        Inventory inventaire = player.getInventory();
        for (int i = 0; i < inventaire.getContainerSize(); i++) {
            ItemStack porte = inventaire.getItem(i);
            if (porte == stack) {
                return stack;
            }
            EtatEveil autre = porte.get(EveilModule.ETAT.get());
            if (etat != null && autre != null && autre.id().equals(etat.id())) {
                return porte;
            }
        }
        if (etat == null) {
            for (ItemStack main : new ItemStack[] {player.getMainHandItem(), player.getOffhandItem()}) {
                if (main.is(stack.getItem()) && main.get(EveilModule.ETAT.get()) == null) {
                    return main;
                }
            }
        }
        return stack;
    }

    /**
     * Raccommodage, sans expérience : l'objet se refait en vivant, {@code raccommodage} point de durabilité par point
     * d'acte et par niveau, la fraction tirée au sort.
     */
    private static void raccommoder(ServerPlayer player, ItemStack stack, EtatEveil etat, double points) {
        if (!stack.isDamaged()) {
            return;
        }
        Optional<Holder.Reference<Enchantment>> mending = enchantement(stack, MENDING);
        int niveau = mending.map(h -> EnchantmentHelper.getTagEnchantmentLevel(h, stack)).orElse(0);
        if (niveau <= 0) {
            return;
        }
        double rendu = points * niveau * FallowConfig.eveilRaccommodage();
        int entier = (int) rendu + (player.getRandom().nextDouble() < rendu - (int) rendu ? 1 : 0);
        if (entier > 0) {
            stack.setDamageValue(Math.max(0, stack.getDamageValue() - entier));
            EveilModule.vu(etat.id(), stack.getDamageValue());
        }
    }

    /** L'état de l'objet, créé au premier acte (identifiant, places selon la qualité de forge). */
    static EtatEveil etat(ItemStack stack) {
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        if (etat == null) {
            etat = EtatEveil.neuf(FallowConfig.eveilPlaces(qualite(stack)));
            stack.set(EveilModule.ETAT.get(), etat);
        }
        return etat;
    }

    /** Seuil de points pour l'éveil numéro {@code n} (0 pour le premier). */
    static double seuil(int n) {
        return FallowConfig.eveilSeuil() * Math.pow(FallowConfig.eveilFacteur(), n);
    }

    /** Le seuil d'où part le chemin vers l'éveil numéro {@code n} (0 pour le premier). */
    static double depart(int n) {
        return n <= 0 ? 0.0 : seuil(n - 1);
    }

    /** Où en est l'objet sur le chemin de son prochain éveil, de 0 à 1. */
    static double avancee(Actes actes, EtatEveil etat) {
        double depart = depart(etat.occupees());
        double total = actes.total(etat.id());
        return Math.max(0.0, Math.min(1.0, (total - depart) / Math.max(1.0, seuil(etat.occupees()) - depart)));
    }

    private static void verifier(ServerPlayer player, ItemStack stack, EtatEveil etat, Actes actes) {
        etat = reconcilier(stack, etat);
        if (etat.pret() || etat.plein()) {
            return;
        }
        if (actes.total(etat.id()) < seuil(etat.occupees())) {
            int palier = Math.min(3, (int) (avancee(actes, etat) * 4));
            if (palier != etat.palier()) {
                stack.set(EveilModule.ETAT.get(), etat.avecPalier(palier));
            }
            return;
        }
        stack.set(EveilModule.ETAT.get(), etat.avecPret(true).avecPalier(0));
        stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true);
        player.displayClientMessage(Component.translatable("eveil.fallow.message.pret", stack.getHoverName()), true);
        player.playNotifySound(SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Retire les traces dont l'enchantement n'est plus sur l'objet (meule, commande). */
    static EtatEveil reconcilier(ItemStack stack, EtatEveil etat) {
        List<EtatEveil.Trace> gardees = new ArrayList<>();
        for (EtatEveil.Trace trace : etat.traces()) {
            Optional<Holder.Reference<Enchantment>> holder = enchantement(stack, trace.enchantement());
            if (holder.isPresent() && EnchantmentHelper.getTagEnchantmentLevel(holder.get(), stack) >= trace.niveau()) {
                gardees.add(trace);
            }
        }
        if (gardees.size() == etat.traces().size()) {
            return etat;
        }
        EtatEveil updated = etat.avecTraces(gardees);
        stack.set(EveilModule.ETAT.get(), updated);
        return updated;
    }

    // ---- la table ----

    /** Les deux choix (au plus) que l'objet révèle, tirés au hasard selon ses actes, toujours les mêmes pour cet éveil. */
    static List<Option> options(ServerLevel level, ItemStack stack, EtatEveil etat) {
        Actes actes = Actes.get(level.getServer());
        Map<ResourceLocation, Double> poids = new LinkedHashMap<>();
        for (HautsFaits.Entree entree : HautsFaits.pour(stack)) {
            double points = actes.points(etat.id(), entree.cle());
            if (points > 0) {
                entree.enchantements().forEach((ench, w) -> poids.merge(ench, points * w, Double::sum));
            }
        }
        Map<Option, Double> eligibles = new LinkedHashMap<>();
        poids.forEach((id, w) -> {
            Optional<Holder.Reference<Enchantment>> holder = level.registryAccess().registryOrThrow(Registries.ENCHANTMENT)
                    .getHolder(ResourceKey.create(Registries.ENCHANTMENT, id));
            if (w <= 0 || holder.isEmpty()) {
                return;
            }
            Option option = option(stack, holder.get(), 0);
            if (option != null) {
                eligibles.put(option, w);
            }
        });
        RandomSource random = RandomSource.create(etat.id().getMostSignificantBits() ^ (31L * etat.occupees() + 7));
        List<Option> choisies = new ArrayList<>();
        while (choisies.size() < 2 && !eligibles.isEmpty()) {
            double somme = eligibles.values().stream().mapToDouble(Double::doubleValue).sum();
            double tirage = random.nextDouble() * somme;
            Option prise = null;
            for (Map.Entry<Option, Double> e : eligibles.entrySet()) {
                tirage -= e.getValue();
                prise = e.getKey();
                if (tirage <= 0) {
                    break;
                }
            }
            choisies.add(prise);
            eligibles.remove(prise);
        }
        return choisies;
    }

    /**
     * Ce que l'enchantement donnerait à l'objet : le niveau suivant (ou {@code niveau} s'il est donné, pour un éclat),
     * ou {@code null} s'il ne prend pas (objet incompatible, niveau déjà atteint, conflit avec ce qu'il porte).
     */
    static Option option(ItemStack stack, Holder<Enchantment> holder, int niveau) {
        if (!stack.supportsEnchantment(holder)) {
            return null;
        }
        int actuel = EnchantmentHelper.getTagEnchantmentLevel(holder, stack);
        int vise = niveau > 0 ? niveau : actuel + 1;
        if (vise <= actuel || vise > Math.max(niveau, holder.value().getMaxLevel())) {
            return null;
        }
        if (actuel == 0 && !EnchantmentHelper.isEnchantmentCompatible(stack.getTagEnchantments().keySet(), holder)) {
            return null;
        }
        return new Option(holder, vise);
    }

    /** Pose l'enchantement, note la trace, et regarde si l'objet a déjà de quoi s'éveiller encore. */
    static void appliquer(ServerPlayer player, ItemStack stack, Option option, Optional<String> eclat, BlockPos table) {
        EtatEveil etat = reconcilier(stack, etat(stack));
        int avant = EnchantmentHelper.getTagEnchantmentLevel(option.enchantement(), stack);
        EnchantmentHelper.updateEnchantments(stack, m -> m.set(option.enchantement(), option.niveau()));
        long jour = player.level().getDayTime() / 24000L + 1;
        EtatEveil.Trace trace = new EtatEveil.Trace(option.enchantement().unwrapKey().orElseThrow().location(),
                option.niveau(), avant, jour, player.getGameProfile().getName(), eclat);
        EtatEveil updated = etat.plus(trace);
        stack.set(EveilModule.ETAT.get(), updated);
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        player.level().playSound(null, table, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.BLOCKS, 1.0F, 0.9F);
        player.displayClientMessage(Component.translatable("eveil.fallow.message.eveille", stack.getHoverName(),
                Enchantment.getFullname(option.enchantement(), option.niveau())), true);
        verifier(player, stack, updated, Actes.get(player.server));
    }

    // ---- l'oubli ----

    /** À bout de durabilité, l'objet oublie son dernier éveil ; il ne l'oubliera qu'une fois tant qu'il n'est pas réparé. */
    static void usure(ServerPlayer player, ItemStack stack) {
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        if (etat == null) {
            return;
        }
        boolean aBout = stack.isDamageableItem() && stack.getDamageValue() >= stack.getMaxDamage() - 1;
        if (!aBout) {
            if (etat.eteint()) {
                stack.set(EveilModule.ETAT.get(), etat.avecEteint(false));
            }
            return;
        }
        if (etat.eteint() || !FallowConfig.eveilEnabled()) {
            return;
        }
        etat = reconcilier(stack, etat);
        if (etat.traces().isEmpty()) {
            stack.set(EveilModule.ETAT.get(), etat.avecEteint(true));
            return;
        }
        List<EtatEveil.Trace> traces = new ArrayList<>(etat.traces());
        EtatEveil.Trace derniere = traces.remove(traces.size() - 1);
        Optional<Holder.Reference<Enchantment>> holder = enchantement(stack, derniere.enchantement());
        holder.ifPresent(h -> EnchantmentHelper.updateEnchantments(stack, m -> m.set(h, derniere.avant())));
        EtatEveil updated = new EtatEveil(etat.id(), etat.places(), traces, false, true, 0);
        stack.set(EveilModule.ETAT.get(), updated);
        stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
        Actes.get(player.server).rabattre(etat.id(), traces.isEmpty() ? 0.0 : seuil(traces.size() - 1));
        Component nom = holder.<Component>map(h -> Enchantment.getFullname(h, derniere.niveau()))
                .orElse(Component.literal(derniere.enchantement().toString()));
        player.displayClientMessage(Component.translatable("eveil.fallow.message.oubli", stack.getHoverName(), nom)
                .withStyle(ChatFormatting.GRAY), false);
        player.playNotifySound(SoundEvents.AMETHYST_CLUSTER_BREAK, SoundSource.PLAYERS, 1.0F, 0.6F);
    }

    /**
     * À l'approche de la fin, un objet éveillé prévient une fois qu'il risque d'oublier ; {@code true} s'il vient de
     * le dire. Réparé, il préviendra de nouveau.
     */
    static boolean alerte(ServerPlayer player, ItemStack stack, boolean dejaDit) {
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        if (etat == null || etat.traces().isEmpty() || etat.eteint() || !stack.isDamageableItem()
                || stack.getDamageValue() < stack.getMaxDamage() * FallowConfig.eveilAlerte()) {
            return false;
        }
        if (!dejaDit) {
            EtatEveil.Trace derniere = etat.traces().get(etat.traces().size() - 1);
            Component nom = enchantement(stack, derniere.enchantement())
                    .<Component>map(h -> Enchantment.getFullname(h, derniere.niveau()))
                    .orElse(Component.literal(derniere.enchantement().toString()));
            player.displayClientMessage(Component.translatable("eveil.fallow.message.alerte", stack.getHoverName(), nom)
                    .withStyle(ChatFormatting.GOLD), true);
            player.playNotifySound(SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.8F, 0.5F);
        }
        return true;
    }

    /**
     * Seul l'éveil enchante : ce que l'objet porte au-delà de ce que ses éveils lui ont donné s'efface (une épée
     * ramassée sur un zombie, un coffre posé avec une structure, une récompense, une commande). Le livre enchanté est
     * laissé à l'appelant, qui le remplace par un livre. {@code true} si l'objet a changé.
     */
    static boolean purifier(ItemStack stack) {
        if (stack.isEmpty() || stack.is(EveilModule.ECLAT_DE_BETE.get()) || stack.is(Items.ENCHANTED_BOOK)) {
            return false;
        }
        if (stack.has(DataComponents.STORED_ENCHANTMENTS) && !stack.getOrDefault(DataComponents.STORED_ENCHANTMENTS,
                ItemEnchantments.EMPTY).isEmpty()) {
            stack.remove(DataComponents.STORED_ENCHANTMENTS);
            return true;
        }
        ItemEnchantments portes = stack.getTagEnchantments();
        if (portes.isEmpty()) {
            return false;
        }
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        boolean change = false;
        ItemEnchantments.Mutable nouveaux = new ItemEnchantments.Mutable(portes);
        for (var entry : portes.entrySet()) {
            ResourceLocation id = entry.getKey().unwrapKey().map(ResourceKey::location).orElse(null);
            int permis = etat == null || id == null ? 0 : etat.niveau(id);
            if (entry.getIntValue() > permis) {
                nouveaux.set(entry.getKey(), permis);
                change = true;
            }
        }
        if (change) {
            stack.set(DataComponents.ENCHANTMENTS, nouveaux.toImmutable());
        }
        return change;
    }

    // ---- outils ----

    static Optional<Holder.Reference<Enchantment>> enchantement(ItemStack stack, ResourceLocation id) {
        return Optional.ofNullable(EveilModule.registres())
                .flatMap(r -> r.registryOrThrow(Registries.ENCHANTMENT).getHolder(ResourceKey.create(Registries.ENCHANTMENT, id)));
    }

    /** Le nom de la qualité de forge d'Overgeared ({@code poor} … {@code master}), {@code none} sans qualité. */
    static String qualite(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(QUALITE);
        if (type == null) {
            return "none";
        }
        Object value = stack.get(type);
        return value instanceof StringRepresentable named ? named.getSerializedName() : "none";
    }

    /** Pour la commande de contrôle : les points de chaque entrée de l'objet et le total. */
    static Map<String, Double> detail(net.minecraft.server.MinecraftServer server, ItemStack stack) {
        Map<String, Double> out = new HashMap<>();
        EtatEveil etat = stack.get(EveilModule.ETAT.get());
        if (etat == null) {
            return out;
        }
        Actes actes = Actes.get(server);
        for (HautsFaits.Entree entree : HautsFaits.pour(stack)) {
            out.put(entree.cle(), actes.points(etat.id(), entree.cle()));
        }
        out.put("total", actes.total(etat.id()));
        out.put("seuil", seuil(etat.occupees()));
        return out;
    }
}
