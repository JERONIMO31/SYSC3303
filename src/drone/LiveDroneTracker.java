package drone;

import java.util.HashMap;
import java.util.function.Consumer;

import utils.StandardizedTime;

public class LiveDroneTracker {

    private HashMap<Integer, DroneInfo> droneMap;

    /**
     * Constructs a LiveDroneTracker with the specified number of drones.
     * Initializes all drones as available.
     * 
     * @param totalDrones The total number of drones to manage
     */
    public LiveDroneTracker(int totalDrones, int agentCapacity, int speed, int acceleration, int deployRate,
            int openNozzleTime, int batteryRange, StandardizedTime standardizedTime, Consumer<String> logger) {
        this.droneMap = new HashMap<>();

        for (int i = 0; i < totalDrones; i++) {
            DroneInfo drone = new DroneInfo(i, agentCapacity, speed, acceleration, deployRate, openNozzleTime,
                    batteryRange, standardizedTime, logger);
            this.droneMap.put(i, drone);
        }
    }

    /**
     * Gets an array of all drone objects.
     * 
     * @return Array containing all DroneInfo objects
     */
    public DroneInfo[] getAllDrones() {
        return this.droneMap.values().toArray(new DroneInfo[0]);
    }

    /**
     * Gets the DroneInfo object for a specific drone ID.
     * 
     * @param droneId The ID of the drone to retrieve
     * @return The DroneInfo object, or null if not found
     */
    public DroneInfo getDroneInfo(int droneId) {
        return this.droneMap.get(droneId);
    }
}
