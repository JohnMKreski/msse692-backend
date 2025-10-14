package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.Event;
import com.arkvalleyevents.msse692_backend.model.EventStatus;
import com.arkvalleyevents.msse692_backend.model.EventType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

// It extends JpaRepository and JpaSpecificationExecutor so you can add Specifications later for filtered searches.
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, JpaSpecificationExecutor<Event> {

    Optional<Event> findBySlug(String slug);

    Page<Event> findByStartAtAfter(LocalDateTime from, Pageable pageable);

    List<Event> findByEventType(EventType eventType);

    List<Event> findByStartAtBetween(LocalDateTime startAt, LocalDateTime endAt);

    List<Event> findByEventLocationContainingIgnoreCase(String eventLocation);

    Page<Event> findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(
            EventStatus status, LocalDateTime from, Pageable pageable
    );

    boolean existsBySlug(String slug);
//
//    // Detail lookups
//    Optional<Event> findBySlug(String slug); // unique index recommended
//
//    // Paging list (with dynamic filters via Specification; see below)
//    Page<Event> findAll(Specification<Event> spec, Pageable pageable);
//
//    // “Upcoming” (usually only PUBLISHED)
//    Page<Event> findByStatusAndStartAtGreaterThanEqualOrderByStartAtAsc(
//            EventStatus status, LocalDateTime from, Pageable pageable);
//
//    // Convenience finders used by your service
//    List<Event> findByTypeIgnoreCase(String type);
//    List<Event> findByStartAtBetween(LocalDateTime start, LocalDateTime end);
//    List<Event> findByLocationContainingIgnoreCase(String location);
}