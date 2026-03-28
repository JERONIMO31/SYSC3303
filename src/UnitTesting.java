//import static org.junit.jupiter.api.Assertions.*;
//import static org.junit.jupiter.api.Assumptions.assumeFalse;
//
//import java.awt.GraphicsEnvironment;
//import java.lang.reflect.Field;
//import java.net.DatagramPacket;
//import java.net.DatagramSocket;
//import java.net.InetAddress;
//import java.nio.charset.StandardCharsets;
//import java.nio.file.Paths;
//import java.time.LocalTime;
//import java.util.HashMap;
//import java.util.Queue;
//
//import javax.swing.JButton;
//import javax.swing.JTextArea;
//import javax.swing.JTextField;
//import javax.swing.SwingUtilities;
//
//import org.junit.jupiter.api.BeforeEach;
//import org.junit.jupiter.api.Test;
//
//import utils.DroneInfo;
//import utils.EndCondition;
//import utils.EventInfo;
//import utils.EventType;
//import utils.Intensity;
//import utils.LiveDroneTracker;
//import utils.LiveFireTracker;
//
//public class UnitTesting {
//
//    private DroneInfo drone;
//    private TestEventInfo fire;
//    private LiveDroneTracker droneTracker;
//    private LiveFireTracker fireTracker;
//
//
//    static class TestEventInfo extends EventInfo {
//        int remainingAgent;
//        Integer assignedDrone = null;
//        int testLatitude;
//        int testLongitude;
//
//        TestEventInfo(int latitude, int longitude, int requiredAgent) {
//            this.testLatitude = latitude;
//            this.testLongitude = longitude;
//            this.remainingAgent = requiredAgent;
//        }
//
//        @Override
//        public synchronized void assignDrone(Integer droneId) {
//            this.assignedDrone = droneId;
//        }
//
//        @Override
//        public synchronized int getRemainingAgentRequired() {
//            return remainingAgent;
//        }
//
//        @Override
//        public synchronized int applyAgent(int amount) {
//            int used = Math.min(amount, remainingAgent);
//            remainingAgent -= used;
//            return used;
//        }
//
//        @Override
//        public synchronized boolean isExtinguished() {
//            return remainingAgent <= 0;
//        }
//
//        @Override
//        public String getLocationKey() {
//            return testLongitude + "," + testLatitude;
//        }
//    }
//
//
//    static class TestGridWithLegend extends GridWithLegend {
//        int updateCalls = 0;
//        int lastQueueSize = -1;
//        int lastAssignedSize = -1;
//
//        TestGridWithLegend(String zoneFilePath, LiveFireTracker fireTracker) {
//            super(zoneFilePath, fireTracker);
//        }
//
//        @Override
//        public void updateFires(Queue<EventInfo> fireQueue, HashMap<String, EventInfo> assignedFires) {
//            updateCalls++;
//            lastQueueSize = fireQueue.size();
//            lastAssignedSize = assignedFires.size();
//        }
//    }
//
//    @BeforeEach
//    void setUp() {
//        drone = new DroneInfo(1);
//        fire = new TestEventInfo(40, 30, 10);
//        droneTracker = new LiveDroneTracker(2);
//        fireTracker = new LiveFireTracker();
//    }
//
//    @Test
//    void testUdpLoopback() throws Exception {
//        byte[] payload = "ping".getBytes(StandardCharsets.UTF_8);
//        try (DatagramSocket receiver = new DatagramSocket(0)) {
//            receiver.setSoTimeout(1000);
//            int port = receiver.getLocalPort();
//            try (DatagramSocket sender = new DatagramSocket()) {
//                DatagramPacket outgoing = new DatagramPacket(
//                        payload,
//                        payload.length,
//                        InetAddress.getLoopbackAddress(),
//                        port);
//                sender.send(outgoing);
//            }
//
//            byte[] buffer = new byte[64];
//            DatagramPacket incoming = new DatagramPacket(buffer, buffer.length);
//            receiver.receive(incoming);
//            String received = new String(incoming.getData(), 0, incoming.getLength(), StandardCharsets.UTF_8);
//            assertEquals("ping", received);
//        }
//    }
//
//    @Test
//    void testAssignDrone() {
//        drone.assignToFire(fire);
//
//        assertFalse(drone.isAvailable());
//        assertEquals(Integer.valueOf(1), fire.assignedDrone);
//        assertEquals("30,40", drone.getAssignedFireLocation());
//    }
//
//    @Test
//    void testTravelToFire() {
//        EventInfo shortFire = new EventInfo(0, 1, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.NOON);
//        drone.assignToFire(shortFire);
//
//        drone.travelToFire();
//
//        assertEquals("(1,0)", drone.getLocationKey());
//    }
//
//    @Test
//    void testTravelHome() {
//        EventInfo homeFire = new EventInfo(0, 0, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.NOON);
//        drone.assignToFire(homeFire);
//
//        drone.travelHome();
//
//        assertTrue(drone.isAvailable());
//        assertEquals("(0,0)", drone.getLocationKey());
//        assertFalse(homeFire.hasDroneAssigned());
//    }
//
//    @Test
//    void testGetReadyDrone() throws InterruptedException {
//        DroneInfo first = droneTracker.getReadyDrone();
//        DroneInfo second = droneTracker.getReadyDrone();
//        DroneInfo noneLeft = droneTracker.getReadyDrone();
//
//        assertNotNull(first);
//        assertNotNull(second);
//        assertNotEquals(first.droneId, second.droneId);
//        assertNull(noneLeft);
//    }
//
//    @Test
//    void testFireTrackerMovesFire() throws InterruptedException {
//        fireTracker.put(fire);
//
//        EventInfo next = fireTracker.getNextEventInfo();
//
//        assertSame(fire, next);
//        assertEquals(0, fireTracker.getFireQueue().size());
//        assertEquals(1, fireTracker.getFiresBeingFought().size());
//        assertEquals(1, fireTracker.getActiveFireCount());
//    }
//
//    @Test
//    void testSchedulerAssignsDrone() throws Exception {
//        assumeFalse(GraphicsEnvironment.isHeadless());
//
//        GUI[] holder = new GUI[1];
//        SwingUtilities.invokeAndWait(() -> holder[0] = new GUI());
//        GUI gui = holder[0];
//
//        LiveFireTracker schedulerFireTracker = new LiveFireTracker();
//        LiveDroneTracker schedulerDroneTracker = new LiveDroneTracker(1);
//        EndCondition schedulerEndCondition = new EndCondition();
//
//        TestEventInfo scheduledFire = new TestEventInfo(10, 20, 1);
//        schedulerFireTracker.put(scheduledFire);
//
//        TestGridWithLegend grid = new TestGridWithLegend(getSampleZoneFilePath(), schedulerFireTracker);
//        setPrivateField(gui, "grid", grid);
//
//        Thread schedulerThread = new Thread(
//                new Scheduler(schedulerDroneTracker, schedulerFireTracker, schedulerEndCondition, gui));
//        schedulerThread.start();
//
//        long deadline = System.currentTimeMillis() + 2000;
//        while (scheduledFire.assignedDrone == null && System.currentTimeMillis() < deadline) {
//            Thread.sleep(10);
//        }
//
//        assertNotNull(scheduledFire.assignedDrone);
//        assertTrue(grid.updateCalls > 0);
//
//        schedulerEndCondition.setStop(true);
//        schedulerThread.interrupt();
//        schedulerThread.join(2000);
//
//        SwingUtilities.invokeAndWait(gui::dispose);
//    }
//
//    @Test
//    void testGuiInitialState() throws Exception {
//        assumeFalse(GraphicsEnvironment.isHeadless());
//
//        GUI[] holder = new GUI[1];
//        SwingUtilities.invokeAndWait(() -> holder[0] = new GUI());
//        GUI gui = holder[0];
//
//        JButton startButton = getPrivateField(gui, "startButton", JButton.class);
//        JButton stopButton = getPrivateField(gui, "stopButton", JButton.class);
//        JTextField zoneFileField = getPrivateField(gui, "zoneFileField", JTextField.class);
//        JTextField eventFileField = getPrivateField(gui, "eventFileField", JTextField.class);
//
//        assertEquals("Fire Fighting Drone Simulation", gui.getTitle());
//        assertTrue(startButton.isEnabled());
//        assertFalse(stopButton.isEnabled());
//        assertTrue(zoneFileField.isEnabled());
//        assertTrue(eventFileField.isEnabled());
//
//        SwingUtilities.invokeAndWait(gui::dispose);
//    }
//
//    @Test
//    void testGuiPrintMessage() throws Exception {
//        assumeFalse(GraphicsEnvironment.isHeadless());
//
//        GUI[] holder = new GUI[1];
//        SwingUtilities.invokeAndWait(() -> holder[0] = new GUI());
//        GUI gui = holder[0];
//
//        gui.printMessage("Hello GUI");
//        SwingUtilities.invokeAndWait(() -> {});
//
//        JTextArea textArea = getPrivateField(gui, "textArea", JTextArea.class);
//        assertTrue(textArea.getText().contains("Hello GUI"));
//
//        SwingUtilities.invokeAndWait(gui::dispose);
//    }
//
//    private static <T> T getPrivateField(Object target, String fieldName, Class<T> type) throws Exception {
//        Field field = target.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        return type.cast(field.get(target));
//    }
//
//    private static void setPrivateField(Object target, String fieldName, Object value) throws Exception {
//        Field field = target.getClass().getDeclaredField(fieldName);
//        field.setAccessible(true);
//        field.set(target, value);
//    }
//
//    private static String getSampleZoneFilePath() {
//        return Paths.get(System.getProperty("user.dir"), "sample_zone_file.csv").toString();
//    }
//}
