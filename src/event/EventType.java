package event;

public enum EventType {
    FIRE_DETECTED,
    DRONE_REQUEST;

    public static EventType fromString(String str) {
        try {
            return EventType.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown EventType: " + str, e);
        }
    }

    public String toString() {
        return this.name();
    }
}
