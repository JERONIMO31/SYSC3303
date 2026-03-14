import drone.LiveDroneTracker;
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

    private HashMap<Integer, Zone> zoneMap;

    public GUISubsystem(GUI gui){

        this.gui = gui;

        this.zoneMap = new HashMap<>();

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
                break;
            case FIRE_EXTINGUISHED:
                break;
            case AGENT_DEPLOYED:
                break;
            default:
                //gui.printMessage("Unknown message type: " + message.type);
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

    public void mainLoop(){
        while (!Thread.currentThread().isInterrupted()) {
            receiveUDPMessage();
        }
    }

}
