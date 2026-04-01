
import java.net.*;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;

import event.EventInfo;
import event.EventType;
import event.Intensity;
import event.FaultType;
import event.LiveFireTracker;
import udp.*;
import utils.StandardizedTime;
import zones.Zone;

public class Scheduler {

    public static final int SCHEDULER_PORT = 1234;
    private long simulationStartTime = System.currentTimeMillis();
    private LiveFireTracker fireTracker;
    private DatagramSocket socket;
    private boolean fireIncidentConnected = false;
    private boolean droneSubsystemConnected = false;
    private StandardizedTime standardTime;
    private SchedulerGUI gui;
    private HashMap<Integer, Zone> zoneMap = new HashMap<>();
    private HashMap<Integer, DroneStatus> droneStatusMap = new HashMap<>();

    private HashMap<String, EventMetrics> eventMetricsMap = new HashMap<>();

    private HashMap<Integer, Long> droneBusyStart = new HashMap<>();
    private HashMap<Integer, Long> droneTotalBusyTime = new HashMap<>();

    private long lastQueueCheckTime = System.currentTimeMillis();
    private long totalQueueTime = 0;
    private int maxQueueLength = 0;

    private static class EventMetrics {
        long creationTime;
        long firstResponseTime = -1;
        long completionTime = -1;
    }

    /**
     * Tracks the status of a drone as reported by the DroneSubsystem.
     */
    private static class DroneStatus {
        int id;
        int longitude;
        int latitude;
        String state;
        String fault;
        int agent;
        String assignedFireKey;

        DroneStatus(int id, int longitude, int latitude, String state, String fault, int agent) {
            this.id = id;
            this.longitude = longitude;
            this.latitude = latitude;
            this.state = state;
            this.fault = fault;
            this.agent = agent;
        }
    }

