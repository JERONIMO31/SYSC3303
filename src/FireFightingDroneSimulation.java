
import utils.*;

/**
 *  Scheduler.java - this application simulates
 *  The Firefighting Drone Simulation.
 */
public class FireFightingDroneSimulation {
    AssemblyTable table = new AssemblyTable();
    Scheduler assembler;
    FireIncidient[] technicians;


    /**
     * Initiates the assembly line.
     */
    public FireFightingDroneSimulation() {
        Component[] components = Component.values();
        AssemblyTable table = new AssemblyTable();
        assembler = new Scheduler(table);
        technicians = new FireIncidient[3];
        for (int i = 0; i < 3; i++) {
            technicians[i] = new FireIncidient(table, components[i]);
        }
        System.out.println("Assembly Line started.");
    }

    /**
     * Main method to start the simulation.
     */
    public static void main(String[] args){
        FireFightingDroneSimulation Scheduler = new FireFightingDroneSimulation();
        Thread assemblerThread = new Thread(Scheduler.assembler);
        Thread[] technicianThreads = new Thread[Scheduler.technicians.length];
        for (int i = 0; i < Scheduler.technicians.length; i++) {
            technicianThreads[i] = new Thread(Scheduler.technicians[i]);
        }
        assemblerThread.start();
        for (Thread technicianThread : technicianThreads) {
            technicianThread.start();
        }

        try {
            assemblerThread.join();
            for (Thread technicianThread : technicianThreads) {
                technicianThread.interrupt();
                technicianThread.join();
            }
        } catch (InterruptedException e) {
            System.out.println("Main thread interrupted: " + e.getMessage());
        }
    }
}