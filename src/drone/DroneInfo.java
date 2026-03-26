package drone;

import java.time.LocalTime;
import java.util.function.Consumer;

import event.EventInfo;
import event.FaultType;
import utils.StandardizedTime;

public class DroneInfo {

    private enum State {
        IDLE, TRAVELING_TO_FIRE, EXTINGUISHING, TRAVELING_HOME, OUT_OF_COMMISSION
    }

    public final int droneId;
    private EventInfo assignedFire = null;
    private int availableAgent;
    private int longitude = 0;
    private int latitude = 0;
    private int agent_capacity;
    private int speed; // m/s
    private int acceleration; // m/s^2
    private int deploy_rate; // L/s
    private int open_nozzle_time; // seconds
    private StandardizedTime standardizedTime;
    private Consumer<String> logger;
    private State currentState = State.IDLE;
    private LocalTime stateExpiration = null; // Time when the current state will expire
    private FaultType currentFault = FaultType.NONE;

    /**
     * Constructs a new DroneInfo with the specified drone ID and parameters.
     * Initializes the drone with full agent capacity at home base (0,0).
     * 
     * @param droneId          The unique identifier for this drone
     * @param agentCapacity    The maximum agent capacity in liters
     * @param speed            The maximum speed in m/s
     * @param acceleration     The acceleration in m/s^2
     * @param deployRate       The agent deploy rate in L/s
     * @param openNozzleTime   The time to open the nozzle in seconds
     * @param standardizedTime The standardized time utility for consistent time
     *                         handling
     * @param logger           Optional logger callback for state/action messages
     */
    public DroneInfo(int droneId, int agentCapacity, int speed, int acceleration, int deployRate, int openNozzleTime,
            StandardizedTime standardizedTime, Consumer<String> logger) {
        this.droneId = droneId;
        this.agent_capacity = agentCapacity;
        this.speed = speed;
        this.acceleration = acceleration;
        this.deploy_rate = deployRate;
        this.open_nozzle_time = openNozzleTime;
        this.standardizedTime = standardizedTime;
        this.logger = logger;
        this.availableAgent = agent_capacity;
    }

    /**
     * Gets the fire currently assigned to this drone.
     * 
     * @return The assigned EventInfo, or null if no fire is assigned
     */
    public EventInfo getAssignedFire() {
        return this.assignedFire;
    }

    /**
     * Unassigns the current fire from this drone and unassigns this drone from the
     * fire.
     */
    public void unassignFire() {
        if (this.assignedFire != null) {
            this.assignedFire.assignDrone(null);
            this.assignedFire = null;
        }
    }

    /**
     * Assigns this drone to a fire event.
     * Also updates the fire to mark this drone as assigned.
     * Notifies waiting threads that the drone has work.
     * 
     * @param fire The fire event to assign to this drone
     */
    public void assignToFire(EventInfo fire) {
        if (fire == null) {
            return;
        }
        if (this.currentState == State.OUT_OF_COMMISSION || this.currentFault != FaultType.NONE) {
            log("Drone " + this.droneId + " is out of commission and cannot be assigned.");
            return;
        }

        if (this.assignedFire != null && this.assignedFire.getLocationKey().equals(fire.getLocationKey())) {
            fire.assignDrone(this.droneId);
            log("Drone " + this.droneId + " already assigned to fire (" + fire.getLocationKey()
                    + "), keeping assignment.");
            return;
        }

        this.longitude = getAccurateLongitude();
        this.latitude = getAccurateLatitude();
        if (this.assignedFire != null && this.assignedFire != fire) {
            log("Drone " + this.droneId + " reassigned from fire (" + this.assignedFire.getLocationKey() + ").");
            this.assignedFire.assignDrone(null);
        }
        this.assignedFire = fire;
        fire.assignDrone(this.droneId);
        this.currentState = State.TRAVELING_TO_FIRE;
        this.stateExpiration = getCurrentTime().plusSeconds(getTravelTime());
        log("Drone " + this.droneId + " assigned to fire (" + fire.getLocationKey() + "), traveling to fire.");
    }

    /**
     * Refills the drone's firefighting agent to maximum capacity.
     */
    public void refillAgent() {
        this.setAgentLevel(agent_capacity);
    }

    /**
     * Deploys firefighting agent to the assigned fire.
     * Simulates the time required to open nozzle and deploy agent.
     * The amount deployed is the minimum of available agent and fire requirement.
     * 
     * @return The amount of agent actually deployed in liters
     */
    public int deployAgent() {
        if (this.assignedFire == null) {
            return 0;
        }
        int agentToDeploy = Math.min(this.getAgentLevel(), this.assignedFire.getRemainingAgentRequired());
        int usedAgent = this.assignedFire.applyAgent(agentToDeploy);
        this.setAgentLevel(this.getAgentLevel() - usedAgent);
        return usedAgent;
    }

    /**
     * Sets the current level of available firefighting agent.
     * 
     * @param amount The new amount of available agent in liters
     */
    private void setAgentLevel(int amount) {
        this.availableAgent = amount;
    }

