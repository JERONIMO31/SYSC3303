import utils.LiveDroneTracker;
import utils.LiveFireTracker;

/**
 * Assembler.java - This class represents the assembler in the
 * Autonomous Drone Assembly Line simulation.
 */
public class Scheduler implements Runnable
{

    LiveFireTracker fireTracker;
    LiveDroneTracker droneTracker;

    /**
     * Initiates the assembly table.
     * 
     * @param t The assembly table used by the assembler.
     */
    public Scheduler(AssemblyTable t) {
        table = t;
        System.out.println("Assembler started.");
    }

    /**
     * The main run method for the assembler thread.
     */
    public void run() {
        Random random = new Random();
        int i;
        while (table.getNumCompletedDrones() < 20) {
            i = random.nextInt(3);
            switch (i) {
                case 0:
                    table.put(Component.CONTROL_FIRMWARE, Component.FRAME);
                    System.out.println("Assembler put CONTROL_FIRMWARE and FRAME on the table.");
                    break;
                case 1:
                    table.put(Component.FRAME, Component.PROPULSION_UNIT);
                    System.out.println("Assembler put FRAME and PROPULSION_UNIT on the table.");
                    break;
                case 2:
                    table.put(Component.PROPULSION_UNIT, Component.CONTROL_FIRMWARE);
                    System.out.println("Assembler put PROPULSION_UNIT and CONTROL_FIRMWARE on the table.");
                    break;
            }
        }
        System.out.println("AssemblyLine has completed 20 drones, terminating Assembler.");
    }
}