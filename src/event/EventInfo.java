package event;

import java.time.LocalTime;

public class EventInfo {
    public final int latitude;
    public final int longitude;
    public final Intensity intensity;
    public final EventType eventType;
    public final FaultType faultType;
    public final LocalTime time;
    private int remainingAgentRequired;
    private Integer droneAssigned = null;
    private boolean faultHandled = false;

    /**
     * Constructs a new EventInfo representing a fire event.
     * Initializes required agent amount based on fire intensity.
     * 
     * @param latitude  The latitude coordinate of the fire
     * @param longitude The longitude coordinate of the fire
     * @param intensity The intensity level of the fire (LOW, MODERATE, HIGH)
     * @param eventType The type of event
     * @param time      The time the fire was reported
     */
    public EventInfo(int latitude, int longitude, Intensity intensity, EventType eventType, LocalTime time) {
        this(latitude, longitude, intensity, eventType, time, FaultType.NONE, null);
    }

    /**
     * Constructs a new EventInfo representing a fire event with an optional
     * explicit remaining agent requirement.
     * If agentRequired is null, defaults are derived from intensity.
     * 
     * @param latitude      The latitude coordinate of the fire
     * @param longitude     The longitude coordinate of the fire
     * @param intensity     The intensity level of the fire (LOW, MODERATE, HIGH)
     * @param eventType     The type of event
     * @param time          The time the fire was reported
     * @param agentRequired Optional remaining agent requirement in liters
     */
    public EventInfo(int latitude, int longitude, Intensity intensity, EventType eventType, LocalTime time,
            Integer agentRequired) {
        this(latitude, longitude, intensity, eventType, time, FaultType.NONE, agentRequired);
    }

    /**
     * Constructs a new EventInfo representing a fire event with an optional
     * fault type.
     * 
     * @param latitude  The latitude coordinate of the fire
     * @param longitude The longitude coordinate of the fire
     * @param intensity The intensity level of the fire (LOW, MODERATE, HIGH)
     * @param eventType The type of event
     * @param time      The time the fire was reported
     * @param faultType The fault type (or NONE)
     */
    public EventInfo(int latitude, int longitude, Intensity intensity, EventType eventType, LocalTime time,
            FaultType faultType) {
        this(latitude, longitude, intensity, eventType, time, faultType, null);
    }

    /**
     * Constructs a new EventInfo representing a fire event with optional
     * fault type and explicit remaining agent requirement.
     * If agentRequired is null, defaults are derived from intensity.
     * 
     * @param latitude      The latitude coordinate of the fire
     * @param longitude     The longitude coordinate of the fire
     * @param intensity     The intensity level of the fire (LOW, MODERATE, HIGH)
     * @param eventType     The type of event
     * @param time          The time the fire was reported
     * @param faultType     The fault type (or NONE)
     * @param agentRequired Optional remaining agent requirement in liters
     */
    public EventInfo(int latitude, int longitude, Intensity intensity, EventType eventType, LocalTime time,
            FaultType faultType, Integer agentRequired) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.intensity = intensity;
        this.eventType = eventType;
        this.time = time;
        this.faultType = faultType == null ? FaultType.NONE : faultType;
        if (agentRequired != null) {
            this.remainingAgentRequired = Math.max(0, agentRequired);
            return;
        }

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

    /**
     * Gets the location key for this fire in format "x,y".
     * 
     * @return The location string
     */
    public String getLocationKey() {
        return this.longitude + "," + this.latitude;
    }

    /**
     * Checks if the fire has been extinguished.
     * 
     * @return true if no more agent is required, false otherwise
     */
    public boolean isExtinguished() {
        return this.remainingAgentRequired <= 0;
    }

    /**
     * Applies firefighting agent to the fire.
     * 
     * @param amount The amount of agent to apply in liters
     * @return The actual amount of agent used
     */
    public int applyAgent(int amount) {
        if (amount > this.remainingAgentRequired) {
            int used = this.remainingAgentRequired;
            this.remainingAgentRequired = 0;
            return used;
        }
        this.remainingAgentRequired -= amount;
        return amount;
    }

    /**
     * Sets the remaining agent required to extinguish the fire.
     *
     * @param amount The new remaining agent amount in liters
     */
    public void setAgent(int amount) {
        this.remainingAgentRequired = amount;
    }

    /**
     * Gets the remaining agent required to extinguish the fire.
     * 
     * @return The remaining agent amount in liters
     */
    public int getRemainingAgentRequired() {
        return this.remainingAgentRequired;
    }

    /**
     * Assigns a drone to this fire.
     * 
     * @param droneId The ID of the drone to assign, or null to unassign
     */
    public void assignDrone(Integer droneId) {
        this.droneAssigned = droneId;
    }

    /**
     * Returns a string representation of this fire event.
     *
     * @return Formatted string with all event details
     */
    public String toString() {
        return String.format(
                "EventInfo{location=(%d,%d), intensity=%s, eventType=%s, faultType=%s, time=%s, remainingAgentRequired=%d, droneAssigned=%s}",
                longitude, latitude, intensity.toString(), eventType.toString(), faultType.toString(), time.toString(),
                remainingAgentRequired,
                droneAssigned == null ? "False" : droneAssigned.toString());
    }

    /**
     * Checks whether the fault associated with this event has been handled.
     *
     * @return true if the fault has been handled
     */
    public boolean isFaultHandled() {
        return faultHandled;
    }

    /**
     * Marks the fault associated with this event as handled.
     */
    public void markFaultHandled() {
        this.faultHandled = true;
    }
}
