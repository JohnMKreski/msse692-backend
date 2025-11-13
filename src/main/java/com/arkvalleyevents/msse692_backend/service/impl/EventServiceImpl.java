package com.arkvalleyevents.msse692_backend.service.impl;

import com.arkvalleyevents.msse692_backend.dto.request.CreateEventDto;
import com.arkvalleyevents.msse692_backend.dto.request.UpdateEventDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDetailDto;
import com.arkvalleyevents.msse692_backend.dto.response.EventDto;
import com.arkvalleyevents.msse692_backend.model.EventType;
import com.arkvalleyevents.msse692_backend.service.EventService;
import com.arkvalleyevents.msse692_backend.service.EventAuditService;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.repository.EventRepository;

import com.arkvalleyevents.msse692_backend.service.mapping.EventMapper;

@Service
@Transactional
public class EventServiceImpl implements EventService {

    private static final Logger log = LoggerFactory.getLogger(EventServiceImpl.class);

    private final EventRepository eventRepository;
    private final EventMapper mapper;
    private final EventAuditService auditService;

    public EventServiceImpl(EventRepository eventRepository, @Qualifier("eventMapperImpl") EventMapper mapper, EventAuditService auditService) {
        this.eventRepository = eventRepository;
        this.mapper = mapper;
        this.auditService = auditService;
    }

    //=========================
    // Commands (state changes)
    //=========================

    // Create
    @Override
    public EventDetailDto createEvent(CreateEventDto input) {
        log.info("Creating new event: {}", input.getEventName());
        Event entity = mapper.toEntity(input);
        // status already defaults to DRAFT in the entity
        // entity.setStatus(EventStatus.DRAFT);\

        // Generate slug from eventName (Entity(Modal))
        String baseSlug = slugify(input.getEventName());
        String uniqueSlug = ensureUniqueSlug(baseSlug);
        entity.setSlug(uniqueSlug);

        Event saved = eventRepository.save(entity);
        auditService.logCreate(saved.getEventId());
        log.info("Event created successfully with ID={} and status={}", saved.getEventId(), saved.getStatus());
        return mapper.toDetailDto(saved); //toDetailDto defined in the mapper to return EventDetailDto and take (Event entity)
    }

    // Update
    @Override
    public EventDetailDto updateEvent(Long eventId, UpdateEventDto request) {
    log.debug("Attempting to update event ID={}", eventId);
    Event existing = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        // Update the event entity with non-null fields from the request DTO
        mapper.updateEntity(existing, request); // partial update (non‑nulls)

        Event saved = eventRepository.save(existing);
        auditService.logUpdate(eventId);
        log.info("Event ID={} updated successfully (status={}).", eventId, existing.getStatus());
        return mapper.toDetailDto(saved);
    }

    // Status changes
    @Override
    public EventDetailDto publishEvent(Long eventId) {
        log.debug("Attempting to publish event ID={}", eventId);

        Event event = load(eventId);

        if (event.getStatus() != EventStatus.DRAFT) {
            log.warn("Cannot publish event ID={} because current status is {}", eventId, event.getStatus());
            throw new IllegalStateException("Only DRAFT events can be published");
        }

        event.setStatus(EventStatus.PUBLISHED);
        Event saved = eventRepository.save(event);
        auditService.logUpdate(eventId);

        log.info("Event ID={} successfully published. Previous status=DRAFT → new status={}", eventId, saved.getStatus());
        return mapper.toDetailDto(saved);
    }

    @Override
    public EventDetailDto unpublishEvent(Long eventId) {
        log.debug("Attempting to unpublish event ID={}", eventId);

        Event event = load(eventId);

        if (event.getStatus() != EventStatus.PUBLISHED) {
            log.warn("Cannot unpublish event ID={} because current status is {}", eventId, event.getStatus());
            throw new IllegalStateException("Only PUBLISHED events can be unpublished");
        }

        event.setStatus(EventStatus.UNPUBLISHED);
        Event saved = eventRepository.save(event);
        auditService.logUpdate(eventId);

        log.info("Event ID={} successfully unpublished. Previous status=PUBLISHED → new status={}", eventId, saved.getStatus());
        return mapper.toDetailDto(saved);
    }

    @Override
    public EventDetailDto cancelEvent(Long eventId) {
        log.debug("Attempting to cancel event ID={}", eventId);
        Event event = load(eventId);

        if (event.getStatus() == EventStatus.CANCELLED) {
            log.warn("Event ID={} is already cancelled.", eventId);
            throw new IllegalStateException("Event is already cancelled");
        }

        event.setStatus(EventStatus.CANCELLED);
        Event saved = eventRepository.save(event);
        auditService.logUpdate(eventId);

        log.info("Event ID={} successfully cancelled. Previous status={} → new status={}", eventId, event.getStatus(), saved.getStatus());
        return mapper.toDetailDto(saved);
    }

    // Delete
    @Override
    public void deleteEvent(Long eventId) {
        log.warn("Deleting event with ID={}", eventId);
        if (!eventRepository.existsById(eventId)) {
            throw new EntityNotFoundException("Event not found: " + eventId);
        }

        auditService.logDelete(eventId);
        eventRepository.deleteById(eventId);
        log.info("Event ID={} deleted.", eventId);
    }

