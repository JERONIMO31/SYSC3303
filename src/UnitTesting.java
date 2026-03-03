import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import drone.DroneInfo;
import drone.LiveDroneTracker;
import event.LiveFireTracker;

import org.junit.jupiter.api.BeforeEach;
import java.time.LocalTime;

import utils.*;

import javax.swing.*;

public class UnitTesting {

    private EndCondition endCondition;
    private LiveDroneTracker droneTracker;
    private LiveFireTracker fireTracker;
    private GUI gui;
    private StandardizedTime standardTime;

    @BeforeEach
    void setUp() {
        this.endCondition = new EndCondition();
        this.droneTracker = new LiveDroneTracker(1);
        this.fireTracker = new LiveFireTracker();
        this.standardTime = new StandardizedTime(LocalTime.now());
        this.gui = new GUI();
    }

    @Test
    void testDroneLogic() {
        DroneInfo info = new DroneInfo(1);
        Drone drone = new Drone(info, droneTracker, endCondition, gui);

        assertNotNull(drone);
        assertEquals(1, info.droneId);
        assertTrue(info.isAvailable());
    }

    @Test
    void testSchedulerLogic() {
        Scheduler scheduler = new Scheduler(droneTracker, fireTracker, endCondition, gui);
        assertNotNull(scheduler);
        endCondition.setStop(true);
        assertNotNull(scheduler);
    }

    @Test
    void testFireIncidentInitialization() {
        FireIncident incident = new FireIncident(fireTracker, "zones.csv", "events.csv", endCondition, standardTime,
                gui);
        assertNotNull(incident);
    }

    // 4. TEST: GUI.java
    @Test
    void testGUIComponents() {
        assertNotNull(gui);
        assertTrue(gui instanceof JFrame);
        assertDoesNotThrow(() -> gui.printMessage("System Test Entry"));
    }

    @Test
    void testMapWrapper() {
        GridWithLegend mapWrapper = new GridWithLegend();
        assertNotNull(mapWrapper);
    }

    @Test
    void testMainEntryPoints() {
        FireFightingDroneSimulation sim = new FireFightingDroneSimulation();
        assertNotNull(sim);
    }

    @Test
    void testGlobalStopSignal() {
        endCondition.setStop(false);
        assertFalse(endCondition.shouldStop());
        endCondition.setStop(true);
        assertTrue(endCondition.shouldStop());
    }
}