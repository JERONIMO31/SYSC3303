package utils;

import java.util.HashMap;
import java.util.HashSet;

public class LiveDroneTracker {

    private HashSet<Integer> readyDrones;
    private HashSet<Integer> busyDrones;
    private HashMap<Integer, DroneInfo> droneMap;

    /**
     * Constructs a LiveDroneTracker with the specified number of drones.
     * Initializes all drones as ready and available.
     * 
     * @param totalDrones The total number of drones to manage
     */
    public LiveDroneTracker(int totalDrones) {
        this.readyDrones = new HashSet<>();
        this.busyDrones = new HashSet<>();
        this.droneMap = new HashMap<>();

        for (int i = 0; i < totalDrones; i++) {
            DroneInfo drone = new DroneInfo(i);
            this.readyDrones.add(i);
            this.droneMap.put(i, drone);
        }
    }

    /**
     * Gets an array of all drone objects.
     * 
     * @return Array containing all DroneInfo objects
     */
    public synchronized DroneInfo[] getAllDrones() {
        return this.droneMap.values().toArray(new DroneInfo[0]);
    }

    /**
     * Gets the next available ready drone.
     * Waits briefly if no drones are currently available.
     * 
     * @return A ready DroneInfo object, or null if none available after waiting
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized DroneInfo getReadyDrone() throws InterruptedException {
        if (this.readyDrones.isEmpty()) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (this.readyDrones.isEmpty()) {
            return null;
        }
        int droneId = this.readyDrones.iterator().next();
        this.readyDrones.remove(droneId);
        this.busyDrones.add(droneId);
        return this.droneMap.get(droneId);
    }

    /**
     * Marks a drone as ready and available for new assignments.
     * Notifies waiting threads that a drone is now available.
     * 
     * @param droneId The ID of the drone to mark as ready
     */
    public synchronized void markDroneAsReady(int droneId) {
        if (this.busyDrones.contains(droneId)) {
            this.busyDrones.remove(droneId);
            this.readyDrones.add(droneId);
            notifyAll();
        }
    }

    /**
     * Gets the DroneInfo object for a specific drone ID.
     * 
     * @param droneId The ID of the drone to retrieve
     * @return The DroneInfo object, or null if not found
     */
    public synchronized DroneInfo getDroneInfo(int droneId) {
        if (this.droneMap.containsKey(droneId)) {
            return this.droneMap.get(droneId);
        }
        return null;
    }
}
