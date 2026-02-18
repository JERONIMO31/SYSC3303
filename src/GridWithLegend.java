import javax.swing.*;
import java.awt.*;
/**
     * Main entry point for the Fire Simulation Map application.
     *
     * Responsibilities:
     * - Create and configure the main JFrame.
     * - Instantiate and configure the GridPanel and LegendPanel.
     * - Load zone data from a CSV file into the grid.
     * - Add legend entries for visualization of fires, extinguished fires, and drone activity.
     * - Assemble the GUI and display it.
*/
public class GridWithLegend {

    public GridWithLegend() {
        JFrame frame = new JFrame("Fire Simulation Map");

        GridPanel grid = new GridPanel(20, 25, "sample_zone_file.csv");
        LegendPanel legend = new LegendPanel();

        // Legend entries
        legend.addLegendItem(Color.LIGHT_GRAY,"Z(n)", "Zone Label");
        legend.addLegendItem(Color.RED, "","Active Fire");
        legend.addLegendItem(Color.GREEN,"", "Extinguished Fire");
        legend.addLegendItem(Color.ORANGE,"D(n)", "Drone Outbound");
        legend.addLegendItem(Color.GREEN,"D(n)", "Drone Extinguishing Fire");
        legend.addLegendItem(Color.MAGENTA, "D(3)","Drone Returning");

        // Layout main panel  
        JPanel mainPanel = new JPanel(new BorderLayout());
        mainPanel.add(grid, BorderLayout.CENTER);
        mainPanel.add(legend, BorderLayout.EAST);

        // Configure frame
        frame.add(mainPanel);
        frame.setSize(800, 500);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setLocationRelativeTo(null);
        frame.setVisible(true);
    }
}