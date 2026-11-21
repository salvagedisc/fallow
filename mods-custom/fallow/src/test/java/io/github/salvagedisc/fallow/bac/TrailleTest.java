package io.github.salvagedisc.fallow.bac;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class TrailleTest {
    // Une corde de 40 blocs, du nord (z = 0) au sud (z = 40), au-dessus d'un fleuve qui coule vers l'est.
    private static final double AX = 10.0;
    private static final double AZ = 0.0;
    private static final double BX = 10.0;
    private static final double BZ = 40.0;

    @Test
    void theRaftIsPlacedAlongTheRope() {
        Traille.Place p = Traille.place(AX, AZ, BX, BZ, 13.0, 10.0);
        assertEquals(0.25, p.t(), 1e-9);
        assertEquals(10.0, p.along(), 1e-9);
        assertEquals(3.0, p.offset(), 1e-9);
        assertEquals(0.0, p.ux(), 1e-9);
        assertEquals(1.0, p.uz(), 1e-9);
        Traille.Place beyond = Traille.place(AX, AZ, BX, BZ, 10.0, 55.0);
        assertEquals(1.0, beyond.t());
        assertEquals(15.0, beyond.offset(), 1e-9);
    }

    @Test
    void theRopeHoldsOnlyAcross() {
        Traille.Place p = Traille.place(AX, AZ, BX, BZ, 12.0, 10.0);
        double[] a = Traille.hold(p, 0.0, 2.0);
        assertEquals(-Traille.STIFFNESS * 2.0, a[0], 1e-9);
        assertEquals(0.0, a[1], 1e-9, "moving along the rope is not held back");
        double[] drifting = Traille.hold(Traille.place(AX, AZ, BX, BZ, 10.0, 10.0), 1.0, 0.0);
        assertEquals(-Traille.DAMPING, drifting[0], 1e-9);
        double[] far = Traille.hold(Traille.place(AX, AZ, BX, BZ, 30.0, 10.0), 0.0, 0.0);
        assertEquals(Traille.MAX_LATERAL, Math.hypot(far[0], far[1]), 1e-9);
    }

    @Test
    void theCurrentAcrossTheRopeCarriesTheFerryOver() {
        Traille.Place middle = Traille.place(AX, AZ, BX, BZ, 10.0, 20.0);
        assertEquals(0.0, Traille.target(middle, 0, 1.5, 0.0));
        assertEquals(Traille.HAND_SPEED, Traille.target(middle, 1, 0.0, 0.0), 1e-9);
        double withCurrent = Traille.target(middle, 1, 1.5, 0.0);
        assertEquals(Traille.HAND_SPEED + Traille.CURRENT_GAIN * 1.5, withCurrent, 1e-9);
        assertEquals(-withCurrent, Traille.target(middle, -1, 1.5, 0.0), 1e-9);
        // Un courant le long de la corde ne fait pas traverser.
        assertEquals(Traille.HAND_SPEED, Traille.target(middle, 1, 0.0, 1.5), 1e-9);
    }

    @Test
    void theFerryStopsAtTheBank() {
        Traille.Place nearB = Traille.place(AX, AZ, BX, BZ, 10.0, 38.0);
        assertEquals(0.0, Traille.target(nearB, 1, 1.5, 0.0));
        assertTrue(Traille.target(nearB, -1, 1.5, 0.0) < 0.0, "it can leave again the other way");
        Traille.Place nearA = Traille.place(AX, AZ, BX, BZ, 10.0, 1.0);
        assertEquals(0.0, Traille.target(nearA, -1, 1.5, 0.0));
    }

    @Test
    void speedIsReachedSmoothly() {
        assertEquals(Traille.ALONG_RATE * 1.0, Traille.along(1.0, 2.0), 1e-9);
        assertEquals(Traille.MAX_ALONG, Traille.along(0.0, 100.0));
        assertEquals(-Traille.MAX_ALONG, Traille.along(100.0, 0.0));
    }
}