    /**
     * Constructs a new Scheduler, creating its own FireTracker, DroneTracker, and
     * GUI.
     */
    public Scheduler(int timeScale, SchedulerGUI gui) {
        this.gui = gui;
        this.fireTracker = new LiveFireTracker();
        try {
            this.socket = new DatagramSocket(SCHEDULER_PORT);
            this.socket.setSoTimeout(10); // Short timeout to keep main loop responsive
        } catch (Exception e) {
            e.printStackTrace();
        }

        gui.printMessage("Schedular waiting for connection from other subsystems...");
        while ((!fireIncidentConnected || !droneSubsystemConnected)
                && !Thread.currentThread().isInterrupted()) {
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
     * Parses a zone data string received via UDP and populates the zone map.
     *
     * @param zonesString The zone data string in format
     *                    "id:x1,y1,x2,y2-id:x1,y1,x2,y2"
     */
    private void parseZoneString(String zonesString) {
        String[] zoneStrings = zonesString.split("-");
        for (String zoneString : zoneStrings) {
            int id = Integer.parseInt(zoneString.split(":")[0]);
            int[] coords = Arrays.stream(zoneString.split(":")[1].split(",")).mapToInt(Integer::parseInt).toArray();
            Zone zone = new Zone(id, coords[0], coords[2], coords[1], coords[3]);
            zoneMap.put(id, zone);
            gui.printMessage(
                    "Zone " + id + ": (" + coords[0] + "," + coords[1] + ") to (" + coords[2] + "," + coords[3] + ")");
        }
    }

    /**
     * Handles an incoming UDP message based on its type.
     * Dispatches to appropriate logic for INIT, NEW_INCIDENT, AGENT_DEPLOYED,
     * and DRONE_STATUS messages.
     *
     * @param message The received message to handle
     */
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
                    if (this.fireIncidentConnected) {
                        gui.printMessage("Received duplicate INIT from FireIncident, ignoring.");
                        return;
                    }
                    fireIncidentConnected = true;
                    String zoneString = message.getData("zoneData");
                    parseZoneString(zoneString);
                    gui.setZoneMap(zoneMap);
                    gui.printMessage("FireIncident subsystem connected.");
                    if (this.standardTime != null) {
                        gui.printMessage("Standard time is somehow not null?");
                        sendInitMessage(FireIncident.FIRE_INCIDENT_PORT);
                    }
                } else if (who.equals("DroneSubsystem")) {
                    if (this.droneSubsystemConnected) {
                        gui.printMessage("Received duplicate INIT from DroneSubsystem, ignoring.");
                        return;
                    }
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
                    String faultTypeText = message.getData("faultType");
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
                            LocalTime.parse(timeText),
                            FaultType.fromString(faultTypeText));
                    newFireDetected(fire);
                    //gui.addFireEvent(fire.getLocationKey(), fire.longitude, fire.latitude,
                            //fire.intensity, fire.time, fire.getRemainingAgentRequired());
                } catch (Exception ex) {
                    gui.printMessage("Invalid NEW_INCIDENT message: " + ex.getMessage());
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
                    gui.updateFireEvent(deployedLocationKey, remainingAgent);

                    Long start = droneBusyStart.get(deployedDroneId);
                    if (start != null) {
                        long duration = System.currentTimeMillis() - start;

                        droneTotalBusyTime.put(
                                deployedDroneId,
                                droneTotalBusyTime.getOrDefault(deployedDroneId, 0L) + duration
                        );
                    }

                    // Clear drone's assignment since it finished deploying
                    DroneStatus deployedDrone = droneStatusMap.get(deployedDroneId);
                    if (deployedDrone != null) {
                        deployedDrone.assignedFireKey = null;
                    }

                    if (fireTracker.isExtinguished(deployedLocationKey)) {
                        EventMetrics em = eventMetricsMap.get(deployedLocationKey);
                        if (em != null) {
                            em.completionTime = System.currentTimeMillis();
                        }
                        gui.printMessage("Fire at (" + deployedLocationKey + ") has been extinguished.");
                        fireTracker.markFireAsDead(deployedLocationKey);
                        gui.extinguishFireEvent(deployedLocationKey);
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
            case DRONE_STATUS:
                String droneData = message.getData("droneData");
                if (droneData != null) {
                    parseDroneStatus(droneData);
                    gui.updateDronePositions(droneData);
                }
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
     * Sends an INIT message containing the start time and time scale
     * to a subsystem at the specified port.
     *
     * @param port The destination port of the subsystem
     */
    private void sendInitMessage(int port) {
        Message message = new Message(MessageType.INIT);
        message.setData("sender", "Scheduler");
        message.setData("startTime", standardTime.getStartTime().toString());
        message.setData("timeScale", String.valueOf(standardTime.getTimeScale()));
        sendMessage(message, port);
    }

    /**
     * Registers a newly detected fire with the fire tracker.
     *
     * @param fire The fire event to register
     */
    private void newFireDetected(EventInfo fire) {
        gui.printMessage("New fire detected: " + fire.toString());
        fireTracker.put(fire);

        String key = fire.getLocationKey();

        EventMetrics em = new EventMetrics();
        em.creationTime = System.currentTimeMillis();
        eventMetricsMap.put(key, em);

        gui.addFireEvent(
                key,
                fire.longitude,
                fire.latitude,
                fire.intensity,
                fire.time,
                fire.getRemainingAgentRequired()
        );
    }

    /**
     * Parses drone status data and updates the drone status map.
     * Preserves assigned fire keys from previous status entries.
     *
     * @param droneData The drone data string in format
     *                  "id:lon:lat:state:fault:agent;..."
     */
    private void parseDroneStatus(String droneData) {
        String[] entries = droneData.split(";");
        for (String entry : entries) {
            String[] parts = entry.split(":");
            int id = Integer.parseInt(parts[0]);
            DroneStatus status = new DroneStatus(
                    id,
                    Integer.parseInt(parts[1]),
                    Integer.parseInt(parts[2]),
                    parts[3],
                    parts[4],
                    Integer.parseInt(parts[5]));
            // Preserve assigned fire key from previous status
            DroneStatus prev = droneStatusMap.get(id);
            if (prev != null && prev.assignedFireKey != null) {
                if (status.state.equals("TRAVELING_TO_FIRE") || status.state.equals("EXTINGUISHING")) {
                    status.assignedFireKey = prev.assignedFireKey;
                } else {
                    fireTracker.requeueFire(prev.assignedFireKey);
                    gui.printMessage("Drone " + id + " is no longer extinguishing fire at (" + prev.assignedFireKey
                            + ") and has been requeued if it was still active.");
                }
            }
            droneStatusMap.put(id, status);
        }
    }

    /**
     * Selects the best drone for a fire based on proximity and availability.
     * Prefers drones already assigned to the same fire, then closest available,
     * then closest reassignable from a lower-priority fire.
     *
     * @param longitude    The fire's longitude coordinate
     * @param latitude     The fire's latitude coordinate
     * @param firePriority The fire's intensity for priority comparison
     * @return The best matching DroneStatus, or null if none eligible
     */
    private DroneStatus selectBestDrone(int longitude, int latitude, Intensity firePriority) {
        if (droneStatusMap.isEmpty()) {
            return null;
        }

        DroneStatus bestDrone = null;
        double bestDistance = Double.MAX_VALUE;
        String requestedLocationKey = longitude + "," + latitude;

        for (DroneStatus drone : droneStatusMap.values()) {
            // If a drone is already assigned to this exact fire, prefer it
            if (requestedLocationKey.equals(drone.assignedFireKey)) {
                if (!"NONE".equals(drone.fault) || "OUT_OF_COMMISSION".equals(drone.state)) {
                    continue; // Skip faulted or decommissioned drones
                }
                return drone;
            }

            boolean isAvailable = isAvailableForAssignment(drone);
            boolean canReassign = drone.assignedFireKey != null
                    && canReassignFrom(drone.assignedFireKey, firePriority);
            boolean eligible = isAvailable || canReassign;

            if (eligible) {
                double dx = drone.longitude - longitude;
                double dy = drone.latitude - latitude;
                double distance = Math.sqrt(dx * dx + dy * dy);
                if (distance < bestDistance) {
                    bestDistance = distance;
                    bestDrone = drone;
                }
            }
        }

        return bestDrone;
    }

    /**
     * Checks whether a drone is available for a new fire assignment.
     * A drone must have no fault, not be out of commission, have no current
     * assignment, and be in IDLE or traveling state with agent remaining.
     *
     * @param drone The drone status to check
     * @return true if the drone can be assigned to a new fire
     */
    private boolean isAvailableForAssignment(DroneStatus drone) {
        if (!"NONE".equals(drone.fault) || "OUT_OF_COMMISSION".equals(drone.state)) {
            return false;
        }
        if (drone.assignedFireKey != null) {
            return false;
        }
        switch (drone.state) {
            case "IDLE":
                return true;
            case "TRAVELING_HOME":
                return drone.agent > 0;
            case "TRAVELING_TO_FIRE":
                return drone.agent > 0;
            default:
                return false;
        }
    }

    /**
     * Checks whether a drone can be reassigned from its current fire
     * to a higher-priority fire.
     *
     * @param assignedFireKey The location key of the drone's current fire
     * @param newFirePriority The intensity of the new fire
     * @return true if the new fire has higher priority than the current one
     */
    private boolean canReassignFrom(String assignedFireKey, Intensity newFirePriority) {
        // Look up the assigned fire's intensity from the tracker
        EventInfo assignedFire = fireTracker.getFiresBeingFought().get(assignedFireKey);
        if (assignedFire == null) {
            return false;
        }
        return assignedFire.intensity.getRank() < newFirePriority.getRank();
    }

    /**
     * Checks if the fire assigned to a drone has an unhandled fault,
     * and if so, sends a DRONE_FAULT message and requeues the fire.
     *
     * @param droneId     The ID of the drone to check
     * @param locationKey The location key of the assigned fire
     */
    private void handleFaultIfPresent(int droneId, String locationKey) {
        EventInfo assignedFire = fireTracker.getFiresBeingFought().get(locationKey);
        if (assignedFire == null) {
            return;
        }
        if (assignedFire.faultType == FaultType.NONE || assignedFire.isFaultHandled()) {
            return;
        }

        Message faultMessage = new Message(MessageType.DRONE_FAULT);
        faultMessage.setData("sender", "Scheduler");
        faultMessage.setData("droneId", String.valueOf(droneId));
        faultMessage.setData("faultType", assignedFire.faultType.toString());
        sendMessage(faultMessage, DroneSubsystem.DRONE_SUBSYSTEM_PORT);

        gui.printMessage("Fault detected (" + assignedFire.faultType + ") for drone " + droneId
                + ". Requeuing fire at (" + locationKey + ").");
        assignedFire.markFaultHandled();
        gui.printMessage("Fault marked handled for fire at (" + locationKey + ").");
        fireTracker.requeueFire(locationKey);

        // Clear drone's assignment since fire was requeued
        DroneStatus drone = droneStatusMap.get(droneId);
        if (drone != null) {
            drone.assignedFireKey = null;
        }
    }

    /**
     * Checks all fires being fought and requeues any that have no drone
     * assigned to them, preventing fires from getting stuck.
     */
    private void requeueOrphanedFires() {
        HashSet<String> claimedFireKeys = new HashSet<>();
        for (DroneStatus drone : droneStatusMap.values()) {
            if (drone.assignedFireKey != null) {
                claimedFireKeys.add(drone.assignedFireKey);
            }
        }
        for (String fireKey : new ArrayList<>(fireTracker.getFiresBeingFought().keySet())) {
            if (!claimedFireKeys.contains(fireKey)) {
                gui.printMessage("Orphaned fire at (" + fireKey + ") has no drone assigned, requeuing.");
                fireTracker.requeueFire(fireKey);
            }
        }
    }

    /**
     * Main execution loop for the scheduler.
     * Monitors fires, updates fire states, and assigns available drones to fires.
     * Continues until end condition is met.
     */
    public void mainLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            long now = System.currentTimeMillis();

            int queueSize = fireTracker.getPendingFireCount();

            long delta = now - lastQueueCheckTime;
            totalQueueTime += queueSize * delta;

            lastQueueCheckTime = now;

            if (queueSize > maxQueueLength) {
                maxQueueLength = queueSize;
            }
            receiveUDPMessage();
            requeueOrphanedFires();
            EventInfo nextFire = fireTracker.peekNextFire();
            if (nextFire != null && !droneStatusMap.isEmpty()) {
                DroneStatus bestDrone = selectBestDrone(nextFire.longitude, nextFire.latitude, nextFire.intensity);
                if (bestDrone != null) {
                    String previousFire = bestDrone.assignedFireKey;
                    bestDrone.assignedFireKey = nextFire.getLocationKey();

                    String fireKey = nextFire.getLocationKey();

                    EventMetrics em = eventMetricsMap.get(fireKey);
                    if (em != null && em.firstResponseTime == -1) {
                        em.firstResponseTime = System.currentTimeMillis();
                    }

                    droneBusyStart.put(bestDrone.id, System.currentTimeMillis());

                    gui.printMessage("Assigning drone " + bestDrone.id + " to fire at (" + nextFire.getLocationKey()
                            + ") with intensity " + nextFire.intensity);

                    Message assignmentRequest = new Message(MessageType.ASSIGNMENT);
                    assignmentRequest.setData("droneId", String.valueOf(bestDrone.id));
                    assignmentRequest.setData("longitude", String.valueOf(nextFire.longitude));
                    assignmentRequest.setData("latitude", String.valueOf(nextFire.latitude));
                    assignmentRequest.setData("intensity", nextFire.intensity.toString());
                    assignmentRequest.setData("eventType", nextFire.eventType.toString());
                    assignmentRequest.setData("faultType", nextFire.faultType.toString());
                    assignmentRequest.setData("time", nextFire.time.toString());
                    assignmentRequest.setData("agentRequired", nextFire.getRemainingAgentRequired());
                    sendMessage(assignmentRequest, DroneSubsystem.DRONE_SUBSYSTEM_PORT);

                    fireTracker.assignFire(bestDrone.id, nextFire.getLocationKey());
                    handleFaultIfPresent(bestDrone.id, nextFire.getLocationKey());

                    if (previousFire != null && !previousFire.equals(nextFire.getLocationKey())) {
                        gui.printMessage("Drone previously at fire (" + previousFire + ") was reassigned.");
                        fireTracker.requeueFire(previousFire);
                    }
                }
            }
        }
        gui.printMessage("Scheduler main loop has been interrupted and will exit.");
        printMetrics();
    }

