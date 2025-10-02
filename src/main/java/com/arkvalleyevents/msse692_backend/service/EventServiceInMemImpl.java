package com.arkvalleyevents.msse692_backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

@Service
@Qualifier("inMemory")
public class EventServiceInMemImpl implements EventService {
    private static final Logger logger = LoggerFactory.getLogger(EventServiceInMemImpl.class);

    private String eventType;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String eventLocation;
    private String eventDescription;


}
