package utils;

public class DroneInfo {

    public final int droneId;
    private EventInfo assignedFire = null;
    private int availableAgent;
    private int longitude = 0;
    private int latitude = 0;
    private static final int AGENT_CAPACITY = 15;
    private static final int SPEED = 15; // m/s
    private static final int ACCELERATION = 5; // m/s^2
    private static final int DEPLOY_RATE = 2; // L/s
    private static final int OPEN_NOZZLE_TIME = 5; // seconds

    /**
     * Constructs a new DroneInfo with the specified drone ID.
     * Initializes the drone with full agent capacity at home base (0,0).
     * 
     * @param droneId The unique identifier for this drone
     */
    public DroneInfo(int droneId) {
        this.droneId = droneId;
        this.availableAgent = AGENT_CAPACITY;
    }

    /**
     * Checks if the drone is available for assignment.
     * 
     * @return true if the drone has no assigned fire, false otherwise
     */
    public synchronized boolean isAvailable() {
        return this.assignedFire == null;
    }

    /**
     * Assigns this drone to a fire event.
     * Also updates the fire to mark this drone as assigned.
     * Notifies waiting threads that the drone has work.
     * 
     * @param fire The fire event to assign to this drone
     */
    public synchronized void assignToFire(EventInfo fire) {
        this.assignedFire = fire;
        fire.assignDrone(this.droneId);
        notifyAll();
    }

    /**
     * Refills the drone's firefighting agent to maximum capacity.
     */
    public synchronized void refillAgent() {
        this.setAgentLevel(AGENT_CAPACITY);
    }

    /**
     * Deploys firefighting agent to the assigned fire.
     * Simulates the time required to open nozzle and deploy agent.
     * The amount deployed is the minimum of available agent and fire requirement.
     * 
     * @return The amount of agent actually deployed in liters
     */
    public int deployAgent() throws InterruptedException {
        if (this.assignedFire == null) {
            return 0;
        }
        int agentToDeploy = Math.min(this.getAgentLevel(), this.assignedFire.getRemainingAgentRequired());
        try {
            Thread.sleep(OPEN_NOZZLE_TIME * 1000 + (agentToDeploy / DEPLOY_RATE) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int usedAgent = this.assignedFire.applyAgent(agentToDeploy);
        this.setAgentLevel(this.getAgentLevel() - usedAgent);
        return usedAgent;
    }

    /**
     * Sets the current level of available firefighting agent.
     * 
     * @param amount The new amount of available agent in liters
     */
    private synchronized void setAgentLevel(int amount) {
        this.availableAgent = amount;
    }

    /**
     * Gets the current level of available firefighting agent.
     * 
     * @return The amount of available agent in liters
     */
    private synchronized int getAgentLevel() {
        return this.availableAgent;
    }

    /**
     * Waits for a fire assignment if the drone is currently available.
     * 
     * @param timeout Maximum time to wait in milliseconds
     * @throws InterruptedException if the thread is interrupted while waiting
     */
    public synchronized void waitForWork(int timeout) throws InterruptedException {
        if (this.assignedFire == null) {
            wait(timeout);
        }
    }

    /**
     * Calculates the travel time from current location to assigned fire.
     * Uses physics-based calculation considering acceleration, max speed, and distance.
     * 
     * @return Travel time in seconds, or 0 if no fire is assigned
     */
    public synchronized double getTravelTime() {
        if (this.assignedFire == null) {
            return 0;
        }
        double distance = Math.sqrt(Math.pow(this.assignedFire.latitude, 2) + Math.pow(this.assignedFire.longitude, 2));
        double timeToMaxSpeed = SPEED / ACCELERATION;
        double distanceToMaxSpeed = 0.5 * ACCELERATION * Math.pow(timeToMaxSpeed, 2);
        double totalTime;
        if (distance < 2 * distanceToMaxSpeed) {
            totalTime = 2 * Math.sqrt(distance / ACCELERATION);
        } else {
            double cruisingDistance = distance - 2 * distanceToMaxSpeed;
            double cruisingTime = cruisingDistance / SPEED;
            totalTime = 2 * timeToMaxSpeed + cruisingTime;
        }
        return totalTime;
    }

    /**
     * Gets the location key of the currently assigned fire.
     * 
     * @return Location string in format "x,y" or "No fire assigned" if no fire
     */
    public synchronized String getAssignedFireLocation() {
        if (this.assignedFire != null) {
            return this.assignedFire.getLocationKey();
        }
        return "No fire assigned";
    }

    /**
     * Checks if the assigned fire has been extinguished.
     * 
     * @return true if assigned fire is extinguished, false otherwise or if no fire assigned
     */
    public synchronized boolean isFireExtinguished() {
        if (this.assignedFire != null) {
            return this.assignedFire.isExtinguished();
        }
        return false;
    }

    /**
     * Simulates the drone traveling to the assigned fire location.
     * Blocks for the calculated travel time and updates drone location.
     */
    public void travelToFire() {
        try {
            Thread.sleep((long) (getTravelTime() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.setLocation(this.assignedFire.longitude, this.assignedFire.latitude);
    }

    /**
     * Updates the drone's current location coordinates.
     * 
     * @param longitude The longitude coordinate
     * @param latitude The latitude coordinate
     */
    private synchronized void setLocation(int longitude, int latitude) {
        this.longitude = longitude;
        this.latitude = latitude;
    }

    /**
     * Simulates the drone traveling back to home base (0,0).
     * Blocks for the calculated travel time, clears the fire assignment,
     * and unassigns the drone from the fire.
     */
    public void travelHome() {
        try {
            Thread.sleep((long) (getTravelTime() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.setLocation(0, 0);
        this.assignedFire.assignDrone(null);    // unassign drone from fire
        this.assignedFire = null;                       // clear assigned fire
    }

    /**
     * Gets the drone's current location as a formatted string.
     * 
     * @return Location string in format "(x,y)"
     */
    public synchronized String getLocationKey() {
        return "(" + this.longitude + "," + this.latitude + ")";
    }
}
