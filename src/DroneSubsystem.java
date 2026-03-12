
import java.net.*;
import java.time.LocalTime;

import drone.DroneInfo;
import drone.LiveDroneTracker;
import drone.LiveDroneTracker.DroneAssignment;
import event.Intensity;
import event.EventInfo;
import event.EventType;
import udp.Message;
import udp.MessageType;
import utils.StandardizedTime;

public class DroneSubsystem {
    private LiveDroneTracker droneTracker;
    private DatagramSocket socket;
    private DroneSubsystemGUI gui;
    private StandardizedTime standardizedTime;
    private boolean readyToStart = false;
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
            int openNozzleTime, DroneSubsystemGUI gui) {
        this.gui = gui;
        try {
            this.socket = new DatagramSocket(DRONE_SUBSYSTEM_PORT);
            this.socket.setSoTimeout(1000);
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
                openNozzleTime, standardizedTime, messageText -> this.gui.printMessage(messageText));

        gui.printMessage("DroneSubsystem connected to scheduler and ready.");

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
                this.standardizedTime = new StandardizedTime(LocalTime.parse(message.getData("startTime")),
                        Integer.parseInt(message.getData("timeScale")));
                this.gui.setStandardTime(this.standardizedTime);
                this.readyToStart = true;
                break;
            case ASSIGNMENT:
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
                gui.printMessage("Received assignment request for fire at (" + longitude + "," + latitude
                        + ") with intensity " + intensity + ".");

                EventInfo fire = new EventInfo(latitude, longitude, intensity, eventType, time, agentRequired);
                DroneAssignment droneAssignment = droneTracker.getDrone(longitude, latitude, intensity);
                if (droneAssignment == null) {
                    gui.printMessage("No available drone for fire at (" + longitude + "," + latitude
                            + ") with intensity " + intensity);
                    break;
                }

                DroneInfo assignedDrone = droneAssignment.drone;
                EventInfo reassignedFire = droneAssignment.reassignedFire;
                if (assignedDrone != null) {
                    assignedDrone.assignToFire(fire);

                    Message response = new Message(MessageType.ASSIGNMENT);
                    response.setData("droneId", String.valueOf(assignedDrone.droneId));
                    response.setData("locationKey", fire.getLocationKey());
                    if (reassignedFire != null) {
                        response.setData("unassignedFire", reassignedFire.getLocationKey());
                        gui.printMessage("Reassigning drone " + assignedDrone.droneId + " from fire ("
                                + reassignedFire.getLocationKey() + ") to (" + fire.getLocationKey() + ").");
                    }
                    sendMessage(response, Scheduler.SCHEDULER_PORT);
                    //sendMessage(response, GUISubsystem.GUI_SUBSYSTEM_PORT);
                    gui.printMessage("Sent assignment response: drone " + assignedDrone.droneId + " -> fire ("
                            + fire.getLocationKey() + ").");
                } else {
                    gui.printMessage(
                            "No available drone for fire at (" + longitude + "," + latitude + ") with intensity "
                                    + intensity);
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

    /**
     * Main execution loop for the drone subsystem.
     * Waits for fire assignments, travels to fires, deploys agent,
     * and returns home for refilling. Continues indefinitely.
     */
    public void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
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
        }
        gui.printMessage("DroneSubsystem main loop has been interrupted and will exit.");
    }
}