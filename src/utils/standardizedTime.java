package utils;

import java.time.Duration;
import java.time.LocalTime;

/**
 * Provides a standardized time system for the simulation.
 * Allows simulation time to be offset from real time.
 */
public class StandardizedTime {
    private LocalTime startTime;
    private int timeScale = 1; // 1 real second = 1 simulation second

    /**
     * Constructs a standardized time system with the given start time.
     * 
     * @param startTime The real-world time when the simulation started
     * @param timeScale The scale factor for simulation time
     */
    public StandardizedTime(LocalTime startTime, int timeScale) {
        this.startTime = startTime;
        this.timeScale = timeScale;
    }

    /**
     * Gets the current simulation time relative to the start time.
     * 
     * @return The relative time since simulation start
     */
    public LocalTime getRelativeTime() {
        Duration elapsed = Duration.between(startTime, LocalTime.now());
        Duration scaledElapsed = elapsed.multipliedBy(timeScale);
        return LocalTime.MIDNIGHT.plus(scaledElapsed);
    }

    public LocalTime getStartTime() {
        return startTime;
    }

    public int getTimeScale() {
        return timeScale;
    }
}
