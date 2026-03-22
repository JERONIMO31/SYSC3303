import drone.DroneInfo;
import drone.LiveDroneTracker;
import event.EventInfo;
import udp.Message;
import udp.MessageType;
import utils.StandardizedTime;
import zones.Zone;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.stream.Collectors;

public class GUISubsystem {

    private DatagramSocket socket;
    private GUI gui;
    private StandardizedTime standardTime;
    private boolean readyToStart = false;
    private boolean contactedFireIncident = false;
    private boolean contactedScheduler = false;
    public static final int GUI_SUBSYSTEM_PORT = 4567;

    private int maxWidth;
    private int maxHeight;

    private HashMap<Integer, Zone> zoneMap;
    private HashMap<String, String> droneMap;
    private ArrayList<String[]> events;

    public GUISubsystem(GUI gui){
        this.gui = gui;
        this.maxHeight = 0;
        this.maxWidth = 0;
        this.zoneMap = new HashMap<>();
        this.events = new ArrayList<>();
        this.droneMap = new HashMap<>();

        try {
            this.socket = new DatagramSocket(GUI_SUBSYSTEM_PORT);
            this.socket.setSoTimeout(1000);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Message InitMessage = new Message(MessageType.INIT);
        InitMessage.setData("sender", "GUISubsystem");

        while (!readyToStart && !Thread.currentThread().isInterrupted()) {
            if (!contactedFireIncident) {
                sendMessage(InitMessage, FireIncident.FIRE_INCIDENT_PORT);
            }
            if (!contactedScheduler) {
                sendMessage(InitMessage, Scheduler.SCHEDULER_PORT);
            }
            receiveUDPMessage();
            readyToStart = contactedFireIncident && contactedScheduler;
        }
        System.out.println("Successfully connected to FireIncident and Scheduler");
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

    private void parseZoneString(HashMap<Integer, Zone> zoneMap, String zonesString){
        String[] zoneStrings = zonesString.split("-");
        for  (String zoneString : zoneStrings) {
            int id = Integer.parseInt(zoneString.split(":")[0]);
            int[] coords = Arrays.stream(zoneString.split(":")[1].split(",")).mapToInt(Integer::parseInt).toArray();
            for (int i = 0; i < 4; i ++) {
                if (i % 2 == 0 && coords[i] > maxWidth){
                    maxWidth = coords[i];
                }
                else if (i % 2 == 1 && coords[i] > maxHeight){
                    maxHeight = coords[i];
                }
            }
            Zone z = new Zone(id, coords[0], coords[1], coords[2], coords[3]);
            zoneMap.put(id, z);
        }
    }

    private void handleMessage(Message message) {
        switch (message.type) {
            case INIT:
                String who = message.getData("sender");
                if (who == null || who.trim().isEmpty()) {
                    //gui.printMessage("INIT message missing sender, ignoring.");
                    break;
                }
                else if (who.equals("FireIncident")){
                    try {
                        parseZoneString(zoneMap, message.getData("zoneData"));
                        contactedFireIncident = true;
                    }
                    catch (Exception e) {
                        System.out.println("Zone parsing failure");
                    }
                }
                else if (who.equals("Scheduler")){
                    contactedScheduler = true;
                }
                break;
            case NEW_INCIDENT:
                String latitudeText = message.getData("latitude");
                String longitudeText = message.getData("longitude");
                String intensityText = message.getData("intensity");
                String eventTypeText = message.getData("eventType");
                String faultTypeText = message.getData("faultType");
                String[] incident_strings = {
                        "Event: " + eventTypeText,
                        "Location: " + latitudeText + "," + longitudeText,
                        "Severity: " + intensityText,
                        "Fault: " + (faultTypeText == null || faultTypeText.trim().isEmpty() ? "NONE" : faultTypeText)
                };
                events.add(0, incident_strings);
                gui.updateEvent(events);
                //gui.updateGrid();
                break;
            case ASSIGNMENT:
                try {
                    String droneIdText = message.getData("droneId");
                    String locationKey = message.getData("locationKey");
                    if (droneIdText == null || locationKey == null || locationKey.trim().isEmpty()) {
                        System.out.println("ASSIGNMENT message missing required fields, ignoring.");
                        break;
                    }

                    droneMap.put(droneIdText, locationKey);
                    String unassignedFire = message.getData("unassignedFire");
                    if (unassignedFire != null && !unassignedFire.isEmpty()) {

                        //gui.printMessage("Drone assigned to fire at (" + unassignedFire + ") was reassigned.");
                        //fireTracker.requeueFire(unassignedFire);
                    }
                    gui.updateDrones(droneMap);
                } catch (Exception ex) {
                    //gui.printMessage("Invalid ASSIGNMENT message: " + ex.getMessage());
                }
                break;
            case FIRE_EXTINGUISHED:
                break;
            case AGENT_DEPLOYED:
                break;
            case DRONE_FAULT:
                try {
                    String droneIdText = message.getData("droneId");
                    String faultTypeTextLocal = message.getData("faultType");
                    String faultSeverityTextLocal = message.getData("faultSeverity");
                    String actionMessage = "Action: unknown";
                    if (faultSeverityTextLocal != null) {
                        String severityUpper = faultSeverityTextLocal.trim().toUpperCase();
                        if (severityUpper.equals("HARD")) {
                            actionMessage = "Action: Out of commission permanently";
                        } else if (severityUpper.equals("SOFT")) {
                            actionMessage = "Action: Going home to reset";
                        }
                    }
                    String[] fault_strings = {
                            "Fault Event",
                            "Drone: " + (droneIdText == null ? "unknown" : droneIdText),
                            "Severity: " + (faultSeverityTextLocal == null ? "unknown" : faultSeverityTextLocal),
                            "Type: " + (faultTypeTextLocal == null ? "unknown" : faultTypeTextLocal),
                            actionMessage
                    };
                    events.add(0, fault_strings);
                    gui.updateEvent(events);
                } catch (Exception ex) {
                    // ignore malformed fault messages in GUI
                }
                break;
            default:
                //gui.printMessage("Unknown message type: " + message.type);
        }
    }

    public int[] getMaxDimensions(){
        return new int[]{maxWidth, maxHeight};
    }


//    public ArrayList<DroneInfo> getDroneList(){
//        return new ArrayList<>();
//    }

    public HashMap<String, String> getDroneMap(){
        return droneMap;
    }

    public ArrayList<EventInfo> getEventList(){
        return new ArrayList<>();
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


    public void mainLoop(){
        while (!Thread.currentThread().isInterrupted()) {
            receiveUDPMessage();
        }
    }

}
