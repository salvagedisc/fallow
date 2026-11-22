package io.github.salvagedisc.fallow.gabarits.client;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Le report de couleur : le dessin du gabarit (le fer du pack actif) prend la teinte de l'objet du mod.
 *
 * <p>Le métal du gabarit est ce qui change entre ses versions fer, or et diamant (le manche, lui, ne change pas), moins
 * les couleurs du bâton. Les couleurs de l'objet du mod et du fer du jeu sont rangées de la plus sombre à la plus
 * claire ; chaque couleur du gabarit, à son rang de clarté, reçoit la teinte du mod à ce rang et l'écart de clarté entre
 * le mod et le fer du jeu. Tout se fait en CIELAB, couleur par couleur (pas pixel par pixel) : le grain du gabarit reste
 * net.
 */
final class Teinte {
    private static final double[] WHITE = {0.95047, 1.0, 1.08883};

    private Teinte() {
    }

    /** Pixels opaques de {@code image} qui ne sont dans aucune des {@code others} à la même place, ni dans {@code palette}. */
    static boolean[] metal(Planche image, List<Planche> others, double[][] palette) {
        boolean[] mask = new boolean[image.size()];
        for (int i = 0; i < mask.length; i++) {
            boolean m = image.a[i] > 0;
            for (Planche o : others) {
                if (m && o.sameSize(image)) {
                    double d = Math.abs(image.r[i] - o.r[i]) + Math.abs(image.g[i] - o.g[i]) + Math.abs(image.b[i] - o.b[i]);
                    m = d > 12 || o.a[i] == 0;
                }
            }
            if (m && palette != null) {
                for (double[] c : palette) {
                    if (Math.abs(image.r[i] - c[0]) + Math.abs(image.g[i] - c[1]) + Math.abs(image.b[i] - c[2]) <= 10) {
                        m = false;
                        break;
                    }
                }
            }
            mask[i] = m;
        }
        return mask;
    }

    /** Les couleurs opaques d'une image, sans doublon. */
    static double[][] palette(Planche image) {
        if (image == null) {
            return null;
        }
        Map<Integer, double[]> colours = new LinkedHashMap<>();
        for (int i = 0; i < image.size(); i++) {
            if (image.a[i] > 0) {
                int key = (int) image.r[i] << 16 | (int) image.g[i] << 8 | (int) image.b[i];
                colours.putIfAbsent(key, new double[] {image.r[i], image.g[i], image.b[i]});
            }
        }
        return colours.values().toArray(new double[0][]);
    }

    /** Recouvrement des formes (pixels opaques) de deux images de même taille, de 0 à 1. */
    static double overlap(Planche a, Planche b) {
        if (a == null || !a.sameSize(b)) {
            return 0;
        }
        int both = 0;
        int either = 0;
        for (int i = 0; i < a.size(); i++) {
            boolean x = a.a[i] > 0;
            boolean y = b.a[i] > 0;
            both += x && y ? 1 : 0;
            either += x || y ? 1 : 0;
        }
        return either == 0 ? 0 : (double) both / either;
    }

    /**
     * Le gabarit {@code template} teint comme {@code source} l'est par rapport à {@code ref}, ou {@code null} si l'un des
     * trois n'a presque pas de métal.
     */
    static Planche transfer(Planche template, List<Planche> templateOthers, Planche ref, List<Planche> refOthers, Planche source,
            double[][] templateHandle, double[][] refHandle, double kl, double kc) {
        boolean[] tm = metal(template, templateOthers, templateHandle);
        boolean[] vm = metal(ref, refOthers, null);
        boolean[] sm = metal(source, List.of(), refHandle);
        if (count(tm) < 4 || count(vm) < 4 || count(sm) < 4) {
            return null;
        }
        double[][] vr = ramp(ref, vm);
        double[][] sr = ramp(source, sm);

        Map<Integer, Integer> index = new LinkedHashMap<>();
        List<double[]> colours = new ArrayList<>();
        for (int i = 0; i < tm.length; i++) {
            if (tm[i]) {
                int key = (int) template.r[i] << 16 | (int) template.g[i] << 8 | (int) template.b[i];
                if (!index.containsKey(key)) {
                    index.put(key, colours.size());
                    colours.add(lab(template.r[i], template.g[i], template.b[i]));
                }
            }
        }
        Integer[] order = new Integer[colours.size()];
        for (int i = 0; i < order.length; i++) {
            order[i] = i;
        }
        Arrays.sort(order, Comparator.comparingDouble(i -> colours.get(i)[0]));
        double[] rank = new double[order.length];
        for (int k = 0; k < order.length; k++) {
            rank[order[k]] = order.length > 1 ? (double) k / (order.length - 1) : 0;
        }
        double[][] mapped = new double[colours.size()][];
        for (int c = 0; c < colours.size(); c++) {
            double r = rank[c];
            double dl = at(sr[0], r) - at(vr[0], r);
            mapped[c] = rgb(colours.get(c)[0] + dl * kl, at(sr[1], r) * kc, at(sr[2], r) * kc);
        }

        Planche out = template.copy();
        for (int i = 0; i < tm.length; i++) {
            if (tm[i]) {
                double[] c = mapped[index.get((int) template.r[i] << 16 | (int) template.g[i] << 8 | (int) template.b[i])];
                out.r[i] = c[0];
                out.g[i] = c[1];
                out.b[i] = c[2];
            }
        }
        return out;
    }

