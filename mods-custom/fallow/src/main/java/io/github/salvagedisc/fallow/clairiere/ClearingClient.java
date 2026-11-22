package io.github.salvagedisc.fallow.clairiere;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.ModIds;
import io.github.salvagedisc.fallow.ciel.FogCache;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.ColorResolver;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.TagsUpdatedEvent;

/**
 * Les clairières vues du client. Le monde garde son vrai biome ; là où une clairière est ouverte, seul ce qui
 * se voit et s'entend prend l'apparence d'un biome de clairière :
 *
 * <ul>
 *   <li>les couleurs de l'herbe, du feuillage et de l'eau : chaque calcul de teinte, dans le jeu comme dans
 *       Sodium, passe par {@link #tint}, qui donne au vrai biome les couleurs de base du biome de clairière. Les
 *       teintes de saison d'Ecliptic Seasons, choisies d'après le vrai biome, s'y ajoutent comme ailleurs ;</li>
 *   <li>la brume d'ATMOSPHERICS et la musique de True Adaptive Music, qui lisent le biome par
 *       {@code ClientLevel.getBiome} : ces lectures passent par {@link #look(BlockPos, Holder)}
 *       (mixins {@code clairiere.AtmosphericsBiomeMixin} et {@code clairiere.MusicBiomeMixin}).</li>
 * </ul>
 *
 * Tout le reste (saisons, neige, sons d'ambiance, F3) lit le vrai biome.
 *
 * <p>Les couleurs sont demandées par les fils qui dessinent les tronçons : ce qu'ils lisent ({@link #index},
 * {@link #looks}, l'heure) ne change jamais sur place, on publie un nouvel objet à chaque changement.
 */
public final class ClearingClient {
    /** Distance à laquelle on voit les volutes d'une vague, et leur nombre au plus par anneau. */
    private static final double PUFF_RANGE = 160.0;
    private static final int MAX_PUFFS = 160;
    private static final boolean ATMOSPHERICS = ModIds.isLoaded(ModIds.ATMOSPHERICS);

    private static volatile ClearingIndex index = ClearingIndex.EMPTY;
    /** Vrai biome → biome de clairière qui lui donne son apparence, pour les biomes qui se dégagent. */
    private static volatile Map<Biome, Holder<Biome>> looks = Map.of();
    private static volatile int ringTicks = 6;
    private static volatile long time;
    private static volatile boolean overworld;
    /** Biome de clairière qui prête ses couleurs de base pendant un calcul de teinte, par fil. */
    private static final ThreadLocal<Biome> PAINTING = new ThreadLocal<>();

    // Fil du client seulement.
    private static final List<Clearing> clearings = new ArrayList<>();
    /** Clairières dont la vague avance, avec le rayon déjà dessiné. */
    private static final Map<Clearing, Integer> waves = new HashMap<>();
    private static boolean looksStale = true;

    private ClearingClient() {
    }

    static void start() {
        NeoForge.EVENT_BUS.addListener(ClientTickEvent.Post.class, event -> tick());
        NeoForge.EVENT_BUS.addListener(ClientPlayerNetworkEvent.LoggingOut.class, event -> forget());
        NeoForge.EVENT_BUS.addListener(TagsUpdatedEvent.class, event -> looksStale = true);
    }

    /**
     * Calcule une teinte : dans une clairière, la règle de couleur lit les couleurs de base du biome de clairière
     * à la place de celles de {@code biome} ({@code clairiere.BiomeLookMixin}), le temps de cet appel et sur ce
     * fil seulement.
     */
    public static int tint(ColorResolver resolver, Biome biome, double x, double z, Operation<Integer> original) {
        Biome look = look(biome, x, z);
        if (look == biome) {
            return original.call(resolver, biome, x, z);
        }
        PAINTING.set(look);
        try {
            return original.call(resolver, biome, x, z);
        } finally {
            PAINTING.set(null);
        }
    }

    /** Le biome de clairière dont {@code biome} prend les couleurs de base en ce moment sur ce fil, ou {@code null}. */
    public static Biome painted(Biome biome) {
        Biome look = PAINTING.get();
        return look == biome ? null : look;
    }

