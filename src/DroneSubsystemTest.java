import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import drone.DroneInfo;
import drone.LiveDroneTracker;
import event.EventInfo;
import event.EventType;
import event.FaultType;
import event.Intensity;
import udp.Message;
import udp.MessageType;
import utils.StandardizedTime;

public class DroneSubsystemTest {

    private DroneSubsystem subsystem;
    private TestGUI gui;
    private TestTracker tracker;
    private TestDrone drone;

    // -------------------------------
    // Test Stubs
    // -------------------------------

    static class TestGUI extends DroneSubsystemGUI {
        List<String> messages = new ArrayList<>();
        StandardizedTime time;

        public TestGUI() {
            super();
        }

        @Override
        public void printMessage(String msg) {
            messages.add(msg);
        }

        @Override
        public void setStandardTime(StandardizedTime t) {
            this.time = t;
        }
    }


    static class TestDrone extends DroneInfo {
        EventInfo lastAssigned;
        FaultType lastFaultApplied;

        public TestDrone(int id) {
            super(id, 1, 1, 1, 1, 1, null, msg -> {});
        }

        @Override
        public void assignToFire(EventInfo fire) {
            lastAssigned = fire;
        }

        @Override
        public void applyFault(FaultType fault) {
            lastFaultApplied = fault;
        }
    }

    static class TestTracker extends LiveDroneTracker {
        TestDrone drone;

        public TestTracker(TestDrone d) {
            super(1, 1, 1, 1, 1, 1, null, msg -> {});
            this.drone = d;
        }

        @Override
        public DroneInfo getDroneInfo(int id) {
            return id == 1 ? drone : null;
        }

        @Override
        public DroneInfo[] getAllDrones() {
            return new DroneInfo[]{drone};
        }
    }

    private void setPrivate(String field, Object value) throws Exception {
        Field f = DroneSubsystem.class.getDeclaredField(field);
        f.setAccessible(true);
        f.set(subsystem, value);
    }

    private <T> T getPrivate(String field, Class<T> type) {
        try {
            Field f = DroneSubsystem.class.getDeclaredField(field);
            f.setAccessible(true);
            return type.cast(f.get(subsystem));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void invokeHandle(Message msg) {
        try {
            Method m = DroneSubsystem.class.getDeclaredMethod("handleMessage", Message.class);
            m.setAccessible(true);
            m.invoke(subsystem, msg);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void setup() throws Exception {
        gui = new TestGUI();
        drone = new TestDrone(1);
        tracker = new TestTracker(drone);

        subsystem = new DroneSubsystem(tracker, gui, new StandardizedTime(LocalTime.now(), 1));

        // Ensure ready
        setPrivate("readyToStart", true);
    }

    @Test
    public void testInitMessage() throws Exception {
        setPrivate("readyToStart", false);   // <-- FIX

        Message init = new Message(MessageType.INIT);
        init.setData("startTime", "12:00:00");
        init.setData("timeScale", "1");

        invokeHandle(init);

        assertNotNull(gui.time);
        assertTrue(getPrivate("readyToStart", Boolean.class));
    }


    @Test
    public void testAssignment() throws Exception {
        // Ensure subsystem is initialized
        setPrivate("readyToStart", false);
        Message init = new Message(MessageType.INIT);
        init.setData("startTime", "12:00:00");
        init.setData("timeScale", "1");
        invokeHandle(init);

        // Now send assignment
        Message m = new Message(MessageType.ASSIGNMENT);
        m.setData("droneId", "1");
        m.setData("longitude", "100");
        m.setData("latitude", "200");
        m.setData("intensity", "HIGH");
        m.setData("eventType", "FIRE_DETECTED");
        m.setData("time", "12:00:00");
        m.setData("agentRequired", "5");

        invokeHandle(m);

        assertNotNull(drone.lastAssigned);
        assertEquals(100, drone.lastAssigned.longitude);
        assertEquals(200, drone.lastAssigned.latitude);
        assertEquals(Intensity.HIGH, drone.lastAssigned.intensity);
        assertEquals(EventType.FIRE_DETECTED, drone.lastAssigned.eventType);
        assertEquals(5, drone.lastAssigned.getRemainingAgentRequired());
        assertTrue(gui.messages.stream().anyMatch(s -> s.contains("Drone 1 assigned")));
    }

    @Test
    public void testFault() {
        Message m = new Message(MessageType.DRONE_FAULT);
        m.setData("droneId", "1");
        m.setData("faultType", "NOZZLE_STUCK");

        invokeHandle(m);

        assertEquals(FaultType.NOZZLE_STUCK, drone.lastFaultApplied);
    }

    @Test
    public void testUnknownMessage() {
        Message m = new Message(MessageType.DRONE_STATUS);
        invokeHandle(m);

        assertTrue(gui.messages.stream().anyMatch(s -> s.contains("Unknown message type")));
    }

}
