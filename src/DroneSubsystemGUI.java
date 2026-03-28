import javax.swing.*;
import java.awt.*;
import utils.StandardizedTime;

public class DroneSubsystemGUI extends JFrame {
    private static final int AGENT_CAPACITY = 15;
    private static final int SPEED = 15; // m/s
    private static final int ACCELERATION = 5; // m/s^2
    private static final int DEPLOY_RATE = 2; // L/s
    private static final int OPEN_NOZZLE_TIME = 5; // seconds

    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private JTextField totalDronesField;
    private JTextField agentCapacityField;
    private JTextField speedField;
    private JTextField accelerationField;
    private JTextField deployRateField;
    private JTextField openNozzleTimeField;
    private DroneSubsystem droneSubsystem;
    private Thread DroneSubsystemThread;
    private StandardizedTime standardTime;

    public DroneSubsystemGUI() {
        setTitle("DroneSubsystem");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        textArea = new JTextArea(20, 80);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textPanel.add(scrollPane, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Total Drones:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        totalDronesField = new JTextField("10", 20);
        textPanel.add(totalDronesField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Agent Capacity:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        agentCapacityField = new JTextField(String.valueOf(AGENT_CAPACITY), 20);
        textPanel.add(agentCapacityField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Speed:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        speedField = new JTextField(String.valueOf(SPEED), 20);
        textPanel.add(speedField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Acceleration:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        accelerationField = new JTextField(String.valueOf(ACCELERATION), 20);
        textPanel.add(accelerationField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Deploy Rate:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        deployRateField = new JTextField(String.valueOf(DEPLOY_RATE), 20);
        textPanel.add(deployRateField, gbc);

        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Open Nozzle Time:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        openNozzleTimeField = new JTextField(String.valueOf(OPEN_NOZZLE_TIME), 20);
        textPanel.add(openNozzleTimeField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 7;
        gbc.weightx = 0;
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startSimulation());
        textPanel.add(startButton, gbc);

        gbc.gridx = 2;
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());
        textPanel.add(stopButton, gbc);

        add(textPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Starts the drone subsystem simulation with the configured parameters.
     * Validates all input fields and launches on a background thread.
     */
    private void startSimulation() {
        String totalDronesText = totalDronesField.getText();
        String agentCapacityText = agentCapacityField.getText();
        String speedText = speedField.getText();
        String accelerationText = accelerationField.getText();
        String deployRateText = deployRateField.getText();
        String openNozzleTimeText = openNozzleTimeField.getText();

        if (totalDronesText == null || totalDronesText.trim().isEmpty()
                || agentCapacityText == null || agentCapacityText.trim().isEmpty()
                || speedText == null || speedText.trim().isEmpty()
                || accelerationText == null || accelerationText.trim().isEmpty()
                || deployRateText == null || deployRateText.trim().isEmpty()
                || openNozzleTimeText == null || openNozzleTimeText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter values for all fields.", "Missing Input",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int totalDrones;
        final int agentCapacity;
        final int speed;
        final int acceleration;
        final int deployRate;
        final int openNozzleTime;
        try {
            totalDrones = Integer.parseInt(totalDronesText.trim());
            agentCapacity = Integer.parseInt(agentCapacityText.trim());
            speed = Integer.parseInt(speedText.trim());
            acceleration = Integer.parseInt(accelerationText.trim());
            deployRate = Integer.parseInt(deployRateText.trim());
            openNozzleTime = Integer.parseInt(openNozzleTimeText.trim());

            if (totalDrones <= 0 || agentCapacity <= 0 || speed <= 0 || acceleration <= 0
                    || deployRate <= 0 || openNozzleTime <= 0) {
                JOptionPane.showMessageDialog(this, "All values must be greater than 0.",
                        "Invalid Input",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "All fields must be whole numbers.", "Invalid Input",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        stopButton.setEnabled(true);

        printMessage("Starting DroneSubsystem...");
        printMessage("Total Drones: " + totalDrones);
        printMessage("Agent Capacity: " + agentCapacity);
        printMessage("Speed: " + speed);
        printMessage("Acceleration: " + acceleration);
        printMessage("Deploy Rate: " + deployRate);
        printMessage("Open Nozzle Time: " + openNozzleTime);

        DroneSubsystemThread = new Thread(() -> {
            try {
                droneSubsystem = new DroneSubsystem(totalDrones, agentCapacity, speed, acceleration, deployRate,
                        openNozzleTime, this);
                printMessage("DroneSubsystem initialized.");
                droneSubsystem.mainLoop();
            } catch (Exception e) {
                printMessage("DroneSubsystem error: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    droneSubsystem = null;
                    setButtonsEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        }, "DroneSubsystem-Main-Thread");
        DroneSubsystemThread.start();
    }

    /**
     * Stops the running drone subsystem simulation.
     */
    private void stopSimulation() {
        printMessage("Stopping DroneSubsystem...");
        if (DroneSubsystemThread != null && DroneSubsystemThread.isAlive()) {
            DroneSubsystemThread.interrupt();
        }
        droneSubsystem = null;
        setButtonsEnabled(true);
        stopButton.setEnabled(false);
    }

    /**
     * Enables or disables all input fields and the start button.
     *
     * @param enabled true to enable, false to disable
     */
    private void setButtonsEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        totalDronesField.setEnabled(enabled);
        agentCapacityField.setEnabled(enabled);
        speedField.setEnabled(enabled);
        accelerationField.setEnabled(enabled);
        deployRateField.setEnabled(enabled);
        openNozzleTimeField.setEnabled(enabled);
    }

    /**
     * Sets the standardized time for timestamped log messages.
     *
     * @param standardTime The standardized time instance
     */
    public void setStandardTime(StandardizedTime standardTime) {
        this.standardTime = standardTime;
    }

    /**
     * Prints a timestamped message to the text area.
     * Thread-safe method that can be called from any thread.
     *
     * @param message The message to print
     */
    public synchronized void printMessage(String message) {
        SwingUtilities.invokeLater(() -> {
            if (this.standardTime != null) {
                String timeStampedMessage = "[" + this.standardTime.getRelativeTime().toString() + "] " + message;
                textArea.append(timeStampedMessage + "\n");
            } else {
                textArea.append(message + "\n");
            }
        });
    }

    /**
     * Entry point for the DroneSubsystem GUI application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            DroneSubsystemGUI gui = new DroneSubsystemGUI();
            gui.setVisible(true);
        });
    }
}
