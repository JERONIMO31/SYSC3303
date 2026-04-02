import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;
import java.lang.reflect.*;
import java.time.LocalTime;
import java.util.Map;
import java.util.HashMap;

import event.Intensity;
import utils.StandardizedTime;
import zones.Zone;

class SchedulerGUITest {

    private SchedulerGUI gui;

    @BeforeEach
    void setUp() {
        gui = new SchedulerGUI();
    }

    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object obj, String fieldName, Class<T> clazz) throws Exception {
        Field field = obj.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return (T) field.get(obj);
    }

    @Test
    void testSetStandardTimeUpdatesLabel() throws Exception {
        StandardizedTime testTime = new StandardizedTime(LocalTime.of(12, 34), 1) {
            @Override
            public LocalTime getRelativeTime() {
                return LocalTime.of(12, 34);
            }
        };

        gui.setStandardTime(testTime);

        Method updateTimeDisplay = SchedulerGUI.class.getDeclaredMethod("updateTimeDisplay");
        updateTimeDisplay.setAccessible(true);
        updateTimeDisplay.invoke(gui);

        JLabel timeLabel = getPrivateField(gui, "timeDisplayLabel", JLabel.class);
        assertEquals("Simulation Time: 12:34", timeLabel.getText());
    }

    @Test
    void testPrintMessageAddsText() throws Exception {
        gui.printMessage("Hello World");

        JTextArea textArea = getPrivateField(gui, "textArea", JTextArea.class);

        Thread.sleep(100);

        String content = textArea.getText();
        assertTrue(content.contains("Hello World"));
    }

    @Test
    void testAddUpdateFireEvent() throws Exception {
        gui.addFireEvent("F1", 10, 20, Intensity.LOW, LocalTime.of(12, 0), 50);

        Object zonePanelObj = getPrivateField(gui, "zoneMapPanel", Object.class);
        Map<String, Object> fireMarkers = getPrivateField(zonePanelObj, "fireMarkers", Map.class);

        assertNotNull(fireMarkers.get("F1"));

        Object fireMarker = fireMarkers.get("F1");
        Field agentField = fireMarker.getClass().getDeclaredField("agentRemaining");
        agentField.setAccessible(true);

        assertEquals(50, agentField.getInt(fireMarker));

        gui.updateFireEvent("F1", 30);
        Thread.sleep(100);

        assertEquals(30, agentField.getInt(fireMarker));
    }

    @Test
    void testExtinguishFireEvent() throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            gui.addFireEvent("F2", 5, 15, Intensity.HIGH, LocalTime.of(10, 0), 100);
        });

        Object zonePanelObj = getPrivateField(gui, "zoneMapPanel", Object.class);

        Field fireMarkersField = zonePanelObj.getClass().getDeclaredField("fireMarkers");
        fireMarkersField.setAccessible(true);

        SwingUtilities.invokeAndWait(() -> {
            gui.extinguishFireEvent("F2");
        });

        Map<String, Object> fireMarkers = (Map<String, Object>) fireMarkersField.get(zonePanelObj);
        Object fireMarker = fireMarkers.get("F2");
        assertNotNull(fireMarker, "FireMarker F2 should exist after extinguish");

        Field agentField = fireMarker.getClass().getDeclaredField("agentRemaining");
        agentField.setAccessible(true);
        assertEquals(0, agentField.getInt(fireMarker), "agentRemaining should be 0 after extinguish");

        Field extinguishedField = fireMarker.getClass().getDeclaredField("extinguishedTime");
        extinguishedField.setAccessible(true);
        assertNotNull(extinguishedField.get(fireMarker), "extinguishedTime should be set after extinguish");
    }

    @Test
    void testZoneMapPanelSetZones() throws Exception {
        Object zonePanelObj = getPrivateField(gui, "zoneMapPanel", Object.class);

        Zone z1 = new Zone(1, 0, 0, 50, 50);
        Zone z2 = new Zone(2, 10, 10, 60, 60);

        Map<Integer, Zone> zones = new HashMap<>();
        zones.put(1, z1);
        zones.put(2, z2);

        Field zonesField = zonePanelObj.getClass().getDeclaredField("zones");
        zonesField.setAccessible(true);
        zonesField.set(zonePanelObj, zones);

        Map<Integer, Zone> internalZones = (Map<Integer, Zone>) zonesField.get(zonePanelObj);
        assertEquals(2, internalZones.size());
        assertTrue(internalZones.containsKey(1));
        assertTrue(internalZones.containsKey(2));
    }
}