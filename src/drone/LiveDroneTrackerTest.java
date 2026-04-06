package drone;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.StandardizedTime;

import java.time.LocalTime;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.*;

class LiveDroneTrackerTest {

    private LiveDroneTracker tracker;

    @BeforeEach
    void setUp() {
        int totalDrones = 5;
        int agentCapacity = 100;
        int speed = 10;
        int acceleration = 2;
        int deployRate = 5;
        int openNozzleTime = 3;
        int batteryRange = 10000;

        StandardizedTime time = new StandardizedTime(LocalTime.now(), 1);

        Consumer<String> logger = msg -> {
        };

        tracker = new LiveDroneTracker(
                totalDrones,
                agentCapacity,
                speed,
                acceleration,
                deployRate,
                openNozzleTime,
                batteryRange,
                time,
                logger);
    }

    @Test
    void testConstructor_initializesCorrectNumberOfDrones() {
        DroneInfo[] drones = tracker.getAllDrones();
        assertEquals(5, drones.length);
    }

    @Test
    void testGetAllDrones_notNull() {
        DroneInfo[] drones = tracker.getAllDrones();
        assertNotNull(drones);
    }

    @Test
    void testGetDroneInfo_validId_returnsCorrectDrone() {
        DroneInfo drone = tracker.getDroneInfo(2);

        assertNotNull(drone);
        assertEquals(2, drone.getId());
    }

    @Test
    void testGetDroneInfo_invalidId_returnsNull() {
        DroneInfo drone = tracker.getDroneInfo(999);
        assertNull(drone);
    }

    @Test
    void testAllDroneIds_unique() {
        DroneInfo[] drones = tracker.getAllDrones();

        boolean[] seen = new boolean[drones.length];

        for (DroneInfo drone : drones) {
            int id = drone.getId();
            assertFalse(seen[id], "Duplicate drone ID found: " + id);
            seen[id] = true;
        }
    }
}