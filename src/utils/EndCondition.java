package utils;

public class EndCondition {
    private volatile boolean shouldStop = false;

    /**
     * Checks if the simulation should stop.
     * 
     * @return true if stop has been requested, false otherwise
     */
    public synchronized boolean shouldStop() {
        return this.shouldStop;
    }

    /**
     * Sets the stop condition for the simulation.
     * 
     * @param stop true to request stopping, false to clear the stop request
     */
    public synchronized void setStop(boolean stop) {
        this.shouldStop = stop;
    }
}
