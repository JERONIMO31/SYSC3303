
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
    private SchedularGUI gui;

    /**
     * Constructs a new Scheduler, creating its own FireTracker, DroneTracker, and
     * GUI.
     */
    public Scheduler(int timeScale, SchedularGUI gui) {
        this.gui = gui;
        this.fireTracker = new LiveFireTracker();
        try {
            this.socket = new DatagramSocket(SCHEDULER_PORT);
            this.socket.setSoTimeout(100); // Set a timeout for receiving UDP messages
        } catch (Exception e) {
            e.printStackTrace();
        }

        gui.printMessage("Schedular waiting for connection from other subsystems...");
        while (!fireIncidentConnected && !Thread.currentThread().isInterrupted()) {
            receiveUDPMessage();
        }

        if (Thread.currentThread().isInterrupted()) {
            return;
        }

        this.standardTime = new StandardizedTime(LocalTime.now(), timeScale);
        gui.setStandardTime(this.standardTime);

        Message message = new Message(MessageType.INIT);
        message.setData("sender", "Scheduler");
        message.setData("startTime", standardTime.getStartTime().toString());
        message.setData("timeScale", String.valueOf(standardTime.getTimeScale()));
        sendMessage(message, FireIncident.FIRE_INCIDENT_PORT);
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
                String who = message.getData("sender");
                if (who.equals("FireIncident")) {
                    fireIncidentConnected = true;
                    gui.printMessage("FireIncident subsystem connected.");
                } else if (who.equals("DroneSubsystem")) {
                    droneSubsystemConnected = true;
                    gui.printMessage("DroneSubsystem connected.");
                } else {
                    gui.printMessage("Unknown sender in INIT message: " + who);
                }
                break;
            case NEW_INCIDENT:
                EventInfo fire = new EventInfo(
                        Integer.parseInt(message.getData("latitude")),
                        Integer.parseInt(message.getData("longitude")),
                        Intensity.fromString(message.getData("intensity")),
                        EventType.fromString(message.getData("eventType")),
                        LocalTime.parse(message.getData("time")));
                newFireDetected(fire);
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
        }
    }

}