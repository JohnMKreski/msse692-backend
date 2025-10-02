package com.arkvalleyevents.msse692_backend.controller;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@org.springframework.web.bind.annotation.RestController
@RequestMapping("/api")
public class EventsController {
    private static final Logger logger = LoggerFactory.getLogger(EventsController.class);

    //Service

//    @Autowired
//    //Service service;
//    public RestController(@Qualifier("exHibernate") Service service) {
//        logger.info("RestController initialized");
//    }

    private HttpServletRequest request;

    //Post /api/events
    @RequestMapping("/events") postEvents(
            @RequestParam String eventType,
            @RequestParam String eventName,
            @RequestParam String eventDate,
            @RequestParam String eventTime,
            @RequestParam String eventLocation,
            @RequestParam String eventDescription
    ) {
        logger.info("POST /api/events called");
        // Implement logic to create a new event
        // service.serviceMethod(name, date, time, location, description);
        // DTOs and response handling would go here \/
        //EventResposne response = new EventResponse(type, name, date, time, location, description);
        //return ResponseEntity.ok(resposne);
    }

}
