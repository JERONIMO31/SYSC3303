package utils;

public class Zone {

    public final int zoneId;
    public final int x1;
    public final int x2;
    public final int y1;
    public final int y2;
    public final int latitude;
    public final int longitude;

    /**
     * Constructs a Zone with the specified boundaries.
     * Calculates the center point (latitude/longitude) from the boundaries.
     * 
     * @param zoneId The unique identifier for this zone
     * @param x1 The starting x coordinate
     * @param x2 The ending x coordinate
     * @param y1 The starting y coordinate
     * @param y2 The ending y coordinate
     */
    public Zone(int zoneId, int x1, int x2, int y1, int y2) {
        this.zoneId = zoneId;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
        this.latitude = (y1 + y2) / 2;
        this.longitude = (x1 + x2) / 2;
    }

    /**
     * Gets the location key for the zone's center point.
     * 
     * @return Location string in format "y,x"
     */
    public String getLocationKey() {
        return this.latitude + "," + this.longitude;
    }
}