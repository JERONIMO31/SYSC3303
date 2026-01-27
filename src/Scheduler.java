import utils.LiveDroneTracker;
import utils.LiveFireTracker;
import utils.DroneInfo;
import utils.EventInfo;
import utils.EndCondition;

public class Scheduler implements Runnable
{

    private LiveFireTracker fireTracker;
    private LiveDroneTracker droneTracker;
    private EndCondition endCondition;
    private GUI gui;

    /**
     * Constructs a new Scheduler thread.
     * 
     * @param liveDroneTracker The tracker managing available drones
     * @param liveFireTracker The tracker managing active fires
     * @param endCondition Shared condition for stopping the simulation
     * @param gui The GUI for printing status messages
     */
    public Scheduler(LiveDroneTracker liveDroneTracker, LiveFireTracker liveFireTracker, EndCondition endCondition, GUI gui) {
        this.droneTracker = liveDroneTracker;
        this.fireTracker = liveFireTracker;
        this.endCondition = endCondition;
        this.gui = gui;
    }

    /**
     * Main execution loop for the scheduler thread.
     * Monitors fires, updates fire states, and assigns available drones to fires.
     * Continues until end condition is met.
     */
    public void run() {
        EventInfo next_fire = null;
        try{
        while (!this.endCondition.shouldStop()) {
            fireTracker.updateLiveFires();
            if (next_fire == null) {
                next_fire = fireTracker.getNextEventInfo();
                if (next_fire != null) {
                    gui.printMessage("Scheduler handling fire at " + next_fire.getLocationKey() + " with intensity " + next_fire.intensity + " and remaining agent requirement of " + next_fire.getRemainingAgentRequired() + "L.");
                }
            } else {
                DroneInfo ready_drone = null;
                ready_drone = droneTracker.getReadyDrone();
                if (ready_drone != null) {
                    ready_drone.assignToFire(next_fire);
                    next_fire.assignDrone(ready_drone.droneId);
                    gui.printMessage("Scheduler assigned Drone " + ready_drone.droneId + " to fire at " + next_fire.getLocationKey() + ".");
                    next_fire = null;
                }
            }
        }
    } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
    }

        gui.printMessage("Scheduler thread stopping operations.");
    }
}