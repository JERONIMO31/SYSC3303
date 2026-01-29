
/**
 * Main application class for the Fire Fighting Drone Simulation.
 * Initializes and displays the GUI.
 */
public class FireFightingDroneSimulation {

    /**
     * Main entry point for the application.
     * Creates and displays the simulation GUI.
     * 
     * @param args Command line arguments (not used)
     */
    public static void main(String[] args) {
        GUI gui = new GUI();
        GridWithLegend w = new GridWithLegend();
        gui.setVisible(true);
    }
}