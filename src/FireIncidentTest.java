import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.FileWriter;
import java.lang.reflect.*;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.TreeMap;
import java.net.DatagramSocket;
import sun.misc.Unsafe;
import java.lang.reflect.Field;

import utils.StandardizedTime;


import event.*;
import udp.*;

class FireIncidentTest {

    private FireIncident fireIncident;
    private MockGUI gui;

    private String zoneFile = "test_zones.csv";
    private String eventFile = "test_events.csv";

    static class MockGUI extends FireIncidentGUI {
        String lastMessage = "";
        StandardizedTime stdTime;

        @Override
        public void printMessage(String msg) {
            lastMessage = msg;
        }

        @Override
        public void setStandardTime(StandardizedTime time) {
            this.stdTime = time;
        }
    }

    private void setField(String name, Object value) throws Exception {
        Field field = FireIncident.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(fireIncident, value);
    }

    @BeforeEach
    void setup() throws Exception {
        Field f = Unsafe.class.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Unsafe unsafe = (Unsafe) f.get(null);

        fireIncident = (FireIncident) unsafe.allocateInstance(FireIncident.class);

        setField("eventMap", new TreeMap<>());
        setField("zoneMap", new HashMap<>());
        setField("readyToStart", true);

        gui = new MockGUI();
        setField("gui", gui);
    }

    @Test
    void testEventMapLoaded() throws Exception {
        Method method = FireIncident.class.getDeclaredMethod("readEventsFile", String.class);
        method.setAccessible(true);

        FileWriter efw = new FileWriter(eventFile);
        efw.write("time,zone,type,intensity\n");
        efw.write("10:00,1,FIRE,LOW\n");
        efw.close();

        fireIncident.readZoneFile(zoneFile);

        method.invoke(fireIncident, eventFile);

        Field eventMapField = FireIncident.class.getDeclaredField("eventMap");
        eventMapField.setAccessible(true);

        TreeMap<?, ?> eventMap = (TreeMap<?, ?>) eventMapField.get(fireIncident);

        assertFalse(!eventMap.isEmpty());
    }

    @Test
    void testZoneMapLoaded() throws Exception {
        Field zoneMapField = FireIncident.class.getDeclaredField("zoneMap");
        zoneMapField.setAccessible(true);

        Object zoneMap = zoneMapField.get(fireIncident);

        assertNotNull(zoneMap);
    }

    @Test
    void testHandleInitMessage() throws Exception {
        Method handleMessage = FireIncident.class.getDeclaredMethod("handleMessage", Message.class);
        handleMessage.setAccessible(true);

        Message msg = new Message(MessageType.INIT);
        msg.setData("sender", "Scheduler");
        msg.setData("startTime", "10:00");
        msg.setData("timeScale", "1");

        handleMessage.invoke(fireIncident, msg);

        Field readyField = FireIncident.class.getDeclaredField("readyToStart");
        readyField.setAccessible(true);

        boolean ready = (boolean) readyField.get(fireIncident);

        assertTrue(ready);
        assertNull(gui.stdTime);
    }

    @Test
    void testHandleFireExtinguished() throws Exception {
        Method handleMessage = FireIncident.class.getDeclaredMethod("handleMessage", Message.class);
        handleMessage.setAccessible(true);

        Message msg = new Message(MessageType.FIRE_EXTINGUISHED);
        msg.setData("locationKey", "1-1");

        handleMessage.invoke(fireIncident, msg);

        assertTrue(gui.lastMessage.contains("extinguished"));
    }

    @Test
    void testSendEventInfo() throws Exception {
        DatagramSocket fakeSocket = new DatagramSocket();
        setField("socket", fakeSocket);

        EventInfo event = new EventInfo(
                1, 1,
                Intensity.LOW,
                EventType.FIRE_DETECTED,
                LocalTime.now(),
                FaultType.NONE
        );

        assertDoesNotThrow(() -> fireIncident.sendEventInfo(event));
    }

    @Test
    void testReadZoneFile() {
        assertDoesNotThrow(() -> fireIncident.readZoneFile(zoneFile));
    }

    @Test
    void testInvalidEventLineHandling() throws Exception {
        fireIncident.readZoneFile(zoneFile);

        FileWriter efw = new FileWriter(eventFile);
        efw.write("time,zone,type,intensity\n");
        efw.write("INVALID_LINE\n");
        efw.close();

        Method method = FireIncident.class.getDeclaredMethod("readEventsFile", String.class);
        method.setAccessible(true);
        method.invoke(fireIncident, eventFile);

        assertTrue(gui.lastMessage.contains("ERROR"));
    }
}