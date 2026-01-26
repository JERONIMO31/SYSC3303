package utils;

import java.time.LocalTime;

public class EventInfo {
    private final LocalTime time;
    private final Integer zoneID;
    private final Event_Type eventType;
    private final Intensity intensity;

    public EventInfo(LocalTime time, Integer zoneID, Event_Type eventType, Intensity intensity){
        this.time = time;
        this.zoneID = zoneID;
        this.eventType = eventType;
        this.intensity = intensity;
    }

    public LocalTime getTime(){return time;}

    public Integer getZoneID(){return zoneID;}

    public Event_Type getEventType(){return eventType;}

    public Intensity getIntensity(){return intensity;}

}
