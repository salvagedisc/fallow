package io.github.salvagedisc.fallow.gabarits.client;

import com.mojang.blaze3d.platform.NativeImage;
import java.io.IOException;
import java.io.InputStream;

/** Une image en couleurs séparées (0 à 255), plus simple à lire et à comparer qu'une {@link NativeImage}. */
final class Planche {
    final int width;
    final int height;
    final double[] r;
    final double[] g;
    final double[] b;
    final double[] a;

    Planche(int width, int height) {
        this.width = width;
        this.height = height;
        int n = width * height;
        this.r = new double[n];
        this.g = new double[n];
        this.b = new double[n];
        this.a = new double[n];
    }

    static Planche read(InputStream in) throws IOException {
        try (NativeImage image = NativeImage.read(in)) {
            Planche p = new Planche(image.getWidth(), image.getHeight());
            for (int y = 0; y < p.height; y++) {
                for (int x = 0; x < p.width; x++) {
                    int abgr = image.getPixelRGBA(x, y);
                    int i = y * p.width + x;
                    p.r[i] = abgr & 0xFF;
                    p.g[i] = abgr >> 8 & 0xFF;
                    p.b[i] = abgr >> 16 & 0xFF;
                    p.a[i] = image.format().hasAlpha() ? abgr >>> 24 : 255;
                }
            }
            return p;
        }
    }

    int size() {
        return width * height;
    }

    boolean sameSize(Planche other) {
        return other != null && other.width == width && other.height == height;
    }

    Planche copy() {
        Planche p = new Planche(width, height);
        System.arraycopy(r, 0, p.r, 0, r.length);
        System.arraycopy(g, 0, p.g, 0, g.length);
        System.arraycopy(b, 0, p.b, 0, b.length);
        System.arraycopy(a, 0, p.a, 0, a.length);
        return p;
    }

    byte[] png() throws IOException {
        try (NativeImage image = new NativeImage(width, height, false)) {
            for (int y = 0; y < height; y++) {
                for (int x = 0; x < width; x++) {
                    int i = y * width + x;
                    int abgr = channel(a[i]) << 24 | channel(b[i]) << 16 | channel(g[i]) << 8 | channel(r[i]);
                    image.setPixelRGBA(x, y, abgr);
                }
            }
            return image.asByteArray();
        }
    }

    private static int channel(double v) {
        return (int) Math.max(0, Math.min(255, Math.round(v)));
    }
}
