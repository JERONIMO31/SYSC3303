import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;

import java.awt.GraphicsEnvironment;
import java.lang.reflect.Field;
import java.time.LocalTime;

import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import utils.DroneInfo;
import utils.EndCondition;
import utils.EventInfo;
import utils.EventType;
import utils.Intensity;
import utils.LiveDroneTracker;
import utils.LiveFireTracker;
import utils.standardizedTime;

public class UnitTesting {

    private DroneInfo drone;
    private TestEventInfo fire;
    private LiveDroneTracker droneTracker;
    private LiveFireTracker fireTracker;
    private EndCondition endCondition;

    /**
     * Lightweight test double for EventInfo so unit tests can control agent
     * requirements and assignment behavior without relying on timing-heavy
     * simulation logic.
     */
    static class TestEventInfo extends EventInfo {
        int remainingAgent;
        Integer assignedDrone = null;
        int testLatitude;
        int testLongitude;

        TestEventInfo(int latitude, int longitude, int requiredAgent) {
            this.testLatitude = latitude;
            this.testLongitude = longitude;
            this.remainingAgent = requiredAgent;
        }

        @Override
        public synchronized void assignDrone(Integer droneId) {
            this.assignedDrone = droneId;
        }

        @Override
        public synchronized int getRemainingAgentRequired() {
            return remainingAgent;
        }

        @Override
        public synchronized int applyAgent(int amount) {
            int used = Math.min(amount, remainingAgent);
            remainingAgent -= used;
            return used;
        }

        @Override
        public synchronized boolean isExtinguished() {
            return remainingAgent <= 0;
        }

        @Override
        public String getLocationKey() {
            return testLongitude + "," + testLatitude;
        }
    }

    @BeforeEach
    void setUp() {
        drone = new DroneInfo(1);
        fire = new TestEventInfo(40, 30, 10);
        droneTracker = new LiveDroneTracker(2);
        fireTracker = new LiveFireTracker();
        endCondition = new EndCondition();
    }

    @Test
    void droneInfo_startsAvailableAtHomeWithNoAssignedFire() {
        assertTrue(drone.isAvailable());
        assertEquals("(0,0)", drone.getLocationKey());
        assertEquals("No fire assigned", drone.getAssignedFireLocation());
        assertFalse(drone.isFireExtinguished());
    }

    @Test
    void droneInfo_assignToFire_marksDroneBusyAndAssignsFire() {
        drone.assignToFire(fire);

        assertFalse(drone.isAvailable());
        assertEquals(Integer.valueOf(1), fire.assignedDrone);
        assertEquals("30,40", drone.getAssignedFireLocation());
    }

    @Test
    void droneInfo_deployAgent_returnsZeroWhenNoFireAssigned() throws InterruptedException {
        assertEquals(0, drone.deployAgent());
    }

    @Test
    void droneInfo_travelTimeIsZeroWithoutAssignment() {
        assertEquals(0.0, drone.getTravelTime());
    }

    @Test
    void droneInfo_travelTimeIsPositiveWhenFireAssigned() {
        drone.assignToFire(fire);

        assertTrue(drone.getTravelTime() > 0);
    }

