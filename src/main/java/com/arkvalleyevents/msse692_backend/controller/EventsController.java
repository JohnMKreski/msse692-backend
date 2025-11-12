package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventAuditDto;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.service.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/events") // API versioned base path (added v1)
//Triggers validation of parameters that have constraint annotations (e.g., @NotNull, @Size) and supports validation groups.
@Validated
public class EventsController {
    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    //Service
    private final EventService eventService;
    private final EventAuditService eventAuditService;

    public EventsController(EventService eventService, EventAuditService eventAuditService) {
        this.eventService = eventService;
        this.eventAuditService = eventAuditService;
        log.info("EventsController initialized");
    }

    // private HttpServletRequest request; // unused

    //Post /api/events
    @PostMapping // POST /api/v1/events
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<EventDetailDto> createEvent(@RequestBody @Valid CreateEventDto dto) {
        log.info("POST /api/events called");
        EventDetailDto created = eventService.createEvent(dto);

        // Build Location: /api/events/{id}]
        /*
        * ServletUriComponentsBuilder - helps to build URIs based on the current request.
        * What it does:
        *   It helps you dynamically build URLs based on the current HTTP request context — especially useful for setting the Location header in a 201 Created response.
        */
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.getEventId())
                .toUri();

        return ResponseEntity.created(location).body(created); // 201 + Location
    }

    //Get /api/events/{id}
    @GetMapping("/{id}") // GET /api/v1/events/{id}
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable("id") Long eventId) {
        log.info("GET /api/events/{}", eventId);
        Optional<EventDetailDto> found = eventService.getEventById(eventId);
        //Return =
        //If the Optional contains an event, wrap it in ResponseEntity.ok(...) → HTTP 200.
        //If it’s empty, return ResponseEntity.notFound().build() → HTTP 404.
        return found.map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    //Put /api/events/{id}
    @PutMapping("/{id}") // PUT /api/v1/events/{id}
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<EventDetailDto> updateEvent(@PathVariable("id") Long eventId, @RequestBody @Valid UpdateEventDto dto) {
        log.info("PUT /api/events/{}", eventId);
        EventDetailDto updated = eventService.updateEvent(eventId, dto);
        return ResponseEntity.ok(updated);
    }

    //Delete /api/events/{id}
    @DeleteMapping("/{id}") // DELETE /api/v1/events/{id}
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long eventId) {
        log.info("DELETE /api/events/{}", eventId);
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ===== Status transitions =====
    @PostMapping("/{id}/publish") // POST /api/v1/events/{id}/publish
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<EventDetailDto> publishEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/publish", eventId);
        EventDetailDto updated = eventService.publishEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/unpublish") // POST /api/v1/events/{id}/unpublish
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<EventDetailDto> unpublishEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/unpublish", eventId);
        EventDetailDto updated = eventService.unpublishEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel") // POST /api/v1/events/{id}/cancel
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    public ResponseEntity<EventDetailDto> cancelEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/cancel", eventId);
        EventDetailDto updated = eventService.cancelEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "List events", description = "Supports paging, sorting, and arbitrary query-string filters.")
    @GetMapping // GET /api/v1/events
    public List<EventDto> listEvents(
            @RequestParam(name = "page", required = false, defaultValue = "0") int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") int size,
            @RequestParam(name = "sort", required = false) String sort,
            @Parameter(hidden = true) @RequestParam Map<String, String> requestParams
    ) {
        Map<String, String> filters = new HashMap<>(requestParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");

        return eventService.listEvents(filters, Math.max(page, 0), Math.max(size, 1), sort);
    }

    // Public upcoming feed (only PUBLISHED future events)
    @GetMapping("/public-upcoming") // GET /api/v1/events/public-upcoming?from=ISO&limit=10
    public List<EventDto> listPublicUpcoming(@RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        LocalDateTime start = (from == null || from.isBlank()) ? LocalDateTime.now() : LocalDateTime.parse(from);
        int effectiveLimit = Math.min(Math.max(limit, 1), 100); // clamp 1..100
        if (effectiveLimit != limit) {
            log.debug("Clamped public-upcoming limit from {} to {}", limit, effectiveLimit);
        }
        return eventService.listPublicUpcoming(start, effectiveLimit);
    }

    // GET /api/events/{id}/audits  (read-only audit trail)
    @GetMapping("/{id}/audits") // GET /api/v1/events/{id}/audits
    public ResponseEntity<java.util.List<EventAuditDto>> getEventAudits(@PathVariable("id") Long eventId,
                                                                        @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        var audits = eventAuditService.getRecentForEvent(eventId, limit)
                .stream()
                .map(a -> {
                    EventAuditDto dto = new EventAuditDto();
                    dto.setId(a.getId());
                    dto.setEventId(a.getEventId());
                    dto.setActorUserId(a.getActorUserId());
                    dto.setAction(a.getAction());
                    dto.setAt(a.getAt());
                    return dto;
                })
                .toList();
        return ResponseEntity.ok(audits);
    }

}
