package io.github.salvagedisc.fallow.hotes.visite;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import io.github.salvagedisc.fallow.clairiere.Clearing;
import io.github.salvagedisc.fallow.clairiere.Clearings;
import io.github.salvagedisc.fallow.hotes.Hote;
import io.github.salvagedisc.fallow.hotes.HotesModule;
import io.github.salvagedisc.fallow.hotes.Peuple;
import io.github.salvagedisc.fallow.hotes.Saison;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.QuartPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.AbstractCandleBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import org.jetbrains.annotations.Nullable;

/**
 * Le calendrier des visites. Chaque soir, dans une clairière où se tient un joueur, un peuple dont le milieu entoure la
 * clairière peut laisser son signe au bord (un masque pendu à un piquet) ; le soir suivant, son émissaire attend à la
 * lisière, une natte posée devant lui, avec trois souhaits. À l'aube il repart : la natte porte son contre-don si on lui a
 * laissé quelque chose, elle est roulée sinon. Le lien monte avec les dons et s'use avec l'oubli ; la troisième visite
 * oubliée de suite, la brume reprend les feux du bord et le peuple ne revient pas de la saison.
 */
public final class Visites {
    /** Le soir commence un peu avant le coucher du soleil (12000). */
    static final int SOIR = 11800;
    /** Plus de nouvelle venue après minuit passé : l'émissaire n'arrive pas pour repartir aussitôt. */
    static final int NUIT_AVANCEE = 20000;
    /** L'aube, quand l'émissaire repart. */
    static final int AUBE = 23000;
    /** Un passage toutes les cinq secondes suffit : les visites se comptent en soirs. */
    private static final int PAS = 100;
    /** Les feux que la brume peut reprendre : à quelques blocs du bord, de part et d'autre. */
    private static final int BORD_DEDANS = 5;
    private static final int BORD_DEHORS = 2;

    private Visites() {
    }

    public static long jour(ServerLevel level) {
        return Math.floorDiv(level.getDayTime(), 24000L);
    }

    private static long heure(ServerLevel level) {
        return Math.floorMod(level.getDayTime(), 24000L);
    }

    // --- Le pas de temps ---

    public static void tick(ServerLevel level) {
        if (level.getGameTime() % PAS != 0 || !FallowConfig.hotesVisites()) {
            return;
        }
        Liens liens = Liens.de(level);
        List<Clearing> clairieres = Clearings.all(level);
        long jour = jour(level);
        long heure = heure(level);

        // D'abord ce qui finit : l'aube, et les visites dont la clairière n'existe plus.
        for (Visite visite : new ArrayList<>(liens.visites())) {
            Clearing clairiere = clairiere(clairieres, visite.clairiere);
            if (clairiere == null || clairiere.closing()) {
                annule(level, liens, visite);
            } else if (visite.phase == Visite.Phase.VENUE && (jour > visite.jour || heure >= AUBE)) {
                aube(level, liens, visite, clairiere);
            }
        }
        Set<Long> centres = new HashSet<>();
        clairieres.forEach(c -> centres.add(c.center().asLong()));
        liens.garde(centres, jour, FallowConfig.hotesRefusPoli());

        if (heure < SOIR || heure >= NUIT_AVANCEE) {
            return;
        }
        for (Clearing clairiere : clairieres) {
            if (clairiere.closing() || joueurs(level, clairiere).isEmpty()) {
                continue;
            }
            Visite visite = liens.visite(clairiere.center());
            if (visite == null) {
                signe(level, liens, clairiere, jour);
            } else if (visite.phase == Visite.Phase.SIGNE && visite.jour <= jour) {
                // Le soir dit, ou plus tard si personne n'était là pour l'accueillir.
                venue(level, liens, visite, clairiere, jour);
            }
        }
    }

    @Nullable
    private static Clearing clairiere(List<Clearing> clairieres, BlockPos centre) {
        for (Clearing c : clairieres) {
            if (c.center().equals(centre)) {
                return c;
            }
        }
        return null;
    }

