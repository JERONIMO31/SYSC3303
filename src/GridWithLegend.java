import utils.EventInfo;
import utils.LiveFireTracker;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;

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
public class GridWithLegend extends JPanel {

    private GridPanel grid;
    private LegendPanel legend;
    public GridWithLegend(String zoneFilePath, LiveFireTracker fireTracker) {

        grid = new GridPanel(20, 25, zoneFilePath);
        legend = new LegendPanel();

        // Legend entries
        legend.addLegendItem(Color.LIGHT_GRAY,"Z(n)", "Zone Label");
        legend.addLegendItem(Color.RED, "","Active Fire");
        legend.addLegendItem(Color.GREEN,"", "Extinguished Fire");
        legend.addLegendItem(Color.ORANGE,"D(n)", "Drone Outbound");
        legend.addLegendItem(Color.GREEN,"D(n)", "Drone Extinguishing Fire");
        legend.addLegendItem(Color.MAGENTA, "D(3)","Drone Returning");

        // Layout main panel
        setLayout(new BorderLayout());
        add(grid, BorderLayout.CENTER);
        add(legend, BorderLayout.EAST);

    }

    public void replaceZoneFile(String zoneFilePath){
        grid.replaceZoneFile(zoneFilePath);
        repaint();
    }

    public void updateFires(Queue<EventInfo> fireQueue, HashMap<String, EventInfo> assignedFires){
        ArrayList<EventInfo> fires = new ArrayList<>();
        fires.addAll(fireQueue);
        fires.addAll(assignedFires.values());
        for (EventInfo fire : fires){
            System.out.println(fire.getLocationKey());
        }
    }
}