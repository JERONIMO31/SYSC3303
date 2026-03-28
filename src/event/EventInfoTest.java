package event;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;
import java.time.LocalTime;

class EventInfoTest {

    @Test
    void testConstructor_DefaultAgentFromIntensity() {
        EventInfo high = new EventInfo(1, 2, Intensity.HIGH, EventType.FIRE_DETECTED, LocalTime.now());
        EventInfo moderate = new EventInfo(1, 2, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now());
        EventInfo low = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        assertEquals(30, high.getRemainingAgentRequired());
        assertEquals(20, moderate.getRemainingAgentRequired());
        assertEquals(10, low.getRemainingAgentRequired());
    }

    @Test
    void testConstructor_WithExplicitAgent() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), 50);
        assertEquals(50, event.getRemainingAgentRequired());
    }

    @Test
    void testConstructor_WithNegativeAgentClampedToZero() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), -10);
        assertEquals(0, event.getRemainingAgentRequired());
    }

    @Test
    void testConstructor_NullFaultDefaultsToNone() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), null, null);
        assertEquals(FaultType.NONE, event.faultType);
    }

    @Test
    void testGetLocationKey() {
        EventInfo event = new EventInfo(10, 20, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
        assertEquals("20,10", event.getLocationKey());
    }

    @Test
    void testApplyAgent_NormalCase() {
        EventInfo event = new EventInfo(1, 2, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now());
        int used = event.applyAgent(5);

        assertEquals(5, used);
        assertEquals(15, event.getRemainingAgentRequired());
    }

    @Test
    void testApplyAgent_OverApply() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
        int used = event.applyAgent(20);

        assertEquals(10, used); // only what was needed
        assertEquals(0, event.getRemainingAgentRequired());
    }

    @Test
    void testIsExtinguished() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
        assertFalse(event.isExtinguished());

        event.applyAgent(10);
        assertTrue(event.isExtinguished());
    }

    @Test
    void testSetAgent() {
        EventInfo event = new EventInfo();
        event.setAgent(99);
        assertEquals(99, event.getRemainingAgentRequired());
    }

    @Test
    void testDroneAssignment() {
        EventInfo event = new EventInfo();

        assertFalse(event.hasDroneAssigned());

        event.assignDrone(42);
        assertTrue(event.hasDroneAssigned());

        event.assignDrone(null);
        assertFalse(event.hasDroneAssigned());
    }

    @Test
    void testToStringContainsImportantFields() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.of(10, 30));
        String str = event.toString();

        assertTrue(str.contains("location=(2,1)"));
        assertTrue(str.contains("LOW"));
        assertTrue(str.contains("FIRE_DETECTED"));
    }

    @Test
    void testFaultHandling() {
        EventInfo event = new EventInfo();

        assertFalse(event.isFaultHandled());

        event.markFaultHandled();
        assertTrue(event.isFaultHandled());
    }
}