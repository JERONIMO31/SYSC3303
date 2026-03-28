package udp;

/**
 * Enumeration of all UDP message types used in the simulation.
 */
public enum MessageType {
    INIT,
    NEW_INCIDENT,
    FIRE_EXTINGUISHED,
    ASSIGNMENT,
    AGENT_DEPLOYED,
    DRONE_FAULT,
    DRONE_STATUS;

    /**
     * Converts a string to a MessageType enum value.
     *
     * @param str The string to convert
     * @return The matching MessageType
     * @throws IllegalStateException if the string doesn't match any MessageType
     */
    public static MessageType fromString(String str) {
        try {
            return MessageType.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown MessageType: " + str, e);
        }
    }

    /**
     * Returns the name of this message type.
     *
     * @return The message type name
     */
    public String toString() {
        return this.name();
    }
}
