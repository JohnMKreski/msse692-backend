package com.arkvalleyevents.msse692_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashSet;
import java.util.Set;

@Entity
//Data annotation generates getters, setters, toString, equals, and hashCode methods
@Data
@NoArgsConstructor
public class Event {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private static final Logger logger = LoggerFactory.getLogger(Event.class);

    @Override
    public String toString() {
        return String.format(
                "Event[eventType='%s', eventName='%s', eventDate='%s', eventTime='%s', eventLocation='%s', eventDescription='%s']",
                eventType, eventName, eventDate, eventTime, eventLocation, eventDescription);
    }

    //========== Fields ==========
    private Long id;
    private String eventType;
    private String eventName;
    private String eventDate;
    private String eventTime;
    private String eventLocation; //Needed with Venue entity?
    private String eventDescription;

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
    // type = a, name = b, date = c, time = d, location = e, description = f
    public static boolean hasNullOrInvalid(Object a, Object b, Object c, Object d, Object e, Object f) {
        try {
            if (a == null || b == null || c == null || d == null || e == null || f == null) return true;
            logger.error("Event has null field(s)");
            return false;
        } catch (Exception ex) {
            return true;
        }
    }

    // Validate all fields are non-null and valid
    public static String validate(String a, String b, String c, String d, String e, String f) {
        if (hasNullOrInvalid(a, b, c, d, e, f)) return "One or more fields are null or invalid";
        return null;
    }


}
