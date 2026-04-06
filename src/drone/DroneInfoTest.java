package drone;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import event.EventInfo;
import event.EventType;
import event.FaultType;
import event.Intensity;
import utils.StandardizedTime;

class DroneInfoTest {

    private DroneInfo drone;
    private StubTime stubTime;
    private List<String> logMessages;
    private StubFire stubFire;

    // --- STUBS ---
    static class StubTime extends StandardizedTime {
        private LocalTime time;

        StubTime(LocalTime time) {
            super(time, 1);
            this.time = time;
        }

        void setTime(LocalTime time) {
            this.time = time;
        }

        @Override
        public LocalTime getRelativeTime() {
            return time;
        }
    }

    static class StubFire extends EventInfo {
        int appliedAgent = 0;
        int remainingAgentRequired;
        Integer assignedDroneId = null;

        StubFire(int latitude, int longitude, int requiredAgent) {
            super(latitude, longitude, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
            this.remainingAgentRequired = requiredAgent;
        }

        @Override
        public String getLocationKey() {
            return "fire1";
        }

        @Override
        public int getRemainingAgentRequired() {
            return remainingAgentRequired;
        }

        @Override
        public int applyAgent(int amount) {
            int deployed = Math.min(amount, remainingAgentRequired);
            appliedAgent += deployed;
            remainingAgentRequired -= deployed;
            return deployed;
        }

        @Override
        public void assignDrone(Integer droneId) {
            assignedDroneId = droneId;
        }
    }

    @BeforeEach
    void setUp() {
        logMessages = new ArrayList<>();
        stubTime = new StubTime(LocalTime.of(12, 0));

        drone = new DroneInfo(
                1, // droneId
                100, // agentCapacity
                10, // speed
                2, // acceleration
                5, // deployRate
                3, // openNozzleTime
                10000, // batteryRange
                stubTime,
                logMessages::add);

        stubFire = new StubFire(10, 20, 50); // latitude, longitude, requiredAgent
    }

    // --- TESTS ---

    @Test
    void testAssignToFire() {
        drone.assignToFire(stubFire);
        assertEquals(stubFire, drone.getAssignedFire());
        assertEquals("TRAVELING_TO_FIRE", drone.getStateName());
        assertEquals(drone.droneId, stubFire.assignedDroneId);
        assertTrue(logMessages.stream().anyMatch(s -> s.contains("assigned to fire")));
    }

    @Test
    void testUnassignFire() {
        drone.assignToFire(stubFire);
        drone.unassignFire();
        assertNull(drone.getAssignedFire());
        assertNull(stubFire.assignedDroneId);
    }

    @Test
    void testRefillAgent() {
        drone.assignToFire(stubFire);
        drone.deployAgent();
        assertTrue(drone.getAvailableAgent() < 100);

        drone.refillAgent();
        assertEquals(100, drone.getAvailableAgent());
    }

    @Test
    void testDeployAgent() {
        drone.assignToFire(stubFire);
        int deployed = drone.deployAgent();
        assertEquals(50, deployed);
        assertEquals(50, drone.getAvailableAgent());
        assertEquals(0, stubFire.remainingAgentRequired);
    }

    @Test
    void testDeployAgentWithoutFire() {
        assertEquals(0, drone.deployAgent());
    }

    @Test
    void testApplyFault() {
        drone.assignToFire(stubFire);
        drone.applyFault(FaultType.DRONE_STUCK);
        assertEquals("TRAVELING_HOME", drone.getStateName());
        assertEquals(FaultType.DRONE_STUCK.name(), drone.getFaultName());
        assertNull(drone.getAssignedFire());
        assertTrue(logMessages.stream().anyMatch(s -> s.contains("returning to base")));
    }

    @Test
    void testAssignToFireWhenOutOfCommission() {
        drone.applyFault(FaultType.NOZZLE_STUCK);
        drone.assignToFire(stubFire);
        assertNull(drone.getAssignedFire());
        assertTrue(logMessages.stream().anyMatch(s -> s.contains("out of commission")));
    }
}