package com.arkvalleyevents.msse692_backend.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.HashSet;
import java.util.Set;

@Entity
@Data
@NoArgsConstructor
public class Artist {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String genre;
    private String biography;
    private String website;

    //A many-to-many relationship in databases means that multiple records in one table can be associated with multiple records in another table
    //mappedBy indicates that the relationship is bidirectional and the ownership is on the other side (ConcertEvent)
    // mappedBy = "artists" means the ownership of the relationship is on the ConcertEvent side. The Artist entity is the inverse side,
    // and the ConcertEvent entity should have a @ManyToMany field named artists that owns the relationship.
    @ManyToMany(mappedBy = "artists")
    private Set<Event> events = new HashSet<>();
}