    /**
     * Gets the current level of available firefighting agent.
     * 
     * @return The amount of available agent in liters
     */
    private int getAgentLevel() {
        return this.availableAgent;
    }

    /**
     * Calculates the travel time from current location to assigned fire.
     * Uses physics-based calculation considering acceleration, max speed, and
     * distance.
     * 
     * @return Travel time in seconds, or 0 if no fire is assigned
     */
    public int getTravelTime() {
        if (this.assignedFire == null) {
            return 0;
        }
        double distance = Math.sqrt(Math.pow(this.assignedFire.latitude - this.latitude, 2)
                + Math.pow(this.assignedFire.longitude - this.longitude, 2));
        if (distance <= 0) {
            return 0;
        }
        if (speed <= 0 || acceleration <= 0) {
            return (int) Math.ceil(distance);
        }

        double timeToMaxSpeed = (double) speed / acceleration;
        double distanceToMaxSpeed = 0.5 * acceleration * Math.pow(timeToMaxSpeed, 2);
        double totalTime;
        if (distance < 2 * distanceToMaxSpeed) {
            totalTime = 2 * Math.sqrt(distance / acceleration);
        } else {
            double cruisingDistance = distance - 2 * distanceToMaxSpeed;
            double cruisingTime = cruisingDistance / speed;
            totalTime = 2 * timeToMaxSpeed + cruisingTime;
        }
        return (int) Math.ceil(totalTime);
    }

