package io.github.salvagedisc.fallow.navires;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class GrosTest {
    @Test
    void noGustInFairWeather() {
        for (long t = 0; t < 20 * 600; t += 7) {
            double[] g = Gros.gust(t, 42L, 0.0F, 0.0F, 120.0, 0.0);
            assertEquals(0.0, g[0]);
            assertEquals(0.0, g[1]);
        }
    }

    @Test
    void stormGustsComeInBurstsAndAreStrongerAloft() {
        int calm = 0;
        int bursts = 0;
        double maxLow = 0.0;
        double maxHigh = 0.0;
        for (long t = 0; t < 20 * 600; t += 3) {
            double[] low = Gros.gust(t, 42L, 1.0F, 1.0F, 64.0, 0.0);
            double[] high = Gros.gust(t, 42L, 1.0F, 1.0F, 240.0, 0.0);
            double s = Math.hypot(low[0], low[1]);
            if (s == 0.0) {
                calm++;
            } else {
                bursts++;
            }
            maxLow = Math.max(maxLow, s);
            maxHigh = Math.max(maxHigh, Math.hypot(high[0], high[1]));
        }
        assertTrue(calm > 0 && bursts > 0, "gusts come and go: " + calm + " calm, " + bursts + " bursts");
        assertTrue(maxLow <= Gros.GUST_SPEED + 1e-9);
        assertTrue(maxHigh > maxLow * 1.5, "stronger aloft: " + maxHigh + " vs " + maxLow);
    }

    @Test
    void theSailDrawsNothingInIronsAndMostOnAReach() {
        // Vent qui souffle vers +x : le bateau cap -x est face au vent.
        assertEquals(0.0, Gros.sail(-1.0, 0.0, 1.0, 0.0));
        double reach = Gros.sail(0.0, 1.0, 1.0, 0.0);
        double run = Gros.sail(1.0, 0.0, 1.0, 0.0);
        double closeHauled = Gros.sail(-0.5, 1.0, 1.0, 0.0);
        assertEquals(1.0, reach, 1e-9);
        assertEquals(0.9, run, 1e-9);
        assertTrue(closeHauled > 0.0 && closeHauled < reach);
        assertEquals(0.0, Gros.sail(0.0, 0.0, 1.0, 0.0));
    }

    @Test
    void sailPushFollowsTheWind() {
        assertEquals(0.015, Gros.sailPush(0.03, 1.0, 0.0, 1.0), 1e-9);
        assertEquals(0.03, Gros.sailPush(0.03, 1.0, 3.0, 1.0), 1e-9);
        assertEquals(0.075, Gros.sailPush(0.03, 1.0, 30.0, 1.0), 1e-9);
        assertEquals(0.0, Gros.sailPush(0.03, 0.0, 6.0, 1.0));
    }
}
