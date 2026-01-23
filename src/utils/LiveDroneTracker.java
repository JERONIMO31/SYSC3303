package utils;

import java.util.HashMap;
import java.util.HashSet;

public class LiveDroneTracker {
    
    private HashSet<Integer> readyDrones;
    private HashSet<Integer> busyDrones;
    private HashMap<Integer, DroneInfo> droneMap;

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

    public synchronized DroneInfo getReadyDrone() {
        if (this.readyDrones.isEmpty()) {
            return null;
        }
        int droneId = this.readyDrones.iterator().next();
        this.readyDrones.remove(droneId);
        this.busyDrones.add(droneId);
        return this.droneMap.get(droneId);
    }

    public synchronized void markDroneAsReady(int droneId) {
        if (this.busyDrones.contains(droneId)) {
            this.busyDrones.remove(droneId);
            this.readyDrones.add(droneId);
            this.droneMap.get(droneId).refillAgent();
        }
    }

    public synchronized DroneInfo getDroneInfo(int droneId) {
        if (this.droneMap.containsKey(droneId)) {
            return this.droneMap.get(droneId);
        }
        return null;
    }
}
