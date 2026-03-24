package zones;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;

public class ZoneReader {
    /**
     * Reads and parses zone definitions from a CSV file.
     * Skips invalid lines and reports errors to the GUI.
     *
     * @param zoneFilePath Path to the CSV file containing zone data
     */
    public static HashMap<Integer, Zone> readZoneFile(String zoneFilePath) {
        HashMap<Integer, Zone> zoneMap = new HashMap<>();
        BufferedReader zoneReader = null;
        try {
            zoneReader = new BufferedReader(new FileReader(zoneFilePath));
            zoneReader.readLine(); // Skip header
            String zoneLine;
            while ((zoneLine = zoneReader.readLine()) != null) {
                try {
                    /*
                     * Zone format is
                     * id,(upperCornerX,upperCornerY),(lowerCornerX,lowerCornerY) with the two sets
                     * of coordinates defining a rectangle (in the sample file, these are both
                     * squares,
                     * but this is not always the case)
                     */
                    String[] row = zoneLine.split(",");
                    if (row.length != 3) {
                        // gui.printMessage("ERROR: Skipping invalid zone line: " + zoneLine);
                        continue;
                    }
                    int zoneID = Integer.parseInt(row[0].trim());

                    String[] start = row[1].replace("(", "").replace(")", "").split(";");
                    String[] end = row[2].replace("(", "").replace(")", "").split(";");

                    int x1 = Integer.parseInt(start[0].trim());
                    int y1 = Integer.parseInt(start[1].trim());

                    int x2 = Integer.parseInt(end[0].trim());
                    int y2 = Integer.parseInt(end[1].trim());

                    Zone zone = new Zone(zoneID, x1, x2, y1, y2);

                    zoneMap.put(zoneID, zone);
                } catch (Exception ex) {
                    // gui.printMessage("ERROR: Skipping invalid zone line: " + zoneLine + " - " +
                    // ex.getMessage());
                }
            }
        } catch (Exception ex) {
            // gui.printMessage("ERROR: Failed to read zones file: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (zoneReader != null) {
                    zoneReader.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        return zoneMap;
    }
}
