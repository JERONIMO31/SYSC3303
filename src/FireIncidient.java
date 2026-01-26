import java.util.HashMap;
import java.util.TreeMap;

import javax.swing.event.DocumentEvent.EventType;

import java.time.LocalTime;

import utils.EventInfo;
import utils.FireInfo;
import utils.Intensity;
import utils.LiveFireTracker;
import utils.EventInfo;
import utils.Event_Type;
import utils.Zone;
import java.io.BufferedReader;
import java.io.FileReader;

public class FireIncidient implements Runnable {

    private LiveFireTracker fireTracker;
    private TreeMap<LocalTime, FireInfo> fireMap;
    private TreeMap<LocalTime, EventInfo> eventMap;
    private HashMap<Integer, Zone> zoneMap;
    private volatile boolean endCondition;
    private final LocalTime startTime;

    public FireIncidient(LiveFireTracker fireTracker, String zoneFilePath, String incidentFilePath, boolean endCondition, LocalTime startTime) {
        this.fireTracker = fireTracker;
        this.endCondition = endCondition;
        this.startTime = startTime;
        // Add code to read zone and incident files and populate fireTracker
        // This is a placeholder for file reading logic
        this.fireMap = new TreeMap<>();
        this.zoneMap = new HashMap<>();

        String eventsFile = "src\\" + incidentFilePath;
        BufferedReader reader = null;
        String line = "";
        try{
            reader = new BufferedReader(new FileReader(eventsFile));
            reader.readLine();
            while((line = reader.readLine())!= null){
                String[] row = line.split(",");
                Event_Type t = Event_Type.valueOf(row[2].trim());
                Intensity i = Intensity.valueOf(row[3].trim());
                EventInfo e = new EventInfo(LocalTime.parse(row[0]),Integer.valueOf(row[1]), t, i);
                eventMap.put(LocalTime.parse(row[0]), e);
            }
        }catch(Exception e){
            e.printStackTrace();
        }

        zoneMap.put(1, new Zone(1, 0, 50, 0, 50));
        zoneMap.put(2, new Zone(2, 51, 100, 0, 50));
        zoneMap.put(3, new Zone(3, 0, 50, 51, 100));
        zoneMap.put(4, new Zone(4, 51, 100, 51, 100));

        for (int i = 0; i < zoneMap.size(); i++) {
            Zone zone = zoneMap.get(i + 1);
            for (int j = 1; j < 4; j++) {
                Intensity intensity;
                switch (j) {
                    case 1:
                        intensity = Intensity.LOW;
                        break;
                    case 2:
                        intensity = Intensity.MODERATE;
                        break;
                    case 3:
                        intensity = Intensity.HIGH;
                    default:
                        intensity = Intensity.LOW;
                        break;
                }
                LocalTime incidentTime = LocalTime.of(0, j); // Example incident times
                fireMap.put(incidentTime, new FireInfo(zone.latitude, zone.longitude, intensity, incidentTime));
            }
        }
        

    }

    public void run() {
        while (!this.endCondition) {
            LocalTime currentTime = LocalTime.now().minusHours(startTime.getHour()).minusMinutes(startTime.getMinute()).minusSeconds(startTime.getSecond());
            if (fireMap.firstKey() != null && !fireMap.firstKey().isAfter(currentTime)) {
                FireInfo fire = fireMap.remove(fireMap.firstKey());
                fireTracker.put(fire);
                System.out.println("New fire reported at " + fire.getLocationKey() + " with intensity " + fire.intensity + " at time " + fire.time + ".");
            }
            try {
                if (fireMap.isEmpty()) {
                    break;
                }
                long timeToNextFire = fireMap.firstKey() != null ? java.time.Duration.between(currentTime, fireMap.firstKey()).toMillis() : -1;
                if (timeToNextFire > 0) {
                    Thread.sleep(timeToNextFire);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        while (!this.endCondition) {
            try {
                Thread.sleep(1000);
                if (fireTracker.getActiveFireCount() == 0) {
                    this.endCondition = true;
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
