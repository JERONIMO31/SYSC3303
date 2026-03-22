package event;

public enum FaultType {
    NONE,
    NOZZLE_STUCK,
    SENSOR_FAILURE,
    DRONE_LOW_BATTERY,
    DRONE_CRASHED;

    public static FaultType fromString(String str) {
        if (str == null) {
            return NONE;
        }
        String normalized = str.trim();
        if (normalized.isEmpty()) {
            return NONE;
        }
        normalized = normalized.toUpperCase().replace(' ', '_').replace('-', '_');
        try {
            return FaultType.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown FaultType: " + str, e);
        }
    }

    public String toString() {
        return this.name();
    }
}
