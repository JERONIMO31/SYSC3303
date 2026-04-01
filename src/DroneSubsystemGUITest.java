import static org.junit.jupiter.api.Assertions.*;

import javax.swing.*;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import utils.StandardizedTime;

public class DroneSubsystemGUITest {

    private DroneSubsystemGUI gui;

    @BeforeEach
    public void setup() {
        gui = new DroneSubsystemGUI();
        gui.setVisible(false);
    }

    private Object getField(String name) throws Exception {
        Field f = DroneSubsystemGUI.class.getDeclaredField(name);
        f.setAccessible(true);
        return f.get(gui);
    }

    private Object callMethod(String name, Class<?>[] paramTypes, Object... args) throws Exception {
        Method m = DroneSubsystemGUI.class.getDeclaredMethod(name, paramTypes);
        m.setAccessible(true);
        return m.invoke(gui, args);
    }

    @Test
    public void testComponentsInitialized() throws Exception {
        assertNotNull(getField("totalDronesField"));
        assertNotNull(getField("agentCapacityField"));
        assertNotNull(getField("speedField"));
        assertNotNull(getField("accelerationField"));
        assertNotNull(getField("deployRateField"));
        assertNotNull(getField("openNozzleTimeField"));
        assertNotNull(getField("startButton"));
        assertNotNull(getField("stopButton"));
        assertNotNull(getField("textArea"));
    }

    @Test
    public void testPrintMessageWithoutTime() throws Exception {
        JTextArea textArea = (JTextArea) getField("textArea");
        callMethod("printMessage", new Class[]{String.class}, "Hello Test");
        Thread.sleep(100); // wait for Swing thread
        assertTrue(textArea.getText().contains("Hello Test"));
    }

    @Test
    public void testPrintMessageWithTime() throws Exception {
        StandardizedTime standardTime = new StandardizedTime(java.time.LocalTime.of(1, 2, 3), 0);
        callMethod("setStandardTime", new Class[]{StandardizedTime.class}, standardTime);

        callMethod("printMessage", new Class[]{String.class}, "Timed Message");

        SwingUtilities.invokeAndWait(() -> {
        });

        // Get textArea content
        JTextArea textArea = (JTextArea) getField("textArea");

        // Assert the message is included
        assertTrue(textArea.getText().contains("Timed Message"),
                "textArea should contain the message with timestamp");
    }

    @Test
    public void testSetButtonsEnabled() throws Exception {
        JButton startButton = (JButton) getField("startButton");
        JTextField totalDronesField = (JTextField) getField("totalDronesField");

        callMethod("setButtonsEnabled", new Class[]{boolean.class}, false);
        assertFalse(startButton.isEnabled());
        assertFalse(totalDronesField.isEnabled());

        callMethod("setButtonsEnabled", new Class[]{boolean.class}, true);
        assertTrue(startButton.isEnabled());
        assertTrue(totalDronesField.isEnabled());
    }

    @Test
    public void testStartSimulationWithInvalidInput() throws Exception {
        JTextField totalDronesField = (JTextField) getField("totalDronesField");
        totalDronesField.setText("abc"); // invalid input

        callMethod("startSimulation", new Class[]{});
        Thread.sleep(100);

        Object thread = getField("DroneSubsystemThread");
        assertNull(thread); // thread should not start
    }

    @Test
    public void testStopSimulation() throws Exception {
        Thread dummyThread = new Thread(() -> {});
        Field threadField = DroneSubsystemGUI.class.getDeclaredField("DroneSubsystemThread");
        threadField.setAccessible(true);
        threadField.set(gui, dummyThread);

        callMethod("stopSimulation", new Class[]{});
        Object droneSubsystem = getField("droneSubsystem");
        assertNull(droneSubsystem);

        JButton startButton = (JButton) getField("startButton");
        JButton stopButton = (JButton) getField("stopButton");
        assertTrue(startButton.isEnabled());
        assertFalse(stopButton.isEnabled());
    }
}