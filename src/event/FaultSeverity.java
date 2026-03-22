package event;

public enum FaultSeverity {
    NONE,
    SOFT,
    HARD;

    public static FaultSeverity fromString(String str) {
        if (str == null) {
            return NONE;
        }
        String normalized = str.trim();
        if (normalized.isEmpty()) {
            return NONE;
        }
        normalized = normalized.toUpperCase().replace(' ', '_').replace('-', '_');
        try {
            return FaultSeverity.valueOf(normalized);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown FaultSeverity: " + str, e);
        }
    }

    public String toString() {
        return this.name();
    }
}
