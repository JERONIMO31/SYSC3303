package utils;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

/**
 * LiveFireTracker.java - Object to track live and dead fires between threads in real-time.
 */
public class LiveFireTracker
{
    private Queue<FireInfo> fireQueue = new LinkedList<>();
    private HashMap<String, FireInfo> firesBeingFought = new HashMap<>();
    private ArrayList<FireInfo> deadFires = new ArrayList<>();

    public synchronized void put(FireInfo fire) {
        this.fireQueue.add(fire);
        notifyAll();
    }
        
    public synchronized FireInfo getNextFireInfo(){
        for (FireInfo fire : this.firesBeingFought.values()) {
            if (!fire.hasDroneAssigned() && !fire.isExtinguished()) {
                return fire;
            }
        }
        if (this.fireQueue.isEmpty()) {
            return null;
        }
        FireInfo fire = this.fireQueue.poll();
        this.firesBeingFought.put(fire.getLocationKey(), fire);
        return fire;
    }

    public synchronized void markFireAsDead(String fireLocationKey) {
        FireInfo fire = this.firesBeingFought.remove(fireLocationKey);
        if (fire != null) {
            this.deadFires.add(fire);
        }
    }

    public synchronized int getActiveFireCount() {
        return this.firesBeingFought.size() + this.fireQueue.size();
    }
}