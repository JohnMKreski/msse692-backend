package com.arkvalleyevents.msse692_backend.model;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HashSet;
import java.util.Set;

@Entity
//Data annotation generates getters, setters, toString, equals, and hashCode methods
@Data
@NoArgsConstructor
public class Event {
    @Override
    public String toString() {
        return String.format(
                "Event[eventType='%s', eventName='%s', eventDate='%s', eventTime='%s', eventLocation='%s', eventDescription='%s']",
                eventType, eventName, eventDate, eventTime, eventLocation, eventDescription);
    }

    //========== Fields ==========
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long eventId;

    private String eventType;

    @NotBlank
    private String eventName;

    @NotNull
    private LocalDate eventDate;

    @NotNull
    private LocalTime eventTime;

    @NotNull
    private LocalDateTime eventDateTime; //Combine date and time?

    private String eventLocation; //Needed with Venue entity? Venue has venue.getAddress() Will keep for events that's dont have a "venue" (e.g., woodsy)

    private String eventDescription;

    @Enumerated(EnumType.STRING)  // Store enum as readable text in DB
    @Column(nullable = false)
    private EventStatus status = EventStatus.DRAFT;  // default state



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



    //Defined as bean
//    //========== Constructors ==========
//    public Event() {
//        logger.info("Event default constructor called");
//    }

    //========== Getters and Setters ==========
    // Lombok @Data generates these automatically

//    public String getEventType() {
//        return eventType;
//    }
//    public void setEventType(String eventType) {
//        this.eventType = eventType;
//    }
//    public String getEventName() { return eventName; }
//    public void setEventName(String eventName) { this.eventName = eventName; }
//    public String getEventDate() { return eventDate; }
//    public void setEventDate(String eventDate) { this.eventDate = eventDate; }
//    public String getEventTime() { return eventTime; }
//    public void setEventTime(String eventTime) { this.eventTime = eventTime; }
//    public String getEventLocation() { return eventLocation; }
//    public void setEventLocation(String eventLocation) { this.eventLocation = eventLocation; }
//    public String getEventDescription() { return eventDescription; }
//    public void setEventDescription(String eventDescription) { this.eventDescription = eventDescription; }


    //========== Other Methods ==========


}
