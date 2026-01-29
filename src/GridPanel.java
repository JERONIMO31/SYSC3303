import javax.swing.*;
import java.awt.*;
import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

import utils.Zone;

/**
 * GridPanel is a custom Swing panel that renders a resizable 2D grid.
 * 
 * Features:
 * - Fixed-size grid of cells with borders
 * - Per-cell background colors and text labels
 * - Zone visualization using thick borders
 * - Automatic zone labeling
 * - Event marking at the center cell of each zone
 *
 * The panel scales dynamically when resized.
 */
class GridPanel extends JPanel {

    /** Number of rows in the grid */
    private int rows;

    /** Number of columns in the grid */
    private int cols;

    /** Background color for each grid cell */
    private Color[][] cellColors;

    /** Text label for each grid cell */
    private String[][] cellTexts;

    /** All zones loaded from the zone definition file */
    private HashMap<Integer, Zone> zones;

    /** Scale factor used to convert real-world coordinates into grid cells */
    private static final int gridScale = 10;

    /**
     * Constructs a GridPanel with the specified grid dimensions.
     *
     * @param rows number of rows in the grid
     * @param cols number of columns in the grid
     */
    public GridPanel(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;

        cellColors = new Color[rows][cols];
        cellTexts  = new String[rows][cols];
        zones      = new HashMap<>();
    }

    /**
     * Reads a CSV file containing zone definitions and loads them into the grid.
     *
     * Expected file format:
     * ZoneID,(x1;y1),(x2;y2)
     *
     * Coordinates are scaled down to fit the grid.
     * The center cell of each zone is automatically marked as an event.
     *
     * @param zoneFilePath path to the zone definition file
     */
    public void readZonesFile(String zoneFilePath) {
        BufferedReader zoneReader = null;

        try {
            zoneReader = new BufferedReader(new FileReader(zoneFilePath));
            zoneReader.readLine(); // Skip header line

            String zoneLine;
            while ((zoneLine = zoneReader.readLine()) != null) {
                try {
                    String[] row = zoneLine.split(",");
                    if (row.length != 3) {
                        continue; // Skip malformed lines
                    }

                    int zoneID = Integer.parseInt(row[0].trim());

                    String[] start = row[1].replace("(", "").replace(")", "").split(";");
                    String[] end   = row[2].replace("(", "").replace(")", "").split(";");

                    // Scale coordinates to grid space
                    int x1 = Integer.parseInt(start[0].trim()) / gridScale;
                    int y1 = Integer.parseInt(start[1].trim()) / gridScale;
                    int x2 = Integer.parseInt(end[0].trim())   / gridScale;
                    int y2 = Integer.parseInt(end[1].trim())   / gridScale;

                    Zone zone = new Zone(zoneID, x1, x2, y1, y2);
                    zones.put(zoneID, zone);

                } catch (Exception ex) {
                    // Skip invalid zone entries
                }
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (zoneReader != null) {
                    zoneReader.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        // Mark the center cell of each zone as an event
        for (Zone z : zones.values()) {

            // ----- Zone label cell (top-left of zone) -----
            int labelRow = z.y1;
            int labelCol = z.x1;

            if (labelRow >= 0 && labelRow < rows && labelCol >= 0 && labelCol < cols) {
                // Do NOT overwrite event cells if they coincide
                if (cellColors[labelRow][labelCol] == null) {
                    setCellColor(labelRow, labelCol, Color.LIGHT_GRAY);
                }
            }

            // ----- Event cell (center of zone) -----
            int eventRow = z.latitude;
            int eventCol = z.longitude;

            if (eventRow >= 0 && eventRow < rows && eventCol >= 0 && eventCol < cols) {
                setCellColor(eventRow, eventCol, Color.RED);
                setCellText(eventRow, eventCol, "M");
            }

            //RECREATING STATIC GUI
            setCellColor(7, 4, Color.ORANGE);
            setCellText(7, 4, "D(2)");
            setCellColor(19, 0, Color.ORANGE);
            setCellText(19, 0, "D(1)");

        }

        repaint();
    }

    /**
     * Sets the background color of a specific grid cell.
     *
     * @param r row index
     * @param c column index
     * @param color color to apply
     */
    public void setCellColor(int r, int c, Color color) {
        cellColors[r][c] = color;
        repaint();
    }

    /**
     * Sets the text label for a specific grid cell.
     *
     * @param r row index
     * @param c column index
     * @param text label to display
     */
    public void setCellText(int r, int c, String text) {
        cellTexts[r][c] = text;
        repaint();
    }

    /**
     * Replaces the current zone map.
     *
     * @param zones map of zone IDs to Zone objects
     */
    public void setZones(HashMap<Integer, Zone> zones) {
        this.zones = zones;
    }

    /**
     * Draws all zones using thick borders and labels.
     *
     * @param g2 graphics context
     * @param cellWidth width of one grid cell
     * @param cellHeight height of one grid cell
     */
    private void drawZones(Graphics2D g2, int cellWidth, int cellHeight) {
        if (zones == null) return;

        g2.setColor(Color.BLACK);
        g2.setStroke(new BasicStroke(4));

        for (Zone z : zones.values()) {
            int x = z.x1 * cellWidth;
            int y = z.y1 * cellHeight;

            int width  = (z.x2 - z.x1) * cellWidth;
            int height = (z.y2 - z.y1) * cellHeight;

            // Draw zone border
            g2.drawRect(x, y, width, height);

            // Draw zone label near top-left corner
            String label = "Z(" + z.zoneId + ")";
            Font font = g2.getFont().deriveFont(Font.BOLD, 12f);
            g2.setFont(font);

            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(label, x + 4, y + fm.getAscent() + 2);
        }

        g2.setStroke(new BasicStroke(1));
    }

    /**
     * Paints the grid, cell contents, and zones.
     *
     * @param g graphics context
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);

        Graphics2D g2 = (Graphics2D) g;

        int cellWidth  = getWidth() / cols;
        int cellHeight = getHeight() / rows;

        // Draw grid cells
        for (int r = 0; r < rows; r++) {
            for (int c = 0; c < cols; c++) {
                int x = c * cellWidth;
                int y = r * cellHeight;

                // Cell background
                if (cellColors[r][c] != null) {
                    g2.setColor(cellColors[r][c]);
                    g2.fillRect(x, y, cellWidth, cellHeight);
                }

                // Cell text
                if (cellTexts[r][c] != null) {
                    g2.setColor(Color.BLACK);
                    FontMetrics fm = g2.getFontMetrics();
                    int tw = fm.stringWidth(cellTexts[r][c]);
                    int th = fm.getAscent();

                    g2.drawString(
                        cellTexts[r][c],
                        x + (cellWidth - tw) / 2,
                        y + (cellHeight + th) / 2 - 2
                    );
                }

                // Cell border
                g2.setColor(Color.BLACK);
                g2.drawRect(x, y, cellWidth, cellHeight);
            }
        }

        // Draw zone overlays
        drawZones(g2, cellWidth, cellHeight);
    }
}