    private static int count(boolean[] mask) {
        int n = 0;
        for (boolean m : mask) {
            n += m ? 1 : 0;
        }
        return n;
    }

    /** L, a et b des pixels du masque, rangés par clarté croissante (a et b suivent leur pixel). */
    private static double[][] ramp(Planche image, boolean[] mask) {
        List<double[]> labs = new ArrayList<>();
        for (int i = 0; i < mask.length; i++) {
            if (mask[i]) {
                labs.add(lab(image.r[i], image.g[i], image.b[i]));
            }
        }
        labs.sort(Comparator.comparingDouble(l -> l[0]));
        double[][] ramp = new double[3][labs.size()];
        for (int k = 0; k < labs.size(); k++) {
            ramp[0][k] = labs.get(k)[0];
            ramp[1][k] = labs.get(k)[1];
            ramp[2][k] = labs.get(k)[2];
        }
        return ramp;
    }

    /** Valeur au rang {@code r} (0 à 1) d'une suite, par interpolation linéaire. */
    private static double at(double[] values, double r) {
        double pos = r * (values.length - 1);
        int lo = (int) Math.floor(pos);
        int hi = Math.min(lo + 1, values.length - 1);
        return values[lo] + (values[hi] - values[lo]) * (pos - lo);
    }

    private static double linear(double c) {
        c /= 255;
        return c <= 0.04045 ? c / 12.92 : Math.pow((c + 0.055) / 1.055, 2.4);
    }

    private static double gamma(double c) {
        c = Math.max(0, Math.min(1, c));
        return 255 * (c <= 0.0031308 ? c * 12.92 : 1.055 * Math.pow(c, 1 / 2.4) - 0.055);
    }

    private static double f(double t) {
        return t > 0.008856 ? Math.cbrt(t) : 7.787 * t + 16.0 / 116;
    }

    private static double fInverse(double t) {
        return t > 0.206893 ? t * t * t : (t - 16.0 / 116) / 7.787;
    }

    static double[] lab(double r, double g, double b) {
        double lr = linear(r);
        double lg = linear(g);
        double lb = linear(b);
        double x = (0.4124 * lr + 0.3576 * lg + 0.1805 * lb) / WHITE[0];
        double y = (0.2126 * lr + 0.7152 * lg + 0.0722 * lb) / WHITE[1];
        double z = (0.0193 * lr + 0.1192 * lg + 0.9505 * lb) / WHITE[2];
        double fx = f(x);
        double fy = f(y);
        double fz = f(z);
        return new double[] {116 * fy - 16, 500 * (fx - fy), 200 * (fy - fz)};
    }

    static double[] rgb(double l, double a, double b) {
        double fy = (l + 16) / 116;
        double x = fInverse(fy + a / 500) * WHITE[0];
        double y = fInverse(fy) * WHITE[1];
        double z = fInverse(fy - b / 200) * WHITE[2];
        double lr = 3.2406255 * x - 1.5372080 * y - 0.4986286 * z;
        double lg = -0.9689307 * x + 1.8757561 * y + 0.0415175 * z;
        double lb = 0.0557101 * x - 0.2040211 * y + 1.0569959 * z;
        return new double[] {gamma(lr), gamma(lg), gamma(lb)};
    }
}