    // =========================
    // Queries (no state change)
    // =========================

    @Override
    @Transactional(readOnly = true)
    public Optional<EventDetailDto> getEventById(Long eventId) {
        log.debug("Fetching event by ID={}", eventId);
        Event event = eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
        log.info("Event retrieved successfully (ID={}, status={})", event.getEventId(), event.getStatus());
//        return mapper.toDetailDto(event);
        return eventRepository.findById(eventId).map(mapper::toDetailDto);
    }

    @Override
    @Transactional(readOnly = true)
    public EventDetailDto getEventBySlug(String slug) {
        log.debug("Fetching event by slug='{}'", slug);
        Event event = eventRepository.findBySlug(slug)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + slug));
        log.info("Event retrieved successfully (slug='{}', ID={}, status={})", slug, event.getEventId(), event.getStatus());
        return mapper.toDetailDto(event);
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listEvents(Map<String, String> filters, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        log.debug("Listing events with filters={}, page={}, size={}, sort='{}'", filters, page, size, sort);

        Specification<Event> spec = buildSpecification(filters);
        Page<Event> pageResult = (spec == null)
                ? eventRepository.findAll(pageable)
                : eventRepository.findAll(spec, pageable);

        List<EventDto> events = pageResult.stream().map(mapper::toDto).toList();
        log.info("Listed {} events (page={}, size={})", events.size(), page, size);
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public Page<EventDto> listEventsPage(Map<String, String> filters, int page, int size, String sort) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(size, 1), parseSort(sort));
        log.debug("Listing events (paged) with filters={}, page={}, size={}, sort='{}'", filters, page, size, sort);

        Specification<Event> spec = buildSpecification(filters);
        Page<Event> pageResult = (spec == null)
                ? eventRepository.findAll(pageable)
                : eventRepository.findAll(spec, pageable);

        Page<EventDto> dtoPage = pageResult.map(mapper::toDto);
        log.info("Listed {} events (paged) of total {} (page={}, size={})", dtoPage.getNumberOfElements(), dtoPage.getTotalElements(), page, size);
        return dtoPage;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listUpcoming(LocalDateTime from, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.ASC, "startAt"));
        log.debug("Listing upcoming events starting after {} (limit={})", from, limit);

        Page<Event> events = eventRepository.findByStartAtAfter(from, pageable);
        List<EventDto> dtos = events.getContent().stream().map(mapper::toDto).toList();

        log.info("Retrieved {} upcoming events (after={})", dtos.size(), from);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> listPublicUpcoming(LocalDateTime from, int limit) {
        Pageable pageable = PageRequest.of(0, Math.max(limit, 1), Sort.by(Sort.Direction.ASC, "startAt"));
        log.debug("Listing PUBLIC upcoming events from {} (limit={})", from, limit);
        Page<Event> events = eventRepository.findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(EventStatus.PUBLISHED, from, pageable);
        List<EventDto> dtos = events.getContent().stream().map(mapper::toDto).toList();
        log.info("Retrieved {} PUBLIC upcoming events (from={})", dtos.size(), from);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getAllEvents() {
        log.debug("Fetching all events (no filters or paging)");
        List<EventDto> events = eventRepository.findAll().stream().map(mapper::toDto).toList();
        log.info("Retrieved {} total events", events.size());
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByType(EventType eventType) {
        log.debug("Fetching events by type='{}'", eventType);
        List<Event> events = eventRepository.findByEventType(eventType);
        List<EventDto> dtos = events.stream().map(mapper::toDto).toList();
        log.info("Retrieved {} events of type='{}'", dtos.size(), eventType);
        return dtos;
    }

//    @Override
//    @Transactional(readOnly = true)
//    public List<EventDto> getEventsByDate(String eventDate) {
//        LocalDate date = LocalDate.parse(eventDate); // expects ISO-8601 yyyy-MM-dd
//        LocalDateTime start = date.atStartOfDay();
//        LocalDateTime end = date.plusDays(1).atStartOfDay().minusNanos(1);
//
//        log.debug("Fetching events on date={}, between {} and {}", eventDate, start, end);
//        List<Event> events = eventRepository.findByStartAtBetween(start, end);
//        List<EventDto> dtos = events.stream().map(mapper::toDto).toList();
//
//        log.info("Retrieved {} events scheduled for {}", dtos.size(), eventDate);
//        return dtos;
//    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByDate(LocalDate date) {
        LocalDateTime startAt  = date.atStartOfDay();
        LocalDateTime endAt  = date.plusDays(1).atStartOfDay().minusNanos(1);

        log.debug("Fetching events occurring on {} ({} to {})", date, startAt , endAt );

        List<Event> events = eventRepository.findByStartAtBetween(startAt , endAt );
        List<EventDto> dtos = events.stream().map(mapper::toDto).toList();

        log.info("Retrieved {} events on {}", dtos.size(), date);
        return dtos;
    }

    @Override
    @Transactional(readOnly = true)
    public List<EventDto> getEventsByLocation(String location) {
        log.debug("Fetching events by location containing '{}'", location);
        List<Event> events = eventRepository.findByEventLocationContainingIgnoreCase(location);
        List<EventDto> dtos = events.stream().map(mapper::toDto).toList();

        log.info("Retrieved {} events matching location='{}'", dtos.size(), location);
        return dtos;
    }

//    public Page<Event> getUpcomingEventsByStatus(EventStatus status, LocalDateTime from, Pageable pageable) {
//        return eventRepository.findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(status, from, pageable);
//    }
//
//    public boolean eventSlugExists(String slug) {
//        return eventRepository.existsBySlug(slug);
//    }

    //=========================
    // Additional filtered queries
    //=========================

    //TODO: add more filtered queries as needed
    // Helpers
    private Event load(Long eventId) {
        return eventRepository.findById(eventId)
                .orElseThrow(() -> new EntityNotFoundException("Event not found: " + eventId));
    }

    private Sort parseSort(String sort) {
        if (sort == null || sort.isBlank()) {
            return Sort.unsorted();
        }
        String s = sort.trim();
        if (s.startsWith("-")) {
            return Sort.by(Sort.Direction.DESC, s.substring(1));
        }
        if (s.endsWith(",desc")) {
            return Sort.by(Sort.Direction.DESC, s.substring(0, s.length() - 5));
        }
        if (s.endsWith(",asc")) {
            return Sort.by(Sort.Direction.ASC, s.substring(0, s.length() - 4));
        }
        return Sort.by(Sort.Direction.ASC, s);
    }

    private Specification<Event> buildSpecification(Map<String, String> filters) {
        if (filters == null || filters.isEmpty()) {
            return null; // no constraints; let repository use simple findAll(pageable)
        }

        // Parse common filters up front
        String statusStr = filters.get("status");
        String ownerStr = filters.get("createdByUserId");
        boolean ownerOrPublished = Boolean.parseBoolean(filters.getOrDefault("ownerOrPublished", "false"));
        String fromStr = filters.get("from");
        String toStr = filters.get("to");

        // Build specification
        Specification<Event> spec = (root, query, cb) -> cb.conjunction();

        // Visibility: either explicit status/owner AND-ed, or special ownerOrPublished OR logic
        if (ownerOrPublished && ownerStr != null && !ownerStr.isBlank() && (statusStr == null || statusStr.isBlank())) {
            try {
                Long ownerId = Long.parseLong(ownerStr.trim());
                Specification<Event> ownerPredicate = (r, q, cbx) -> cbx.equal(r.get("createdByUserId"), ownerId);
                Specification<Event> publishedPredicate = (r, q, cbx) -> cbx.equal(r.get("status"), EventStatus.PUBLISHED);
                spec = spec.and(ownerPredicate.or(publishedPredicate));
            } catch (NumberFormatException ex) {
                log.debug("Ignoring invalid createdByUserId for ownerOrPublished: {}", ownerStr);
            }
        } else {
            // status filter (any caller can set this; controllers decide policy)
            if (statusStr != null && !statusStr.isBlank()) {
                try {
                    EventStatus status = EventStatus.fromString(statusStr);
                    if (status != null) {
                        spec = spec.and((root, query, cbx) -> cbx.equal(root.get("status"), status));
                    }
                } catch (IllegalArgumentException ignored) {
                    log.debug("Ignoring invalid status filter: {}", statusStr);
                }
            }
            // createdBy filter (for EDITOR scope)
            if (ownerStr != null && !ownerStr.isBlank()) {
                try {
                    Long ownerId = Long.parseLong(ownerStr.trim());
                    spec = spec.and((root, query, cbx) -> cbx.equal(root.get("createdByUserId"), ownerId));
                } catch (NumberFormatException ex) {
                    log.debug("Ignoring invalid createdByUserId filter: {}", ownerStr);
                }
            }
        }

        // Optional date range filters (ISO-8601 LocalDateTime)
        if (fromStr != null && !fromStr.isBlank()) {
            try {
                LocalDateTime from = LocalDateTime.parse(fromStr.trim());
                spec = spec.and((root, query, cbx) -> cbx.greaterThanOrEqualTo(root.get("startAt"), from));
            } catch (Exception ex) {
                log.debug("Ignoring invalid 'from' filter: {}", fromStr);
            }
        }
        if (toStr != null && !toStr.isBlank()) {
            try {
                LocalDateTime to = LocalDateTime.parse(toStr.trim());
                spec = spec.and((root, query, cbx) -> cbx.lessThanOrEqualTo(root.get("startAt"), to));
            } catch (Exception ex) {
                log.debug("Ignoring invalid 'to' filter: {}", toStr);
            }
        }

        return spec;
    }

    // =========================
    // Slug generation helpers
    // =========================

    private String slugify(String title) {
        if (title == null || title.isBlank()) return null;
        return title
                .trim()
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "") // remove special chars
                .replaceAll("\\s+", "-");        // replace spaces with hyphens
    }

    private String ensureUniqueSlug(String baseSlug) {
        String slug = baseSlug;
        int counter = 1;
        while (eventRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }

}