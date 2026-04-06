package event;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.PriorityQueue;
import java.util.Queue;

/**
 * LiveFireTracker.java - Object to track live and dead fires,
 * prioritized by intensity (highest first) then by time (oldest first).
 */
public class LiveFireTracker {
    /**
     * Comparator that prioritizes fires by intensity (HIGH > MODERATE > LOW),
     * then by time (earliest first).
     */
    private static final Comparator<EventInfo> FIRE_PRIORITY = Comparator
            .comparingInt((EventInfo e) -> e.intensity.getRank())
            .reversed()
            .thenComparing(e -> e.time);

    private Queue<EventInfo> fireQueue = new PriorityQueue<>(FIRE_PRIORITY);
    private HashMap<String, EventInfo> firesBeingFought = new HashMap<>();
    private ArrayList<EventInfo> deadFires = new ArrayList<>();

    /**
     * Adds a new fire to the priority queue of fires awaiting assignment.
     * 
     * @param fire The fire event to add
     */
    public void put(EventInfo fire) {
        this.fireQueue.add(fire);
    }

    /**
     * Retrieves a fire by its location key from the fires being fought.
     *
     * @param fireLocationKey The location key of the fire to retrieve
     * @return The EventInfo for the fire, or null if not found
     */
    public EventInfo getFire(String fireLocationKey) {
        return this.firesBeingFought.get(fireLocationKey);
    }

    /**
     * Updates the remaining agent for a fire being fought.
     *
     * @param fireLocationKey The location key of the fire
     * @param remainingAgent  The updated remaining agent amount
     */
    public void deployAgent(String fireLocationKey, int remainingAgent) {
        EventInfo fire = this.firesBeingFought.get(fireLocationKey);
        if (fire != null) {
            fire.setAgent(remainingAgent);
        }
    }

    /**
     * Checks whether a fire has been fully extinguished.
     *
     * @param fireLocationKey The location key of the fire to check
     * @return true if the fire exists and is extinguished
     */
    public boolean isExtinguished(String fireLocationKey) {
        EventInfo fire = this.firesBeingFought.get(fireLocationKey);
        if (fire != null && fire.isExtinguished()) {
            return true;
        }
        return false;
    }

    /**
     * Peeks at the highest-priority fire in the queue without removing it.
     * 
     * @return The highest-priority fire event, or null if the queue is empty
     */
    public EventInfo peekNextFire() {
        return this.fireQueue.peek();
    }

    /**
     * Removes the highest-priority fire from the queue and moves it
     * into the fires being fought map.
     * 
     * @return The highest-priority fire event, or null if the queue is empty
     */
    public void assignFire(int droneId, String locationKey) {
        if (this.fireQueue.isEmpty()) {
            return;
        }

        EventInfo fire = null;
        if (locationKey != null && !locationKey.isEmpty()) {
            for (EventInfo queuedFire : this.fireQueue) {
                if (locationKey.equals(queuedFire.getLocationKey())) {
                    fire = queuedFire;
                    break;
                }
            }
            if (fire != null) {
                this.fireQueue.remove(fire);
            }
        }

        if (fire == null) {
            return;
        }

        this.firesBeingFought.put(fire.getLocationKey(), fire);
        fire.assignDrone(droneId);
    }

    /**
     * Removes a fire from the fires being fought and places it back
     * into the priority queue for reassignment.
     * 
     * @param fireLocationKey The location key of the fire to requeue
     */
    public void requeueFire(String fireLocationKey) {
        EventInfo fire = this.firesBeingFought.remove(fireLocationKey);
        if (fire != null) {
            fire.assignDrone(null);
            this.fireQueue.add(fire);
        }
    }

    /**
     * Marks a fire as extinguished and moves it to the dead fires list.
     * 
     * @param fireLocationKey The location key of the fire to mark as dead
     */
    public void markFireAsDead(String fireLocationKey) {
        EventInfo fire = this.firesBeingFought.remove(fireLocationKey);
        if (fire != null) {
            this.deadFires.add(fire);
        }
    }

    /**
     * Gets the map of fires currently being fought, keyed by location.
     *
     * @return The map of fires being fought
     */
    public HashMap<String, EventInfo> getFiresBeingFought() {
        return firesBeingFought;
    }

    public int getPendingFireCount() {
        return fireQueue.size();
    }
}