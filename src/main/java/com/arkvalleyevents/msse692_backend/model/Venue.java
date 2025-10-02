package com.arkvalleyevents.msse692_backend.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.URL;

import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class Venue {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @Min(1)
    private Integer capacity;

    @Column(length = 2000)
    private String description;

    @URL
    private String website;

    // One-to-Many is used here because one venue can host multiple events
    // mappedBy indicates that the relationship is bidirectional and the ownership is on the other side
    // cascade = CascadeType.ALL ensures that any operation (like delete) on Venue will cascade to its events
    // orphanRemoval = true ensures that if an event is removed from the venue's event set, it will be deleted from the database
    @OneToMany(mappedBy = "venue", cascade = CascadeType.ALL, orphanRemoval = true)
    // This method defines a Set<Event> to represent the events associated with a venue using JPA/Hibernate
    private Set<Event> events = new HashSet<>();
}