    /**
     * Updates the drone's current location coordinates.
     * 
     * @param longitude The longitude coordinate
     * @param latitude  The latitude coordinate
     */
    private void setLocation(int longitude, int latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Gets the interpolated longitude based on travel progress.
     * Calculates position between origin and destination during travel states.
     *
     * @return The current interpolated longitude coordinate
     */
    public int getAccurateLongitude() {
        if (stateExpiration == null) {
            return this.longitude;
        }
        int targetLongitude;
        switch (currentState) {
            case TRAVELING_TO_FIRE:
                if (this.assignedFire == null) {
                    return this.longitude;
                }
                targetLongitude = this.assignedFire.longitude;
                break;
            case TRAVELING_HOME:
                targetLongitude = 0;
                break;
            default:
                return this.longitude;
        }
        int deltaLongitude = targetLongitude - this.longitude;
        double travelTime = (currentState == State.TRAVELING_HOME)
                ? calculateTravelTimeTo(0, 0)
                : getTravelTime();
        if (travelTime <= 0) {
            return targetLongitude;
        }
        double timeInState = travelTime
                - (stateExpiration.toSecondOfDay() - getCurrentTime().toSecondOfDay());
        double progress = Math.min(timeInState / travelTime, 1.0);
        progress = Math.max(progress, 0.0);
        return this.longitude + (int) (deltaLongitude * progress);
    }

    /**
     * Gets the interpolated latitude based on travel progress.
     * Calculates position between origin and destination during travel states.
     *
     * @return The current interpolated latitude coordinate
     */
    public int getAccurateLatitude() {
        if (stateExpiration == null) {
            return this.latitude;
        }
        int targetLatitude;
        switch (currentState) {
            case TRAVELING_TO_FIRE:
                if (this.assignedFire == null) {
                    return this.latitude;
                }
                targetLatitude = this.assignedFire.latitude;
                break;
            case TRAVELING_HOME:
                targetLatitude = 0;
                break;
            default:
                return this.latitude;
        }
        int deltaLatitude = targetLatitude - this.latitude;
        double travelTime = (currentState == State.TRAVELING_HOME)
                ? calculateTravelTimeTo(0, 0)
                : getTravelTime();
        if (travelTime <= 0) {
            return targetLatitude;
        }
        double timeInState = travelTime
                - (stateExpiration.toSecondOfDay() - getCurrentTime().toSecondOfDay());
        double progress = Math.min(timeInState / travelTime, 1.0);
        progress = Math.max(progress, 0.0);
        return this.latitude + (int) (deltaLatitude * progress);
    }

    /**
     * Calculates the time required to deploy a given amount of agent.
     *
     * @param amount The amount of agent to deploy in liters
     * @return The deployment time in seconds
     */
    private int calculateAgentDeploymentTime(int amount) {
        return open_nozzle_time + (amount / deploy_rate);
    }

    /**
     * Calculates travel time from current position to a target location.
     * Uses physics-based calculation considering acceleration and max speed.
     *
     * @param targetLongitude The target longitude coordinate
     * @param targetLatitude  The target latitude coordinate
     * @return Travel time in seconds
     */
    private int calculateTravelTimeTo(int targetLongitude, int targetLatitude) {
        double distance = Math.sqrt(Math.pow(targetLatitude - this.latitude, 2)
                + Math.pow(targetLongitude - this.longitude, 2));
        if (distance <= 0) {
            return 0;
        }
        if (speed <= 0 || acceleration <= 0) {
            return (int) Math.ceil(distance);
        }
        double timeToMaxSpeed = (double) speed / acceleration;
        double distanceToMaxSpeed = 0.5 * acceleration * Math.pow(timeToMaxSpeed, 2);
        double totalTime;
        if (distance < 2 * distanceToMaxSpeed) {
            totalTime = 2 * Math.sqrt(distance / acceleration);
        } else {
            double cruisingDistance = distance - 2 * distanceToMaxSpeed;
            double cruisingTime = cruisingDistance / speed;
            totalTime = 2 * timeToMaxSpeed + cruisingTime;
        }
        return (int) Math.ceil(totalTime);
    }

    /**
     * Gets the current simulation time.
     * Falls back to real time if standardized time is not set.
     *
     * @return The current time
     */
    private LocalTime getCurrentTime() {
        if (this.standardizedTime != null) {
            return this.standardizedTime.getRelativeTime();
        }
        return LocalTime.now();
    }

    /**
     * Checks and handles state transitions based on time expiration.
     * Manages the full lifecycle: traveling to fire, extinguishing,
     * traveling home, and handling faults/out-of-commission states.
     *
     * @return The fire event if agent was just deployed, null otherwise
     */
    public EventInfo checkStateTransition() {
        if (stateExpiration != null && !getCurrentTime().isBefore(stateExpiration)) {
            switch (currentState) {
                case TRAVELING_TO_FIRE:
                    currentState = State.EXTINGUISHING;
                    if (assignedFire == null) {
                        currentState = State.IDLE;
                        stateExpiration = null;
                        return null;
                    }
                    setLocation(assignedFire.longitude, assignedFire.latitude);
                    int deployAmount = Math.min(this.getAgentLevel(), this.assignedFire.getRemainingAgentRequired());
                    stateExpiration = getCurrentTime()
                            .plusSeconds(calculateAgentDeploymentTime(deployAmount));
                    log("Drone " + this.droneId + " arrived at fire (" + assignedFire.getLocationKey()
                            + "), starting extinguish.");
                    break;
                case EXTINGUISHING:
                    deployAgent();
                    log("Drone " + this.droneId + " deployed agent at fire ("
                            + (assignedFire != null ? assignedFire.getLocationKey() : "unknown")
                            + "), remaining required: "
                            + (assignedFire != null ? assignedFire.getRemainingAgentRequired() : "unknown") + ".");
                    int travelHomeTime = calculateTravelTimeTo(0, 0);
                    EventInfo fire = assignedFire; // Store reference to fire before unassigning
                    unassignFire();
                    currentState = State.TRAVELING_HOME;
                    stateExpiration = getCurrentTime().plusSeconds(travelHomeTime);
                    log("Drone " + this.droneId + " returning to base.");
                    return fire; // Return the fire that was just extinguished
                case TRAVELING_HOME:
                    setLocation(0, 0);
                    refillAgent();
                    if (currentFault != FaultType.NONE) {
                        if (currentFault.isHardFault()) {
                            currentState = State.OUT_OF_COMMISSION;
                            stateExpiration = null;
                            log("Drone " + this.droneId + " is out of commission for the remainder of the simulation.");
                        } else {
                            int downtime = 5; // Default downtime for soft faults
                            currentState = State.OUT_OF_COMMISSION;
                            stateExpiration = getCurrentTime().plusSeconds(downtime);
                            log("Drone " + this.droneId + " is out of commission for " + downtime
                                    + " seconds due to fault.");
                        }
                        break;
                    }
                    currentState = State.IDLE;
                    stateExpiration = null;
                    log("Drone " + this.droneId + " arrived at base and refilled.");
                    break;
                case OUT_OF_COMMISSION:
                    if (!currentFault.isHardFault()) {
                        currentState = State.IDLE;
                        stateExpiration = null;
                        currentFault = FaultType.NONE;
                        log("Drone " + this.droneId + " returned to service.");
                    }
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    /**
     * Applies a fault to this drone, causing it to return to base.
     * Hard faults permanently decommission the drone; soft faults
     * cause temporary downtime.
     *
     * @param faultType The type of fault to apply
     */
    public void applyFault(FaultType faultType) {

        this.longitude = getAccurateLongitude();
        this.latitude = getAccurateLatitude();
        unassignFire();

        this.currentFault = faultType;

        int travelHomeTime = calculateTravelTimeTo(0, 0);
        this.currentState = State.TRAVELING_HOME;
        this.stateExpiration = getCurrentTime().plusSeconds(travelHomeTime);
        log("Drone " + this.droneId + " returning to base due to " + (faultType.isHardFault() ? "HARD" : "SOFT")
                + " fault.");
    }

    /**
     * Gets the name of the drone's current state.
     *
     * @return The state name as a string
     */
    public String getStateName() {
        return this.currentState.name();
    }

    /**
     * Gets the name of the drone's current fault type.
     *
     * @return The fault name as a string
     */
    public String getFaultName() {
        return this.currentFault.name();
    }

    /**
     * Gets the current amount of available firefighting agent.
     *
     * @return The available agent in liters
     */
    public int getAvailableAgent() {
        return this.availableAgent;
    }

    /**
     * Logs a message using the configured logger callback.
     *
     * @param message The message to log
     */
    private void log(String message) {
        if (this.logger != null) {
            this.logger.accept(message);
        }
    }
}
