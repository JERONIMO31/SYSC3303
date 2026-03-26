package event;

public enum Intensity {
    HIGH(3),
    MODERATE(2),
    LOW(1);

    private final int rank;

    Intensity(int rank) {
        this.rank = rank;
    }

    /**
     * Returns the numeric rank of this intensity (higher = more severe).
     * 
     * @return The rank value
     */
    public int getRank() {
        return this.rank;
    }

    /**
     * Converts a string to an Intensity enum value.
     *
     * @param str The string to convert
     * @return The matching Intensity
     * @throws IllegalStateException if the string doesn't match any Intensity
     */
    public static Intensity fromString(String str) {
        try {
            return Intensity.valueOf(str);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Unknown Intensity: " + str, e);
        }
    }

    /**
     * Returns the name of this intensity level.
     *
     * @return The intensity name
     */
    public String toString() {
        return this.name();
    }
}
