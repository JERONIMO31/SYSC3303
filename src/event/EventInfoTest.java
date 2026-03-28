package event;

import org.junit.jupiter.api.Test;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

public class EventInfoTest {

    // -------------------------------
    // CONSTRUCTOR TESTS
    // -------------------------------

    @Test
    void testDefaultAgentFromIntensity() {
        EventInfo high = new EventInfo(0, 0, Intensity.HIGH, EventType.FIRE_DETECTED, LocalTime.now());
        EventInfo moderate = new EventInfo(0, 0, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now());
        EventInfo low = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        assertEquals(30, high.getRemainingAgentRequired());
        assertEquals(20, moderate.getRemainingAgentRequired());
        assertEquals(10, low.getRemainingAgentRequired());
    }

    @Test
    void testExplicitAgentOverridesIntensity() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), 50);
        assertEquals(50, event.getRemainingAgentRequired());
    }

    @Test
    void testNegativeAgentClampedToZero() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), -5);
        assertEquals(0, event.getRemainingAgentRequired());
    }

    @Test
    void testNullFaultDefaultsToNone() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), null, null);
        assertEquals(FaultType.NONE, event.faultType);
    }

    // -------------------------------
    // LOCATION + BASIC METHODS
    // -------------------------------

    @Test
    void testGetLocationKey() {
        EventInfo event = new EventInfo(10, 20, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());
        assertEquals("20,10", event.getLocationKey());
    }

    @Test
    void testToStringContainsFields() {
        EventInfo event = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.of(12, 0));
        String str = event.toString();

        assertTrue(str.contains("location=(2,1)"));
        assertTrue(str.contains("LOW"));
        assertTrue(str.contains("FIRE_DETECTED"));
    }

    // -------------------------------
    // AGENT LOGIC
    // -------------------------------

    @Test
    void testApplyAgentNormal() {
        EventInfo event = new EventInfo(0, 0, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now());

        int used = event.applyAgent(5);

        assertEquals(5, used);
        assertEquals(15, event.getRemainingAgentRequired());
    }

    @Test
    void testApplyAgentExact() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        int used = event.applyAgent(10);

        assertEquals(10, used);
        assertEquals(0, event.getRemainingAgentRequired());
        assertTrue(event.isExtinguished());
    }

    @Test
    void testApplyAgentOverkill() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        int used = event.applyAgent(50);

        assertEquals(10, used); // only what was needed
        assertEquals(0, event.getRemainingAgentRequired());
    }

    @Test
    void testIsExtinguished() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        assertFalse(event.isExtinguished());

        event.applyAgent(10);

        assertTrue(event.isExtinguished());
    }

    @Test
    void testSetAgent() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        event.setAgent(99);

        assertEquals(99, event.getRemainingAgentRequired());
    }

    // -------------------------------
    // DRONE ASSIGNMENT
    // -------------------------------

    @Test
    void testAssignDrone() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        event.assignDrone(42);

        assertTrue(event.toString().contains("42"));

        event.assignDrone(null);

        assertTrue(event.toString().contains("False"));
    }

    // -------------------------------
    // FAULT HANDLING
    // -------------------------------

    @Test
    void testFaultInitiallyNotHandled() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        assertFalse(event.isFaultHandled());
    }

    @Test
    void testMarkFaultHandled() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        event.markFaultHandled();

        assertTrue(event.isFaultHandled());
    }

    @Test
    void testFaultHandledIdempotent() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        event.markFaultHandled();
        event.markFaultHandled();

        assertTrue(event.isFaultHandled()); // still true, no side effects
    }

    // -------------------------------
    // EDGE CASES
    // -------------------------------

    @Test
    void testApplyAgentZero() {
        EventInfo event = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now());

        int used = event.applyAgent(0);

        assertEquals(0, used);
        assertEquals(10, event.getRemainingAgentRequired());
    }

    @Test
    void testMultipleAgentApplications() {
        EventInfo event = new EventInfo(0, 0, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now());

        event.applyAgent(5);
        event.applyAgent(5);
        event.applyAgent(10);

        assertTrue(event.isExtinguished());
    }
}