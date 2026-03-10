
import java.net.*;
import java.time.LocalTime;

import event.EventInfo;
import event.EventType;
import event.Intensity;
import event.LiveFireTracker;
import udp.*;
import utils.StandardizedTime;

public class Scheduler {

    public static final int SCHEDULER_PORT = 1234;
    private LiveFireTracker fireTracker;
    private DatagramSocket socket;
    private boolean fireIncidentConnected = false;
    private boolean droneSubsystemConnected = false;
    private StandardizedTime standardTime;
    private SchedulerGUI gui;

    /**
     * Constructs a new Scheduler, creating its own FireTracker, DroneTracker, and
     * GUI.
     */
    public Scheduler(int timeScale, SchedulerGUI gui) {
        this.gui = gui;
        this.fireTracker = new LiveFireTracker();
        try {
            this.socket = new DatagramSocket(SCHEDULER_PORT);
            this.socket.setSoTimeout(1000); // Set a timeout for receiving UDP messages
        } catch (Exception e) {
            e.printStackTrace();
        }

        gui.printMessage("Schedular waiting for connection from other subsystems...");
        while ((!fireIncidentConnected || !droneSubsystemConnected) && !Thread.currentThread().isInterrupted()) {
            receiveUDPMessage();
        }

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        this.standardTime = new StandardizedTime(LocalTime.now(), timeScale);
        gui.setStandardTime(this.standardTime);
        sendInitMessage(FireIncident.FIRE_INCIDENT_PORT);
        sendInitMessage(DroneSubsystem.DRONE_SUBSYSTEM_PORT);
        gui.printMessage("Scheduler initialized and starting main loop...");
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
        if (message == null || message.type == null) {
            gui.printMessage("Ignoring malformed message.");
            return;
        }

        switch (message.type) {
            case INIT:
                String who = message.getData("sender");
                if (who == null || who.trim().isEmpty()) {
                    gui.printMessage("INIT message missing sender, ignoring.");
                    break;
                }

                if (who.equals("FireIncident")) {
                    fireIncidentConnected = true;
                    gui.printMessage("FireIncident subsystem connected.");
                    if (this.standardTime != null) {
                        sendInitMessage(FireIncident.FIRE_INCIDENT_PORT);
                    }
                } else if (who.equals("DroneSubsystem")) {
                    droneSubsystemConnected = true;
                    gui.printMessage("DroneSubsystem connected.");
                    if (this.standardTime != null) {
                        sendInitMessage(DroneSubsystem.DRONE_SUBSYSTEM_PORT);
                    }
                } else {
                    gui.printMessage("Unknown sender in INIT message: " + who);
                }
                break;
            case NEW_INCIDENT:
                try {
                    String latitudeText = message.getData("latitude");
                    String longitudeText = message.getData("longitude");
                    String intensityText = message.getData("intensity");
                    String eventTypeText = message.getData("eventType");
                    String timeText = message.getData("time");
                    if (latitudeText == null || longitudeText == null || intensityText == null || eventTypeText == null
                            || timeText == null) {
                        gui.printMessage("NEW_INCIDENT message missing required fields, ignoring.");
                        break;
                    }

                    EventInfo fire = new EventInfo(
                            Integer.parseInt(latitudeText),
                            Integer.parseInt(longitudeText),
                            Intensity.fromString(intensityText),
                            EventType.fromString(eventTypeText),
                            LocalTime.parse(timeText));
                    newFireDetected(fire);
                } catch (Exception ex) {
                    gui.printMessage("Invalid NEW_INCIDENT message: " + ex.getMessage());
                }
                break;
            case ASSIGNMENT:
                try {
                    String droneIdText = message.getData("droneId");
                    String locationKey = message.getData("locationKey");
                    if (droneIdText == null || locationKey == null || locationKey.trim().isEmpty()) {
                        gui.printMessage("ASSIGNMENT message missing required fields, ignoring.");
                        break;
                    }

                    int droneId = Integer.parseInt(droneIdText);
                    gui.printMessage("Drone " + droneId + " assigned to fire at (" + locationKey + ")");
                    fireTracker.assignFire(droneId, locationKey);
                    String unassignedFire = message.getData("unassignedFire");
                    if (unassignedFire != null && !unassignedFire.isEmpty()) {
                        gui.printMessage("Drone assigned to fire at (" + unassignedFire + ") was reassigned.");
                        fireTracker.requeueFire(unassignedFire);
                    }
                } catch (Exception ex) {
                    gui.printMessage("Invalid ASSIGNMENT message: " + ex.getMessage());
                }
                break;
            case AGENT_DEPLOYED:
                try {
                    String deployedDroneIdText = message.getData("droneId");
                    String deployedLocationKey = message.getData("locationKey");
                    String remainingAgentText = message.getData("remainingAgent");
                    if (deployedDroneIdText == null || deployedLocationKey == null || remainingAgentText == null) {
                        gui.printMessage("AGENT_DEPLOYED message missing required fields, ignoring.");
                        break;
                    }

                    int deployedDroneId = Integer.parseInt(deployedDroneIdText);
                    int remainingAgent = Integer.parseInt(remainingAgentText);
                    gui.printMessage("Drone " + deployedDroneId + " deployed agent to fire at ("
                            + deployedLocationKey + "). Remaining agent required: " + remainingAgent);
                    fireTracker.deployAgent(deployedLocationKey, remainingAgent);
                    if (fireTracker.isExtinguished(deployedLocationKey)) {
                        gui.printMessage("Fire at (" + deployedLocationKey + ") has been extinguished.");
                        fireTracker.markFireAsDead(deployedLocationKey);
                        Message extinguishedMessage = new Message(MessageType.FIRE_EXTINGUISHED);
                        extinguishedMessage.setData("locationKey", deployedLocationKey);
                        sendMessage(extinguishedMessage, FireIncident.FIRE_INCIDENT_PORT);
                    } else {
                        gui.printMessage(
                                "Fire at (" + deployedLocationKey + ") is still active after agent deployment.");
                        fireTracker.requeueFire(deployedLocationKey);
                    }
                } catch (Exception ex) {
                    gui.printMessage("Invalid AGENT_DEPLOYED message: " + ex.getMessage());
                }
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

    private void sendInitMessage(int port) {
        Message message = new Message(MessageType.INIT);
        message.setData("sender", "Scheduler");
        message.setData("startTime", standardTime.getStartTime().toString());
        message.setData("timeScale", String.valueOf(standardTime.getTimeScale()));
        sendMessage(message, port);
    }

    private void newFireDetected(EventInfo fire) {
        gui.printMessage("New fire detected: " + fire.toString());
        fireTracker.put(fire);
    }

    /**
     * Main execution loop for the scheduler.
     * Monitors fires, updates fire states, and assigns available drones to fires.
     * Continues until end condition is met.
     */
    public void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            receiveUDPMessage();
            EventInfo nextFire = fireTracker.peekNextFire();
            if (nextFire != null) {
                gui.printMessage("Requesting drone assignment for fire at (" + nextFire.getLocationKey()
                        + ") with intensity " + nextFire.intensity);
                Message assignmentRequest = new Message(MessageType.ASSIGNMENT);
                assignmentRequest.setData("longitude", String.valueOf(nextFire.longitude));
                assignmentRequest.setData("latitude", String.valueOf(nextFire.latitude));
                assignmentRequest.setData("intensity", nextFire.intensity.toString());
                assignmentRequest.setData("eventType", nextFire.eventType.toString());
                assignmentRequest.setData("time", nextFire.time.toString());
                assignmentRequest.setData("agentRequired", nextFire.getRemainingAgentRequired());
                sendMessage(assignmentRequest, DroneSubsystem.DRONE_SUBSYSTEM_PORT);
            }
        }
        gui.printMessage("Scheduler main loop has been interrupted and will exit.");
    }

}