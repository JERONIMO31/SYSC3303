package utils;

import java.time.LocalTime;

/**
 * Provides a standardized time system for the simulation.
 * Allows simulation time to be offset from real time.
 */
public class standardizedTime {
    private LocalTime startTime;

    /**
     * Constructs a standardized time system with the given start time.
     * 
     * @param startTime The real-world time when the simulation started
     */
    public standardizedTime(LocalTime startTime) {
        this.startTime = startTime;
    }

    /**
     * Gets the current simulation time relative to the start time.
     * 
     * @return The relative time since simulation start
     */
    public LocalTime getRelativeTime() {
        return LocalTime.now().minusHours(startTime.getHour())
                .minusMinutes(startTime.getMinute())
                .minusSeconds(startTime.getSecond());
    }
}
