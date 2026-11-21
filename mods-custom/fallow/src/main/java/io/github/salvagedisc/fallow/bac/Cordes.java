package io.github.salvagedisc.fallow.bac;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Les cordes de bac tendues dans chaque monde, du pieu qui la tient au pieu d'en face. Le pieu « porteur » (celui où
 * la corde a été attachée en premier) l'inscrit quand son chunk se charge et l'efface quand il part.
 */
final class Cordes {
    /** Écart horizontal au plus entre une poulie et la corde qu'elle peut prendre, en blocs. */
    static final double REACH = 4.0;

    private static final Map<ResourceKey<Level>, Map<BlockPos, BlockPos>> LINES = new ConcurrentHashMap<>();

    record Line(BlockPos a, BlockPos b) {
    }

    private Cordes() {
    }

    static void add(Level level, BlockPos owner, BlockPos other) {
        LINES.computeIfAbsent(level.dimension(), key -> new ConcurrentHashMap<>()).put(owner.immutable(), other.immutable());
    }

    static void remove(Level level, BlockPos owner) {
        Map<BlockPos, BlockPos> lines = LINES.get(level.dimension());
        if (lines != null) {
            lines.remove(owner);
        }
    }

    /** Vrai si une corde tend encore de {@code a} à {@code b}, dans un sens ou dans l'autre. */
    static boolean linked(Level level, BlockPos a, BlockPos b) {
        Map<BlockPos, BlockPos> lines = LINES.getOrDefault(level.dimension(), Map.of());
        return b.equals(lines.get(a)) || a.equals(lines.get(b));
    }

    /** Le point d'attache de la corde sur un pieu : le haut du pieu. */
    static Vec3 knot(BlockPos post) {
        return new Vec3(post.getX() + 0.5, post.getY() + 0.9, post.getZ() + 0.5);
    }

    /**
     * La corde la plus proche du point (x, y, z), à moins de {@link #REACH} blocs à l'horizontale et passant entre deux
     * blocs sous lui et dix au-dessus, ou {@code null}.
     */
    static Line near(Level level, double x, double y, double z) {
        Line best = null;
        double bestDistance = REACH;
        for (Map.Entry<BlockPos, BlockPos> entry : new HashMap<>(LINES.getOrDefault(level.dimension(), Map.of())).entrySet()) {
            Vec3 a = knot(entry.getKey());
            Vec3 b = knot(entry.getValue());
            Traille.Place place = Traille.place(a.x, a.z, b.x, b.z, x, z);
            if (place.t() <= 0.0 || place.t() >= 1.0) {
                continue;
            }
            double ropeY = a.y + (b.y - a.y) * place.t();
            if (ropeY < y - 2.0 || ropeY > y + 10.0) {
                continue;
            }
            if (place.offset() < bestDistance) {
                bestDistance = place.offset();
                best = new Line(entry.getKey(), entry.getValue());
            }
        }
        return best;
    }
}
