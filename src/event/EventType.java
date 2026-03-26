package event;

public enum EventType {
    FIRE_DETECTED,
    DRONE_REQUEST;

    /**
     * Converts a string to an EventType enum value.
     *
     * @param str The string to convert
     * @return The matching EventType
     * @throws IllegalStateException if the string doesn't match any EventType
     */
    public static EventType fromString(String str) {
        try {
            return EventType.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown EventType: " + str, e);
        }
    }

    /**
     * Returns the name of this event type.
     *
     * @return The event type name
     */
    public String toString() {
        return this.name();
    }
}