    /** Le biome dont {@code (x, z)} prend les couleurs : celui de la clairière s'il est dégagé, le sien sinon. */
    private static Biome look(Biome biome, double x, double z) {
        ClearingIndex current = index;
        if (current.isEmpty() || !overworld) {
            return biome;
        }
        Holder<Biome> look = looks.get(biome);
        return look != null && current.coversAt(x, z, time, ringTicks) ? look.value() : biome;
    }

    /** Même chose pour une lecture de biome par position (brume, musique). */
    public static Holder<Biome> look(BlockPos pos, Holder<Biome> biome) {
        ClearingIndex current = index;
        if (current.isEmpty() || !overworld) {
            return biome;
        }
        Holder<Biome> look = looks.get(biome.value());
        return look != null && current.coversAt(pos.getX() + 0.5, pos.getZ() + 0.5, time, ringTicks) ? look : biome;
    }

    /** Les clairières connues du client, vagues en cours comprises ; vide hors de la Surface. Fil du client. */
    public static List<Clearing> open(ClientLevel level) {
        return level.dimension() == Level.OVERWORLD ? clearings : List.of();
    }

    /** Rayon dégagé d'une clairière en ce moment, vague comprise, lissé d'une image à l'autre. */
    public static float radius(Clearing clearing, float partialTick) {
        return clearing.smoothRadiusAt(time + (double) partialTick, ringTicks);
    }

    /**
     * Part de brume levée en ce point (0 hors des clairières, 1 à {@code blocks} blocs ou plus à l'intérieur du bord,
     * adoucie entre les deux), là où le biome se dégage. Fil du client.
     */
    public static float liftAt(ClientLevel level, Vec3 at, float partialTick, double blocks) {
        List<Clearing> open = open(level);
        if (open.isEmpty() || !clears(level, BlockPos.containing(at))) {
            return 0.0F;
        }
        double inside = 0.0;
        for (Clearing clearing : open) {
            float radius = radius(clearing, partialTick);
            if (radius <= 0.0F || !clears(level, clearing.center())) {
                continue;
            }
            double dx = clearing.center().getX() + 0.5 - at.x, dz = clearing.center().getZ() + 0.5 - at.z;
            inside = Math.max(inside, radius - Math.sqrt(dx * dx + dz * dz));
        }
        float t = (float) Mth.clamp(inside / blocks, 0.0, 1.0);
        return t * t * (3.0F - 2.0F * t);
    }

    /**
     * Le premier bloc libre au-dessus du sol en (x, z) : on descend depuis MOTION_BLOCKING à travers le feuillage et
     * l'air sous la ramure, jusqu'à un bloc plein ou un liquide (32 blocs au plus).
     */
    private static int groundBelowLeaves(ClientLevel level, BlockPos.MutableBlockPos pos, int x, int z) {
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        for (int i = 0; i < 32; i++) {
            BlockState below = level.getBlockState(pos.set(x, y - 1, z));
            if (!below.is(BlockTags.LEAVES) && (below.blocksMotion() || !below.getFluidState().isEmpty())) {
                break;
            }
            y--;
        }
        return y;
    }

    /** Vrai si le biome de {@code pos} fait partie de ceux qui se dégagent (ni océan, ni plage, ni désert, ni grotte). */
    public static boolean clears(ClientLevel level, BlockPos pos) {
        return looks.containsKey(level.getBiome(pos).value());
    }

