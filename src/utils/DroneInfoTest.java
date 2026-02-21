package utils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DroneInfoTest {

    private DroneInfo drone;
    private TestEventInfo fire;

    // Simple stub for testing
    static class TestEventInfo extends EventInfo {
        int remainingAgent;
        Integer assignedDrone = null;
        int latitude;
        int longitude;

        public TestEventInfo(int latitude, int longitude, int requiredAgent) {
            this.latitude = latitude;
            this.longitude = longitude;
            this.remainingAgent = requiredAgent;
        }

        @Override
        public void assignDrone(Integer droneId) {
            this.assignedDrone = droneId;
        }

        @Override
        public int getRemainingAgentRequired() {
            return remainingAgent;
        }

        @Override
        public int applyAgent(int amount) {
            int used = Math.min(amount, remainingAgent);
            remainingAgent -= used;
            return used;
        }

        @Override
        public boolean isExtinguished() {
            return remainingAgent <= 0;
        }

        @Override
        public String getLocationKey() {
            return latitude + "," + longitude;
        }
    }

    @BeforeEach
    void setup() {
        drone = new DroneInfo(1);
        fire = new TestEventInfo(30, 40, 10);
    }

    @Test
    void testInitialState() {
        assertTrue(drone.isAvailable());
        assertEquals("(0,0)", drone.getLocationKey());
        assertEquals("No fire assigned", drone.getAssignedFireLocation());
    }

    @Test
    void testAssignToFire() {
        drone.assignToFire(fire);

        assertFalse(drone.isAvailable());
        assertEquals(1, fire.assignedDrone);
        assertEquals("30,40", drone.getAssignedFireLocation());
    }

    @Test
    void testRefillAgent() {
        drone.refillAgent(); // should reset to full capacity
        drone.assignToFire(fire);

        assertDoesNotThrow(() -> drone.refillAgent());
    }

    @Test
    void testDeployAgentReducesFireAndDroneAgent() throws InterruptedException {
        drone.assignToFire(fire);

        int deployed = drone.deployAgent();

        assertEquals(10, deployed);
        assertTrue(fire.isExtinguished());
    }

    @Test
    void testDeployAgentWithNoFire() throws InterruptedException {
        int deployed = drone.deployAgent();
        assertEquals(0, deployed);
    }

    @Test
    void testTravelTimeCalculation() {
        drone.assignToFire(fire);

        double travelTime = drone.getTravelTime();

        assertTrue(travelTime > 0);
    }

    @Test
    void testTravelToFireUpdatesLocation() {
        drone.assignToFire(fire);

        drone.travelToFire();

        assertEquals("(100,100)", drone.getLocationKey());
    }

    @Test
    void testTravelHomeResetsState() {
        drone.assignToFire(fire);

        drone.travelToFire();
        drone.travelHome();

        assertTrue(drone.isAvailable());
        assertEquals("(0,0)", drone.getLocationKey());
        assertNull(fire.assignedDrone);
    }

    @Test
    void testIsFireExtinguished() throws InterruptedException {
        drone.assignToFire(fire);
        drone.deployAgent();

        assertTrue(drone.isFireExtinguished());
    }
}