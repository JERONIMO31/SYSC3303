import org.junit.jupiter.api.*;
import javax.swing.*;
import java.lang.reflect.Method;

import utils.StandardizedTime;
import java.time.LocalTime;

import static org.junit.jupiter.api.Assertions.*;

class FireIncidentGUITest {

    private FireIncidentGUI gui;

    @BeforeEach
    void setUp() {
        gui = new FireIncidentGUI();

        gui.setStandardTime(new StandardizedTime(LocalTime.of(0, 0, 0), 1));
    }

    @AfterEach
    void tearDown() {
        gui.dispose();
    }

    @Test
    void testInitialButtonStates() {
        JButton startButton = getPrivateField(gui, "startButton", JButton.class);
        JButton stopButton = getPrivateField(gui, "stopButton", JButton.class);

        assertTrue(startButton.isEnabled());
        assertFalse(stopButton.isEnabled());
    }

    @Test
    void testPrintMessageAppendsText() throws Exception {
        JTextArea textArea = getPrivateField(gui, "textArea", JTextArea.class);

        gui.printMessage("Hello World");
        SwingUtilities.invokeAndWait(() -> {
        });
        assertTrue(textArea.getText().contains("Hello World"));
    }

    @Test
    void testSetButtonsEnabledViaReflection() throws Exception {
        JButton startButton = getPrivateField(gui, "startButton", JButton.class);
        JButton zoneBrowse = getPrivateField(gui, "zoneFileBrowseButton", JButton.class);
        JButton eventBrowse = getPrivateField(gui, "eventFileBrowseButton", JButton.class);

        Method method = FireIncidentGUI.class.getDeclaredMethod("setButtonsEnabled", boolean.class);
        method.setAccessible(true);

        method.invoke(gui, false);
        assertFalse(startButton.isEnabled());
        assertFalse(zoneBrowse.isEnabled());
        assertFalse(eventBrowse.isEnabled());

        method.invoke(gui, true);
        assertTrue(startButton.isEnabled());
        assertTrue(zoneBrowse.isEnabled());
        assertTrue(eventBrowse.isEnabled());
    }

    @Test
    void testStopSimulationViaReflection() throws Exception {
        JButton startButton = getPrivateField(gui, "startButton", JButton.class);
        JButton stopButton = getPrivateField(gui, "stopButton", JButton.class);

        Thread dummyThread = new Thread(() -> {
        });
        setPrivateField(gui, "fireIncidentThread", dummyThread);
        setPrivateField(gui, "fireIncident", null);
        Method stopMethod = FireIncidentGUI.class.getDeclaredMethod("stopSimulation");
        stopMethod.setAccessible(true);
        stopMethod.invoke(gui);

        assertNull(getPrivateField(gui, "fireIncident", Object.class));
        assertTrue(startButton.isEnabled());
        assertFalse(stopButton.isEnabled());
    }

    /** Utility to get private fields */
    @SuppressWarnings("unchecked")
    private <T> T getPrivateField(Object obj, String fieldName, Class<T> clazz) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (T) field.get(obj);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /** Utility to set private fields */
    private void setPrivateField(Object obj, String fieldName, Object value) {
        try {
            var field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(obj, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}