package event;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class LiveFireTrackerTest {

    private LiveFireTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new LiveFireTracker();
    }

    private EventInfo createFire(int lat, int lon, Intensity intensity, int timeSec) {
        return new EventInfo(
                lat,
                lon,
                intensity,
                EventType.FIRE_DETECTED,
                LocalTime.ofSecondOfDay(timeSec)
        );
    }

    @Test
    void testPut_andPeekNextFire_priorityOrder() {
        EventInfo low = createFire(1, 1, Intensity.LOW, 100);
        EventInfo high = createFire(2, 2, Intensity.HIGH, 200);
        EventInfo moderate = createFire(3, 3, Intensity.MODERATE, 50);

        tracker.put(low);
        tracker.put(high);
        tracker.put(moderate);

        EventInfo next = tracker.peekNextFire();

        assertEquals(Intensity.HIGH, next.intensity);
    }

    @Test
    void testPriority_sameIntensity_earlierTimeWins() {
        EventInfo early = createFire(1, 1, Intensity.HIGH, 10);
        EventInfo late = createFire(2, 2, Intensity.HIGH, 100);

        tracker.put(late);
        tracker.put(early);

        EventInfo next = tracker.peekNextFire();

        assertEquals(early, next);
    }

    @Test
    void testAssignFire_movesToBeingFought() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        assertTrue(tracker.getFiresBeingFought().containsKey(fire.getLocationKey()));
    }

    @Test
    void testAssignFire_invalidLocation_doesNothing() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, "invalid");

        assertTrue(tracker.getFiresBeingFought().isEmpty());
    }

    @Test
    void testAssignFire_emptyQueue_doesNothing() {
        tracker.assignFire(1, "1,1");
        assertTrue(tracker.getFiresBeingFought().isEmpty());
    }

    @Test
    void testDeployAgent_updatesRemainingAgent() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        tracker.deployAgent(fire.getLocationKey(), 5);

        int remaining = tracker.getFiresBeingFought()
                .get(fire.getLocationKey())
                .getRemainingAgentRequired();

        assertEquals(5, remaining);
    }

    @Test
    void testDeployAgent_nonExistingFire_doesNothing() {
        tracker.deployAgent("fake", 10);
    }

    @Test
    void testIsExtinguished_true() {
        EventInfo fire = createFire(1, 2, Intensity.LOW, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        fire.setAgent(0);

        assertTrue(tracker.isExtinguished(fire.getLocationKey()));
    }

    @Test
    void testIsExtinguished_false() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        fire.setAgent(10);

        assertFalse(tracker.isExtinguished(fire.getLocationKey()));
    }

    @Test
    void testIsExtinguished_nonExisting_returnsFalse() {
        assertFalse(tracker.isExtinguished("fake"));
    }

    @Test
    void testRequeueFire_movesBackToQueue() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        tracker.requeueFire(fire.getLocationKey());

        assertFalse(tracker.getFiresBeingFought().containsKey(fire.getLocationKey()));
        assertNotNull(tracker.peekNextFire());
    }

    @Test
    void testRequeueFire_invalidKey_doesNothing() {
        tracker.requeueFire("fake");
    }

    @Test
    void testMarkFireAsDead_removesFromActive() {
        EventInfo fire = createFire(1, 2, Intensity.HIGH, 100);

        tracker.put(fire);
        tracker.assignFire(1, fire.getLocationKey());

        tracker.markFireAsDead(fire.getLocationKey());

        assertFalse(tracker.getFiresBeingFought().containsKey(fire.getLocationKey()));
    }

    @Test
    void testMarkFireAsDead_invalidKey_doesNothing() {
        tracker.markFireAsDead("fake");
    }

    @Test
    void testPeekNextFire_emptyQueue_returnsNull() {
        assertNull(tracker.peekNextFire());
    }
}