import java.util.HashMap;
import java.util.TreeMap;
import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;

import utils.EventInfo;
import utils.Intensity;
import utils.LiveFireTracker;
import utils.standardizedTime;
import utils.EventType;
import utils.Zone;
import utils.ZoneReader;
import utils.EndCondition;


public class FireIncident implements Runnable {

    private LiveFireTracker fireTracker;
    private TreeMap<LocalTime, EventInfo> eventMap;
    private HashMap<Integer, Zone> zoneMap;
    private EndCondition endCondition;
    private standardizedTime standardTime;
    private GUI gui;

    /**
     * Constructs a new FireIncident thread.
     * Reads zone and event data from files during initialization.
     * 
     * @param fireTracker The tracker for managing reported fires
     * @param zoneFilePath Path to the CSV file containing zone definitions
     * @param eventFilePath Path to the CSV file containing fire events
     * @param endCondition Shared condition for stopping the simulation
     * @param standardTime The standardized time system for the simulation
     * @param gui The GUI for printing status messages and errors
     */
    public FireIncident(LiveFireTracker fireTracker, String zoneFilePath, String eventFilePath,
        EndCondition endCondition, standardizedTime standardTime, GUI gui) {
        this.fireTracker = fireTracker;
        this.endCondition = endCondition;
        this.standardTime = standardTime;
        this.gui = gui;

        this.eventMap = new TreeMap<>();
        this.zoneMap = ZoneReader.readZoneFile(zoneFilePath);

        readEventsFile(eventFilePath);
    }


    /**
     * Reads and parses fire event data from a CSV file.
     * Skips invalid lines and reports errors to the GUI.
     *
     * @param eventFilePath Path to the CSV file containing event data
     */
    private void readEventsFile(String eventFilePath) {
        String eventsFile = eventFilePath;
        BufferedReader eventReader = null;
        try {
            eventReader = new BufferedReader(new FileReader(eventsFile));
            eventReader.readLine(); // Skip header
            String eventLine;
            while ((eventLine = eventReader.readLine()) != null) {
                try {
                    String[] row = eventLine.split(",");
                    if (row.length != 4) {
                        gui.printMessage("ERROR: Skipping invalid event line: " + eventLine);
                        continue;
                    }
                    EventType type = EventType.valueOf(row[2].trim().toUpperCase());
                    Intensity intensity = Intensity.valueOf(row[3].trim().toUpperCase());
                    LocalTime eventTime = LocalTime.parse(row[0].trim());
                    int zoneID = Integer.parseInt(row[1].trim());
                    Zone zone = zoneMap.get(zoneID);
                    EventInfo eventInfo = new EventInfo(zone.latitude, zone.longitude, intensity, type, eventTime);
                    eventMap.put(eventTime, eventInfo);
                } catch (Exception ex) {
                    gui.printMessage("ERROR: Skipping invalid event line: " + eventLine + " - " + ex.getMessage());
                }
            }
        } catch (Exception ex) {
            gui.printMessage("ERROR: Failed to read events file: " + ex.getMessage());
            ex.printStackTrace();
        } finally {
            try {
                if (eventReader != null) {
                    eventReader.close();
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    /**
     * Main execution loop for the fire incident thread.
     * Reports fires at their scheduled times, then monitors until all fires are extinguished.
     * Sets end condition when all events are processed and fires are out.
     */
    public void run() {
        while (!this.endCondition.shouldStop()) {
            LocalTime currentTime = standardTime.getRelativeTime();
            if (eventMap.firstKey() != null && !eventMap.firstKey().isAfter(currentTime)) {
                EventInfo fire = eventMap.remove(eventMap.firstKey());
                fireTracker.put(fire);
                gui.printMessage("New fire reported at " + fire.getLocationKey() + " with intensity " + fire.intensity
                        + " at time " + fire.time + ".");
            }
            try {
                if (eventMap.isEmpty()) {
                    break;
                }
                long timeToNextFire = eventMap.firstKey() != null
                        ? java.time.Duration.between(currentTime, eventMap.firstKey()).toMillis()
                        : -1;
                if (timeToNextFire > 0) {
                    Thread.sleep(Math.min(timeToNextFire, 1000));
                } 
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        while (!this.endCondition.shouldStop()) {
            try {
                Thread.sleep(1000);
                if (fireTracker.getActiveFireCount() == 0) {
                    gui.printMessage("FireIncident thread ending as all events have been processed and fires extinguished.");
                    this.endCondition.setStop(true);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        gui.printMessage("FireIncident thread stopping operations.");
    }
}