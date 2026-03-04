import javax.swing.*;

import drone.DroneInfo;
import drone.LiveDroneTracker;
import event.LiveFireTracker;

import java.awt.*;
import java.io.File;

import utils.*;

public class GUI extends JFrame {

    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private JTextField zoneFileField;
    private JButton zoneFileBrowseButton;
    private JTextField eventFileField;
    private JButton eventFileBrowseButton;
    private StandardizedTime standardTime;
    private DroneSubsystem drone;
    private FireIncident fireIncident;
    private Scheduler scheduler;
    private EndCondition endCondition;
    private Thread droneThread;
    private Thread incidentThread;
    private Thread schedulerThread;
    private GridWithLegend grid;

    /**
     * Constructs the GUI window for the fire fighting drone simulation.
     * Initializes all UI components including text area, file selectors, and
     * control buttons.
     */
    public GUI() {
        setTitle("Fire Fighting Drone Simulation");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new GridLayout());
        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Output text area
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

        // Zone file
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Zone File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        zoneFileField = new JTextField(20);
        textPanel.add(zoneFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        zoneFileBrowseButton = new JButton("Browse");
        zoneFileBrowseButton.addActionListener(e -> browseFile(zoneFileField));
        textPanel.add(zoneFileBrowseButton, gbc);

        // Event file
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        textPanel.add(new JLabel("Event File:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        eventFileField = new JTextField(20);
        textPanel.add(eventFileField, gbc);

        gbc.gridx = 2;
        gbc.weightx = 0;
        eventFileBrowseButton = new JButton("Browse");
        eventFileBrowseButton.addActionListener(e -> browseFile(eventFileField));
        textPanel.add(eventFileBrowseButton, gbc);

        // Start button
        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startSimulation());
        textPanel.add(startButton, gbc);

        // Stop button
        gbc.gridx = 2;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());
        textPanel.add(stopButton, gbc);

        // add(new GridWithLegend(""));
        add(textPanel);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Opens a file chooser dialog to select a file.
     * Updates the target text field with the selected file path.
     * 
     * @param targetField The text field to update with the selected file path
     */
    private void browseFile(JTextField targetField) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setCurrentDirectory(new File(System.getProperty("user.dir")));
        int result = fileChooser.showOpenDialog(this);
        if (result == JFileChooser.APPROVE_OPTION) {
            File selectedFile = fileChooser.getSelectedFile();
            targetField.setText(selectedFile.getAbsolutePath());
        }
    }

    /**
     * Starts the simulation with the selected zone and event files.
     * Validates file paths, initializes all simulation components,
     * and starts the drone, scheduler, and fire incident threads.
     * Disables control buttons during simulation.
     */
    private void startSimulation() {
        String zoneFile = zoneFileField.getText();
        String eventFile = eventFileField.getText();

        // Validate file paths
        if (zoneFile == null || zoneFile.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select a zone file.", "Missing Zone File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (eventFile == null || eventFile.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please select an event file.", "Missing Event File",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!new File(zoneFile).exists()) {
            JOptionPane.showMessageDialog(this, "Zone file does not exist: " + zoneFile, "File Not Found",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }
        if (!new File(eventFile).exists()) {
            JOptionPane.showMessageDialog(this, "Event file does not exist: " + eventFile, "File Not Found",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Disable buttons during simulation
        setButtonsEnabled(false);
        stopButton.setEnabled(true);

        printMessage("Starting simulation...");
        printMessage("Zone File: " + zoneFile);
        printMessage("Event File: " + eventFile);

        this.standardTime = new StandardizedTime(java.time.LocalTime.now());
        this.endCondition = new EndCondition();
        LiveFireTracker fireTracker = new LiveFireTracker();
        LiveDroneTracker droneTracker = new LiveDroneTracker(1);
        DroneInfo[] allDrones = droneTracker.getAllDrones();
        DroneInfo droneInfo = allDrones[0];
        droneTracker.markDroneAsReady(droneInfo.droneId);
        drone = new DroneSubsystem(droneInfo, droneTracker, endCondition, this);
        droneThread = new Thread(drone);
        fireIncident = new FireIncident(fireTracker, zoneFile, eventFile, endCondition, standardTime, this);
        incidentThread = new Thread(fireIncident);
        scheduler = new Scheduler(droneTracker, fireTracker, endCondition, this);
        schedulerThread = new Thread(scheduler);
        if (grid == null) {
            grid = new GridWithLegend(zoneFile, fireTracker);
            add(grid);
            pack();
        } else {
            grid.replaceZoneFile(zoneFile);
        }

        droneThread.start();
        incidentThread.start();
        schedulerThread.start();

        // Monitor threads and re-enable buttons when all are done
        new Thread(() -> {
            try {
                droneThread.join();
                incidentThread.join();
                schedulerThread.join();
                SwingUtilities.invokeLater(() -> {
                    setButtonsEnabled(true);
                    stopButton.setEnabled(false);
                    printMessage("Simulation completed.");
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    /**
     * Stops the currently running simulation.
     * Sets the end condition flag to signal all threads to stop.
     */
    private void stopSimulation() {
        if (endCondition != null) {
            endCondition.setStop(true);
        }
        if (droneThread != null) {
            droneThread.interrupt();
        }
        if (incidentThread != null) {
            incidentThread.interrupt();
        }
        if (schedulerThread != null) {
            schedulerThread.interrupt();
        }
        printMessage("Stopping simulation...");
    }

    /**
     * Enables or disables the control buttons and input fields.
     * 
     * @param enabled true to enable controls, false to disable
     */
    private void setButtonsEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        zoneFileBrowseButton.setEnabled(enabled);
        eventFileBrowseButton.setEnabled(enabled);
        zoneFileField.setEnabled(enabled);
        eventFileField.setEnabled(enabled);
    }

    /**
     * Prints a message to the GUI text area with timestamp.
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

    public void updateEvents(LiveFireTracker fireTracker) {
        grid.updateFires(fireTracker.getFireQueue(), fireTracker.getFiresBeingFought());
    }
}