    /** Les joueurs dans la clairière (pas les spectateurs) : à son aplomb, à une quarantaine de blocs de hauteur près. */
    static List<ServerPlayer> joueurs(ServerLevel level, Clearing clairiere) {
        List<ServerPlayer> out = new ArrayList<>();
        for (ServerPlayer p : level.players()) {
            if (!p.isSpectator() && clairiere.covers(p.getX(), p.getZ(), clairiere.radius())
                    && Math.abs(p.getY() - clairiere.center().getY()) < 40) {
                out.add(p);
            }
        }
        return out;
    }

    /** Une note en marge pour chaque joueur de la clairière. */
    private static void note(ServerLevel level, Clearing clairiere, String cle, Peuple peuple) {
        Component texte = Component.translatable("message.fallow.hotes." + cle,
                Component.translatable("screen.fallow.hote." + peuple.id() + ".titre"),
                Component.translatable("screen.fallow.hote." + peuple.id() + ".nom"));
        for (ServerPlayer p : joueurs(level, clairiere)) {
            p.displayClientMessage(texte, true);
        }
    }

    // --- Le signe de la veille ---

    /** Les voisins déjà lus, par monde et par clairière (centre et rayon) : les biomes de génération ne changent pas. */
    private static final Map<ServerLevel, Map<Long, List<Peuple>>> VOISINS = new java.util.WeakHashMap<>();

    /** Les peuples dont le milieu entoure la clairière, du plus présent au moins présent (deux au plus). */
    static List<Peuple> voisins(ServerLevel level, Clearing clairiere) {
        long cle = clairiere.center().asLong() * 31L + clairiere.radius();
        return VOISINS.computeIfAbsent(level, l -> new java.util.HashMap<>())
                .computeIfAbsent(cle, k -> lireVoisins(level, clairiere));
    }

