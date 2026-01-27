package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

/**
 * LiveFireTracker.java - Object to track live and dead fires between threads in
 * real-time.
 */
public class LiveFireTracker {
    private Queue<EventInfo> fireQueue = new LinkedList<>();
    private HashMap<String, EventInfo> firesBeingFought = new HashMap<>();
    private ArrayList<EventInfo> deadFires = new ArrayList<>();

    /**
     * Adds a new fire to the queue of fires awaiting assignment.
     * Notifies waiting threads that a new fire is available.
     * 
     * @param fire The fire event to add
     */
    public synchronized void put(EventInfo fire) {
        this.fireQueue.add(fire);
        notifyAll();
    }

    /**
     * Gets the next fire event that needs a drone assignment.
     * First checks existing fires for any without drones, then retrieves from queue.
     * 
     * @return The next fire event, or null if none available
     * @throws InterruptedException if interrupted while waiting
     */
    public synchronized EventInfo getNextEventInfo() throws InterruptedException {
        for (EventInfo fire : this.firesBeingFought.values()) {
            if (!fire.hasDroneAssigned() && !fire.isExtinguished()) {
                return fire;
            }
        }
        if (this.fireQueue.isEmpty()) {
            try {
                wait(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }
        }
        if (this.fireQueue.isEmpty()) {
            return null;
        }

        EventInfo fire = this.fireQueue.poll();
        this.firesBeingFought.put(fire.getLocationKey(), fire);
        return fire;
    }

    /**
     * Marks a fire as extinguished and moves it to the dead fires list.
     * 
     * @param fireLocationKey The location key of the fire to mark as dead
     */
    public synchronized void markFireAsDead(String fireLocationKey) {
        EventInfo fire = this.firesBeingFought.remove(fireLocationKey);
        if (fire != null) {
            this.deadFires.add(fire);
        }
    }

    /**
     * Gets the total count of active fires (queued + being fought).
     * 
     * @return The number of active fires
     */
    public synchronized int getActiveFireCount() {
        return this.firesBeingFought.size() + this.fireQueue.size();
    }

    /**
     * Updates the status of all fires being fought.
     * Moves any extinguished fires to the dead fires list.
     */
    public synchronized void updateLiveFires() {
        ArrayList<String> extinguishedFires = new ArrayList<>();
        for (EventInfo fire : this.firesBeingFought.values()) {
            if (fire.isExtinguished()) {
                extinguishedFires.add(fire.getLocationKey());
            }
        }
        for (String fireKey : extinguishedFires) {
            markFireAsDead(fireKey);
        }
    }
}