    @Test
    void droneInfo_waitForWorkReturnsAfterAssignmentNotification() throws Exception {
        Thread assigner = new Thread(() -> {
            try {
                Thread.sleep(100);
                drone.assignToFire(fire);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });

        assigner.start();
        drone.waitForWork(1000);
        assigner.join();

        assertFalse(drone.isAvailable());
        assertEquals(Integer.valueOf(1), fire.assignedDrone);
    }

    @Test
    void eventInfo_constructorSetsAgentRequirementByIntensity() {
        EventInfo low = new EventInfo(1, 2, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.NOON);
        EventInfo moderate = new EventInfo(1, 2, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.NOON);
        EventInfo high = new EventInfo(1, 2, Intensity.HIGH, EventType.FIRE_DETECTED, LocalTime.NOON);

        assertEquals(10, low.getRemainingAgentRequired());
        assertEquals(20, moderate.getRemainingAgentRequired());
        assertEquals(30, high.getRemainingAgentRequired());
    }

    @Test
    void eventInfo_applyAgentReducesRemainingAgentAndExtinguishesFire() {
        EventInfo event = new EventInfo(5, 6, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.NOON);

        assertEquals(4, event.applyAgent(4));
        assertEquals(6, event.getRemainingAgentRequired());
        assertFalse(event.isExtinguished());

        assertEquals(6, event.applyAgent(10));
        assertEquals(0, event.getRemainingAgentRequired());
        assertTrue(event.isExtinguished());
    }

    @Test
    void eventInfo_assignmentStateChangesWithAssignAndUnassign() {
        EventInfo event = new EventInfo(5, 6, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.NOON);

        assertFalse(event.hasDroneAssigned());

        event.assignDrone(7);
        assertTrue(event.hasDroneAssigned());

        event.assignDrone(null);
        assertFalse(event.hasDroneAssigned());
    }

    @Test
    void eventInfo_locationKeyUsesLongitudeThenLatitude() {
        EventInfo event = new EventInfo(12, 34, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.NOON);

        assertEquals("34,12", event.getLocationKey());
    }

    @Test
    void liveDroneTracker_initializesAllDronesAndProvidesLookup() {
        DroneInfo[] drones = droneTracker.getAllDrones();

        assertEquals(2, drones.length);
        assertNotNull(droneTracker.getDroneInfo(0));
        assertNotNull(droneTracker.getDroneInfo(1));
        assertNull(droneTracker.getDroneInfo(99));
    }

    @Test
    void liveDroneTracker_getReadyDroneReturnsDistinctDronesUntilExhausted() throws InterruptedException {
        DroneInfo first = droneTracker.getReadyDrone();
        DroneInfo second = droneTracker.getReadyDrone();
        DroneInfo noneLeft = droneTracker.getReadyDrone();

        assertNotNull(first);
        assertNotNull(second);
        assertNotEquals(first.droneId, second.droneId);
        assertNull(noneLeft);
    }

    @Test
    void liveDroneTracker_markDroneAsReadyMakesBusyDroneAvailableAgain() throws InterruptedException {
        DroneInfo first = droneTracker.getReadyDrone();
        DroneInfo second = droneTracker.getReadyDrone();

        assertNotNull(first);
        assertNotNull(second);
        assertNull(droneTracker.getReadyDrone());

        droneTracker.markDroneAsReady(first.droneId);

        DroneInfo availableAgain = droneTracker.getReadyDrone();
        assertNotNull(availableAgain);
        assertEquals(first.droneId, availableAgain.droneId);
    }

    @Test
    void liveFireTracker_putAndGetNextEventMovesFireToBeingFought() throws InterruptedException {
        fireTracker.put(fire);

        EventInfo next = fireTracker.getNextEventInfo();

        assertSame(fire, next);
        assertEquals(0, fireTracker.getFireQueue().size());
        assertEquals(1, fireTracker.getFiresBeingFought().size());
        assertEquals(1, fireTracker.getActiveFireCount());
    }

    @Test
    void liveFireTracker_prioritizesUnassignedActiveFireAlreadyBeingFought() throws InterruptedException {
        fireTracker.put(fire);
        EventInfo firstRetrieved = fireTracker.getNextEventInfo();

        assertSame(fire, firstRetrieved);

        EventInfo secondRetrieved = fireTracker.getNextEventInfo();
        assertSame(fire, secondRetrieved);
    }

    @Test
    void liveFireTracker_markFireAsDeadRemovesItFromActiveTracking() throws InterruptedException {
        fireTracker.put(fire);
        fireTracker.getNextEventInfo();

        fireTracker.markFireAsDead(fire.getLocationKey());

        assertEquals(0, fireTracker.getActiveFireCount());
        assertFalse(fireTracker.getFiresBeingFought().containsKey(fire.getLocationKey()));
    }

    @Test
    void liveFireTracker_updateLiveFiresRemovesExtinguishedFires() throws InterruptedException {
        fireTracker.put(fire);
        fireTracker.getNextEventInfo();

        fire.applyAgent(10);
        assertTrue(fire.isExtinguished());

        fireTracker.updateLiveFires();

        assertEquals(0, fireTracker.getActiveFireCount());
        assertFalse(fireTracker.getFiresBeingFought().containsKey(fire.getLocationKey()));
    }

    @Test
    void endCondition_defaultsFalseAndCanBeToggled() {
        assertFalse(endCondition.shouldStop());

        endCondition.setStop(true);
        assertTrue(endCondition.shouldStop());

        endCondition.setStop(false);
        assertFalse(endCondition.shouldStop());
    }

    @Test
    void standardizedTime_returnsAReasonableRelativeTime() {
        standardizedTime time = new standardizedTime(LocalTime.now().minusSeconds(1));
        LocalTime relative = time.getRelativeTime();

        assertNotNull(relative);
        assertTrue(relative.getHour() == 0 || relative.getHour() == 23);
    }

    @Test
    void gui_initialState_hasExpectedTitleAndButtonStates() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());

        GUI[] holder = new GUI[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new GUI());
        GUI gui = holder[0];

        JButton startButton = getPrivateField(gui, "startButton", JButton.class);
        JButton stopButton = getPrivateField(gui, "stopButton", JButton.class);
        JTextField zoneFileField = getPrivateField(gui, "zoneFileField", JTextField.class);
        JTextField eventFileField = getPrivateField(gui, "eventFileField", JTextField.class);

        assertEquals("Fire Fighting Drone Simulation", gui.getTitle());
        assertTrue(startButton.isEnabled());
        assertFalse(stopButton.isEnabled());
        assertTrue(zoneFileField.isEnabled());
        assertTrue(eventFileField.isEnabled());

        SwingUtilities.invokeAndWait(gui::dispose);
    }

    @Test
    void gui_printMessage_appendsToTextArea() throws Exception {
        assumeFalse(GraphicsEnvironment.isHeadless());

        GUI[] holder = new GUI[1];
        SwingUtilities.invokeAndWait(() -> holder[0] = new GUI());
        GUI gui = holder[0];

        gui.printMessage("Hello GUI");
        SwingUtilities.invokeAndWait(() -> {});

        JTextArea textArea = getPrivateField(gui, "textArea", JTextArea.class);
        assertTrue(textArea.getText().contains("Hello GUI"));

        SwingUtilities.invokeAndWait(gui::dispose);
    }

    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
        Field field = target.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return type.cast(field.get(target));
    }
}
