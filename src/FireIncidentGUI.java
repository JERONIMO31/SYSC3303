
import javax.swing.*;
import java.awt.*;
import java.io.File;

import utils.*;

public class FireIncidentGUI extends JFrame {
    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private JTextField zoneFileField;
    private JButton zoneFileBrowseButton;
    private JTextField eventFileField;
    private JButton eventFileBrowseButton;
    private StandardizedTime standardTime;
    private FireIncident fireIncident;
    private Thread fireIncidentThread;

    public FireIncidentGUI() {
        setTitle("FireIncident");
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

        fireIncidentThread = new Thread(() -> {
            try {
                this.fireIncident = new FireIncident(zoneFile, eventFile, this);
                printMessage("FireIncident initialized.");
                this.fireIncident.mainLoop();
                printMessage("FireIncident finished.");
            } catch (Exception e) {
                printMessage("FireIncident error: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    fireIncident = null;
                    setButtonsEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        }, "FireIncident-Main-Thread");
        fireIncidentThread.start();
    }

    private void stopSimulation() {
        printMessage("Stopping simulation...");
        if (fireIncidentThread != null && fireIncidentThread.isAlive()) {
            fireIncidentThread.interrupt();
        }
        setButtonsEnabled(true);
        stopButton.setEnabled(false);
        this.fireIncident = null;
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
     * Sets the standardized time for timestamped log messages.
     *
     * @param standardTime The standardized time instance
     */
    public void setStandardTime(StandardizedTime standardTime) {
        this.standardTime = standardTime;
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

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            FireIncidentGUI gui = new FireIncidentGUI();
            gui.setVisible(true);
        });
    }
}