    private static List<Peuple> lireVoisins(ServerLevel level, Clearing clairiere) {
        Map<Peuple, Integer> score = new EnumMap<>(Peuple.class);
        BlockPos c = clairiere.center();
        var source = level.getChunkSource().getGenerator().getBiomeSource();
        var climat = level.getChunkSource().randomState().sampler();
        // Le biome de génération, autour de la clairière (dedans, la clairière a pris le sien) : à la hauteur du centre
        // et une vingtaine de blocs plus bas, pour les grottes luxuriantes et les abîmes.
        for (int anneau : new int[] {clairiere.radius() + 12, clairiere.radius() + 40}) {
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6.0;
                int x = c.getX() + (int) Math.round(Math.cos(a) * anneau);
                int z = c.getZ() + (int) Math.round(Math.sin(a) * anneau);
                for (int y : new int[] {c.getY(), c.getY() - 24}) {
                    Holder<Biome> biome = source.getNoiseBiome(QuartPos.fromBlock(x), QuartPos.fromBlock(y), QuartPos.fromBlock(z), climat);
                    for (Peuple p : Peuple.values()) {
                        if (p.aime(biome)) {
                            score.merge(p, 1, Integer::sum);
                        }
                    }
                }
            }
        }
        List<Peuple> out = new ArrayList<>(score.keySet());
        out.sort((a, b) -> Integer.compare(score.get(b), score.get(a)));
        if (out.isEmpty()) {
            // Une clairière que rien n'entoure de connu (un mod de biomes sans tags) : les Discrets passent partout.
            return List.of(Peuple.ECORCE);
        }
        return out.subList(0, Math.min(2, out.size()));
    }

    private static void signe(ServerLevel level, Liens liens, Clearing clairiere, long jour) {
        Peuple peuple = null;
        long plusTot = Long.MAX_VALUE;
        for (Peuple p : voisins(level, clairiere)) {
            Lien lien = liens.lienOuNouveau(clairiere.center(), p, jour + FallowConfig.hotesPremiereVisite());
            if (lien.prochaine <= jour + 1 && lien.prochaine < plusTot) {
                peuple = p;
                plusTot = lien.prochaine;
            }
        }
        if (peuple == null) {
            return;
        }
        Bord bord = bord(level, clairiere, level.getRandom());
        if (bord == null) {
            return;
        }
        Visite visite = new Visite(peuple, clairiere.center(), jour + 1);
        visite.signe = bord.dehors;
        poseSigne(level, bord, peuple);
        liens.ajoute(visite);
        note(level, clairiere, "signe", peuple);
        Fallow.LOGGER.info("hotes : signe des {} au bord de la clairière {}", peuple.id(), clairiere.center());
    }

    private static void poseSigne(ServerLevel level, Bord bord, Peuple peuple) {
        BlockState state = HotesModule.SIGNE.get().defaultBlockState().setValue(SigneBlock.FACING, bord.versCentre);
        level.setBlock(bord.dehors, state, Block.UPDATE_ALL);
        if (level.getBlockEntity(bord.dehors) instanceof SigneBlockEntity signe) {
            signe.peuple(peuple);
        }
        level.sendParticles(ParticleTypes.CLOUD, bord.dehors.getX() + 0.5, bord.dehors.getY() + 1.0, bord.dehors.getZ() + 0.5,
                10, 0.2, 0.5, 0.2, 0.01);
    }

    // --- La venue ---

    private static void venue(ServerLevel level, Liens liens, Visite visite, Clearing clairiere, long jour) {
        Bord bord = null;
        BlockState piquet = null;
        if (visite.signe != null && level.getBlockState(visite.signe).is(HotesModule.SIGNE.get())) {
            // Le piquet s'efface d'abord : l'émissaire prend sa place.
            piquet = level.getBlockState(visite.signe);
            level.removeBlock(visite.signe, false);
            bord = bord(level, clairiere, visite.signe);
        }
        if (bord == null) {
            bord = bord(level, clairiere, level.getRandom());
        }
        if (bord == null) {
            // Aucune place au bord ce soir (la lisière s'est remplie) : le piquet revient, la visite attendra.
            if (piquet != null) {
                level.setBlock(visite.signe, piquet, Block.UPDATE_ALL);
                if (level.getBlockEntity(visite.signe) instanceof SigneBlockEntity signe) {
                    signe.peuple(visite.peuple);
                }
            }
            return;
        }
        Hote hote = HotesModule.HOTE.get().create(level);
        if (hote == null) {
            return;
        }
        Lien lien = liens.lienOuNouveau(clairiere.center(), visite.peuple, jour);
        visite.jour = jour;
        visite.phase = Visite.Phase.VENUE;
        visite.signe = null;
        visite.demandes.clear();
        visite.demandes.addAll(Demandes.tire(visite.peuple, Saison.de(level), lien.palier(), level.getRandom()));
        visite.donnes = 0;
        visite.refus = false;

        BlockState natte = HotesModule.NATTE.get().defaultBlockState().setValue(NatteBlock.FACING, bord.versCentre.getOpposite());
        level.setBlock(bord.natte, natte, Block.UPDATE_ALL);
        if (level.getBlockEntity(bord.natte) instanceof NatteBlockEntity be) {
            be.attendre(true);
        }
        visite.natte = bord.natte;

        float lacet = bord.versCentre.toYRot();
        hote.moveTo(bord.dehors.getX() + 0.5, bord.dehors.getY(), bord.dehors.getZ() + 0.5, lacet, 0.0F);
        hote.setYHeadRot(lacet);
        hote.setYBodyRot(lacet);
        hote.setPeuple(visite.peuple);
        hote.ancrer(bord.dehors);
        level.addFreshEntity(hote);
        visite.hote = hote.getUUID();
        level.sendParticles(ParticleTypes.CLOUD, hote.getX(), hote.getY() + 1.0, hote.getZ(), 30, 0.3, 0.9, 0.3, 0.01);
        liens.change();
        note(level, clairiere, "venue", visite.peuple);
        Fallow.LOGGER.info("hotes : les {} sont venus à la clairière {} ({} souhaits)", visite.peuple.id(), clairiere.center(),
                visite.demandes.size());
    }

    // --- Les dons ---

    static void donne(ServerLevel level, Visite visite, ServerPlayer player) {
        if (visite.natte != null) {
            level.playSound(null, visite.natte, SoundEvents.WOOL_PLACE, SoundSource.BLOCKS, 0.7F, 0.9F);
        }
    }

    /** Un refus poli par saison (le nombre de jours de la configuration) : il ne pèse pas sur le lien. */
    static boolean refusPossible(ServerLevel level, Visite visite) {
        Lien lien = Liens.de(level).lien(visite.clairiere, visite.peuple);
        return lien == null || jour(level) - lien.dernierRefus >= FallowConfig.hotesRefusPoli();
    }

    static void refuse(ServerLevel level, Visite visite) {
        // Un refus poli n'a de sens que les mains vides : après un don, il ne coûte pas le refus de la saison.
        if (visite.refus || visite.donnes != 0 || !refusPossible(level, visite)) {
            return;
        }
        Liens liens = Liens.de(level);
        Lien lien = liens.lienOuNouveau(visite.clairiere, visite.peuple, jour(level));
        lien.dernierRefus = jour(level);
        visite.refus = true;
        liens.change();
    }

    // --- L'aube ---

    private static void aube(ServerLevel level, Liens liens, Visite visite, Clearing clairiere) {
        if (visite.natte != null && !level.isLoaded(visite.natte)) {
            // La natte est hors de portée : l'aube attendra que quelqu'un revienne la voir.
            return;
        }
        Lien lien = liens.lienOuNouveau(visite.clairiere, visite.peuple, visite.jour);
        Palier avant = lien.palier();
        int dons = visite.nombreDeDons();
        NatteBlockEntity natte = visite.natte != null && level.getBlockEntity(visite.natte) instanceof NatteBlockEntity be ? be : null;
        String cle;
        if (dons > 0) {
            lien.points += dons * 2 + (dons == visite.demandes.size() ? 1 : 0);
            lien.oublis = 0;
            if (natte != null) {
                natte.vide();
                // Jamais ce qu'on vient de donner : le contre-don est autre chose.
                java.util.Set<net.minecraft.world.item.Item> donnes = new java.util.HashSet<>();
                for (int i = 0; i < visite.demandes.size(); i++) {
                    if (visite.donne(i)) {
                        donnes.add(visite.demandes.get(i).icone());
                    }
                }
                natte.pose(Demandes.contreDon(visite.peuple, lien.palier(), dons, donnes, level.getRandom()));
                natte.attendre(false);
            }
            cle = lien.palier() != avant ? "aube.palier." + lien.palier().id() : "aube.don";
        } else if (visite.refus) {
            if (natte != null) {
                NatteBlock.efface(level, visite.natte);
            }
            cle = "aube.refus";
        } else {
            lien.oublis++;
            lien.points = Math.max(0, lien.points - 2);
            if (natte != null) {
                natte.attendre(false);
                level.setBlock(visite.natte, level.getBlockState(visite.natte).setValue(NatteBlock.ROULEE, true), Block.UPDATE_ALL);
            }
            cle = "aube.oubli";
            if (lien.oublis >= 2) {
                eteint(level, clairiere);
                cle = "aube.oubli.brume";
            }
            if (lien.oublis >= 3) {
                // Le troisième oubli : le peuple ne revient pas de la saison, et le lien perd un palier.
                lien.points = Math.min(lien.points, Math.max(0, lien.palier().precedent().seuil()));
                lien.oublis = 0;
                lien.prochaine = jour(level) + FallowConfig.hotesRefusPoli();
                cle = "aube.oubli.saison";
            }
        }
        if (!cle.equals("aube.oubli.saison")) {
            // Un jour plus tôt dans la saison du Peuple ; le Silence n'en a pas (il vient en toute saison, sans avance).
            boolean saison = visite.peuple.saison() != Saison.AUCUNE && visite.peuple.saison() == Saison.de(level);
            int ecart = FallowConfig.hotesIntervalle() - lien.palier().ordinal() - (saison ? 1 : 0);
            // Compté depuis ce matin : une aube retardée (natte hors des tronçons chargés) ne rapproche pas le retour.
            lien.prochaine = jour(level) + Math.max(2, ecart);
        }
        dissout(level, visite);
        liens.retire(visite);
        liens.change();
        note(level, clairiere, cle, visite.peuple);
        Fallow.LOGGER.info("hotes : aube, les {} repartent de {} ({} dons, lien {} points, retour le jour {})",
                visite.peuple.id(), visite.clairiere, dons, lien.points, lien.prochaine);
    }

    /** L'émissaire repart dans la brume, s'il est là. */
    private static void dissout(ServerLevel level, Visite visite) {
        if (visite.hote == null) {
            return;
        }
        Entity entite = level.getEntity(visite.hote);
        if (entite instanceof Hote hote) {
            level.sendParticles(ParticleTypes.CLOUD, hote.getX(), hote.getY() + 1.0, hote.getZ(), 30, 0.3, 0.9, 0.3, 0.01);
            hote.discard();
        }
    }

    /** La brume reprend les feux du bord : feux de camp et bougies à quelques blocs de la lisière s'éteignent. */
    private static void eteint(ServerLevel level, Clearing clairiere) {
        BlockPos c = clairiere.center();
        int r = clairiere.radius();
        int dehors = r + BORD_DEHORS;
        int dedans = Math.max(0, r - BORD_DEDANS);
        BlockPos.MutableBlockPos curseur = new BlockPos.MutableBlockPos();
        for (int dx = -dehors; dx <= dehors; dx++) {
            for (int dz = -dehors; dz <= dehors; dz++) {
                int d2 = dx * dx + dz * dz;
                if (d2 > dehors * dehors || d2 < dedans * dedans) {
                    continue;
                }
                for (int dy = -6; dy <= 10; dy++) {
                    curseur.set(c.getX() + dx, c.getY() + dy, c.getZ() + dz);
                    if (!level.isLoaded(curseur)) {
                        continue;
                    }
                    BlockState state = level.getBlockState(curseur);
                    if ((state.getBlock() instanceof CampfireBlock || state.getBlock() instanceof AbstractCandleBlock)
                            && state.hasProperty(BlockStateProperties.LIT) && state.getValue(BlockStateProperties.LIT)) {
                        level.setBlock(curseur, state.setValue(BlockStateProperties.LIT, false), Block.UPDATE_ALL);
                        level.sendParticles(ParticleTypes.CLOUD, curseur.getX() + 0.5, curseur.getY() + 0.6, curseur.getZ() + 0.5,
                                6, 0.2, 0.2, 0.2, 0.01);
                    }
                }
            }
        }
        level.playSound(null, c, SoundEvents.FIRE_EXTINGUISH, SoundSource.BLOCKS, 0.5F, 0.6F);
    }

    // --- Ce qui interrompt une visite ---

    /** La clairière s'est refermée : le signe tombe, l'émissaire s'en va, la natte reste roulée avec ce qu'elle porte. */
    private static void annule(ServerLevel level, Liens liens, Visite visite) {
        if (visite.signe != null && level.isLoaded(visite.signe) && level.getBlockState(visite.signe).is(HotesModule.SIGNE.get())) {
            level.removeBlock(visite.signe, false);
        }
        roule(level, visite);
        dissout(level, visite);
        liens.retire(visite);
    }

    private static void roule(ServerLevel level, Visite visite) {
        if (visite.natte != null && level.isLoaded(visite.natte) && level.getBlockEntity(visite.natte) instanceof NatteBlockEntity natte) {
            natte.attendre(false);
            level.setBlock(visite.natte, level.getBlockState(visite.natte).setValue(NatteBlock.ROULEE, true), Block.UPDATE_ALL);
        }
    }

    /**
     * Un joueur a frappé l'émissaire : il se dissout, le lien chute (deux paliers d'un coup au plus), et le peuple ne
     * revient pas avant un long moment. La natte reste, roulée, avec ce qu'on y avait posé.
     */
    public static void frappe(ServerLevel level, Hote hote) {
        Liens liens = Liens.de(level);
        Visite visite = liens.visiteDe(hote.getUUID());
        if (visite == null) {
            return;
        }
        Lien lien = liens.lienOuNouveau(visite.clairiere, visite.peuple, jour(level));
        lien.points = Math.max(0, Math.min(lien.points - 6, lien.palier().precedent().seuil()));
        lien.oublis = 0;
        lien.prochaine = jour(level) + FallowConfig.hotesRefusPoli();
        roule(level, visite);
        liens.retire(visite);
        liens.change();
        for (ServerPlayer p : level.players()) {
            if (p.blockPosition().closerThan(visite.clairiere, 96)) {
                p.displayClientMessage(Component.translatable("message.fallow.hotes.frappe",
                        Component.translatable("screen.fallow.hote." + visite.peuple.id() + ".titre")), true);
            }
        }
    }

    /** L'émissaire est parti sans qu'on l'ait frappé (/kill, le vide) : la visite s'arrête, la natte se roule, rien n'est perdu. */
    public static void interrompt(ServerLevel level, Hote hote) {
        Liens liens = Liens.de(level);
        Visite visite = liens.visiteDe(hote.getUUID());
        if (visite == null) {
            return;
        }
        roule(level, visite);
        liens.retire(visite);
        liens.change();
    }

    /** Vrai tant que l'émissaire a une visite en cours : sinon il n'a plus rien à faire là et repart. */
    public static boolean enVisite(ServerLevel level, Hote hote) {
        return Liens.de(level).visiteDe(hote.getUUID()) != null;
    }

    // --- Commandes de banc ---

    /** Une visite tout de suite, dans la clairière du joueur : l'émissaire et la natte, comme un soir de venue. */
    public static boolean visiteImmediate(ServerPlayer player, Peuple peuple) {
        ServerLevel level = player.serverLevel();
        Clearing clairiere = clairiereDe(player);
        if (clairiere == null) {
            return false;
        }
        Liens liens = Liens.de(level);
        Visite deja = liens.visite(clairiere.center());
        if (deja != null) {
            annule(level, liens, deja);
        }
        Visite visite = new Visite(peuple, clairiere.center(), jour(level));
        liens.ajoute(visite);
        venue(level, liens, visite, clairiere, jour(level));
        if (visite.phase != Visite.Phase.VENUE) {
            liens.retire(visite);
            liens.change();
            return false;
        }
        return true;
    }

    /** L'aube tout de suite pour la visite de la clairière du joueur. */
    public static boolean aubeImmediate(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Clearing clairiere = clairiereDe(player);
        if (clairiere == null) {
            return false;
        }
        Liens liens = Liens.de(level);
        Visite visite = liens.visite(clairiere.center());
        if (visite == null || visite.phase != Visite.Phase.VENUE) {
            return false;
        }
        aube(level, liens, visite, clairiere);
        return true;
    }

    @Nullable
    public static Clearing clairiereDe(ServerPlayer player) {
        for (Clearing c : Clearings.all(player.serverLevel())) {
            if (!c.closing() && c.covers(player.getX(), player.getZ(), c.radius())) {
                return c;
            }
        }
        return null;
    }

    // --- Le bord de la clairière ---

    /**
     * Une place au bord : {@code dehors}, à un bloc hors de la lisière, où pend le signe puis où se tient l'émissaire ;
     * {@code natte}, un bloc en dedans, face à lui : on lui parle debout près de la natte, sans sortir de la clairière. {@code versCentre} : la direction de la clairière.
     */
    record Bord(BlockPos dehors, BlockPos natte, Direction versCentre) {
    }

    @Nullable
    private static Bord bord(ServerLevel level, Clearing clairiere, RandomSource hasard) {
        double depart = hasard.nextDouble() * Math.PI * 2.0;
        for (int i = 0; i < 24; i++) {
            Bord bord = bord(level, clairiere, depart + i * Math.PI / 12.0);
            if (bord != null) {
                return bord;
            }
        }
        return null;
    }

    /** La place au bord, du côté où se tenait le signe. */
    @Nullable
    private static Bord bord(ServerLevel level, Clearing clairiere, BlockPos signe) {
        double a = Math.atan2(signe.getZ() - clairiere.center().getZ(), signe.getX() - clairiere.center().getX());
        Bord bord = bord(level, clairiere, a);
        return bord != null ? bord : bord(level, clairiere, level.getRandom());
    }

    @Nullable
    private static Bord bord(ServerLevel level, Clearing clairiere, double angle) {
        BlockPos c = clairiere.center();
        int r = clairiere.radius();
        double cos = Math.cos(angle), sin = Math.sin(angle);
        int dx = Mth.floor(c.getX() + 0.5 + cos * (r + 1)), dz = Mth.floor(c.getZ() + 0.5 + sin * (r + 1));
        int nx = Mth.floor(c.getX() + 0.5 + cos * (r - 1)), nz = Mth.floor(c.getZ() + 0.5 + sin * (r - 1));
        if (!level.isLoaded(new BlockPos(dx, c.getY(), dz)) || !level.isLoaded(new BlockPos(nx, c.getY(), nz))) {
            return null;
        }
        int dy = HotesModule.sol(level, dx, c.getY(), dz, 12);
        int ny = HotesModule.sol(level, nx, c.getY(), nz, 12);
        if (dy == Integer.MIN_VALUE || ny == Integer.MIN_VALUE || Math.abs(dy - ny) > 3) {
            return null;
        }
        BlockPos natte = new BlockPos(nx, ny, nz);
        if (!level.getBlockState(natte).canBeReplaced() || !level.getFluidState(natte).isEmpty()) {
            return null;
        }
        Direction versCentre = Direction.fromYRot(Math.toDegrees(angle) + 90.0);
        return new Bord(new BlockPos(dx, dy, dz), natte, versCentre);
    }

    /** Ce que les Hôtes savent d'une clairière, pour la commande de banc. */
    public static List<Component> etat(ServerPlayer player) {
        List<Component> out = new ArrayList<>();
        Clearing clairiere = clairiereDe(player);
        if (clairiere == null) {
            return out;
        }
        ServerLevel level = player.serverLevel();
        Liens liens = Liens.de(level);
        out.add(Component.literal("Clairière " + clairiere.center().toShortString() + ", rayon " + clairiere.radius()
                + ", jour " + jour(level) + ", saison " + Saison.de(level).id()
                + ", voisins " + voisins(level, clairiere).stream().map(Peuple::id).toList()));
        for (Map.Entry<Peuple, Lien> e : liens.liens(clairiere.center()).entrySet()) {
            Lien l = e.getValue();
            out.add(Component.literal("  " + e.getKey().id() + " : " + l.points + " points (" + l.palier().id() + "), "
                    + l.oublis + " oublis, prochaine visite jour " + l.prochaine));
        }
        Visite v = liens.visite(clairiere.center());
        if (v != null) {
            StringBuilder s = new StringBuilder("  visite : " + v.peuple.id() + ", " + v.phase.name().toLowerCase(java.util.Locale.ROOT)
                    + ", jour " + v.jour
                    + (v.signe != null ? ", signe " + v.signe.toShortString() : "")
                    + (v.natte != null ? ", natte " + v.natte.toShortString() : "") + ", souhaits");
            for (int i = 0; i < v.demandes.size(); i++) {
                Demande d = v.demandes.get(i);
                s.append(' ').append(d.nombre()).append('×').append(new ItemStack(d.icone()).getHoverName().getString())
                        .append(v.donne(i) ? " (donné)" : "");
            }
            out.add(Component.literal(s.toString()));
        }
        return out;
    }
}
