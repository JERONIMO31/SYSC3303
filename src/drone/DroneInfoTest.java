package drone;

import event.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.StandardizedTime;

import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class DroneInfoTest {

    private DroneInfo drone;
    private EventInfo fire;
    private List<String> logs;

    // Fake time controller
    static class MockTime extends StandardizedTime {
        private LocalTime time;

        public MockTime(LocalTime start) {
            super(start, 1);
            this.time = start;
        }

        public void setTime(LocalTime t) {
            this.time = t;
        }

        @Override
        public LocalTime getRelativeTime() {
            return time;
        }
    }

    private MockTime mockTime;

    @BeforeEach
    void setup() {
        logs = new ArrayList<>();
        mockTime = new MockTime(LocalTime.of(0, 0));

        drone = new DroneInfo(
                1,
                100, // capacity
                10,  // speed
                5,   // acceleration
                10,  // deploy rate
                2,   // nozzle time
                mockTime,
                logs::add
        );

        fire = new EventInfo(10, 10, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
    }

    // -------------------------------
    // BASIC FAULT BEHAVIOR
    // -------------------------------
    @Test
    void testApplySoftFault() {
        drone.assignToFire(fire);

        mockTime.setTime(mockTime.getRelativeTime().plusSeconds(1));

        drone.applyFault(FaultSeverity.SOFT, 10);

        assertFalse(drone.isAvailableForFire());
        assertTrue(logs.stream().anyMatch(s -> s.contains("returning to base")));
    }

    @Test
    void testApplyHardFault() {
        drone.applyFault(FaultSeverity.HARD, 0);

        // simulate transition immediately
        drone.checkStateTransition();

        assertFalse(drone.isAvailableForFire());
    }

    @Test
    void testApplyFaultIgnoredWhenNone() {
        drone.applyFault(FaultSeverity.NONE, 10);
        assertTrue(drone.isAvailableForFire());
    }

    @Test
    void testApplyFaultWhileAlreadyOutOfCommission() {
        drone.applyFault(FaultSeverity.HARD, 0);
        drone.checkStateTransition();

        drone.applyFault(FaultSeverity.SOFT, 5);

        assertTrue(logs.stream().anyMatch(s -> s.contains("already out of commission")));
    }

    // -------------------------------
    // FAULT + ASSIGNMENT INTERACTION
    // -------------------------------

    @Test
    void testCannotAssignWhenFaulted() {
        drone.applyFault(FaultSeverity.SOFT, 10);
        drone.assignToFire(fire);

        assertNull(drone.getAssignedFire());
    }

    @Test
    void testUnassignOnFault() {
        drone.assignToFire(fire);
        assertNotNull(drone.getAssignedFire());

        drone.applyFault(FaultSeverity.SOFT, 5);

        assertNull(drone.getAssignedFire());
    }

    // -------------------------------
    // STATE TRANSITIONS WITH FAULTS
    // -------------------------------

    @Test
    void testSoftFaultRecovery() {
        drone.applyFault(FaultSeverity.SOFT, 5);

        // simulate time passing
        mockTime.setTime(mockTime.getRelativeTime().plusSeconds(10));
        drone.checkStateTransition(); // go OUT_OF_COMMISSION
        drone.checkStateTransition(); // recover

        assertTrue(drone.isAvailableForFire());
    }

    @Test
    void testHardFaultNeverRecovers() {
        drone.applyFault(FaultSeverity.HARD, 0);

        mockTime.setTime(mockTime.getRelativeTime().plusSeconds(100));
        drone.checkStateTransition();
        drone.checkStateTransition();

        assertFalse(drone.isAvailableForFire());
    }

    @Test
    void testFaultDuringTravelToFire() {
        drone.assignToFire(fire);

        drone.applyFault(FaultSeverity.SOFT, 5);

        assertFalse(drone.isAvailableForFire());
        assertNull(drone.getAssignedFire());
    }

    @Test
    void testFaultTriggersReturnHome() {
        drone.assignToFire(fire);

        // simulate time passing so drone is no longer at base
        mockTime.setTime(mockTime.getRelativeTime().plusSeconds(1));

        drone.applyFault(FaultSeverity.SOFT, 5);

        assertTrue(logs.stream().anyMatch(s -> s.contains("returning to base due to")));
    }

    // -------------------------------
    // AVAILABILITY LOGIC
    // -------------------------------

    @Test
    void testAvailableInitially() {
        assertTrue(drone.isAvailableForFire());
    }

    @Test
    void testNotAvailableWhenExtinguishing() {
        drone.assignToFire(fire);

        // force transition
        mockTime.setTime(mockTime.getRelativeTime().plusSeconds(100));
        drone.checkStateTransition();

        assertFalse(drone.isAvailableForFire());
    }

    // -------------------------------
    // EDGE CASES
    // -------------------------------

    @Test
    void testDeployAgentReducesFireAndDrone() {
        drone.assignToFire(fire);

        int used = drone.deployAgent();

        assertTrue(used > 0);
        assertTrue(fire.getRemainingAgentRequired() < 10);
    }

    @Test
    void testDeployAgentNoFire() {
        int used = drone.deployAgent();
        assertEquals(0, used);
    }

    @Test
    void testTravelTimeZeroWhenNoFire() {
        assertEquals(0, drone.getTravelTime());
    }

    @Test
    void testAssignSameFireDoesNotDuplicate() {
        drone.assignToFire(fire);
        drone.assignToFire(fire);

        assertEquals(fire, drone.getAssignedFire());
    }
}