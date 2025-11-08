package com.arkvalleyevents.msse692_backend.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
//Data annotation generates getters, setters, toString, equals, and hashCode methods
@Data
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Event {

    //========== Fields ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    @Column(unique = true, nullable = false, length = 255)
    private String slug; // URL-friendly unique identifier, e.g., "summer-music-fest-2023"

    @NotBlank
    @Column(length = 255)
    private String eventName;

    @Enumerated(EnumType.STRING)  // Store enum as readable text in DB
    private EventType eventType;

    @NotNull
    private LocalDateTime startAt;

    @NotNull
    private LocalDateTime endAt;

    private String eventLocation; //Needed with Venue entity? Venue has venue.getAddress() Will keep for events that's dont have a "venue" (e.g., woodsy)

    //@Lob is an annotation used to specify that a field should be persisted as a Large Object. It is typically used for fields that may store large amounts of data.
    @Lob
    private String eventDescription;

    @Enumerated(EnumType.STRING)  // Store enum as readable text in DB
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;  // default state

    // Auditing & concurrency
    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    // Ownership principal IDs (FK columns created_by, last_modified_by)
    // Populated via AuditorAware<Long>
    @CreatedBy
    @Column(name = "created_by")
    private Long createdByUserId;

    @LastModifiedBy
    @Column(name = "last_modified_by")
    private Long lastModifiedByUserId;



    //========== Relationships ==========
    //Found on google, could be good reference

    // Many-to-One relationship with Venue
    /**
     * In JPA/Hibernate, `fetch` is an attribute used in relationship annotations (like `@ManyToOne`, `@OneToMany`, etc.) to specify how related entities are loaded from the database.
     *
     * - `FetchType.LAZY`: Related entities are loaded only when accessed (on-demand). This improves performance by avoiding unnecessary data loading.
     * - `FetchType.EAGER`: Related entities are loaded immediately with the parent entity (at query time).
     *
     * Example:
     * ```java
     * @ManyToOne(fetch = FetchType.LAZY) // Venue is loaded only when accessed
     * private Venue venue;
     * ```
     *
     * Use `LAZY` for large or optional relationships, and `EAGER` when you always need the related data.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    //JoinColumn specifies the foreign key column in the Event table that references the primary key of the Venue table
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @ManyToMany
    @JoinTable(
            name = "event_artist",
            joinColumns = @JoinColumn(name = "event_id"),
            inverseJoinColumns = @JoinColumn(name = "artist_id")
    )
    private Set<Artist> artists = new HashSet<>();

    // ElementCollection is used to define a collection of basic types or embeddable objects
    // CollectionTable specifies the table that holds the collection
    // JoinColumn specifies the foreign key column in the collection table that references the primary key of the Event table
    // Column specifies the column in the collection table that holds the elements of the collection
    // This setup allows an Event to have multiple associated image URLs stored in a separate table
    // This is useful for storing multiple images related to an event without creating a separate entity
    // Each image URL is stored as a string in the event_image_urls table, linked to the Event by event_id
    @ElementCollection
    @CollectionTable(name = "event_image_urls", joinColumns = @JoinColumn(name = "event_id"))
    @Column(name = "image_url")
    private Set<String> imageUrls = new HashSet<>();

}
