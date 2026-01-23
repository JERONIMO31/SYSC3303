import utils.Component;

/**
 * Technician.java - This class represents a technician in the
 * Autonomous Drone Assembly Line simulation.
 */
public class FireIncidient implements Runnable {

    AssemblyTable table;
    Component specialty;

    /**
     * Initiates the technician.
     * 
     * @param t The assembly table used by the technician.
     * @param s The component specialty of the technician.
     */
    public FireIncidient(AssemblyTable t, Component s) {
        table = t;
        specialty = s;
        System.out.println("Technician started with specialty: " + specialty + ".");
    }

    /**
     * The main run method for the technician thread.
     */
    public void run() {
        try {
        while (table.getNumCompletedDrones() < 20) {
            if (table.Assemble(specialty)) {
                System.out.println("Technician with specialty " + specialty + " assembled a drone.");
            }
            Thread.sleep(100);
        }
        } catch (InterruptedException e) {}
        System.out.println("AssemblyLine has completed 20 drones, terminating Technician with specialty: " + specialty + ".");
    }
}
