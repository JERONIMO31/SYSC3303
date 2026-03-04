package drone;

import java.time.LocalTime;
import java.util.function.Consumer;

import event.EventInfo;
import utils.StandardizedTime;

public class DroneInfo {

    private enum State {
        IDLE, TRAVELING_TO_FIRE, EXTINGUISHING, TRAVELING_HOME
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
     * Checks if the drone is available for assignment.
     * 
     * @return true if the drone has no assigned fire, false otherwise
     */
    public boolean isAvailable() {
        return this.assignedFire == null;
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

        if (this.assignedFire != null && this.assignedFire.getLocationKey().equals(fire.getLocationKey())) {
            fire.assignDrone(this.droneId);
            log("Drone " + this.droneId + " already assigned to fire (" + fire.getLocationKey() + "), keeping assignment.");
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
        double travelTime = getTravelTime();
        if (travelTime <= 0) {
            return targetLongitude;
        }
        double timeInState = travelTime
                - (stateExpiration.toSecondOfDay() - getCurrentTime().toSecondOfDay());
        double progress = Math.min(timeInState / travelTime, 1.0);
        progress = Math.max(progress, 0.0);
        return this.longitude + (int) (deltaLongitude * progress);
    }

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
        double travelTime = getTravelTime();
        if (travelTime <= 0) {
            return targetLatitude;
        }
        double timeInState = travelTime
                - (stateExpiration.toSecondOfDay() - getCurrentTime().toSecondOfDay());
        double progress = Math.min(timeInState / travelTime, 1.0);
        progress = Math.max(progress, 0.0);
        return this.latitude + (int) (deltaLatitude * progress);
    }

    private int calculateAgentDeploymentTime(int amount) {
        return open_nozzle_time + (amount / deploy_rate);
    }

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

    private LocalTime getCurrentTime() {
        if (this.standardizedTime != null) {
            return this.standardizedTime.getRelativeTime();
        }
        return LocalTime.now();
    }

    public boolean isAvailableForFire() {
        switch (currentState) {
            case TRAVELING_HOME:
                return (availableAgent > 0);
            case IDLE:
                return true;
            case TRAVELING_TO_FIRE:
                return (availableAgent > 0);
            case EXTINGUISHING:
                return false;
            default:
                break;
        }
        return false;
    }

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
                    currentState = State.IDLE;
                    stateExpiration = null;
                    setLocation(0, 0);
                    refillAgent();
                    log("Drone " + this.droneId + " arrived at base and refilled.");
                    break;
                default:
                    break;
            }
        }
        return null;
    }

    private void log(String message) {
        if (this.logger != null) {
            this.logger.accept(message);
        }
    }
}
