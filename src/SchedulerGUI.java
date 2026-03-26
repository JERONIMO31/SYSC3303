import javax.swing.*;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import event.Intensity;
import utils.StandardizedTime;
import zones.Zone;

public class SchedulerGUI extends JFrame {
    private JTextArea textArea;
    private JButton startButton;
    private JButton stopButton;
    private JTextField timeScaleField;
    private JLabel timeDisplayLabel;
    private Timer timeUpdateTimer;
    private ZoneMapPanel zoneMapPanel;
    private Scheduler scheduler;
    private Thread schedulerThread;
    private StandardizedTime standardTime;

    public SchedulerGUI() {
        setTitle("Scheduler");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        JPanel textPanel = new JPanel();
        textPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        timeDisplayLabel = new JLabel("Simulation Time: 00:00");
        timeDisplayLabel.setFont(new Font("Monospaced", Font.BOLD, 16));
        textPanel.add(timeDisplayLabel, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 3;
        gbc.weightx = 1.0;
        gbc.weighty = 1.0;
        gbc.fill = GridBagConstraints.BOTH;
        textArea = new JTextArea(30, 40);
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        textPanel.add(scrollPane, gbc);

        gbc.weighty = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        gbc.weightx = 0;
        textPanel.add(new JLabel("Time Scale:"), gbc);

        gbc.gridx = 1;
        gbc.weightx = 1.0;
        timeScaleField = new JTextField("1", 20);
        textPanel.add(timeScaleField, gbc);

        gbc.gridx = 1;
        gbc.gridy = 3;
        gbc.weightx = 0;
        startButton = new JButton("Start");
        startButton.addActionListener(e -> startSimulation());
        textPanel.add(startButton, gbc);

        gbc.gridx = 2;
        stopButton = new JButton("Stop");
        stopButton.setEnabled(false);
        stopButton.addActionListener(e -> stopSimulation());
        textPanel.add(stopButton, gbc);

        add(textPanel, BorderLayout.WEST);

        zoneMapPanel = new ZoneMapPanel();
        zoneMapPanel.setPreferredSize(new Dimension(700, 600));
        zoneMapPanel.setBorder(BorderFactory.createTitledBorder("Zone Map"));
        add(zoneMapPanel, BorderLayout.CENTER);

        pack();
        setLocationRelativeTo(null);
    }

    /**
     * Starts the scheduler simulation with the configured time scale.
     * Validates input and launches the scheduler on a background thread.
     */
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

    /**
     * Stops the running scheduler simulation and resets the UI.
     */
    private void stopSimulation() {
        printMessage("Stopping scheduler...");
        if (schedulerThread != null && schedulerThread.isAlive()) {
            schedulerThread.interrupt();
        }
        scheduler = null;
        standardTime = null;
        if (timeUpdateTimer != null) {
            timeUpdateTimer.stop();
            timeUpdateTimer = null;
        }
        timeDisplayLabel.setText("Simulation Time: 00:00");
        setButtonsEnabled(true);
        stopButton.setEnabled(false);
    }

    /**
     * Enables or disables the start button and time scale input.
     *
     * @param enabled true to enable, false to disable
     */
    private void setButtonsEnabled(boolean enabled) {
        startButton.setEnabled(enabled);
        timeScaleField.setEnabled(enabled);
    }

    /**
     * Sets the standardized time and starts the time display update timer.
     *
     * @param standardTime The standardized time instance
     */
    public void setStandardTime(StandardizedTime standardTime) {
        this.standardTime = standardTime;
        if (timeUpdateTimer == null) {
            timeUpdateTimer = new Timer(500, e -> updateTimeDisplay());
            timeUpdateTimer.start();
        }
    }

    /**
     * Updates the simulation time display label.
     */
    private void updateTimeDisplay() {
        if (this.standardTime != null) {
            String time = this.standardTime.getRelativeTime().toString();
            timeDisplayLabel.setText("Simulation Time: " + time);
        } else {
            timeDisplayLabel.setText("Simulation Time: 00:00");
        }
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
     * Sets the zone map and updates the zone map panel.
     *
     * @param zoneMap The map of zone IDs to Zone objects
     */
    public void setZoneMap(HashMap<Integer, Zone> zoneMap) {
        SwingUtilities.invokeLater(() -> zoneMapPanel.setZones(zoneMap));
    }

    /**
     * Adds a fire event marker to the zone map.
     *
     * @param locationKey    The fire's location key
     * @param longitude      The fire's longitude
     * @param latitude       The fire's latitude
     * @param intensity      The fire's intensity
     * @param startTime      The time the fire started
     * @param agentRemaining The remaining agent required
     */
    public void addFireEvent(String locationKey, int longitude, int latitude, Intensity intensity, LocalTime startTime,
            int agentRemaining) {
        SwingUtilities.invokeLater(() -> {
            zoneMapPanel.addFire(locationKey, longitude, latitude, intensity, startTime, agentRemaining);
        });
    }

    /**
     * Updates the agent remaining for an existing fire marker.
     *
     * @param locationKey    The fire's location key
     * @param agentRemaining The updated remaining agent
     */
    public void updateFireEvent(String locationKey, int agentRemaining) {
        SwingUtilities.invokeLater(() -> {
            zoneMapPanel.updateFire(locationKey, agentRemaining);
        });
    }

    /**
     * Marks a fire as extinguished on the zone map.
     *
     * @param locationKey The fire's location key
     */
    public void extinguishFireEvent(String locationKey) {
        SwingUtilities.invokeLater(() -> {
            zoneMapPanel.extinguishFire(locationKey,
                    standardTime != null ? standardTime.getRelativeTime() : LocalTime.MIDNIGHT);
        });
    }

    /**
     * Updates drone position markers on the zone map.
     *
     * @param droneData The drone data string in format
     *                  "id:lon:lat:state:fault:agent;..."
     */
    public void updateDronePositions(String droneData) {
        SwingUtilities.invokeLater(() -> zoneMapPanel.updateDrones(droneData));
    }

    private static class FireMarker {
        final int longitude;
        final int latitude;
        final Intensity intensity;
        final LocalTime startTime;
        int agentRemaining;
        LocalTime extinguishedTime;

        FireMarker(int longitude, int latitude, Intensity intensity, LocalTime startTime, int agentRemaining) {
            this.longitude = longitude;
            this.latitude = latitude;
            this.intensity = intensity;
            this.startTime = startTime;
            this.agentRemaining = agentRemaining;
        }
    }

    private static class DroneMarker {
        int id;
        int longitude;
        int latitude;
        String state;
        String fault;
        int agent;

        DroneMarker(int id, int longitude, int latitude, String state, String fault, int agent) {
            this.id = id;
            this.longitude = longitude;
            this.latitude = latitude;
            this.state = state;
            this.fault = fault;
            this.agent = agent;
        }
    }

    /**
     * Custom JPanel that renders zones, fire markers, and drone markers
     * on a scaled coordinate grid.
     */
    private static class ZoneMapPanel extends JPanel {
        private HashMap<Integer, Zone> zones;
        private ConcurrentHashMap<String, FireMarker> fireMarkers = new ConcurrentHashMap<>();
        private volatile DroneMarker[] droneMarkers = new DroneMarker[0];
        private BufferedImage fireImage;

        private static final int DRONE_SIZE = 12;
        private static final int GRID_SPACING = 10;
        private static final int FIRE_MARKER_SIZE = 20;
        private static final int DRONE_BAY_WIDTH = 150;
        private static final Font ZONE_FONT = new Font("SansSerif", Font.BOLD, 12);
        private static final Font INFO_FONT = new Font("SansSerif", Font.PLAIN, 10);
        private static final Font DRONE_FONT = new Font("SansSerif", Font.BOLD, 10);

        public ZoneMapPanel() {
            try {
                fireImage = ImageIO.read(new File("src/images/fire.png"));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        private Color getDroneColor(String state) {
            switch (state) {
                case "IDLE":
                    return new Color(0, 150, 0);
                case "TRAVELING_TO_FIRE":
                    return new Color(0, 100, 255);
                case "EXTINGUISHING":
                    return Color.ORANGE;
                case "TRAVELING_HOME":
                    return new Color(100, 100, 255);
                case "OUT_OF_COMMISSION":
                    return Color.RED;
                default:
                    return Color.GRAY;
            }
        }

        private void drawDroneMarker(Graphics2D g2, DroneMarker drone, int cx, int cy, int labelOffset) {
            Color droneColor = getDroneColor(drone.state);
            g2.setColor(droneColor);
            int[] xPoints = { cx, cx - DRONE_SIZE / 2, cx + DRONE_SIZE / 2 };
            int[] yPoints = { cy - DRONE_SIZE / 2, cy + DRONE_SIZE / 2, cy + DRONE_SIZE / 2 };
            g2.fillPolygon(xPoints, yPoints, 3);
            g2.setColor(Color.BLACK);
            g2.drawPolygon(xPoints, yPoints, 3);

            g2.setFont(DRONE_FONT);
            int dLineHeight = g2.getFontMetrics().getHeight();
            int labelX = cx + DRONE_SIZE / 2 + labelOffset;
            int labelY = cy - DRONE_SIZE / 2;
            g2.setColor(Color.BLACK);
            g2.drawString("Drone" + drone.id, labelX, labelY);
            g2.drawString(drone.state, labelX, labelY + dLineHeight);
            if (!"NONE".equals(drone.fault)) {
                g2.setColor(Color.RED);
                g2.drawString(drone.fault, labelX, labelY + dLineHeight * 2);
                g2.setColor(Color.BLACK);
                g2.drawString("Agent: " + drone.agent, labelX, labelY + dLineHeight * 3);
            } else {
                g2.drawString("Agent: " + drone.agent, labelX, labelY + dLineHeight * 2);
            }
        }

        private void drawFireMarker(Graphics2D g2, FireMarker fire, int fx, int fy) {
            boolean extinguished = fire.extinguishedTime != null;

            if (extinguished) {
                g2.setColor(new Color(100, 100, 100, 150));
                g2.fillOval(fx - FIRE_MARKER_SIZE / 2, fy - FIRE_MARKER_SIZE / 2, FIRE_MARKER_SIZE, FIRE_MARKER_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawOval(fx - FIRE_MARKER_SIZE / 2, fy - FIRE_MARKER_SIZE / 2, FIRE_MARKER_SIZE, FIRE_MARKER_SIZE);
            } else if (fireImage != null) {
                g2.drawImage(fireImage, fx - FIRE_MARKER_SIZE / 2, fy - FIRE_MARKER_SIZE / 2,
                        FIRE_MARKER_SIZE, FIRE_MARKER_SIZE, null);
            } else {
                g2.setColor(Color.RED);
                g2.fillOval(fx - FIRE_MARKER_SIZE / 2, fy - FIRE_MARKER_SIZE / 2, FIRE_MARKER_SIZE, FIRE_MARKER_SIZE);
                g2.setColor(Color.BLACK);
                g2.drawOval(fx - FIRE_MARKER_SIZE / 2, fy - FIRE_MARKER_SIZE / 2, FIRE_MARKER_SIZE, FIRE_MARKER_SIZE);
            }

            g2.setFont(INFO_FONT);
            int lineHeight = g2.getFontMetrics().getHeight();
            int textX = fx + FIRE_MARKER_SIZE / 2 + 2;
            int textY = fy - 2;
            g2.setColor(Color.BLACK);
            g2.drawString("(" + fire.longitude + "," + fire.latitude + ")", textX, textY);
            g2.drawString(fire.intensity.toString(), textX, textY + lineHeight);
            g2.drawString("Start: " + fire.startTime.toString(), textX, textY + lineHeight * 2);
            g2.drawString("Agent: " + fire.agentRemaining, textX, textY + lineHeight * 3);
            if (extinguished) {
                g2.setColor(new Color(0, 128, 0));
                g2.drawString("Out: " + fire.extinguishedTime.toString(), textX, textY + lineHeight * 4);
            }
        }

        private void drawGrid(Graphics2D g2, int offsetX, int offsetY, int maxX, int maxY, double scale) {
            g2.setColor(new Color(220, 220, 220));
            for (int gx = 0; gx <= maxX; gx += GRID_SPACING) {
                int sx = offsetX + (int) (gx * scale);
                g2.drawLine(sx, offsetY, sx, offsetY + (int) (maxY * scale));
            }
            for (int gy = 0; gy <= maxY; gy += GRID_SPACING) {
                int sy = offsetY + (int) (gy * scale);
                g2.drawLine(offsetX, sy, offsetX + (int) (maxX * scale), sy);
            }
        }

        private void drawZones(Graphics2D g2, int offsetX, int offsetY, double scale) {
            for (Zone z : zones.values()) {
                int zx1 = Math.min(z.x1, z.x2);
                int zy1 = Math.min(z.y1, z.y2);
                int zx2 = Math.max(z.x1, z.x2);
                int zy2 = Math.max(z.y1, z.y2);

                int rx = offsetX + (int) (zx1 * scale);
                int ry = offsetY + (int) (zy1 * scale);
                int rw = (int) ((zx2 - zx1) * scale);
                int rh = (int) ((zy2 - zy1) * scale);

                g2.setColor(Color.BLACK);
                g2.drawRect(rx, ry, rw, rh);
                g2.setFont(ZONE_FONT);
                g2.drawString("Zone " + z.zoneId, rx + 4, ry + 15);
            }
        }

        /**
         * Sets the zones to display on the map.
         *
         * @param zones The map of zone IDs to Zone objects
         */
        public void setZones(HashMap<Integer, Zone> zones) {
            this.zones = zones;
            repaint();
        }

        /**
         * Adds a fire marker to the map.
         *
         * @param locationKey    The fire's location key
         * @param longitude      The fire's longitude
         * @param latitude       The fire's latitude
         * @param intensity      The fire's intensity
         * @param startTime      The time the fire started
         * @param agentRemaining The remaining agent required
         */
        public void addFire(String locationKey, int longitude, int latitude, Intensity intensity, LocalTime startTime,
                int agentRemaining) {
            fireMarkers.put(locationKey, new FireMarker(longitude, latitude, intensity, startTime, agentRemaining));
            repaint();
        }

        /**
         * Updates the remaining agent for a fire marker.
         *
         * @param locationKey    The fire's location key
         * @param agentRemaining The updated remaining agent
         */
        public void updateFire(String locationKey, int agentRemaining) {
            FireMarker marker = fireMarkers.get(locationKey);
            if (marker != null) {
                marker.agentRemaining = agentRemaining;
                repaint();
            }
        }

        /**
         * Marks a fire as extinguished with the given time.
         *
         * @param locationKey The fire's location key
         * @param time        The time the fire was extinguished
         */
        public void extinguishFire(String locationKey, LocalTime time) {
            FireMarker marker = fireMarkers.get(locationKey);
            if (marker != null) {
                marker.agentRemaining = 0;
                marker.extinguishedTime = time;
                repaint();
            }
        }

        /**
         * Updates all drone markers from a status data string.
         *
         * @param droneData The drone data string in format
         *                  "id:lon:lat:state:fault:agent;..."
         */
        public void updateDrones(String droneData) {
            if (droneData == null || droneData.isEmpty())
                return;
            String[] entries = droneData.split(";");
            DroneMarker[] markers = new DroneMarker[entries.length];
            for (int i = 0; i < entries.length; i++) {
                String[] parts = entries[i].split(":");
                markers[i] = new DroneMarker(
                        Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        parts[3],
                        parts[4],
                        Integer.parseInt(parts[5]));
            }
            this.droneMarkers = markers;
            repaint();
        }

        /**
         * Paints the zone map including grid lines, zones, fire markers,
         * and drone markers with labels.
         */
        @Override
        protected void paintComponent(Graphics g) {
            super.paintComponent(g);
            if (zones == null || zones.isEmpty()) {
                g.setColor(Color.GRAY);
                g.drawString("No zones loaded", getWidth() / 2 - 40, getHeight() / 2);
                return;
            }

            Graphics2D g2 = (Graphics2D) g;

            Insets insets = getInsets();
            int panelW = getWidth() - insets.left - insets.right - DRONE_BAY_WIDTH;
            int panelH = getHeight() - insets.top - insets.bottom;

            int maxX = 0, maxY = 0;
            for (Zone z : zones.values()) {
                maxX = Math.max(maxX, Math.max(z.x1, z.x2));
                maxY = Math.max(maxY, Math.max(z.y1, z.y2));
            }
            if (maxX == 0 || maxY == 0)
                return;

            double scaleX = (double) panelW / maxX;
            double scaleY = (double) panelH / maxY;
            double scale = Math.min(scaleX, scaleY);

            int offsetX = insets.left + (int) ((panelW - maxX * scale) / 2);
            int offsetY = insets.top + (int) ((panelH - maxY * scale) / 2);

            drawGrid(g2, offsetX, offsetY, maxX, maxY, scale);
            drawZones(g2, offsetX, offsetY, scale);

            for (FireMarker fire : fireMarkers.values()) {
                int fx = offsetX + (int) (fire.longitude * scale);
                int fy = offsetY + (int) (fire.latitude * scale);
                drawFireMarker(g2, fire, fx, fy);
            }

            java.util.List<DroneMarker> bayDrones = new java.util.ArrayList<>();
            for (DroneMarker drone : droneMarkers) {
                if ("IDLE".equals(drone.state) || "OUT_OF_COMMISSION".equals(drone.state)) {
                    bayDrones.add(drone);
                    continue;
                }
                int dx = offsetX + (int) (drone.longitude * scale);
                int dy = offsetY + (int) (drone.latitude * scale);
                drawDroneMarker(g2, drone, dx, dy, 2);
            }

            // Draw Drone Bay on the right for IDLE and OUT_OF_COMMISSION drones
            int bayX = insets.left + panelW + 5;
            int bayY = offsetY;
            int bayHeight = (int) (maxY * scale);

            g2.setColor(new Color(240, 240, 240));
            g2.fillRect(bayX, bayY, DRONE_BAY_WIDTH - 10, bayHeight);
            g2.setColor(Color.BLACK);
            g2.drawRect(bayX, bayY, DRONE_BAY_WIDTH - 10, bayHeight);
            g2.setFont(ZONE_FONT);
            g2.drawString("Drone Bay", bayX + 4, bayY + 15);

            g2.setFont(DRONE_FONT);
            int dLineHeight = g2.getFontMetrics().getHeight();
            int bayEntryHeight = dLineHeight * 4 + 5;
            int bayCurrentY = bayY + 25;

            for (DroneMarker drone : bayDrones) {
                drawDroneMarker(g2, drone, bayX + 15, bayCurrentY + DRONE_SIZE, 4);
                bayCurrentY += bayEntryHeight;
            }
        }
    }

    /**
     * Entry point for the Scheduler GUI application.
     *
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            SchedulerGUI gui = new SchedulerGUI();
            gui.setVisible(true);
        });
    }
}
