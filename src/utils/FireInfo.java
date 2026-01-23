package utils;

import java.time.LocalTime;

public class FireInfo {
    public final int latitude;
    public final int longitude;
    public final Intensity intensity;
    public final LocalTime time;
    private int remainingAgentRequired;
    private Integer droneAssigned = null;

    public FireInfo(int latitude, int longitude, Intensity intensity, LocalTime time) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.intensity = intensity;
        this.time = time;
        switch (intensity) {
            case HIGH:
                this.remainingAgentRequired = 30;
                break;
            case MODERATE:
                this.remainingAgentRequired = 20;
                break;
            case LOW:
                this.remainingAgentRequired = 10;
                break;
        }
    }

    public String getLocationKey() {
        return this.latitude + "," + this.longitude;
    }

    public synchronized boolean isExtinguished() {
        return this.remainingAgentRequired <= 0;
    }

    public synchronized int applyAgent(int amount) {
        if (amount > this.remainingAgentRequired) {
            this.remainingAgentRequired = 0;
            return this.remainingAgentRequired;
        }
        this.remainingAgentRequired -= amount;
        return amount;
    }

    public synchronized int getRemainingAgentRequired() {
        return this.remainingAgentRequired;
    }

    public synchronized boolean hasDroneAssigned() {
        return this.droneAssigned != null;
    }

    public synchronized void assignDrone(int droneId) {
        this.droneAssigned = droneId;
    }
}
