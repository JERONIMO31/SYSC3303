
import utils.DroneInfo;
import utils.LiveDroneTracker;


public class Drone implements Runnable
{
    private LiveDroneTracker droneTracker;
    private DroneInfo droneInfo;
    private volatile boolean endCondition;

    public Drone(DroneInfo info, LiveDroneTracker tracker, boolean endCondition) {
        this.droneInfo = info;
        this.droneTracker = tracker;
        this.endCondition = endCondition;
    }

    public void run() {
        while(!endCondition) {
            try {
                droneInfo.waitForWork(1000);
                if (!droneInfo.isAvailable()) {
                    droneInfo.travelToFire();
                    System.out.println("Drone " + droneInfo.droneId + " arrived at fire at " + droneInfo.getLocationKey() + ".");
                    int deployed = droneInfo.deployAgent();
                    System.out.println("Drone " + droneInfo.droneId + " deployed " + deployed + "L of agent to fire at " + droneInfo.getLocationKey() + ".");
                    droneInfo.travelHome();
                    System.out.println("Drone " + droneInfo.droneId + " returned home from fire at " + droneInfo.getLocationKey() + ".");
                    droneInfo.refillAgent();
                    System.out.println("Drone " + droneInfo.droneId + " refilled agent.");
                    droneTracker.markDroneAsReady(droneInfo.droneId);
                    System.out.println("Drone " + droneInfo.droneId + " is now ready for new assignments.");
                }
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}