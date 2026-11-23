package io.github.salvagedisc.fallow.clairiere;

import io.github.salvagedisc.fallow.Fallow;
import io.github.salvagedisc.fallow.FallowConfig;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.living.MobSpawnEvent;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jetbrains.annotations.Nullable;

/**
 * Ouvre et referme les clairières. Une clairière ne touche pas au monde : le biome reste celui que le
 * générateur a posé, avec son climat, ses saisons, ses cultures et sa faune. Le serveur retient seulement
 * le disque ({@link ClearingSources}) et l'annonce aux clients, qui y lèvent la brume et y ravivent les
 * couleurs ({@code ClearingClient}). Côté jeu, une clairière n'a qu'un effet : les monstres n'y apparaissent
 * plus à la surface.
 *
 * <p>Seuls les biomes du tag {@code fallow:peut_devenir_clairiere} se dégagent : océans, plages, déserts et
 * grottes gardent leur brume, même à l'intérieur du disque.
 */
public final class Clearings {
    public static final TagKey<Biome> CONVERTIBLE =
            TagKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, "peut_devenir_clairiere"));
    /** Les trois biomes de clairière : l'apparence d'un point dégagé, selon que son biome est froid, tempéré ou chaud. */
    public static final ResourceKey<Biome> LOOK = biome("clairiere");
    public static final ResourceKey<Biome> LOOK_WARM = biome("clairiere_chaude");
    public static final ResourceKey<Biome> LOOK_COLD = biome("clairiere_froide");
    /** En dessous : apparence froide. Au-dessus de {@link #WARM_ABOVE} : chaude. */
    public static final float COLD_BELOW = 0.3F;
    public static final float WARM_ABOVE = 1.0F;

    private Clearings() {
    }

    private static ResourceKey<Biome> biome(String path) {
        return ResourceKey.create(Registries.BIOME, ResourceLocation.fromNamespaceAndPath(Fallow.MOD_ID, path));
    }

    /** Avant la 0.8.0, une clairière remplaçait le biome : ces cases existent encore dans les anciens mondes. */
    static boolean isOldClearing(Holder<Biome> biome) {
        return biome.is(LOOK) || biome.is(LOOK_WARM) || biome.is(LOOK_COLD);
    }

    /** Les clairières ouvertes du monde (vide hors de l'Overworld) : pour les modules qui s'y rattachent, comme les Hôtes. */
    public static List<Clearing> all(ServerLevel level) {
        if (level.dimension() != Level.OVERWORLD) {
            return List.of();
        }
        return ClearingSources.get(level).index().all();
    }

    /** Ouvre une clairière de rayon {@code radius} autour de {@code center} et l'annonce aux joueurs. */
    public static void open(ServerLevel level, BlockPos center, int radius, @Nullable ServerPlayer player) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        ClearingSources sources = ClearingSources.get(level);
        Clearing clearing = sources.add(center, radius, level.getGameTime());
        send(level, ClearingSync.OPENED, clearing);
        int columns = liftedColumns(level, sources.index(), clearing);
        Fallow.LOGGER.info("clairiere : ouverte autour de {} (rayon {}), {} colonnes dégagées", center, radius, columns);
        if (player != null) {
            player.displayClientMessage(Component.translatable(columns > 0 ? "message.fallow.clairiere.dissipee"
                    : "message.fallow.clairiere.rien"), true);
        }
    }

    /**
     * Ouvre la clairière de {@code center} si aucune n'y est posée : pour une source déplacée (piston, machine
     * de Create) ou posée sans joueur (distributeur, structure), qui n'a pas reçu {@code setPlacedBy}.
     */
    public static void ensureOpen(ServerLevel level, BlockPos center, int radius) {
        if (level.dimension() == Level.OVERWORLD && !ClearingSources.get(level).has(center)) {
            open(level, center, radius, null);
        }
    }

    /** Ce qui tenait la clairière de {@code center} a disparu : la brume revient, sauf là où une autre la retient. */
    public static void close(ServerLevel level, BlockPos center, @Nullable ServerPlayer player) {
        if (level.dimension() != Level.OVERWORLD) {
            return;
        }
        Clearing clearing = ClearingSources.get(level).remove(center);
        if (clearing == null) {
            return;
        }
        send(level, ClearingSync.CLOSED, clearing.closedAt(level.getGameTime()));
        Fallow.LOGGER.info("clairiere : refermée autour de {} (rayon {})", center, clearing.radius());
        if (clearing.since() == 0L) {
            // Clairière d'un monde d'avant la 0.8.0 : ses cases de biome de clairière retournent au biome du lieu.
            OldClearings.restore(level, center, clearing.radius());
        }
        if (player != null) {
            player.displayClientMessage(Component.translatable("message.fallow.clairiere.retour"), true);
        }
    }

    /** Referme toutes les clairières dont le centre est dans le disque, et rend les anciennes cases de clairière. */
    static int closeWithin(ServerLevel level, BlockPos center, int radius, @Nullable ServerPlayer player) {
        if (level.dimension() != Level.OVERWORLD) {
            return 0;
        }
        List<Clearing> removed = ClearingSources.get(level).removeWithin(center, radius);
        for (Clearing clearing : removed) {
            send(level, ClearingSync.CLOSED, clearing.closedAt(level.getGameTime()));
        }
        OldClearings.restore(level, center, radius);
        if (player != null && !removed.isEmpty()) {
            player.displayClientMessage(Component.translatable("message.fallow.clairiere.retour"), true);
        }
        return removed.size();
    }

    /** Envoie toutes les clairières du monde à un joueur qui arrive. */
    static void sendAll(ServerPlayer player) {
        ServerLevel overworld = player.server.overworld();
        PacketDistributor.sendToPlayer(player, new ClearingSync(ClearingSync.ALL,
                ClearingSources.get(overworld).index().all(), FallowConfig.clairiereRingTicks()));
    }

    private static void send(ServerLevel level, int kind, Clearing clearing) {
        PacketDistributor.sendToPlayersInDimension(level,
                new ClearingSync(kind, List.of(clearing), FallowConfig.clairiereRingTicks()));
    }

    /** Vrai si {@code pos} est dans une clairière et que son biome se dégage. */
    public static boolean isClear(ServerLevel level, BlockPos pos) {
        if (level.dimension() != Level.OVERWORLD) {
            return false;
        }
        ClearingIndex index = ClearingSources.get(level).index();
        if (index.isEmpty() || !index.covers(pos.getX() + 0.5, pos.getZ() + 0.5, null)) {
            return false;
        }
        Holder<Biome> biome = level.getBiome(pos);
        return biome.is(CONVERTIBLE) || isOldClearing(biome);
    }

    /**
     * Colonnes (une tous les 4 blocs) que cette clairière dégage et qu'aucune autre ne dégageait déjà. Le biome
     * est lu à la surface là où le tronçon est chargé, au niveau de la mer sinon ; rien n'est chargé pour ça.
     */
    private static int liftedColumns(ServerLevel level, ClearingIndex index, Clearing clearing) {
        int count = 0;
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        int cx = clearing.center().getX(), cz = clearing.center().getZ(), radius = clearing.radius();
        for (int x = cx - radius; x <= cx + radius; x += Clearing.RING) {
            for (int z = cz - radius; z <= cz + radius; z += Clearing.RING) {
                if (!clearing.covers(x, z, radius) || index.covers(x, z, clearing)) {
                    continue;
                }
                LevelChunk chunk = level.getChunkSource().getChunkNow(x >> 4, z >> 4);
                int y = chunk == null ? level.getChunkSource().getGenerator().getSeaLevel()
                        : chunk.getHeight(Heightmap.Types.WORLD_SURFACE, x & 15, z & 15);
                if (level.getBiome(pos.set(x, y, z)).is(CONVERTIBLE)) {
                    count++;
                }
            }
        }
        return count;
    }

    /** Dans une clairière, aucun monstre n'apparaît de lui-même : c'est ce que faisait l'ancien biome de clairière. */
    static void onSpawnCheck(MobSpawnEvent.PositionCheck event) {
        if (event.getSpawnType() != MobSpawnType.NATURAL
                || event.getEntity().getType().getCategory() != MobCategory.MONSTER
                || !(event.getLevel().getLevel() instanceof ServerLevel level)
                || !FallowConfig.clairiereEnabled()) {
            return;
        }
        if (isClear(level, event.getEntity().blockPosition())) {
            event.setResult(MobSpawnEvent.PositionCheck.Result.FAIL);
        }
    }
}