    /** Reçoit l'annonce du serveur, sur le fil du client. */
    static void receive(ClearingSync sync) {
        ringTicks = Math.max(1, sync.ringTicks());
        ClientLevel level = Minecraft.getInstance().level;
        long now = level == null ? time : level.getGameTime();
        switch (sync.kind()) {
            case ClearingSync.ALL -> {
                boolean redraw = !clearings.isEmpty() || !sync.clearings().isEmpty();
                clearings.clear();
                waves.clear();
                clearings.addAll(sync.clearings());
                looksStale = true;
                for (Clearing clearing : clearings) {
                    int drawn = clearing.radiusAt(now, ringTicks);
                    if (drawn < clearing.radius()) {
                        waves.put(clearing, drawn);
                    }
                }
                index = new ClearingIndex(clearings);
                if (redraw && level != null) {
                    level.clearTintCaches();
                    Minecraft.getInstance().levelRenderer.allChanged();
                    clearFog();
                }
            }
            case ClearingSync.OPENED -> {
                for (Clearing opened : sync.clearings()) {
                    clearings.removeIf(clearing -> clearing.center().equals(opened.center()));
                    waves.keySet().removeIf(clearing -> clearing.center().equals(opened.center()));
                    clearings.add(opened);
                    waves.put(opened, 0);
                }
                index = new ClearingIndex(clearings);
            }
            case ClearingSync.CLOSED -> {
                for (Clearing closed : sync.clearings()) {
                    clearings.removeIf(clearing -> clearing.center().equals(closed.center()));
                    waves.keySet().removeIf(clearing -> clearing.center().equals(closed.center()));
                    clearings.add(closed);
                    waves.put(closed, closed.radius());
                }
                index = new ClearingIndex(clearings);
            }
            default -> Fallow.LOGGER.warn("clairiere : annonce inconnue {}", sync.kind());
        }
    }

    private static void forget() {
        clearings.clear();
        waves.clear();
        index = ClearingIndex.EMPTY;
        looks = Map.of();
        looksStale = true;
        overworld = false;
    }

    private static void tick() {
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        time = level.getGameTime();
        overworld = level.dimension() == Level.OVERWORLD;
        if (looksStale) {
            looks = looks(level);
            looksStale = false;
        }
        if (waves.isEmpty()) {
            return;
        }
        boolean ended = false;
        for (Iterator<Map.Entry<Clearing, Integer>> it = waves.entrySet().iterator(); it.hasNext();) {
            Map.Entry<Clearing, Integer> wave = it.next();
            Clearing clearing = wave.getKey();
            int drawn = wave.getValue(), now = clearing.radiusAt(time, ringTicks);
            if (now != drawn) {
                ringPassed(level, clearing, Math.min(drawn, now), Math.max(drawn, now));
                wave.setValue(now);
            }
            if (clearing.closing() ? now <= 0 : now >= clearing.radius()) {
                it.remove();
                if (clearing.closing()) {
                    clearings.remove(clearing);
                    ended = true;
                }
            }
        }
        if (ended) {
            index = new ClearingIndex(clearings);
        }
    }

    /** L'apparence de chaque biome qui se dégage : clairière froide, tempérée ou chaude selon sa température. */
    private static Map<Biome, Holder<Biome>> looks(ClientLevel level) {
        Registry<Biome> biomes = level.registryAccess().registryOrThrow(Registries.BIOME);
        Holder<Biome> temperate = biomes.getHolder(Clearings.LOOK).orElse(null);
        Holder<Biome> warm = biomes.getHolder(Clearings.LOOK_WARM).orElse(null);
        Holder<Biome> cold = biomes.getHolder(Clearings.LOOK_COLD).orElse(null);
        Map<Biome, Holder<Biome>> map = new IdentityHashMap<>();
        if (temperate == null || warm == null || cold == null) {
            return map;
        }
        biomes.holders().filter(biome -> biome.is(Clearings.CONVERTIBLE)).forEach(biome -> {
            float temperature = biome.value().getModifiedClimateSettings().temperature();
            map.put(biome.value(), temperature < Clearings.COLD_BELOW ? cold
                    : temperature > Clearings.WARM_ABOVE ? warm : temperate);
        });
        return map;
    }

