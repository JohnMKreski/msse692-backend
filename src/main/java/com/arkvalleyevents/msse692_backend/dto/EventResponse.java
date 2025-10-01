package com.arkvalleyevents.msse692_backend.dto;

import lombok.Data;

@Data
public class EventResponse {
    private final String eventType;
    private final String eventName;
    private final String eventDate;
    private final String eventTime;
    private final String eventLocation;
    private final String eventDescription;

    public EventResponse(String eventType, String eventName, String eventDate, String eventTime, String eventLocation, String eventDescription) {
        this.eventType = eventType;
        this.eventName = eventName;
        this.eventDate = eventDate;
        this.eventTime = eventTime;
        this.eventLocation = eventLocation;
        this.eventDescription = eventDescription;
    }

    public String getEventType() {
        return eventType;
    }
    public String getEventName() {
        return eventName;
    }
    public String getEventDate() {
        return eventDate;
    }
    public String getEventTime() {
        return eventTime;
    }
    public String getEventLocation() {
        return eventLocation;
    }
    public String getEventDescription() {
        return eventDescription;
    }
}
