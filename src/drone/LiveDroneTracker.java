package drone;

import java.util.HashMap;

import event.EventInfo;
import event.Intensity;

public class LiveDroneTracker {

    private HashMap<Integer, DroneInfo> droneMap;

    /**
     * Simple result type pairing a drone with an optionally displaced fire.
     * If reassignedFire is non-null, the drone was reassigned from that fire.
     */
    public static class DroneAssignment {
        public final DroneInfo drone;
        public final EventInfo reassignedFire;

        public DroneAssignment(DroneInfo drone, EventInfo reassignedFire) {
            this.drone = drone;
            this.reassignedFire = reassignedFire;
        }
    }

    /**
     * Constructs a LiveDroneTracker with the specified number of drones.
     * Initializes all drones as available.
     * 
     * @param totalDrones The total number of drones to manage
     */
    public LiveDroneTracker(int totalDrones, int agentCapacity, int speed, int acceleration, int deployRate,
            int openNozzleTime) {
        this.droneMap = new HashMap<>();

        for (int i = 0; i < totalDrones; i++) {
            DroneInfo drone = new DroneInfo(i, agentCapacity, speed, acceleration, deployRate, openNozzleTime);
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
     * Finds the best drone for a fire at the given location with the given
     * priority.
     * Prefers an available (unassigned) drone closest to the location.
     * If none are available, attempts to reassign a drone from a lower-priority
     * fire.
     * 
     * @param longitude    The longitude of the fire location
     * @param latitude     The latitude of the fire location
     * @param firePriority The intensity of the fire requesting a drone
     * @return A DroneAssignment containing the drone and any displaced fire, or
     *         null if no drone can be assigned
     */
    public DroneAssignment getDrone(int longitude, int latitude, Intensity firePriority) {
        // Find the closest drone that is either available or assigned to a
        // lower-priority fire
        DroneInfo bestDrone = null;
        double bestDistance = Double.MAX_VALUE;

        for (DroneInfo drone : this.droneMap.values()) {
            boolean eligible = drone.isAvailable()
                    || (drone.getAssignedFire() != null
                            && isLowerPriority(drone.getAssignedFire().intensity, firePriority));
            if (eligible) {
                double distance = distanceTo(drone, longitude, latitude);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDrone = drone;
                }
            }
        }

        if (bestDrone == null) {
            return null;
        }

        EventInfo displaced = bestDrone.getAssignedFire();
        if (displaced != null) {
            bestDrone.unassignFire();
        }
        return new DroneAssignment(bestDrone, displaced);
    }

    /**
     * Calculates distance from a drone's current position to a target location.
     */
    private double distanceTo(DroneInfo drone, int longitude, int latitude) {
        String locKey = drone.getLocationKey(); // "(x,y)"
        String[] parts = locKey.substring(1, locKey.length() - 1).split(",");
        int dx = Integer.parseInt(parts[0]) - longitude;
        int dy = Integer.parseInt(parts[1]) - latitude;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Returns true if intensity a is strictly lower priority than intensity b.
     */
    private boolean isLowerPriority(Intensity a, Intensity b) {
        return a.getRank() < b.getRank();
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
