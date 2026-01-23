package utils;

public class DroneInfo {
    
    public final int droneId;
    private FireInfo assignedFire = null;
    private int availableAgent;
    private int longitude = 0;
    private int latitude = 0;
    private static final int AGENT_CAPACITY = 15;
    private static final int SPEED = 15; // m/s
    private static final int ACCELERATION = 5; // m/s^2
    private static final int DEPLOY_RATE = 2; // L/s
    private static final int OPEN_NOZZLE_TIME = 5; // seconds
    
    public DroneInfo(int droneId) {
        this.droneId = droneId;
        this.availableAgent = AGENT_CAPACITY;
    }

    public synchronized boolean isAvailable() {
        return this.assignedFire == null;
    }

    public synchronized void assignToFire(FireInfo fire) {
        this.assignedFire = fire;
        fire.assignDrone(this.droneId);
        notifyAll();
    }

    public synchronized void refillAgent() {
        this.availableAgent = AGENT_CAPACITY;
    }

    public synchronized int deployAgent() {
        if (this.assignedFire == null) {
            return 0;
        }
        int agentToDeploy = Math.min(this.availableAgent, this.assignedFire.getRemainingAgentRequired());
        try {
            Thread.sleep(OPEN_NOZZLE_TIME * 1000 + (agentToDeploy / DEPLOY_RATE) * 1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        int usedAgent = this.assignedFire.applyAgent(agentToDeploy);
        this.availableAgent -= usedAgent;
        return usedAgent;
    }

    public synchronized void waitForWork(int timeout) throws InterruptedException {
        if (this.assignedFire == null) {
            wait(timeout);
        }
    }

    private double getTravelTime() {
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

    public synchronized void travelToFire() {
        try {
            Thread.sleep((long)(getTravelTime() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.longitude = assignedFire.longitude;
        this.latitude = assignedFire.latitude;
    }

    public synchronized void travelHome() {
        try {
            Thread.sleep((long)(getTravelTime() * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        this.longitude = 0;
        this.latitude = 0;
        this.assignedFire = null;
    }

    public synchronized String getLocationKey() {
        return "(" + this.longitude + "," + this.latitude + ")";
    }
}