    public void printMetrics() {
        long totalResponse = 0;
        long maxResponse = 0;

        long totalCompletion = 0;
        long maxCompletion = 0;

        int completedCount = 0;

        for (EventMetrics em : eventMetricsMap.values()) {

            if (em.firstResponseTime != -1) {
                long response = em.firstResponseTime - em.creationTime;
                totalResponse += response;
                maxResponse = Math.max(maxResponse, response);
            }

            if (em.completionTime != -1) {
                long completion = em.completionTime - em.creationTime;
                totalCompletion += completion;
                maxCompletion = Math.max(maxCompletion, completion);
                completedCount++;
            }
        }

        gui.printMessage("===== METRICS =====");

        if (completedCount > 0) {
            gui.printMessage("Avg Response Time: " + String.format("%.4f",(totalResponse / (double)completedCount)) + " ms");
            gui.printMessage("Max Response Time: " + String.format("%.4f",(double)maxResponse) + " ms");

            gui.printMessage("Avg Completion Time: " + String.format("%.4f",(totalCompletion / (double)completedCount)) + " ms");
            gui.printMessage("Max Completion Time: " + String.format("%.4f",(double)maxCompletion) + " ms");
        }

        long totalSimTime = System.currentTimeMillis() - simulationStartTime;

        for (int droneId : droneTotalBusyTime.keySet()) {
            long busy = droneTotalBusyTime.get(droneId);
            double utilization = (busy * 100.0) / totalSimTime;

            gui.printMessage("Drone " + droneId + " Utilization: " + String.format("%.4f",utilization) + "%");
        }

        double avgQueue = totalQueueTime / (double) totalSimTime;

        gui.printMessage("Average Queue Length: " + String.format("%.4f",avgQueue));
        gui.printMessage("Max Queue Length: " + maxQueueLength);
    }
}