    /**
     * La vague vient de passer entre {@code lo} et {@code hi} blocs du centre : les tronçons qu'elle traverse
     * sont redessinés, la brume d'ATMOSPHERICS relit ses biomes et suit en douceur, des volutes filent au sol.
     */
    private static void ringPassed(ClientLevel level, Clearing clearing, int lo, int hi) {
        int cx = clearing.center().getX(), cz = clearing.center().getZ();
        level.clearTintCaches();
        for (int x = (cx - hi) >> 4; x <= (cx + hi) >> 4; x++) {
            for (int z = (cz - hi) >> 4; z <= (cz + hi) >> 4; z++) {
                if (crosses(x, z, cx, cz, lo, hi)) {
                    // Seule la surface change de teinte : on ne redessine que les sections autour du sol du tronçon,
                    // pas ses 24 sections de haut en bas (la roche des profondeurs ne bouge pas).
                    int bottom = Integer.MAX_VALUE, top = Integer.MIN_VALUE;
                    for (int corner = 0; corner < 5; corner++) {
                        int bx = (x << 4) + (corner == 4 ? 8 : (corner & 1) * 15);
                        int bz = (z << 4) + (corner == 4 ? 8 : (corner >> 1) * 15);
                        int surface = level.getHeight(Heightmap.Types.WORLD_SURFACE, bx, bz);
                        bottom = Math.min(bottom, surface);
                        top = Math.max(top, surface);
                    }
                    Minecraft.getInstance().levelRenderer.setBlocksDirty(x << 4, Math.max(level.getMinBuildHeight(), bottom - 24),
                            z << 4, (x << 4) + 15, Math.min(level.getMaxBuildHeight() - 1, top + 8), (z << 4) + 15);
                }
            }
        }
        clearFog();
        puffs(level, clearing, lo, hi);
    }

    private static void clearFog() {
        if (ATMOSPHERICS) {
            FogCache.clear();
            FogCache.slowFade();
        }
    }

    /** Vrai si le tronçon a au moins un point entre {@code lo} et {@code hi} blocs du centre. */
    private static boolean crosses(int chunkX, int chunkZ, int cx, int cz, long lo, long hi) {
        int minX = chunkX << 4, maxX = minX + 15, minZ = chunkZ << 4, maxZ = minZ + 15;
        long nx = Math.max(minX, Math.min(cx, maxX)) - cx, nz = Math.max(minZ, Math.min(cz, maxZ)) - cz;
        long fx = Math.max(Math.abs(minX - cx), Math.abs(maxX - cx)), fz = Math.max(Math.abs(minZ - cz), Math.abs(maxZ - cz));
        return nx * nx + nz * nz <= hi * hi && fx * fx + fz * fz >= lo * lo;
    }

    /** Des volutes au sol sur l'anneau : poussées vers l'extérieur quand la brume se lève, vers le centre quand elle revient. */
    private static void puffs(ClientLevel level, Clearing clearing, int lo, int hi) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.player == null || minecraft.player.distanceToSqr(clearing.center().getX(), minecraft.player.getY(),
                clearing.center().getZ()) > (clearing.radius() + PUFF_RANGE) * (clearing.radius() + PUFF_RANGE)) {
            return;
        }
        Map<Biome, Holder<Biome>> clearable = looks;
        RandomSource random = level.random;
        double push = clearing.closing() ? -0.12 : 0.12;
        int count = (int) Math.min(MAX_PUFFS, Math.PI * (hi * hi - lo * lo) / 16.0 + 4);
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int i = 0; i < count; i++) {
            double angle = random.nextDouble() * Math.PI * 2.0, distance = lo + random.nextDouble() * (hi - lo);
            double dx = Math.cos(angle), dz = Math.sin(angle);
            double x = clearing.center().getX() + 0.5 + dx * distance, z = clearing.center().getZ() + 0.5 + dz * distance;
            // Le client ne reçoit que WORLD_SURFACE et MOTION_BLOCKING : celle sans feuillage y reste vide (le bas du
            // monde). On descend donc sous les feuilles depuis MOTION_BLOCKING.
            int ground = groundBelowLeaves(level, pos, Mth.floor(x), Mth.floor(z));
            if (!clearable.containsKey(level.getBiome(pos.set(x, ground, z)).value())) {
                continue;
            }
            level.addParticle(ParticleTypes.CLOUD, true, x, ground + 0.4 + random.nextDouble() * 1.5, z,
                    dx * push, 0.02, dz * push);
        }
    }
}
