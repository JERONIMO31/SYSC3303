package udp;

public enum MessageType {
    INIT,
    NEW_INCIDENT,
    FIRE_EXTINGUISHED,
    ASSIGNMENT,
    AGENT_DEPLOYED,
    DRONE_FAULT;

    public static MessageType fromString(String str) {
        try {
            return MessageType.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown MessageType: " + str, e);
        }
    }

    public String toString() {
        return this.name();
    }
}
