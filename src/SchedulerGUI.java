import javax.swing.*;
import java.awt.*;
import utils.StandardizedTime;

public class SchedulerGUI extends JFrame {
    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private JTextField timeScaleField;
    private Scheduler scheduler;
    private Thread schedulerThread;
    private StandardizedTime standardTime;

    public SchedulerGUI() {
        setTitle("Scheduler");
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
        textPanel.add(new JLabel("Time Scale:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        timeScaleField = new JTextField("1", 20);
        textPanel.add(timeScaleField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 2;
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

    private void startSimulation() {
        String timeScaleText = timeScaleField.getText();
        if (timeScaleText == null || timeScaleText.trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "Please enter a time scale.", "Missing Time Scale",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        final int timeScale;
        try {
            timeScale = Integer.parseInt(timeScaleText.trim());
            if (timeScale <= 0) {
                JOptionPane.showMessageDialog(this, "Time scale must be greater than 0.", "Invalid Time Scale",
                        JOptionPane.ERROR_MESSAGE);
                return;
            }
        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Time scale must be a whole number.", "Invalid Time Scale",
                    JOptionPane.ERROR_MESSAGE);
            return;
        }

        setButtonsEnabled(false);
        stopButton.setEnabled(true);

        printMessage("Starting scheduler...");
        printMessage("Time Scale: " + timeScale);

        schedulerThread = new Thread(() -> {
            try {
                scheduler = new Scheduler(timeScale, this);
                printMessage("Scheduler initialized.");
                scheduler.mainLoop();
            } catch (Exception e) {
                printMessage("Scheduler error: " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    scheduler = null;
                    setButtonsEnabled(true);
                    stopButton.setEnabled(false);
                });
            }
        }, "Scheduler-Main-Thread");
        schedulerThread.start();
    }

    private void stopSimulation() {
        printMessage("Stopping scheduler...");
        if (schedulerThread != null && schedulerThread.isAlive()) {
            schedulerThread.interrupt();
        }
        scheduler = null;
        setButtonsEnabled(true);
        stopButton.setEnabled(false);
    }

    private void setButtonsEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        timeScaleField.setEnabled(enabled);
    }

    public void setStandardTime(StandardizedTime standardTime) {
        this.standardTime = standardTime;
    }

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
            SchedulerGUI gui = new SchedulerGUI();
            gui.setVisible(true);
        });
    }
}
