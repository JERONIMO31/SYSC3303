
import utils.DroneInfo;
import utils.LiveDroneTracker;
import utils.EndCondition;

public class Drone implements Runnable {
    private LiveDroneTracker droneTracker;
    private DroneInfo droneInfo;
    private EndCondition endCondition;
    private GUI gui;

    /**
     * Constructs a new Drone thread.
     * 
     * @param info The DroneInfo object containing drone state and capabilities
     * @param tracker The LiveDroneTracker for managing drone availability
     * @param endCondition Shared condition for stopping the simulation
     * @param gui The GUI for printing status messages
     */
    public Drone(DroneInfo info, LiveDroneTracker tracker, EndCondition endCondition, GUI gui) {
        this.droneInfo = info;
        this.droneTracker = tracker;
        this.endCondition = endCondition;
        this.gui = gui;
    }

    /**
     * Main execution loop for the drone thread.
     * Waits for fire assignments, travels to fires, deploys agent,
     * and returns home for refilling. Continues until end condition is met.
     */
    public void run() {
        while (!endCondition.shouldStop()) {
            try {
                droneInfo.waitForWork(1000);
                if (!droneInfo.isAvailable()) {
                    gui.printMessage("Drone " + droneInfo.droneId + " is heading to fire at "
                            + droneInfo.getAssignedFireLocation() + ". Expected travel time: "
                            + String.format("%.2f", droneInfo.getTravelTime()) + " seconds.");
                    droneInfo.travelToFire();
                    gui.printMessage(
                            "Drone " + droneInfo.droneId + " arrived at fire at " + droneInfo.getLocationKey() + ".");
                    gui.printMessage("Drone " + droneInfo.droneId + " starting agent deployment.");
                    int deployed = droneInfo.deployAgent();
                    gui.printMessage("Drone " + droneInfo.droneId + " deployed " + deployed + "L of agent to fire at "
                            + droneInfo.getLocationKey() + ".");
                    if (droneInfo.isFireExtinguished()) {
                        gui.printMessage("Fire at " + droneInfo.getAssignedFireLocation() + " has been extinguished!");
                    }
                    gui.printMessage("Drone " + droneInfo.droneId + " is returning home. Expected travel time: "
                            + String.format("%.2f", droneInfo.getTravelTime()) + " seconds.");
                    droneInfo.travelHome();
                    gui.printMessage("Drone " + droneInfo.droneId + " returned home.");
                    droneInfo.refillAgent();
                    gui.printMessage("Drone " + droneInfo.droneId + " refilled agent.");
                    droneTracker.markDroneAsReady(droneInfo.droneId);
                    gui.printMessage("Drone " + droneInfo.droneId + " is now ready for new assignments.");
                }

            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
        gui.printMessage("Drone " + droneInfo.droneId + " stopping operations.");
    }
}