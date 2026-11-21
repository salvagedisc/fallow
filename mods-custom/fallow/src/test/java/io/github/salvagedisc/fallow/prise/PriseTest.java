package io.github.salvagedisc.fallow.prise;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class PriseTest {
    @Test
    void smallFishPullNothing() {
        assertEquals(0.0, Prise.share(2.0, 200.0));
        assertEquals(0.0, Prise.target(5.0, 100.0, 1.2));
    }

    @Test
    void heavyFishAndLightBoatGoFaster() {
        double raft = 6 * Prise.KG_PER_MASS;
        double tuna = Prise.target(120.0, raft, 1.0);
        double sturgeon = Prise.target(100.0, raft, 1.0);
        double monster = Prise.target(700.0, raft, 1.0);
        assertTrue(tuna > sturgeon && monster > tuna, "the heavier the fish, the faster");
        assertTrue(Prise.target(120.0, 60 * Prise.KG_PER_MASS, 1.0) < tuna / 5.0, "a big hull barely moves");
        assertTrue(monster < Prise.FISH_SPEED, "never faster than the fish swims free");
        assertEquals(Prise.FISH_SPEED * 120.0 / (120.0 + raft), tuna, 1e-9);
    }

    @Test
    void theFishSurgesAndWanders() {
        for (long t = 0; t < 2000; t += 7) {
            double s = Prise.surge(t, 0.3);
            assertTrue(s >= 0.5 && s <= 1.2);
            double h = Prise.heading(1.0, t, 0.3);
            assertTrue(Math.abs(h - 1.0) <= Prise.WANDER + 1e-9);
        }
    }

    @Test
    void theFishPullsButNeverHoldsBack() {
        assertEquals(Prise.RATE * 0.5, Prise.pull(0.5, 1.0), 1e-9);
        assertEquals(0.0, Prise.pull(2.0, 1.0));
        assertEquals(Prise.MAX_ACCEL, Prise.pull(-5.0, 1.0));
    }
}
