import utils.LiveDroneTracker;
import utils.LiveFireTracker;
import utils.DroneInfo;
import utils.FireInfo;

public class Scheduler implements Runnable
{

    private LiveFireTracker fireTracker;
    private LiveDroneTracker droneTracker;
    private volatile boolean endCondition;

    public Scheduler(LiveDroneTracker liveDroneTracker, LiveFireTracker liveFireTracker, boolean endCondition) {
        this.droneTracker = liveDroneTracker;
        this.fireTracker = liveFireTracker;
        this.endCondition = endCondition;
    }

    public void run() {
        while (!this.endCondition) {
            FireInfo next_fire = fireTracker.getNextFireInfo();
            if (next_fire != null) {
                System.out.println("Scheduler found unassigned fire at " + next_fire.getLocationKey() + ".");
                DroneInfo ready_drone = null;
                while (ready_drone == null) {
                    ready_drone = droneTracker.getReadyDrone();
                }
                ready_drone.assignToFire(next_fire);
                next_fire.assignDrone(ready_drone.droneId);
                System.out.println("Scheduler assigned Drone " + ready_drone.droneId + " to fire at " + next_fire.getLocationKey() + ".");
            } else {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }

        }
    }
}