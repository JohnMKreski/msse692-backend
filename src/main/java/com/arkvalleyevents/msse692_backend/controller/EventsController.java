package com.arkvalleyevents.msse692_backend.controller;

import com.arkvalleyevents.msse692_backend.dto.response.ApiErrorDto;
import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventAuditDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventPageResponse;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;
import com.arkvalleyevents.msse692_backend.service.EventService;
import com.arkvalleyevents.msse692_backend.security.policy.EventAccessPolicy;
import com.arkvalleyevents.msse692_backend.security.context.UserContext;
import com.arkvalleyevents.msse692_backend.security.context.UserContextProvider;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
 
import org.springframework.data.domain.Page;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.ValidationException;

import java.net.URI;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
 

@RestController
@RequestMapping("/api/v1/events") // API versioned base path (added v1)
@Tag(name = "Events", description = "Event management and public feeds")
//Triggers validation of parameters that have constraint annotations (e.g., @NotNull, @Size) and supports validation groups.
@Validated
public class EventsController {
    private static final Logger log = LoggerFactory.getLogger(EventsController.class);

    //Service
    private final EventService eventService;
    private final EventAuditService eventAuditService;
    private final EventAccessPolicy eventAccessPolicy;
    private final UserContextProvider userContextProvider;

    public EventsController(EventService eventService, EventAuditService eventAuditService, EventAccessPolicy eventAccessPolicy, UserContextProvider userContextProvider) {
        this.eventService = eventService;
        this.eventAuditService = eventAuditService;
        this.eventAccessPolicy = eventAccessPolicy;
        this.userContextProvider = userContextProvider;
        log.info("EventsController initialized");
    }

    // private HttpServletRequest request; // unused

