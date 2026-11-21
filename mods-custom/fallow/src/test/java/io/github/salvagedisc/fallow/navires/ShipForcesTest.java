package io.github.salvagedisc.fallow.navires;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ShipForcesTest {
    @Test
    void theWindSeesTheFaceItBlowsAgainst() {
        // Vent selon x : il voit la face en z (10 de large, 4 de haut).
        assertEquals(40.0, ShipForces.area(30.0, 10.0, 4.0, 5.0, 0.0), 1e-9);
        // Vent selon z : il voit la longueur en x.
        assertEquals(120.0, ShipForces.area(30.0, 10.0, 4.0, 0.0, -2.0), 1e-9);
        assertEquals(0.0, ShipForces.area(30.0, 10.0, 0.0, 5.0, 0.0));
        assertEquals(0.0, ShipForces.area(30.0, 10.0, 4.0, 0.0, 0.0));
        assertEquals(ShipForces.MAX_AREA, ShipForces.area(400.0, 400.0, 50.0, 1.0, 1.0));
    }

    @Test
    void aShipRunningWithTheWindIsNoLongerPushed() {
        double[] still = ShipForces.wind(50.0, 6.0, 0.0, 0.0, 0.0, 1.0);
        assertEquals(ShipForces.WIND_COEFFICIENT * 50.0 * 6.0, still[0], 1e-9);
        assertEquals(0.0, still[1], 1e-9);
        double[] running = ShipForces.wind(50.0, 6.0, 0.0, 6.0, 0.0, 1.0);
        assertEquals(0.0, running[0], 1e-9);
        double[] against = ShipForces.wind(50.0, 6.0, 0.0, -4.0, 0.0, 1.0);
        assertTrue(against[0] > still[0], "a ship sailing into the wind is held back harder");
        assertEquals(0.0, ShipForces.wind(50.0, 6.0, 0.0, 0.0, 0.0, 0.0)[0]);
    }

    @Test
    void theEdgeHeadwindRisesAndBrakesOnlyBeyondTheEdge() {
        assertEquals(0.0, ShipForces.edge(-ShipForces.EDGE_BAND, 10.0));
        assertEquals(ShipForces.EDGE_ACCELERATION, ShipForces.edge(0.0, 10.0), 1e-9);
        double beyond = ShipForces.edge(24.0, 0.0);
        assertTrue(beyond > ShipForces.EDGE_ACCELERATION);
        assertTrue(ShipForces.edge(24.0, 5.0) > beyond, "what still heads out is braked");
        assertEquals(beyond, ShipForces.edge(24.0, -5.0), 1e-9);
        assertEquals(ShipForces.MAX_EDGE_ACCELERATION, ShipForces.edge(10000.0, 100.0));
    }
}
