package com.arkvalleyevents.msse692_backend.repository;

import com.arkvalleyevents.msse692_backend.model.Event;
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

    List<Event> findByStartAtAfter(LocalDateTime from, Pageable pageable);

    List<Event> findByTypeIgnoreCase(String type);

    List<Event> findByStartAtBetween(LocalDateTime start, LocalDateTime end);

    List<Event> findByLocationContainingIgnoreCase(String location);
}