    //Post /api/events
    @PostMapping // POST /api/v1/events
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "Create event", description = "Creates a new event (status=DRAFT) and returns its details.")
    @ApiResponses({
        @ApiResponse(responseCode = "201", description = "Created",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
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
    @Operation(summary = "Get event by id", description = "Returns event details; visibility depends on role (ADMIN all, EDITOR own, public PUBLISHED only).")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<EventDetailDto> getEvent(@PathVariable("id") Long eventId) {
        log.info("GET /api/events/{}", eventId);
        EventDetailDto dto = eventService.getEventDetailOrThrow(eventId);
        // Delegate visibility policy using UserContextProvider (only here for now)
        UserContext uc = userContextProvider.current();
        eventAccessPolicy.assertCanView(dto, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
        return ResponseEntity.ok(dto);
    }

    //Put /api/events/{id}
    @PutMapping("/{id}") // PUT /api/v1/events/{id}
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "Update event", description = "Partially updates non-null fields of an event and returns updated details.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<EventDetailDto> updateEvent(@PathVariable("id") Long eventId, @RequestBody @Valid UpdateEventDto dto) {
        log.info("PUT /api/events/{}", eventId);
        UserContext uc = userContextProvider.current();
        EventDetailDto existing = eventService.getEventDetailOrThrow(eventId);
        eventAccessPolicy.assertCanModify(existing, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
        EventDetailDto updated = eventService.updateEvent(eventId, dto);
        return ResponseEntity.ok(updated);
    }

    //Delete /api/events/{id}
    @DeleteMapping("/{id}") // DELETE /api/v1/events/{id}
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Delete event", description = "Deletes an event by id (ADMIN only). Returns 204 on success.")
    @ApiResponses({
        @ApiResponse(responseCode = "204", description = "No Content"),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<Void> deleteEvent(@PathVariable("id") Long eventId) {
        log.info("DELETE /api/events/{}", eventId);
        eventService.deleteEvent(eventId);
        return ResponseEntity.noContent().build(); // 204 No Content
    }

    // ===== Status transitions =====
    @PostMapping("/{id}/publish") // POST /api/v1/events/{id}/publish
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "Publish event", description = "Transitions an event from DRAFT to PUBLISHED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Illegal state",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<EventDetailDto> publishEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/publish", eventId);
        UserContext uc = userContextProvider.current();
        EventDetailDto existing = eventService.getEventDetailOrThrow(eventId);
        eventAccessPolicy.assertCanModify(existing, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
        EventDetailDto updated = eventService.publishEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/unpublish") // POST /api/v1/events/{id}/unpublish
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "Unpublish event", description = "Transitions an event from PUBLISHED to UNPUBLISHED.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Illegal state",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<EventDetailDto> unpublishEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/unpublish", eventId);
        UserContext uc = userContextProvider.current();
        EventDetailDto existing = eventService.getEventDetailOrThrow(eventId);
        eventAccessPolicy.assertCanModify(existing, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
        EventDetailDto updated = eventService.unpublishEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @PostMapping("/{id}/cancel") // POST /api/v1/events/{id}/cancel
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "Cancel event", description = "Cancels an event. Returns updated details; 409 if already cancelled.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(schema = @Schema(implementation = EventDetailDto.class))),
        @ApiResponse(responseCode = "404", description = "Not Found",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class))),
        @ApiResponse(responseCode = "409", description = "Illegal state",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public ResponseEntity<EventDetailDto> cancelEvent(@PathVariable("id") Long eventId) {
        log.info("POST /api/events/{}/cancel", eventId);
        UserContext uc = userContextProvider.current();
        EventDetailDto existing = eventService.getEventDetailOrThrow(eventId);
        eventAccessPolicy.assertCanModify(existing, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
        EventDetailDto updated = eventService.cancelEvent(eventId);
        return ResponseEntity.ok(updated);
    }

    @Operation(summary = "List events", description = "Supports paging, sorting, and arbitrary query-string filters.")
    @GetMapping // GET /api/v1/events
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EventPageResponse.class))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public EventPageResponse listEvents(
            @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "sort", required = false) String sort,
            @Parameter(hidden = true) @RequestParam Map<String, String> requestParams
    ) {
        Map<String, String> filters = new HashMap<>(requestParams);
        filters.remove("page");
        filters.remove("size");
        filters.remove("sort");
        String safeSort = normalizeSort(sort);
        UserContext uc = userContextProvider.current();
        Page<EventDto> pageResult = eventService.listEventsPageScoped(filters, Math.max(page, 0), Math.max(size, 1), safeSort, uc);
        return EventPageResponse.from(pageResult);
    }

    private static final java.util.Set<String> ALLOWED_SORT_FIELDS = java.util.Set.of("startAt", "eventName");

    private String normalizeSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return "startAt,asc"; // default
        }
        String s = sort.trim();
        String field = s;
        if (s.startsWith("-")) {
            field = s.substring(1);
        } else if (s.endsWith(",desc")) {
            field = s.substring(0, s.length() - 5);
        } else if (s.endsWith(",asc")) {
            field = s.substring(0, s.length() - 4);
        }
        if (!ALLOWED_SORT_FIELDS.contains(field)) {
            throw new IllegalArgumentException("Unsupported sort field: " + field);
        }
        return s;
    }

    // Public upcoming feed (only PUBLISHED future events)
    @GetMapping("/public-upcoming") // GET /api/v1/events/public-upcoming?from=ISO&limit=10
    @Operation(summary = "List upcoming public events", description = "Returns future PUBLISHED events starting at 'from' (Instant), limited by 'limit'.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EventDto.class)))),
        @ApiResponse(responseCode = "400", description = "Bad Request",
            content = @Content(schema = @Schema(implementation = ApiErrorDto.class)))
    })
    public List<EventDto> listPublicUpcoming(
            @RequestParam(name = "from", required = false) Instant from,
            @RequestParam(name = "limit", required = false, defaultValue = "10") @Min(1) @Max(100) int limit) {
        // Accept full ISO-8601 instants (e.g., 2025-11-12T21:16:46.100Z). Spring will bind to Instant.
        // Convert to UTC LocalDateTime to match service contract.
        if (limit < 1 || limit > 100) {
            throw new ValidationException("Parameter 'limit' must be between 1 and 100");
        }
        Instant effectiveFrom = (from == null) ? Instant.now() : from;
        LocalDateTime start = LocalDateTime.ofInstant(effectiveFrom, ZoneOffset.UTC);
        return eventService.listPublicUpcoming(start, limit);
    }

    // GET /api/events/{id}/audits  (read-only audit trail)
    @GetMapping("/{id}/audits") // GET /api/v1/events/{id}/audits
    @Operation(summary = "List event audits", description = "Returns recent audit records for an event; consider restricting to ADMIN/owner in production.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                array = @ArraySchema(schema = @Schema(implementation = EventAuditDto.class))))
    })
    public ResponseEntity<java.util.List<EventAuditDto>> getEventAudits(@PathVariable("id") Long eventId,
                                                                        @RequestParam(name = "limit", required = false, defaultValue = "10") int limit) {
        // Protect audits to ADMIN or owner
        UserContext uc = userContextProvider.current();
        EventDetailDto existing = eventService.getEventDetailOrThrow(eventId);
        eventAccessPolicy.assertCanModify(existing, java.util.Optional.ofNullable(uc.userId()), uc.admin(), uc.editor());
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

    /**
     * List events created by the authenticated user ("My Events").
        * Uses strict ownership listing (no role-based expansion) so only the caller's events are returned.
        * Authorization decision: restricted to ADMIN/EDITOR because only these roles can create events.
        * If regular USERS later gain create capability, change to isAuthenticated() and add status visibility policy.
     */
    @GetMapping("/mine") // GET /api/v1/events/mine
    @PreAuthorize("hasAnyRole('ADMIN','EDITOR')")
    @Operation(summary = "List my events", description = "Returns a paged list of events created by the current authenticated user. Supports paging & sorting.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "OK",
            content = @Content(mediaType = "application/json",
                schema = @Schema(implementation = EventPageResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public EventPageResponse listMyEvents(
            @RequestParam(name = "page", required = false, defaultValue = "0") @Min(0) int page,
            @RequestParam(name = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
            @RequestParam(name = "sort", required = false) String sort,
            @RequestParam(name = "eventType", required = false) String eventType,
            @RequestParam(name = "status", required = false) String status,
            @RequestParam(name = "from", required = false) String from,
            @RequestParam(name = "to", required = false) String to
    ) {
        String safeSort = normalizeSort(sort);
        UserContext uc = userContextProvider.current();
        Long uid = uc.userId();
        if (uid == null) {
            // Treated as unauthorized – could also throw a dedicated exception mapped to 401/403
            throw new org.springframework.security.access.AccessDeniedException("User context missing");
        }
        // Build optional filters (parity with general list; graceful ignore invalid values handled in spec builder)
        java.util.Map<String, String> filters = new java.util.HashMap<>();
        if (eventType != null && !eventType.isBlank()) filters.put("eventType", eventType.trim());
        if (status != null && !status.isBlank()) filters.put("status", status.trim());
        if (from != null && !from.isBlank()) filters.put("from", from.trim());
        if (to != null && !to.isBlank()) filters.put("to", to.trim());
        Page<EventDto> pageResult = eventService.listEventsByOwnerFiltered(uid, filters, Math.max(page, 0), Math.max(size, 1), safeSort);
        return EventPageResponse.from(pageResult);
    }

    // Removed legacy role helpers in favor of UserContextProvider
}
