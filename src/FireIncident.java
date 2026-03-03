import java.util.HashMap;
import java.util.TreeMap;

import event.EventInfo;
import event.EventType;
import event.Intensity;
import udp.*;

import java.io.BufferedReader;
import java.io.FileReader;
import java.time.LocalTime;

import utils.StandardizedTime;
import zones.Zone;
import zones.ZoneReader;

import java.net.*;

public class FireIncident {

    public static final int FIRE_INCIDENT_PORT = 2345;

    private TreeMap<LocalTime, EventInfo> eventMap;
    private HashMap<Integer, Zone> zoneMap;
    private StandardizedTime standardTime;
    private FireIncidentGUI gui;
    private DatagramSocket socket;
    private boolean readyToStart = false;

    /**
     * Constructs a new FireIncident thread.
     * Reads zone and event data from files during initialization.
     * 
     * @param zoneFilePath  Path to the CSV file containing zone definitions
     * @param eventFilePath Path to the CSV file containing fire events
     * @param standardTime  The standardized time system for the simulation
     * @param gui           The GUI for printing status messages and errors
     */
    public FireIncident(String zoneFilePath, String eventFilePath, FireIncidentGUI gui) {

        this.eventMap = new TreeMap<>();
        this.zoneMap = ZoneReader.readZoneFile(zoneFilePath);
        this.gui = gui;

        readEventsFile(eventFilePath);
        readZoneFile(zoneFilePath);

        try {
            this.socket = new DatagramSocket(FIRE_INCIDENT_PORT);
            this.socket.setSoTimeout(100); // Set a timeout for receiving UDP messages
        } catch (Exception e) {
            e.printStackTrace();
        }

        gui.printMessage("FireIncident initialized and waiting for scheduler connection...");

        Message message = new Message(MessageType.INIT);
        message.setData("sender", "FireIncident");

        while (!readyToStart && !Thread.currentThread().isInterrupted()) {
            sendMessage(message, Scheduler.SCHEDULER_PORT);
            receiveUDPMessage();
            if (!readyToStart) {
                try {
                    Thread.sleep(1000); // Wait before retrying
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    private void receiveUDPMessage() {
        DatagramPacket packet = new DatagramPacket(new byte[1024], 1024);
        try {
            socket.receive(packet);
            Message message = Message.fromDatagramPacket(packet);
            handleMessage(message);
        } catch (SocketTimeoutException e) {
            // Timeout occurred, no message received
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void handleMessage(Message message) {
        switch (message.type) {
            case INIT:
                if (this.readyToStart) {
                    gui.printMessage("Received duplicate INIT message, ignoring.");
                    return;
                }
                this.standardTime = new StandardizedTime(LocalTime.parse(message.getData("startTime")),
                        Integer.parseInt(message.getData("timeScale")));
                this.gui.setStandardTime(this.standardTime);
                this.readyToStart = true;
                break;
            default:
                gui.printMessage("Unknown message type: " + message.type);
        }
    }

    private void sendMessage(Message message, int port) {
        try {
            DatagramPacket packet = message.toDatagramPacket();
            packet.setAddress(InetAddress.getByName("localhost"));
            packet.setPort(port);
            socket.send(packet);
        } catch (Exception e) {
            e.printStackTrace();
        }
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

    public void readZoneFile(String zoneFilePath) {
        this.zoneMap = new HashMap<>();
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
    }

    public void sendEventInfo(EventInfo eventInfo) {
        Message message = new Message(MessageType.NEW_INCIDENT);
        message.setData("latitude", eventInfo.latitude);
        message.setData("longitude", eventInfo.longitude);
        message.setData("intensity", eventInfo.intensity.toString());
        message.setData("eventType", eventInfo.eventType.toString());
        message.setData("time", eventInfo.time.toString());
        sendMessage(message, Scheduler.SCHEDULER_PORT);
    }

    /**
     * Main execution loop for the fire incident thread.
     * Reports fires at their scheduled times, then monitors until all fires are
     * extinguished.
     * Sets end condition when all events are processed and fires are out.
     */
    public void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            LocalTime currentTime = standardTime.getRelativeTime();
            if (eventMap.firstKey() != null && !eventMap.firstKey().isAfter(currentTime)) {
                EventInfo fire = eventMap.remove(eventMap.firstKey());

                gui.printMessage("New fire reported at " + fire.getLocationKey() + " with intensity " + fire.intensity
                        + " at time " + fire.time + ".");
                sendEventInfo(fire);
            }
            try {
                if (eventMap.isEmpty()) {
                    Thread.sleep(1000);
                    continue;
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
    }

}