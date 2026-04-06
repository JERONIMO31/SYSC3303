import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.LocalTime;
import java.util.HashMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import event.EventInfo;
import event.EventType;
import event.Intensity;
import event.FaultType;
import utils.StandardizedTime;
import zones.Zone;

public class SchedulerTest {

    private Scheduler scheduler;
    private SchedulerGUI mockGui;

    @BeforeEach
    public void setup() throws Exception {
        mockGui = new SchedulerGUI() {
            @Override
            public void printMessage(String msg) {
            }

            @Override
            public void setZoneMap(HashMap<Integer, Zone> map) {
            }

            @Override
            public void setStandardTime(StandardizedTime t) {
            }

            @Override
            public void addFireEvent(String key, int lon, int lat, Intensity intensity, LocalTime time,
                    int remainingAgent) {
            }

            @Override
            public void updateFireEvent(String key, int remainingAgent) {
            }

            @Override
            public void extinguishFireEvent(String key) {
            }

            @Override
            public void updateDronePositions(String droneData) {
            }
        };

        Constructor<Scheduler> ctor = Scheduler.class.getDeclaredConstructor(int.class, SchedulerGUI.class,
                boolean.class);
        ctor.setAccessible(true);
        scheduler = ctor.newInstance(1, mockGui, true);
    }

    private Object getField(String name) throws Exception {
        Field f = Scheduler.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(scheduler);
    }

    private Object callMethod(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = Scheduler.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(scheduler, args);
    }

    @Test
    public void testNewFireDetected() throws Exception {
        EventInfo fire = new EventInfo(10, 20, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(), FaultType.NONE);
        callMethod("newFireDetected", new Class[] { EventInfo.class }, fire);

        @SuppressWarnings("unchecked")
        HashMap<String, ?> eventMetricsMap = (HashMap<String, ?>) getField("eventMetricsMap");
        assertTrue(eventMetricsMap.containsKey(fire.getLocationKey()));
    }

    @Test
    public void testParseDroneStatus() throws Exception {
        String droneData = "1:100:200:IDLE:NONE:5:10000;2:150:250:TRAVELING_TO_FIRE:NONE:3:8000";
        callMethod("parseDroneStatus", new Class[] { String.class }, droneData);

        @SuppressWarnings("unchecked")
        HashMap<Integer, ?> droneStatusMap = (HashMap<Integer, ?>) getField("droneStatusMap");
        assertEquals(2, droneStatusMap.size());
    }

    @Test
    public void testSelectBestDrone() throws Exception {
        callMethod("parseDroneStatus", new Class[] { String.class }, "1:10:10:IDLE:NONE:5:10000");

        EventInfo fire = new EventInfo(12, 12, Intensity.MODERATE, EventType.FIRE_DETECTED, LocalTime.now(),
                FaultType.NONE);
        callMethod("newFireDetected", new Class[] { EventInfo.class }, fire);

        Object drone = callMethod("selectBestDrone", new Class[] { int.class, int.class, Intensity.class }, 12, 12,
                Intensity.MODERATE);
        assertNotNull(drone);
    }

    @Test
    public void testCanReassignFrom() throws Exception {
        EventInfo oldFire = new EventInfo(10, 10, Intensity.LOW, EventType.FIRE_DETECTED, LocalTime.now(),
                FaultType.NONE);

        Object fireTracker = getField("fireTracker");

        Method getFiresMethod = fireTracker.getClass().getDeclaredMethod("getFiresBeingFought");
        getFiresMethod.setAccessible(true);
        @SuppressWarnings("unchecked")
        HashMap<String, EventInfo> firesBeingFought = (HashMap<String, EventInfo>) getFiresMethod.invoke(fireTracker);

        firesBeingFought.put(oldFire.getLocationKey(), oldFire);

        boolean result = (boolean) callMethod("canReassignFrom", new Class[] { String.class, Intensity.class },
                oldFire.getLocationKey(), Intensity.HIGH);

        assertTrue(result);
    }

    @Test
    public void testParseZoneString() throws Exception {
        String zoneStr = "1:0,0,10,10-2:5,5,15,15";
        callMethod("parseZoneString", new Class[] { String.class }, zoneStr);

        @SuppressWarnings("unchecked")
        HashMap<Integer, ?> zoneMap = (HashMap<Integer, ?>) getField("zoneMap");
        assertEquals(2, zoneMap.size());
    }
}