
import java.net.*;
import java.time.LocalTime;

import drone.DroneInfo;
import drone.LiveDroneTracker;
import event.Intensity;
import event.EventInfo;
import event.EventType;
import event.FaultType;
import udp.Message;
import udp.MessageType;
import utils.StandardizedTime;

public class DroneSubsystem {
    private LiveDroneTracker droneTracker;
    private DatagramSocket socket;
    private DroneSubsystemGUI gui;
    private StandardizedTime standardizedTime;
    private boolean readyToStart = false;
    private volatile boolean stopRequested = false;
    public static final int DRONE_SUBSYSTEM_PORT = 3456;

    /**
     * Constructs a new DroneSubsystem.
     * 
     * @param totalDrones    The number of drones to manage
     * @param agentCapacity  Agent capacity per drone
     * @param speed          Drone speed
     * @param acceleration   Drone acceleration
     * @param deployRate     Agent deploy rate
     * @param openNozzleTime Nozzle open time
     * @param gui            The GUI for printing status messages
     */
    public DroneSubsystem(int totalDrones, int agentCapacity, int speed, int acceleration, int deployRate,
            int openNozzleTime, int batteryRange, DroneSubsystemGUI gui) {
        this.gui = gui;
        try {
            this.socket = new DatagramSocket(DRONE_SUBSYSTEM_PORT);
            this.socket.setSoTimeout(10); // Short timeout to keep main loop responsive
        } catch (Exception e) {
            e.printStackTrace();
        }

        gui.printMessage("DroneSubsystem initialized and waiting for scheduler connection...");

        Message message = new Message(MessageType.INIT);
        message.setData("sender", "DroneSubsystem");

        while (!readyToStart && !Thread.currentThread().isInterrupted()) {
            sendMessage(message, Scheduler.SCHEDULER_PORT);
            receiveUDPMessage();
            if (!readyToStart) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        this.droneTracker = new LiveDroneTracker(totalDrones, agentCapacity, speed, acceleration, deployRate,
                openNozzleTime, batteryRange, standardizedTime, messageText -> this.gui.printMessage(messageText));

        gui.printMessage("DroneSubsystem connected to scheduler and ready.");

    }

    // TEST-ONLY constructor (does NOT open a socket)
    public DroneSubsystem(LiveDroneTracker tracker, DroneSubsystemGUI gui, StandardizedTime time) {
        this.droneTracker = tracker;
        this.gui = gui;
        this.standardizedTime = time;
        this.readyToStart = true;
    }

    /**
     * Receives and processes a single UDP message.
     * Silently ignores socket timeouts when no message is available.
     */
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

    /**
     * Handles an incoming UDP message based on its type.
     * Dispatches to INIT, ASSIGNMENT, and DRONE_FAULT handlers.
     *
     * @param message The received message to handle
     */
    private void handleMessage(Message message) {
        switch (message.type) {
            case INIT:
                if (this.readyToStart) {
                    gui.printMessage("Received duplicate INIT message, ignoring.");
                    return;
                }
                this.standardizedTime = new StandardizedTime(LocalTime.parse(message.getData("startTime")),
                        Integer.parseInt(message.getData("timeScale")));
                this.gui.setStandardTime(this.standardizedTime);
                this.readyToStart = true;
                break;
            case ASSIGNMENT:
                try {
                    String droneIdText = message.getData("droneId");
                    int longitude = Integer.parseInt(message.getData("longitude"));
                    int latitude = Integer.parseInt(message.getData("latitude"));
                    Intensity intensity = Intensity.fromString(message.getData("intensity"));
                    EventType eventType = EventType.fromString(message.getData("eventType"));
                    LocalTime time = LocalTime.parse(message.getData("time"));
                    String agentRequiredText = message.getData("agentRequired");
                    Integer agentRequired = null;
                    if (agentRequiredText != null && !agentRequiredText.trim().isEmpty()) {
                        agentRequired = Integer.parseInt(agentRequiredText.trim());
                    }

                    if (droneIdText == null) {
                        gui.printMessage("ASSIGNMENT message missing droneId, ignoring.");
                        break;
                    }
                    int droneId = Integer.parseInt(droneIdText);
                    DroneInfo assignedDrone = droneTracker.getDroneInfo(droneId);
                    if (assignedDrone == null) {
                        gui.printMessage("ASSIGNMENT for unknown drone " + droneId + ", ignoring.");
                        break;
                    }

                    EventInfo fire = new EventInfo(latitude, longitude, intensity, eventType, time, agentRequired);
                    assignedDrone.assignToFire(fire);
                    gui.printMessage("Drone " + droneId + " assigned to fire at (" + fire.getLocationKey() + ").");
                } catch (Exception ex) {
                    gui.printMessage("Invalid ASSIGNMENT message: " + ex.getMessage());
                }
                break;
            case DRONE_FAULT:
                try {
                    String droneIdText = message.getData("droneId");
                    String faultTypeString = message.getData("faultType");
                    FaultType faultType = FaultType.fromString(faultTypeString);
                    if (droneIdText == null || faultType == null) {
                        gui.printMessage("DRONE_FAULT message missing required fields, ignoring.");
                        break;
                    }
                    int droneId = Integer.parseInt(droneIdText);
                    DroneInfo faultedDrone = droneTracker.getDroneInfo(droneId);
                    if (faultedDrone == null) {
                        gui.printMessage("DRONE_FAULT for unknown drone " + droneId + ", ignoring.");
                        break;
                    }
                    faultedDrone.setPendingFault(faultType);
                } catch (Exception ex) {
                    gui.printMessage("Invalid DRONE_FAULT message: " + ex.getMessage());
                }
                break;
            case SIMULATION_COMPLETE:
                gui.printMessage("Simulation complete. DroneSubsystem will shut down.");
                stopRequested = true;
                break;

            default:
                gui.printMessage("Unknown message type: " + message.type);
        }
    }

    /**
     * Sends a UDP message to the specified port on localhost.
     *
     * @param message The message to send
     * @param port    The destination port
     */
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
     * Main execution loop for the drone subsystem.
     * Waits for fire assignments, travels to fires, deploys agent,
     * and returns home for refilling. Continues indefinitely.
     */
    public void mainLoop() {
        LocalTime lastStatusTime = standardizedTime.getRelativeTime();
        while (!Thread.currentThread().isInterrupted() && !stopRequested) {
            receiveUDPMessage();

            for (DroneInfo drone : this.droneTracker.getAllDrones()) {
                EventInfo event = drone.checkStateTransition();
                if (event != null) {
                    Message deployMessage = new Message(MessageType.AGENT_DEPLOYED);
                    deployMessage.setData("droneId", String.valueOf(drone.droneId));
                    deployMessage.setData("locationKey", event.getLocationKey());
                    deployMessage.setData("remainingAgent", String.valueOf(event.getRemainingAgentRequired()));
                    sendMessage(deployMessage, Scheduler.SCHEDULER_PORT);
                    gui.printMessage("Sent AGENT_DEPLOYED from drone " + drone.droneId + " for fire ("
                            + event.getLocationKey() + "), remaining agent required: "
                            + event.getRemainingAgentRequired() + ".");
                }
            }

            // Send drone status every real second
            LocalTime now = standardizedTime.getRelativeTime();
            if (now.toSecondOfDay() - lastStatusTime.toSecondOfDay() >= 1) {
                lastStatusTime = now;
                sendDroneStatus();
            }
        }
        gui.printMessage("DroneSubsystem main loop is exiting.");
    }

    /**
     * Sends the current status of all drones to the Scheduler.
     * Status includes position, state, fault type, and available agent.
     */
    private void sendDroneStatus() {
        StringBuilder sb = new StringBuilder();
        DroneInfo[] drones = this.droneTracker.getAllDrones();
        for (int i = 0; i < drones.length; i++) {
            DroneInfo d = drones[i];
            if (i > 0)
                sb.append(";");
            sb.append(d.droneId).append(":")
                    .append(d.getAccurateLongitude()).append(":")
                    .append(d.getAccurateLatitude()).append(":")
                    .append(d.getStateName()).append(":")
                    .append(d.getFaultName()).append(":")
                    .append(d.getAvailableAgent()).append(":")
                    .append((int) d.getExactRemainingBatteryRange());
        }
        Message status = new Message(MessageType.DRONE_STATUS);
        status.setData("droneData", sb.toString());
        sendMessage(status, Scheduler.SCHEDULER_PORT);
    }
}
