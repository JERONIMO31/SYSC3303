package event;

public enum FaultType {
    NONE,
    NOZZLE_STUCK,
    DRONE_STUCK,
    PACKET_LOSS;

    /**
     * Converts a string to a FaultType enum value.
     * Returns NONE for null or empty strings. Normalizes input
     * by uppercasing and replacing spaces/hyphens with underscores.
     *
     * @param str The string to convert
     * @return The matching FaultType, or NONE if null/empty
     * @throws IllegalStateException if the normalized string doesn't match any
     *                               FaultType
     */
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

    /**
     * Returns the name of this fault type.
     *
     * @return The fault type name
     */
    public String toString() {
        return this.name();
    }

    /**
     * Checks whether this fault type is a hard (permanent) fault.
     *
     * @return true if the fault permanently decommissions a drone
     */
    public boolean isHardFault() {
        return this == NOZZLE_STUCK;
    }
}
